package pt.test.task.predictor

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.mkConst
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class Predictor {
    fun predict(code: String): List<Int> {
        return traverseAndSolve(buildPathTree(parse(code)))
    }

    fun predict(path: Path): List<Int> {
        return traverseAndSolve(buildPathTree(parse(path)))
    }

    private fun parse(code: String) = StaticJavaParser.parse(code)

    private fun parse(path: Path) = StaticJavaParser.parse(path)

    private fun buildPathTree(cu: CompilationUnit): Node? {
        val stmts = cu.getClassByName("Main").get().getMethodsByName("method")[0].body.get().statements
        return buildScopePathTree(stmts, 1, stmts.size - 2)
    }

    private fun buildScopePathTree(stmts: NodeList<Statement>, left: Int, right: Int): Node? {
        var root: Node? = null
        for (i in right downTo left) {
            val stmt = stmts[i]
            if (stmt is IfStmt) {
                val curr = buildIfNode(stmt)
                updateLeafs(curr, root)
                root = curr
            } else if (stmt is ExpressionStmt) {
                val expr = stmt.expression
                if (expr is AssignExpr) {
                    root = AssignNode(expr.value.toString().toInt(), root)
                }
                break
            }
        }
        return root
    }

    private fun buildIfNode(stmt: IfStmt): Node {
        val thstmts = (stmt.thenStmt as? BlockStmt)?.statements ?: let { throw IllegalArgumentException() }
        val elstmts = try {
            (stmt.elseStmt.get() as? BlockStmt)?.statements ?: let { throw IllegalArgumentException() }
        } catch (_: NoSuchElementException) {
            null
        }

        val cond = stmt.condition
        if (cond is ArrayAccessExpr) {
            return IfNode(
                cond.index.toString().toInt(),
                buildScopePathTree(thstmts, 0, thstmts.size - 1),
                elstmts?.let { buildScopePathTree(it, 0, elstmts.size - 1) }
            )
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun updateLeafs(pt: Node, npt: Node?) {
        when (pt) {
            is AssignNode -> {
                pt.next?.let { updateLeafs(it, npt) } ?: let { pt.next = npt }
            }

            is IfNode -> {
                pt.th?.let { updateLeafs(it, npt) } ?: let { pt.th = npt }
                pt.el?.let { updateLeafs(it, npt) } ?: let { pt.el = npt }
            }
        }
    }

    private fun traverseAndSolve(pt: Node?): List<Int> {
        val results = mutableListOf<Int>()
        with(KContext()) {
            traverseAndSolve(mutableListOf(), mutableMapOf(), pt, null, results)
        }
        return results.distinct().sorted()
    }

    private fun KContext.traverseAndSolve(
        assertions: MutableList<KExpr<KBoolSort>>,
        conds: MutableMap<Int, KApp<KBoolSort, *>>,
        curr: Node?,
        xvalue: Int?,
        results: MutableList<Int>
    ) {
        if (curr == null) {
            KZ3Solver(this).use { solver ->
                for (it in assertions) {
                    solver.assert(it)
                }
                if (solver.check(timeout = 1.seconds) == KSolverStatus.SAT && (xvalue != null)) {
                    results.add(xvalue)
                }
            }
            return
        }
        when (curr) {
            is AssignNode -> {
                traverseAndSolve(assertions, conds, curr.next, curr.xvalue, results)
            }

            is IfNode -> {
                val cond = conds.getOrPut(curr.condIndex) { boolSort.mkConst("cond_${curr.condIndex}") }

                assertions.add(cond)
                traverseAndSolve(assertions, conds, curr.th, xvalue, results)
                assertions.removeLast()

                assertions.add(cond.not())
                traverseAndSolve(assertions, conds, curr.el, xvalue, results)
                assertions.removeLast()
            }
        }
    }
}

private interface Node

private data class AssignNode(
    val xvalue: Int,
    var next: Node?
) : Node

private data class IfNode(
    val condIndex: Int,
    var th: Node?,
    var el: Node?
) : Node
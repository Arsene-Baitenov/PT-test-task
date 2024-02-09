package pt.test.task

import pt.test.task.predictor.Predictor
import java.io.File

fun main(args: Array<String>) {
    val f = File(args[0])
    val p = Predictor()
    println(p.predict(f.toPath()))
}
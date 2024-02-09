package pttesttask.predictor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PredictorTest {
    val predictor = Predictor()

    @Test
    fun `prediction of default code results`() {
        assertEquals(
            predictor.predict(
                """
            public class Main {
                public static void method(boolean... conditions) {
                    int x;
                    x = 1;
                    if (conditions[0]) {
                        x = 2;
                        if (conditions[1]) {
                            x = 3;
                        }
                        x = 4;
                        if (conditions[2]) {
                            x = 5;
                        }
                    }
                    if (conditions[3]) {
                        x = 6;
                    }
                    System.out.println(x);
                }
            }
        """
            ),
            mutableListOf(1, 4, 5, 6)
        )
    }

    @Test
    fun `prediction of code with else branches results`() {
        assertEquals(
            predictor.predict(
                """
            public class Main {
                public static void method(boolean... conditions) {
                    int x;
                    x = 1;
                    if (conditions[0]) {
                        x = 5;
                        if (conditions[1]) {
                            x = 6;
                        } else {
                            x = 7;
                        }
                    } else {
                        x = 10;
                    }
                    if (conditions[52]) {
                        x = 37;
                    }
                    System.out.println(x);
                }
            }
        """
            ),
            mutableListOf(6, 7, 10, 37)
        )
    }

    @Test
    fun `prediction of code with repeating condotions`() {
        assertEquals(
            predictor.predict(
                """
            public class Main {
                public static void method(boolean... conditions) {
                    int x;
                    x = 1;
                    if (conditions[0]) {
                        x = 5;
                        if (conditions[1]) {
                            x = 6;
                        } else {
                            x = 7;
                        }
                    } else {
                        x = 10;
                    }
                    if (conditions[1]) {
                        x = 37;
                    }
                    System.out.println(x);
                }
            }
        """
            ),
            mutableListOf(7, 10, 37)
        )
    }
}
package scalapipe

object DSLHelper {

    def ifThen[T](cond: Boolean, thenp: => T) = {
        if (cond) thenp
    }

    def ifThenElse[T](cond: Boolean, thenp: => T, elsep: => T): T = {
        if (cond) thenp else elsep
    }

}

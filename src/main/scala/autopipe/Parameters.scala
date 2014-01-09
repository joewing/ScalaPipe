
package autopipe

import scala.collection.mutable.HashMap

private[autopipe] class Parameters {

    private class Param[T](val name: Symbol, var value: T)
                                 (implicit val m: Manifest[T]) {

        def isInstance(v: Any) = {
            m.runtimeClass.isInstance(v)
        }

        def set(v: Any) {
            value = v.asInstanceOf[T]
        }

    }

    private val params = new HashMap[Symbol, Param[_]]

    private def add[T : Manifest](name: Symbol, value: T) {
        params += ((name, new Param[T](name, value)))
    }

    // Parameters and defaults.
    add('queueDepth, 256)
    add('defaultHost, "localhost")
    add('timeTrialOutput, null: String)
    add('timeTrialBufferSize, 8192)
    add('timeTrialAffinity, 0)
    add('share, 1)                 // Share FPGA resources within a block:
                                        //    0 - no sharing
                                        //    1 - share resources used in different states
                                        //    2 - share all resources
    add('profile, false)         // Insert counters for profiling
    add('fpga, "Simulation")    // Default FPGA device to target
    add('trace, false)            // Set to generate address traces from C code.

    def set(name: Symbol, value: Any) {
        params.get(name) match {
            case Some(p) =>
                try {
                    p.set(value)
                } catch {
                    case ex: ClassCastException =>
                        Error.raise("invalid type for parameter " + name.name)
                }
            case None => Error.raise("invalid parameter: " + name.name)
        }
    }

    def get[T](name: Symbol): T = params(name).value.asInstanceOf[T]

}


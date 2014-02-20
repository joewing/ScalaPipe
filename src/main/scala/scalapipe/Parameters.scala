package scalapipe

private[scalapipe] abstract class Parameters {

    private class Param[T](val name: Symbol, var value: T)
                          (implicit val m: Manifest[T]) {

        def isInstance(v: Any) = {
            m.runtimeClass.isInstance(v)
        }

        def set(v: Any) {
            value = v.asInstanceOf[T]
        }

    }

    private var params = Map[Symbol, Param[_]]()

    protected def add[T : Manifest](name: Symbol, value: T) {
        params += (name -> new Param[T](name, value))
    }

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

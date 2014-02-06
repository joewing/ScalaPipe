package scalapipe

private[scalapipe] object Platforms extends Enumeration {

    val ANY     = Value("ANY")
    val C       = Value("C")
    val OpenCL  = Value("OpenCL")
    val HDL     = Value("HDL")

    def withName(name: String, info: DebugInfo): Platforms.Value = {
        if (!values.exists(p => p.toString == name)) {
            Error.raise(s"invalid platform: $name", info)
        }
        return withName(name)
    }

}

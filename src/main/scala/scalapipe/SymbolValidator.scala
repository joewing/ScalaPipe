package scalapipe

object SymbolValidator {

    private val symbolRegex = """^[A-Za-z_][A-Za-z0-9_]*$""".r

    /** Make sure a string is a valid identifier. */
    def validate(s: String, info: DebugInfo) {
        if (symbolRegex.findFirstIn(s).isEmpty) {
            Error.raise(s"invalid identifier: $s", info)
        }
    }

    /** Make sure a string is a valid type name. */
    def validateType(s: String, info: DebugInfo) {
        if (symbolRegex.findFirstIn(s).isEmpty) {
            Error.raise(s"invalid type name: $s", info)
        }
    }

}

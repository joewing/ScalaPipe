package scalapipe

private[scalapipe] trait DebugInfo {

    private[scalapipe] var fileName = ""
    private[scalapipe] var lineNumber = 0

    def collectDebugInfo {
        try {
            throw new Exception("DEBUG")
        } catch {
            case e: Exception =>
                val trace = e.getStackTrace().filter {
                    !_.getClassName().startsWith("scalapipe.")
                }
                if (!trace.isEmpty) {
                    fileName = trace.head.getFileName
                    lineNumber = trace.head.getLineNumber
                }
        }
    }

}

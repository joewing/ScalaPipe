package autopipe

private[autopipe] trait DebugInfo {

    private[autopipe] var fileName = ""
    private[autopipe] var lineNumber = 0

    def collectDebugInfo {
        try {
            throw new Exception("DEBUG")
        } catch {
            case e: Exception =>
                val trace = e.getStackTrace().filter {
                    !_.getClassName().startsWith("autopipe.")
                }
                fileName = trace.head.getFileName
                lineNumber = trace.head.getLineNumber
        }
    }

}

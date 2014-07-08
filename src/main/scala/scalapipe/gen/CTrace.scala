package scalapipe.gen

import scalapipe._

/** Trait for outputing address traces. */
trait CTrace extends CNodeEmitter with ASTUtils {

    private var cycles = 0

    def emitSymbol(node: ASTSymbolNode): String

    def emitSymbolBase(node: ASTSymbolNode): String

    def write(code: String): Unit

    def updateTraceClocks(count: Int) {
        cycles += count
    }

    private def getOffset(node: ASTSymbolNode): String = {
        val location = "(char*)&" + emitSymbol(node)
        val base = "(char*)&" + emitSymbolBase(node)
        val baseOffset = kt.getBaseOffset(node.symbol)
        return s"((unsigned)($location - $base) + $baseOffset)"
    }

    override def emit(node: ASTNode) {

        if (kt.parameters.get[Boolean]('trace)) {

            // Idle cycles.
            if (cycles > 0) {
                write(s"""fprintf(kernel->trace_fd, "I$cycles:0\\n");""")
                cycles = 0
            }

            // Trace inputs.
            val inputOffset = 0
            for (i <- localInputs(node)) {
                val offset = kt.inputIndex(i) + inputOffset
                val size = kt.inputType(i).bytes.toHexString
                write(s"""fprintf(kernel->trace_fd, "C%x:$size\\n", """ +
                      s"""kernel->trace_streams[$offset]);""")
            }

            // Trace reads.
            for (src <- localSources(node) if !kt.getType(src).flat) {
                val offsetStr = getOffset(src)
                val size = src.valueType.bytes.toHexString
                write("fprintf(kernel->trace_fd, \"R%x:" + size +
                      "\\n\", " + offsetStr + ");")
            }

            // Trace writes.
            for (dest <- localDests(node) if !kt.getType(dest).flat) {
                val offsetStr = getOffset(dest)
                val size = dest.valueType.bytes.toHexString
                write("fprintf(kernel->trace_fd, \"W%x:" + size +
                      "\\n\", " + offsetStr + ");")
            }

            // Trace outputs.
            val outputOffset = kt.inputs.size
            for (o <- localOutputs(node)) {
                val offset = kt.outputIndex(o) + outputOffset
                val size = kt.outputType(o).bytes.toHexString
                write(s"""fprintf(kernel->trace_fd, "P%x:$size\\n", """ +
                      s"""kernel->trace_streams[$offset]);""")
            }

        }

        super.emit(node)

    }

}

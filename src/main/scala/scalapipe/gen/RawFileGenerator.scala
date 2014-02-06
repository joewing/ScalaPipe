package scalapipe.gen

import java.lang.ClassLoader
import java.io.{File, InputStreamReader, LineNumberReader}
import java.net.URL

import scalapipe._

private[scalapipe] object RawFileGenerator extends Generator {

    private def emit(srcName: String) {

        try {

            var cl = getClass.getClassLoader
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader
            }
            val is = cl.getResourceAsStream(s"code/$srcName")
            if (is != null) {

                val reader = new LineNumberReader(new InputStreamReader(is))
                var atEnd = false
                while (!atEnd) {
                    val str = reader.readLine
                    if (str != null) {
                        write(str)
                    } else {
                        atEnd = true
                    }
                }

            } else {
                Error.raise(s"Resource not found: $srcName")
            }
        } catch {
            case ex: Exception =>
                ex.printStackTrace()
                Error.raise(s"Could not read resource: $srcName")
        }

    }

    def emitFile(dir: File, srcName: String, destName: String = null) {
        emit(srcName)
        val name = if (destName != null) destName else srcName
        writeFile(dir, name)
    }

}


package autopipe.gen

import java.lang.ClassLoader
import java.io.File
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.net.URL

import autopipe._

private[autopipe] object RawFileGenerator extends Generator {

    private def emit(name: String) {

        try {

            var cl = getClass.getClassLoader
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader
            }
            val is = cl.getResourceAsStream("code/" + name)
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
                Error.raise("Resource not found: " + name)
            }
        } catch {
            case ex: Exception =>
                ex.printStackTrace()
                Error.raise("Could not read resource: " + name)
        }

    }

    def emitFile(dir: File, name: String) {
        emit(name)
        writeFile(dir, name)
    }

}


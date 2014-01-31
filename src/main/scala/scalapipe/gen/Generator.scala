package scalapipe.gen

import scalapipe._

import scala.collection.mutable.ListBuffer
import java.io.{FileOutputStream, PrintStream, File}

private[gen] class Generator {

    private var values = Map[String, String]()
    private val result = new ListBuffer[String]
    private var level = 0

    def enter() {
        level += 1
    }

    def leave() {
        if (level <= 0) {
            sys.error("invalid nesting level")
        }
        level -= 1
    }

    private def indent(): String = {
        Array.range(0, level).foldLeft("") { (a, b) => a + "    " }
    }

    def getOutput(): String = {
        val o = result.foldLeft("") { (a, b) => a + b }
        clear
        o
    }

    protected def clear() {
        result.clear
    }

    def write() {
        result += "\n"
    }

    def write(s: String) {
        result += indent() + s + "\n"
    }

    protected def writeLeft(s: String) {
        result += s + "\n"
    }

    protected def write(gen: Generator) {
        for (line <- gen.result) {
            result += indent() + line
        }
        gen.clear
    }

    protected def write(l: Seq[Generator]) {
        for (g <- l) {
            write(g)
        }
    }

    def extract(): Generator = {
        val gen = new Generator
        gen.result ++= result
        gen.level = level
        clear
        gen
    }

    def writeFile(dir: File, name: String) {
        val file = new File(dir, name)
        val ps = new PrintStream(new FileOutputStream(file))
        ps.println(getOutput)
        ps.close
    }

}

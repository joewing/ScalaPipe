
package autopipe.gen

import autopipe._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.immutable.ListSet

import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._

import java.io.FileOutputStream
import java.io.PrintStream
import java.io.File

private[gen] class Generator {

    private val values = new HashMap[String, String]
    private val result = new ListBuffer[String]
    private var level = 0

    protected def reset {
        values.clear
    }

    protected def set(name: String, value: Any) {
        values += ((name, value.toString))
    }

    private object ExprParser extends StdTokenParsers {

        type Tokens = StdLexical
        val lexical = new StdLexical
        lexical.delimiters ++= List("(", ")", "+", "-", "*", "/")

        private def mult(a: String, b: String): String = {
            (a.toInt * b.toInt).toString
        }

        private def div(a: String, b: String): String = {
            (a.toInt / b.toInt).toString
        }

        private def add(a: String, b: String): String = {
            (a.toInt + b.toInt).toString
        }

        private def sub(a: String, b: String): String = {
            (a.toInt - b.toInt).toString
        }

        def factor: Parser[String] = {
            "(" ~> expr <~ ")" |
            numericLit |
            ident ^^ { n => values(n) }
        }

        def invfactor: Parser[String] =
            factor ^^ { t => (1 / t.toInt).toString }

        def term: Parser[String] =
            factor ~ rep("*" ~> factor | "/" ~> invfactor) ^^ { case x ~ rest =>
                rest.foldLeft(x)( (a, b) => (a.toInt * b.toInt).toString)
            }

        def negterm: Parser[String] = term ^^ { t => "-" + t }

        def expr: Parser[String] =
            term ~ rep("+" ~> term | "-" ~> negterm) ^^ { case x ~ rest =>
                rest.foldLeft(x)( (a, b) => (a.toInt + b.toInt).toString)
            }

        def parse(str: String): String = {
            expr(new lexical.Scanner(str)).get
        }

    }

    private def process(str: String): String = {
        if (values.isEmpty) {
            return str
        } else {
            val start = str.indexOf('$')
            val end = str.indexOf('$', start + 1)
            if (start >= 0 && end > 0) {
                val before = str.substring(0, start)
                val expr = str.substring(start + 1, end)
                val after = str.substring(end + 1)
                if (start + 1 == end) {
                    return before + "$" + process(after)
                } else {
                    return before + ExprParser.parse(expr) + process(after)
                }
            } else {
                return str;
            }
        }
    }

    protected def enter() {
        level += 1
    }

    protected def leave() {
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

    protected def write() {
        result += "\n"
    }

    protected def write(s: String) {
        result += indent() + process(s) + "\n"
    }

    protected def writeLeft(s: String) {
        result += process(s) + "\n"
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


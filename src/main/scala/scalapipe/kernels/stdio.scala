package scalapipe.kernels

import scalapipe._
import scalapipe.dsl._

object stdio {

    class stdioFunc(_name: String) extends Func(_name) {
        include("stdio.h")
        external("C")
    }

    class stdlibFunc(_name: String) extends Func(_name) {
        include("stdlib.h")
        external("C")
    }

    class unistdFunc(_name: String) extends Func(_name) {
        include("unistd.h")
        external("C")
    }

    class timeFunc(_name: String) extends Func(_name) {
        include("sys/time.h")
        external("C")
    }

    val FILE = new AutoPipeNative("FILE")
    val FILEPTR = new AutoPipePointer(FILE)
    val VOIDPTR = new AutoPipePointer(VOID)
    val TIMEVAL = new AutoPipeNative("struct timeval")
    val TIMEVALPTR = new AutoPipePointer(TIMEVAL)

    val SEEK_SET = 0
    val SEEK_CUR = 1
    val SEEK_END = 2

    val fopen = new stdioFunc("fopen") {
        returns(FILEPTR)
    }

    val fprintf = new stdioFunc("fprintf") {
        returns(SIGNED32)
    }

    val fscanf = new stdioFunc("fscanf") {
        returns(SIGNED32)
    }

    val fread = new stdioFunc("fread") {
        returns(UNSIGNED32)
    }

    val fwrite = new stdioFunc("fwrite") {
        returns(UNSIGNED32)
    }

    val printf = new stdioFunc("printf") {
        returns(SIGNED32)
    }

    val fgetc = new stdioFunc("fgetc") {
        returns(SIGNED8)
    }

    val fputc = new stdioFunc("fputc") {
        returns(SIGNED32)
    }

    val fflush = new stdioFunc("fflush") {
        returns(SIGNED32)
    }

    val fseek = new stdioFunc("fseek") {
        returns(SIGNED32)
    }

    val rewind = new stdioFunc("rewind") {
        returns(VOID)
    }

    val feof = new stdioFunc("feof") {
        returns(SIGNED32)
    }

    val fclose = new stdioFunc("fclose") {
        returns(SIGNED32)
    }

    val exit = new stdioFunc("exit") {
        returns(VOID)
    }

    val srand = new stdlibFunc("srand") {
        returns(VOID)
    }

    val rand = new stdlibFunc("rand") {
        returns(SIGNED32)
    }

    val malloc = new stdlibFunc("malloc") {
        returns(VOIDPTR)
    }

    val free = new stdlibFunc("free") {
        returns(VOID)
    }

    val usleep = new unistdFunc("usleep") {
        returns(SIGNED32)
    }

    val sleep = new unistdFunc("sleep") {
        returns(UNSIGNED32)
    }

    val gettimeofday = new timeFunc("gettimeofday") {
        returns(SIGNED32)
    }

}

package examples
import scalapipe.kernels._
import scalapipe.dsl._
import scalapipe._

object MergeSort extends App {

    val itemCount = 1 << 20
    val VTYPE = UNSIGNED32

    val Seed = new Kernel("Seed") {
        val out = output(UNSIGNED32)
        out = 1
        stop
    }

    val Generate = new Kernel("Generate") {

        val seed = input(UNSIGNED32)
        val out = output(UNSIGNED32)
        val state = local(UNSIGNED32)
        val i = local(UNSIGNED32, 0)

        state = seed

        while (i <> itemCount) {

            // Taps at 32, 31, 29, 1.
            state = (state >> 1) ^ (-(state & 1) & 0xD0000001);

            // Output the result.
            out = state

            i += 1
        }

    }

    class Merge(size: Int) extends Kernel("Merge" + size) {

        val in = input(VTYPE)
        val out = output(VTYPE)

        val buffer = local(Vector(VTYPE, size))
        val ptr1 = local(UNSIGNED32)
        val ptr2 = local(UNSIGNED32)
        val a = local(VTYPE)
        val b = local(VTYPE)

        ptr1 = 0
        while (ptr1 <> size) {
            buffer(ptr1) = in
            ptr1 += 1
        }
        a = buffer(0)
        b = in
        ptr1 = 0
        ptr2 = 0
        while (ptr1 <> size || ptr2 <> size) {
            if (ptr1 <> size && (ptr2 == size || a < b)) {
                out = a
                ptr1 += 1
                a = buffer(ptr1 % size)
            } else if (ptr2 <> size) {
                out = b
                ptr2 += 1
                if (ptr2 <> size) {
                    b = in
                }
            }
        }

    }

    val SelectLast = new Kernel("SelectLast") {
        val x0 = input(VTYPE)
        val y0 = output(VTYPE)
        val temp = local(VTYPE)
        val i = local(UNSIGNED32, 1)
        while (i <> itemCount) {
            temp = x0
            i += 1
        }
        y0 = x0
    }

    val Output = new Kernel("Output") {
        val x0 = input(VTYPE)
        stdio.printf("%u\n", x0)
    }

    val app = new Application {

        def createPipeline(in: Stream, size: Int, total: Int): Stream = {
            if (size < total) {
                val m = new Merge(size)
                createPipeline(m(in), size * 2, total)
            } else {
                in
            }
        }

        param('fpga, "Saturn")
        param('bram, false)

        val seed = Seed()
        val unsorted = Generate(seed)
        val sorted = createPipeline(unsorted, 1, itemCount)
        val last = SelectLast(sorted)
        Output(last)

        map(Seed -> ANY_KERNEL, CPU2FPGA())
        map(ANY_KERNEL -> Output, FPGA2CPU())
    }
    app.emit("MergeSort")

}

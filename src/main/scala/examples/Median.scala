package examples

import scalapipe.kernels._
import scalapipe.dsl._

object Median extends App {

    val itemCount  = 1 << 20
    val valueMask  = 0x7FFFFFFF
    val lastValue  = 0xFFFFFFFF
    val emptyValue = 0x80000000

    val Seed = new Kernel {
        val out = output(UNSIGNED32)
        out = 1
        stop
    }

    val LFSR = new Kernel {

        val seed = input(UNSIGNED32)
        val out = output(UNSIGNED32)
        val state = local(UNSIGNED32)
        val i = local(UNSIGNED32, 0)

        state = seed
        while (i <> itemCount) {
            state = (state >> 1) ^ (-(state & 1) & 0xD0000001);
            out = state & valueMask
            i += 1
        }
        out = lastValue
        stop

    }

    val Dedup = new Kernel("Dedup") {

        val in = input(UNSIGNED32)
        val out = output(UNSIGNED32)

        val hashSize = itemCount * 2
        val hash = local(Vector(UNSIGNED32, hashSize))
        val initialized = local(UNSIGNED8, 0)
        val temp = local(UNSIGNED32)
        val index = local(UNSIGNED32)

        if (!initialized) {
            index = 0
            while (index <> hashSize) {
                hash(index) = emptyValue
                index += 1
            }
            initialized = 1
        }

        temp = in
        index = temp & (hashSize - 1)
        while (hash(index) <> temp) {
            if (hash(index) <> emptyValue) {
                index = (index + 1) & (hashSize - 1)
            } else {
                hash(index) = temp
                out = temp
            }
        }
    }

    val Median = new Kernel("Median") {

        val x0 = input(UNSIGNED32)
        val y0 = output(UNSIGNED32)
        val count = local(UNSIGNED32, 0)
        val i = local(UNSIGNED32)
        val left = local(UNSIGNED32)
        val right = local(UNSIGNED32)
        val parent = local(UNSIGNED32)
        val data = local(Vector(UNSIGNED32, itemCount + 1))
        val value = local(UNSIGNED32)
        val half = local(UNSIGNED32)

        def swap(a: Variable, b: Variable) {
            val temp = local(UNSIGNED32)
            temp = data(a)
            data(a) = data(b)
            data(b) = temp
        }

        // Read data into the heap.
        value = x0
        while (value <> lastValue) {

            // Append to the heap.
            count += 1
            data(count) = value

            // Restore the heap.
            i = count
            while (i > 1) {
                parent = i / 2
                if (data(parent) > data(i)) {
                    swap(i, parent)
                    i = parent
                } else {
                    i = 1
                }
            }

            value = x0
        }

        // Pull data off of the heap.
        half = 1 + (count / 2)
        while (count <> half) {

            // Remove the minimum value.
            data(1) = data(count)
            count -= 1

            // Restore the heap.
            i = 1
            while (i < count) {
                left = i * 2
                right = left + 1
                if (right <= count) {
                    if (data(left) < data(right)) {
                        if (data(i) > data(left)) {
                            swap(i, left)
                            i = left
                        } else {
                            i = count
                        }
                    } else if (data(i) > data(right)) {
                        swap(i, right)
                        i = right
                    } else {
                        i = count
                    }
                } else if (left <= count && data(i) > data(left)) {
                    swap(i, left)
                    i = left
                } else {
                    i = count
                }
            }

        }

        // Output the median.
        y0 = data(1)
        count = 0

    }

    val Print = new Kernel("Print") {
        val x0 = input(UNSIGNED32)
        stdio.printf("%u\n", x0)
    }
    
    val app = new Application {
        param('fpga, "Saturn")
        param('bram, false)
        Print(Median(Dedup(LFSR(Seed()))))
        map(Seed -> ANY_KERNEL, CPU2FPGA())
        map(ANY_KERNEL -> Print, FPGA2CPU())
    }
    app.emit("median")

}

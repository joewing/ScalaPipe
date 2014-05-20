package examples

import scalapipe.kernels._
import scalapipe.dsl._

object StreamSort extends App {

    val itemCount = 1 << 20
    val VTYPE = SIGNED32

    val Input = new Kernel("Input") {
        val y0 = output(VTYPE)
        val count = local(UNSIGNED32, 0)
        while (count < itemCount) {
            y0 = stdio.rand()
            count += 1
        }
        stop
    }

    val HeapSort = new Kernel("HeapSort") {

        val x0 = input(VTYPE)
        val y0 = output(VTYPE)
        val count = local(UNSIGNED32, 0)
        val i = local(UNSIGNED32)
        val left = local(UNSIGNED32)
        val right = local(UNSIGNED32)
        val parent = local(UNSIGNED32)
        val data = local(Vector(VTYPE, itemCount + 1))

        def swap(a: Variable, b: Variable) {
            val temp = local(VTYPE)
            temp = data(a)
            data(a) = data(b)
            data(b) = temp
        }

        // Read data into the heap.
        while (count < itemCount) {

            // Append to the heap.
            count += 1
            data(count) = x0

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
        }

        // Pull data off of the heap.
        while (count <> 0) {

            // Remove the minimum value.
            y0 = data(1)
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


    }

    val Output = new Kernel("Output") {
        val x0 = input(VTYPE)
        stdio.printf("%d\n", x0)
    }


    val app = new Application {
        Output(HeapSort(Input()))
    }
    app.emit("StreamSort")

}

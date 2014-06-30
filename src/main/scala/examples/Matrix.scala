package examples

import scalapipe._
import scalapipe.dsl._
import scalapipe.kernels._

object Matrix extends App {

    val matrixLength = 256
    val matrixSize = matrixLength * matrixLength
    val productCount = 8

    val Seed = new Kernel {
        val out = output(UNSIGNED32)
        out = 1
        stop
    }

    val RandomMatrix = new Kernel {
        val seed = input(UNSIGNED32)
        val out = output(FLOAT32)
        val state = local(UNSIGNED32)
        val i = local(UNSIGNED32, 0)
        state = seed
        while (i <> matrixSize) {
            state = (state >> 1) ^ (-(state & 1) & 0xD0000001);
            out = FLOAT32(state >> 16)
            i += 1
        }
        stop
    }

    val Distribute = new Kernel {
        val x0 = input(FLOAT32)
        val x1 = input(FLOAT32)
        val y0 = output(FLOAT32)
        val y1 = output(FLOAT32)

        val a = local(Vector(FLOAT32, matrixSize))
        val b = local(Vector(FLOAT32, matrixSize))
        val i = local(UNSIGNED32)
        val j = local(UNSIGNED32)
        val k = local(UNSIGNED32)

        // Load the matrices.
        i = 0
        while (i <> matrixSize) {
            a(i) = x0
            b(i) = x1
            i += 1
        }

        // Distribute the data in the right order.
        k = 0
        while (k <> matrixLength) {
            j = 0
            while (j <> matrixLength) {
                i = 0
                while (i <> matrixLength) {
                    y0 = a(k * matrixLength + i)
                    y1 = b(i * matrixLength + j)
                    i += 1
                }
                j += 1
            }
            k += 1
        }
    }

    val Splitter = new EqualSplit(FLOAT32, productCount)
    val Joiner = new EqualJoin(FLOAT32, productCount)

    // Compute a dot product.
    val DotProduct = new Kernel {
        val x0 = input(FLOAT32)
        val x1 = input(FLOAT32)
        val y0 = output(FLOAT32)
        val sum = local(FLOAT32)
        val i = local(UNSIGNED32)
        i = 0
        sum = 0.0
        while (i <> matrixLength) {
            sum += x0 * x1
            i += 1
        }
        y0 = sum
    }

    // Output the result.
    val Print = new Kernel {
        val x0 = input(FLOAT32)
        val i = local(UNSIGNED32, 0)

        stdio.printf("%g ", x0)
        i += 1
        if (i == matrixLength) {
            stdio.printf("\n")
            i = 0
        }
    }

    val app = new Application {

        val mat1 = RandomMatrix(Seed())
        val mat2 = RandomMatrix(Seed())
        val streams = Distribute(mat1, mat2)
        val left = Splitter(streams(0))
        val right = Splitter(streams(1))
        val results = Array.tabulate(productCount) { i =>
            DotProduct(left(i), right(i))()
        }
        Print(Joiner(results))

    }
    app.emit("matrix")

}

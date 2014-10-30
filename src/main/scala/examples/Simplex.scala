package examples

import scalapipe.dsl._
import scalapipe.kernels._

object Simplex extends App {

    val maxVariables   = 3
    val maxConstraints = 2
    val rowCount = maxConstraints + 1
    val columnCount = maxConstraints + maxVariables + 2
    val read_file = false  // Read from a file or use pre-defined data.

    val VALUE_TYPE = FLOAT32
    val ROW_TYPE    = Vector(VALUE_TYPE, columnCount)

    // Read the initial canonical matrix.
    val Parser = new Kernel("Parser") {

        val out         = output(VALUE_TYPE)

        if(read_file) {
            val filename    = config(STRING, 'filename, "input.txt")
            val fd          = local(stdio.FILEPTR, 0)
            val value       = local(VALUE_TYPE)
            val rc          = local(SIGNED32)

            // Open the file.
            if (fd == 0) {
                fd = stdio.fopen(filename, "r")
                if (fd == 0) {
                    stdio.printf("ERROR: could not open %s\n", filename)
                    stdio.exit(-1)
                }
            }

            // Read a value from the file.
            rc = stdio.fscanf(fd, " %g", addr(value))
            if (rc == 1) {
                out = value
            } else {
                stdio.fclose(fd)
                stop
            }

        } else {

            // Example from Wikipedia.
            val data = Array(
                1.0, -2.0, -3.0, -4.0, 0.0, 0.0, 0.0,
                0.0, 3.0, 2.0, 1.0, 1.0, 0.0, 10.0,
                0.0, 2.0, 5.0, 3.0, 0.0, 1.0, 15.0
            )
            for (i <- data) {
                out = i
            }
            stop

        }

    }

    // Stream the canonical matrix.
    val Streamer = new Kernel("Streamer") {

        val init_in     = input(VALUE_TYPE)
        val feedback    = input(VALUE_TYPE)
        val out1        = output(VALUE_TYPE)
        val out2        = output(VALUE_TYPE)
        val result      = output(VALUE_TYPE)

        val value       = local(VALUE_TYPE)
        val x           = local(SIGNED32, 0)
        val y           = local(SIGNED32, 0)
        val init        = local(BOOL, false)

        // Read the next input.
        if (init) {
            value = feedback
        } else {
            value = init_in
        }

        result = value
        out1 = value
        out2 = value

        // Update our position in the matrix.
        x += 1
        if (x == columnCount) {
            x = 0
            y += 1
            if (y == rowCount) {
                init = true
                y = 0
            }
        }

    }

    // Split the matrix by row.
    val ArraySplitter = new Kernel("ArraySplitter") {

        val array_in    = input(VALUE_TYPE)

        val value       = local(VALUE_TYPE)
        val x           = local(SIGNED32, 0)
        val y           = local(SIGNED32, 0)

        // Give the current input to the correct row processor.
        value = array_in
        switch (y) {
            for (i <- Range(0, rowCount)) {
                val out = output(VALUE_TYPE)
                when (i) {
                    out = value
                }
            }
        }

        // Update our position in the matrix.
        x += 1
        if (x == columnCount) {
            x = 0
            y += 1
            if (y == rowCount) {
                y = 0
            }
        }

    }

    // Select the pivot row and column.
    val PivotSelect = new Kernel("PivotSelect") {

        val array_in    = input(VALUE_TYPE)
        val prow_out    = output(VALUE_TYPE)
        val pivot_out   = output(VALUE_TYPE)
        val pindex_out  = output(SIGNED32)
        val column_out  = output(SIGNED32)

        val row         = local(ROW_TYPE)
        val prow        = local(ROW_TYPE)
        val value       = local(VALUE_TYPE)
        val x           = local(SIGNED32, 0)
        val y           = local(SIGNED32, 0)
        val column      = local(SIGNED32, 0)
        val pivot       = local(SIGNED32, -1)
        val best        = local(VALUE_TYPE)

        // Read the matrix one row at a time.
        value = array_in
        row(x) = value

        // If this is the top row, we look for negative columns.
        if (y == 0 && value < 0 && column == 0) {
            column = x
        }

        // Next column.
        x += 1
        if (x == columnCount) {

            // End of the column.
            // Check if this row is the best, if so remember it.
            if (row(column) > 0) {
                value = row(columnCount - 1) / row(column)
                if (pivot < 0 || value < best) {
                    pivot = y
                    best = value
                    for(i <- 0 until columnCount) {
                        prow(i) = row(i)
                    }
                }
            }

            // Next row.
            x = 0
            y += 1
            if (y == rowCount) {

                // End of the matrix.
                // Output the pivot information.
                pivot_out = prow(column)
                pindex_out = pivot
                column_out = column
                for (i <- 0 until columnCount) {
                    prow_out = prow(i)
                }

                // Reset for next iteration.
                pivot = -1
                column = 0
                x = 0
                y = 0
            }
        }

    }

    // Perform row operations.
    val RowProcessor = new Kernel("RowProcessor") {

        val row_in      = input(VALUE_TYPE)
        val prow_in     = input(VALUE_TYPE)
        val pivot_in    = input(VALUE_TYPE)
        val pindex_in   = input(SIGNED32)
        val column_in   = input(SIGNED32)
        val row_out     = output(VALUE_TYPE)
        val index       = config(SIGNED32, 'index)

        val row         = local(ROW_TYPE)
        val prow        = local(ROW_TYPE)
        val pivot       = local(VALUE_TYPE)
        val pindex      = local(SIGNED32)
        val column      = local(SIGNED32)
        val x           = local(SIGNED32)
        val k           = local(VALUE_TYPE)

        // Read our row.
        x = 0
        while (x < columnCount) {
            row(x) = row_in
            x += 1
        }

        // Read the pivot.
        pivot = pivot_in
        pindex = pindex_in
        column = column_in

        // Update the row.
        if (pindex == index) {
            k = 1.0 / pivot
            x = 0
            while (x < columnCount) {
                row_out = k * prow_in
                x += 1
            }
        } else {
            k = -row(column) / pivot
            x = 0
            while (x < columnCount) {
                row_out = row(x) + k * prow_in
                x += 1
            }
        }

    }

    // Buffer a row of the array.
    val RowBuffer = new Kernel("RowBuffer") {

        val array_in    = input(VALUE_TYPE)
        val array_out   = output(VALUE_TYPE)

        val row         = local(ROW_TYPE)
        val x           = local(SIGNED32, 0)

        row(x) = array_in
        x += 1
        if (x == columnCount) {
            x = 0
            while (x < columnCount) {
                array_out = row(x)
                x += 1
            }
            x = 0
        }

    }

    // Construct a canonical array from rows.
    val ArrayBuilder = new Kernel("ArrayBuilder") {

        val array_out = output(VALUE_TYPE)

        val x = local(SIGNED32, 0)
        val y = local(SIGNED32, 0)

        switch (y) {
            for (i <- Range(0, rowCount)) {
                val in = input(VALUE_TYPE)
                when (i) {
                    array_out = in
                }
            }
        }

        // Update our position in the matrix.
        x += 1
        if (x == columnCount) {
            x = 0
            y += 1
            if (y == rowCount) {
                y = 0
            }
        }

    }

    // Output the result.
    // This will terminate the program when a solution is found.
    val Output = new Kernel("Output") {

        val in = input(VALUE_TYPE)
        val t = local(VALUE_TYPE)
        val o = local(FLOAT32)
        val x = local(SIGNED32, 0)
        val y = local(SIGNED32, 0)
        val is_min = local(BOOL, true)

        t = in
        if (y == 0 && t < 0) {
            is_min = false
        }

        o = FLOAT32(t)
        stdio.printf("%g ", o)
        x += 1
        if (x == columnCount) {
            x = 0
            stdio.printf("\n")
            y += 1
            if (y == rowCount) {
                stdio.printf("\n")
                if (is_min) {
                    stdio.exit(0)
                }
                is_min = true
                y = 0
            }
        }

    }

    val DupValues = new Duplicate(VALUE_TYPE, rowCount)
    val DupInt = new Duplicate(SIGNED32, rowCount)

    val app = new Application {

        if (false) {
            // Run in hardware.
            map(Parser -> ANY_KERNEL, CPU2FPGA())
            map(ANY_KERNEL -> Output, FPGA2CPU())
        }

        val cycle = Cycle()
        val stream = Streamer(Parser(), cycle)
        val split = ArraySplitter(stream(0))
        val pivot = PivotSelect(stream(1))
        val prow = DupValues(pivot(0))
        val pvalue = DupValues(pivot(1))
        val pindex = DupInt(pivot(2))
        val column = DupInt(pivot(3))
        val rows = Array.tabulate(rowCount) { i =>
            val row = RowProcessor(split(i), prow(i), pvalue(i),
                                   pindex(i), column(i), 'index -> i)
            RowBuffer(row)()
        }
        cycle(ArrayBuilder(rows))
        Output(stream(2))
    }
    app.emit("simplex")

}

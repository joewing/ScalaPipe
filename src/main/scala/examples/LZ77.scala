package examples

import scalapipe._
import scalapipe.dsl._
import scalapipe.kernels._

object LZ77 extends App {

    val compress = true
    val rawFile = "data"
    val compressedFile = "data.cmp"

    val offsetBits = 10
    val lengthBits = 6
    val dictSize    = 1 << offsetBits
    val totalBits = 8 + offsetBits + lengthBits
    val offsetShift = 8 + lengthBits
    val dictMask = dictSize - 1
    val lengthMask = lengthBits - 1

    val DICT = new Vector(UNSIGNED8, dictSize)

    val FileReader = new Kernel {

        val out = output(UNSIGNED16)
        val fn = config(STRING, 'input, rawFile)

        val fd = local(stdio.FILEPTR, 0)

        if (fd == 0) {
            fd = stdio.fopen(fn, "rb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        if (!stdio.feof(fd)) {
            out = stdio.fgetc(fd)
        } else {
            out = 0xFFFF
            stop
        }

    }

    val CompressedFileReader = new Kernel {

        val out = output(UNSIGNED32)
        val fn = config(STRING, 'input, compressedFile)

        val fd = local(stdio.FILEPTR, 0)
        val temp = local(UNSIGNED32)

        if (fd == 0) {
            fd = stdio.fopen(fn, "rb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        if (!stdio.feof(fd)) {
            temp  = stdio.fgetc(fd) << 16
            temp |= stdio.fgetc(fd) << 8
            temp |= stdio.fgetc(fd)
            out = temp
        } else {
            out = 0xFFFFFFFF
            stop
        }

    }

    val FileWriter = new Kernel {

        val in = input(UNSIGNED16)
        val fn = config(STRING, 'output, rawFile)

        val fd = local(stdio.FILEPTR, 0)
        val temp = local(UNSIGNED32)

        if (fd == 0) {
            fd = stdio.fopen(fn, "wb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        temp = in
        if (temp == 0xFFFF) {
            stdio.fclose(fd)
            stdio.exit(0)
        } else {
            stdio.fputc(temp, fd)
        }

    }

    val CompressedFileWriter = new Kernel {

        val in = input(UNSIGNED32)
        val fn = config(STRING, 'output, compressedFile)

        val fd = local(stdio.FILEPTR, 0)
        val temp = local(UNSIGNED32)

        if (fd == 0) {
            fd = stdio.fopen(fn, "wb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        temp = in
        if (temp == 0xFFFFFFFF) {
            stdio.fclose(fd)
            stdio.exit(0)
        } else {
            stdio.fputc((temp >> 16) & 0xFF, fd)
            stdio.fputc((temp >>  8) & 0xFF, fd)
            stdio.fputc((temp >>  0) & 0xFF, fd)
        }

    }

    val Compress = new Kernel {

        val in = input(UNSIGNED16)
        val out = output(UNSIGNED32)

        val dictionary = local(DICT)
        val buffer = local(DICT)
        val initialized = local(BOOL, false)

        val value = local(UNSIGNED16)
        val offset = local(UNSIGNED32, 0)    // Offset in the buffer.
        val matches = local(BOOL, true)
        val matchOffset = local(UNSIGNED32, 0)
        val x = local(UNSIGNED32)
        val y = local(UNSIGNED32)
        val pointer = local(UNSIGNED32, 0)

        if (!initialized) {
            x = 0
            while (x < dictSize) {
                dictionary(x) = x
                x += 1
            }
            initialized = true
        }

        // Read a byte of input.
        value = in

        // Check if we're done.
        if (value == 0xFFFF) {

            // Done; output the current match, if any.
            if (offset > 0) {
                out = (matchOffset << offsetShift) |
                        ((offset - 1) << 8) | buffer(offset)
            }

            // "Done" marker.
            out = 0xFFFFFFFF
            stop

        }

        // Update the buffer.
        buffer(offset) = value

        // Look for a match.
        // We loop over each possible dictionary position.
        matches = false
        x = 0
        while (x < dictSize && !matches) {
            y = 0
            matches = true
            while (y <= offset && matches) {
                matches = buffer(y) == dictionary((x + y) & dictMask)
                y += 1
            }
            x += 1
        }

        if (!matches || offset == lengthMask) {

            // No match (or overflow); output the longest match.
            out = (matchOffset << offsetShift) | (offset << 8) | value

            // Update the dictionary.
            pointer = matchOffset + offset
            dictionary(pointer) = value

            // Reset the offset.
            offset = 0

        } else {

            // Match; update the match offset and continue.
            offset += 1
            matchOffset = x - 1

        }

    }

    val Decompress = new Kernel {

        val in = input(UNSIGNED32)
        val out = output(UNSIGNED16)

        val dictionary = local(DICT)
        val initialized = local(BOOL, false)
        val value = local(UNSIGNED32)
        val pointer = local(UNSIGNED32, 0)
        val offset = local(UNSIGNED32)
        val length = local(UNSIGNED32)
        val x = local(UNSIGNED32)

        if (!initialized) {
            x = 0
            while (x < dictSize) {
                dictionary(x) = x
                x += 1
            }
            initialized = true
        }

        // Read a compressed word.
        value = in

        // Check for the end of the data stream.
        if (value == 0xFFFFFFFF) {
            out = 0xFFFF
            stop
        }

        // Output the match from the dictionary.
        offset = value >> offsetShift
        length = (value >> 8) & lengthMask
        x = 0
        while (x < length) {
            out = dictionary((offset + x) & dictMask)
            x += 1
        }

        // Output the new character.
        value &= 0xFF
        out = value

        // Update the dictionary.
        pointer = offset + length
        dictionary(pointer) = value

    }

    val app = new Application {

        param('profile, true)

        if (compress) {
            val input = FileReader()
            val compressed = Compress(input)
            CompressedFileWriter(compressed)
        } else {
            val input = CompressedFileReader()
            val raw = Decompress(input)
            FileWriter(raw)
        }

    }
    app.emit("lz77")

}


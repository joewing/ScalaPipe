package blocks

import autopipe.dsl._

/** BMP file reader.
 * Reads 24 or 32 bit bitmaps.
 */
object BMPReader extends Kernel {

    val data_out   = output(UNSIGNED32)
    val width_out  = output(SIGNED32)
    val height_out = output(SIGNED32)
    val file_name  = config(STRING, 'file, "in.bmp")

    val fd = local(stdio.FILEPTR, 0)
    val header = local(AutoPipeArray(UNSIGNED8, 14))
    val dib_header = local(AutoPipeArray(UNSIGNED8, 40))
    val offset = local(UNSIGNED32)
    val compression = local(UNSIGNED32)
    val width = local(SIGNED32)
    val height = local(SIGNED32)
    val depth = local(UNSIGNED16)
    val x = local(SIGNED32, 0)
    val y = local(SIGNED32, 0)
    val pixel = local(UNSIGNED32)

    if (fd == 0) {

        // Open the file.
        fd = stdio.fopen(file_name, "rb")
        if (fd == 0) {
            stdio.printf("""ERROR: could not open %s\n""", file_name)
            stdio.exit(-1)
        }

        // Read the header.
        // Format:
        //     type (2 bytes: BM)
        //     size (4 bytes)
        //     reserved (4 bytes)
        //     offset (4 bytes)
        stdio.fread(addr(header), 1, 14, fd)
        if (header(0) <> 'B' || header(1) <> 'M') {
            stdio.fclose(fd)
            stdio.printf("""ERROR: invalid BMP header in %s\n""", file_name)
            stdio.exit(-1)
        }

        offset  = header(10) << 0
        offset |= header(11) << 8
        offset |= header(12) << 16
        offset |= header(13) << 24

        // Read the DIB header.
        stdio.fread(addr(dib_header), 1, 40, fd)
        if (dib_header(0) <> 40 || dib_header(1) <> 0 ||
            dib_header(2) <> 0 || dib_header(3) <> 0) {
            stdio.fclose(fd)
            stdio.printf("""ERROR: unknown DIB header in %s\n""", file_name)
            stdio.exit(-1)
        }

        width  = dib_header(4) << 0
        width |= dib_header(5) << 8
        width |= dib_header(6) << 16
        width |= dib_header(7) << 24
        width_out = width

        height  = dib_header(8)  << 0
        height |= dib_header(9)  << 8
        height |= dib_header(10) << 16
        height |= dib_header(11) << 24
        height_out = height

        // Image depth.
        // We support 24 or 32.
        depth  = dib_header(14) << 0
        depth |= dib_header(15) << 8
        if (depth <> 24 && depth <> 32) {
            stdio.printf("""ERROR: unsupported bit depth in %s: %hu\n""",
                         file_name, depth)
            stdio.exit(-1)
        }

        // We do not support compression.
        compression  = dib_header(16) << 0
        compression |= dib_header(17) << 8
        compression |= dib_header(18) << 16
        compression |= dib_header(19) << 24
        if (compression <> 0) {
            stdio.printf("""ERROR: unsupported compression in %s: %u\n""",
                         file_name, compression)
            stdio.exit(-1)
        }

        // Seek to the data.
        stdio.fseek(fd, offset, stdio.SEEK_SET)

    } else {

        // Read a pixel.
        pixel  = stdio.fgetc(fd)
        pixel |= stdio.fgetc(fd) << 8
        pixel |= stdio.fgetc(fd) << 16
        if (depth == 32) {
            stdio.fgetc(fd)
        }
        data_out = pixel

        x += 1
        if (x == width) {
            while ((x & 3) <> 0) {
                stdio.fgetc(fd)
                x += 1
            }
            x = 0
            y += 1
            if (y == height) {
                stdio.fclose(fd)
                stop
            }
        }

    }

}

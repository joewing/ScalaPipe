package scalapipe.kernels

import scalapipe.dsl._

/** BMP file writer.
 * Writes 24-bit bitmaps.
 */
object BMPWriter extends Kernel {

    val data_in     = input(UNSIGNED32)
    val width_in    = input(SIGNED32)
    val height_in   = input(SIGNED32)
    val file_name   = config(STRING, 'file, "out.bmp")

    val fd = local(stdio.FILEPTR, 0)
    val header = local(new AutoPipeArray(UNSIGNED8, 14))
    val dib_header = local(new AutoPipeArray(UNSIGNED8, 40))
    val row_size = local(UNSIGNED32)
    val width = local(SIGNED32)
    val height = local(SIGNED32)
    val image_size = local(UNSIGNED32)
    val size = local(UNSIGNED32)
    val x = local(SIGNED32, 0)
    val y = local(SIGNED32, 0)
    val pixel = local(UNSIGNED32)

    if (fd == 0) {

        // Read the width and height.
        width = width_in
        height = height_in

        // Open the file.
        fd = stdio.fopen(file_name, "wb")
        if (fd == 0) {
            stdio.printf("""ERROR: could not open %s\n""", file_name)
            stdio.exit(-1)
        }

        // Write the header.
        // Format:
        //     type (2 bytes: BM)
        //     size (4 bytes)
        //     reserved (4 bytes)
        //     offset (4 bytes)
        row_size = (width * 3 + 3) & ~3
        image_size = row_size * height
        size = image_size + 14 + 40
        header(0) = 'B'
        header(1) = 'M'
        header(2) = (size >>  0) & 0xFF
        header(3) = (size >>  8) & 0xFF
        header(4) = (size >> 16) & 0xFF
        header(5) = (size >> 24) & 0xFF
        header(6) = 0
        header(7) = 0
        header(8) = 0
        header(9) = 0
        header(10) = 14 + 40
        header(11) = 0
        header(12) = 0
        header(13) = 0
        stdio.fwrite(addr(header), 1, 14, fd)

        // Write the DIB header.
        dib_header(0) = 40    // size (4 bytes)
        dib_header(1) = 0
        dib_header(2) = 0
        dib_header(3) = 0
        dib_header(4) = (width >>  0) & 0xFF     // width (4 bytes)
        dib_header(5) = (width >>  8) & 0xFF
        dib_header(6) = (width >> 16) & 0xFF
        dib_header(7) = (width >> 24) & 0xFF
        dib_header(8)  = (height >>  0) & 0xFF // height (4 bytes)
        dib_header(9)  = (height >>  8) & 0xFF
        dib_header(10) = (height >> 16) & 0xFF
        dib_header(11) = (height >> 24) & 0xFF
        dib_header(12) = 1    // color planes (2 bytes)
        dib_header(13) = 0
        dib_header(14) = 24  // bits per pixel (2 bytes)
        dib_header(15) = 0
        dib_header(16) = 0    // compression (4 bytes)
        dib_header(17) = 0
        dib_header(18) = 0
        dib_header(19) = 0
        dib_header(20) = (image_size >>  0) & 0xFF    // image size (4 bytes)
        dib_header(21) = (image_size >>  8) & 0xFF    // image size (4 bytes)
        dib_header(22) = (image_size >> 16) & 0xFF    // image size (4 bytes)
        dib_header(23) = (image_size >> 24) & 0xFF    // image size (4 bytes)
        dib_header(24) = 0x13    // horizontal resolution (4 bytes)
        dib_header(25) = 0x0B
        dib_header(26) = 0
        dib_header(27) = 0
        dib_header(28) = 0x13    // vertical resolution (4 bytes)
        dib_header(29) = 0x0B
        dib_header(30) = 0
        dib_header(31) = 0
        dib_header(32) = 0        // color pallate size (4 bytes, default 0)
        dib_header(33) = 0
        dib_header(34) = 0
        dib_header(35) = 0
        dib_header(36) = 0        // number of important colors (4 bytes)
        dib_header(37) = 0
        dib_header(38) = 0
        dib_header(39) = 0
        stdio.fwrite(addr(dib_header), 1, 40, fd)

    } else {

        pixel = data_in        
        stdio.fputc((pixel >>  0) & 0xFF, fd)
        stdio.fputc((pixel >>  8) & 0xFF, fd)
        stdio.fputc((pixel >> 16) & 0xFF, fd)

        x += 1
        if (x == width) {

            // Pad row to a four-byte multiple.
            while ((x & 3) <> 0) {
                stdio.fputc(0, fd)
                x += 1
            }

            // Start the next row.
            x = 0
            y += 1
            if (y == height) {
                stdio.fclose(fd)
                stdio.exit(-1)
            }
        }

    }

}

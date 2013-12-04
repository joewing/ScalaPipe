
package blocks

import autopipe.dsl._

class BMPReader(file: String = "in.bmp") extends AutoPipeBlock {

   val data_out   = output(UNSIGNED32)
   val width_out  = output(SIGNED32)
   val height_out = output(SIGNED32)

   val fd = local(stdio.FILEPTR, 0)
   val header = local(new AutoPipeArray(UNSIGNED8, 14))
   val dib_header = local(new AutoPipeArray(UNSIGNED8, 40))
   val offset = local(UNSIGNED32)
   val width = local(SIGNED32)
   val height = local(SIGNED32)
   val x = local(SIGNED32, 0)
   val y = local(SIGNED32, 0)
   val pixel = local(UNSIGNED32)

   if (fd == 0) {

      // Open the file.
      fd = stdio.fopen(file, "rb")
      if (fd == 0) {
         stdio.printf("""ERROR: could not open %s\n""", file)
         stdio.exit(-1)
      }

      // Read the header.
      // Format:
      //    type (2 bytes: BM)
      //    size (4 bytes)
      //    reserved (4 bytes)
      //    offset (4 bytes)
      stdio.fread(addr(header), 1, 14, fd)
      if (header(0) <> 'B' || header(1) <> 'M') {
         stdio.fclose(fd)
         stdio.printf("""ERROR: invalid BMP header in %s\n""", file)
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
         stdio.printf("""ERROR: unknown DIB header in %s\n""", file)
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

      // Seek to the data.
      stdio.fseek(fd, offset, stdio.SEEK_SET)

   } else {

      // Read a pixel.
      pixel  = stdio.fgetc(fd)
      pixel |= stdio.fgetc(fd) << 8
      pixel |= stdio.fgetc(fd) << 16
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


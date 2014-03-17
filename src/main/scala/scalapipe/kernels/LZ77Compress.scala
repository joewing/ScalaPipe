package scalapipe.kernels

import scalapipe.dsl._

class LZ77Compress(
        val offsetBits: Int,    // Bits to make up the offset.
        val lengthBits: Int     // Bits to make up the run length.
    ) extends Kernel {

    val dictSize = 1 << offsetBits
    val DICT = Vector(UNSIGNED8, dictSize)
    val lengthMask = lengthBits - 1
    val dictMask = dictSize - 1
    val offsetShift = 8 + lengthBits

    val in = input(UNSIGNED16)      // Byte stream (0xFFFF at EOF).
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

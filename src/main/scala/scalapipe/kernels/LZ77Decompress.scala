package scalapipe.kernels

import scalapipe.dsl._

class LZ77Decompress(
        val offsetBits: Int,
        val lengthBits: Int
    ) extends Kernel {

    val dictSize = 1 << offsetBits
    val DICT = Vector(UNSIGNED8, dictSize)
    val dictMask = dictSize - 1
    val lengthMask = (1 << lengthBits) - 1
    val offsetShift = 8 + lengthBits

    val in = input(UNSIGNED32)
    val out = output(UNSIGNED16)

    val dictionary = local(DICT)
    val initialized = local(BOOL, false)
    val value = local(UNSIGNED32)
    val offset = local(UNSIGNED32)
    val length = local(UNSIGNED32)

    if (!initialized) {
        for (x <- 0 until dictSize) {
            dictionary(x) = x
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
    for (x <- 0 until length) {
        out = dictionary(offset)
        offset = (offset + 1 & dictMask)
    }

    // Output the new character.
    value &= 0xFF
    out = value

    // Update the dictionary.
    dictionary(offset) = value

}

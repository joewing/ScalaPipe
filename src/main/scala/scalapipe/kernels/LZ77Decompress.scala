package scalapipe.kernels

import scalapipe.dsl._

class LZ77Decompress(
        val offsetBits: Int,
        val lengthBits: Int
    ) extends Kernel {

    val dictSize = 1 << offsetBits
    val DICT = Vector(UNSIGNED8, dictSize)
    val dictMask = dictSize - 1
    val lengthMask = lengthBits - 1
    val offsetShift = 8 + lengthBits

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

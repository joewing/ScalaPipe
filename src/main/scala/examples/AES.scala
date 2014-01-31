package examples

import scala.annotation.tailrec
import scala.collection.immutable.HashMap

import scalapipe._
import scalapipe.dsl._
import scalapipe.kernels._

object AES extends App {

    val keyBits = 128
    val rounds = 10

    val STATE = new Vector(UNSIGNED8, 16)

    @tailrec
    def power(n: Int, i: Int = 0, x: Int = 1): Int = {
        if (i < n) {
            val y = x << 1
            if ((x & 0x80) != 0) {
                return power(n, i + 1, (x ^ y ^ 0x1B) & 0xFF)
            } else {
                return power(n, i + 1, (x ^ y) & 0xFF)
            }
        } else {
            return x
        }
    }

    val powers = Array.tabulate(256) { i => power(i) }
    val logs = HashMap(powers.zipWithIndex: _*) + ((0, 0))

    def sbox(i: Int): Int = {
        if (i == 0x00) {
            return 0x63
        } else {
            var x = powers(255 - logs(i))
            var y = x
                      y = ((y << 1) | (y >> 7)) & 0xFF
            x ^= y; y = ((y << 1) | (y >> 7)) & 0xFF
            x ^= y; y = ((y << 1) | (y >> 7)) & 0xFF
            x ^= y; y = ((y << 1) | (y >> 7)) & 0xFF
            x ^= y ^ 0x63
            return x
        }
    }

    val rcon = Array(0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40,
                          0x80, 0x1B, 0x36, 0x6C, 0xD8, 0xAB)

    def getSchedule(key: Seq[Int]): Seq[Int] = {
        val result = Array.fill[Int](16 * 11)(0)
        for (i <- 0 until 16) {
            result(i) = key(i)
        }
        for (i <- 16 until 16 * 11 by 4) {
            val a = result(i - 4)
            val b = result(i - 3)
            val c = result(i - 2)
            val d = result(i - 1)
            if ((i & 15) == 0) {
                result(i + 0) = result(i - 16) ^ sbox(b) ^ rcon(i / 16)
                result(i + 1) = result(i - 15) ^ sbox(c)
                result(i + 2) = result(i - 14) ^ sbox(d)
                result(i + 3) = result(i - 13) ^ sbox(a)
            } else {
                result(i + 0) = result(i - 16) ^ a
                result(i + 1) = result(i - 15) ^ b
                result(i + 2) = result(i - 14) ^ c
                result(i + 3) = result(i - 13) ^ d
            }
        }
        result
    }

    val isbox = HashMap(Array.tabulate(256)(sbox).zipWithIndex: _*)

    val xtime = new Func {
        val x = input(UNSIGNED8)
        returns(UNSIGNED8)

        if ((x & 0x80) <> 0) {
            return (x << 1) ^ 0x1B
        } else {
            return x << 1
        }

    }

    val SetKey = new Kernel {
        val key = 0 to 15
        val outs = Array.tabulate(11)(i => output(STATE))
        val state = local(STATE)
        val schedule = getSchedule(key)
        for (k <- 0 until 11) {
            for (i <- 0 until 16) {
                state(i) = schedule(k * 16 + i)
            }
            val out = outs(k)
            out = state
        }
        stop
    }

    val AddRoundKey = new Kernel {

        val sin    = input(STATE)
        val kin    = input(STATE)
        val out    = output(STATE)

        val state = local(STATE)
        val key    = local(STATE)
        val first = local(BOOL, true)

        if (first) {
            first = false
            key = kin
        }
        state = sin
        for (i <- 0 until 16) {
            state(i) ^= key(i)
        }
        out = state

    }

    val SubBytes = new Kernel {

        val in  = input(STATE)
        val out = output(STATE)

        val i = local(UNSIGNED32)
        val state = local(STATE)
        val temp = local(UNSIGNED8)

        i = 0
        state = in
        while (i < 16) {
            switch (state(i)) {
                for (x <- 0 until 256) {
                    when (x) {
                        temp = sbox(x)
                    }
                }
            }
            state(i) = temp
            i += 1
        }
        out = state

    }

    val InvSubBytes = new Kernel {

        val in  = input(STATE)
        val out = output(STATE)

        val i = local(UNSIGNED32)
        val state = local(STATE)
        val temp = local(UNSIGNED8)

        i = 0
        state = in
        while (i < 16) {
            switch (state(i)) {
                for (x <- 0 until 256) {
                    when (x) {
                        temp = isbox(x)
                    }
                }
            }
            state(i) = temp
            i += 1
        }
        out = state

    }

    val ShiftRows = new Kernel {

        val in = input(STATE)
        val out = output(STATE)

        val state = local(STATE)
        val temp = local(STATE)

        state = in
        temp(0)  = state(0)
        temp(4)  = state(4)
        temp(8)  = state(8)
        temp(12) = state(12)
        temp(1)  = state(5)
        temp(5)  = state(9)
        temp(9)  = state(13)
        temp(13) = state(1)
        temp(10) = state(2)
        temp(2)  = state(10)
        temp(14) = state(6)
        temp(6)  = state(14)
        temp(3)  = state(15)
        temp(15) = state(11)
        temp(11) = state(7)
        temp(7)  = state(3)
        out = temp

    }

    val InvShiftRows = new Kernel {

        val in = input(STATE)
        val out = output(STATE)

        val state = local(STATE)
        val temp = local(STATE)

        state = in
        temp(0)  = state(0)
        temp(4)  = state(4)
        temp(8)  = state(8)
        temp(12) = state(12)
        temp(13) = state(9)
        temp(9)  = state(5)
        temp(5)  = state(1)
        temp(1)  = state(13)
        temp(2)  = state(10)
        temp(10) = state(2)
        temp(14) = state(6)
        temp(6)  = state(14)
        temp(3)  = state(7)
        temp(7)  = state(11)
        temp(11) = state(15)
        temp(15) = state(3)
        out = state

    }

    val MixColumns = new Kernel {

        val in = input(STATE)
        val out = output(STATE)

        val state = local(STATE)
        val i = local(UNSIGNED8)
        val a = local(UNSIGNED8)
        val b = local(UNSIGNED8)
        val c = local(UNSIGNED8)
        val d = local(UNSIGNED8)

        state = in
        i = 0
        while (i < 16) {
            a = state(i + 0)
            b = state(i + 1)
            c = state(i + 2)
            d = state(i + 3)
            state(i + 0) = xtime(a ^ b) ^ b ^ c ^ d
            state(i + 1) = xtime(b ^ c) ^ a ^ c ^ d
            state(i + 2) = xtime(c ^ d) ^ a ^ b ^ d
            state(i + 3) = xtime(a ^ d) ^ a ^ b ^ c
            i += 4
        }
        out = state

    }

    val InvMixColumns = new Kernel {

        val in = input(STATE)
        val out = output(STATE)

        val state = local(STATE)
        val i = local(UNSIGNED8)
        val a = local(UNSIGNED8)
        val b = local(UNSIGNED8)
        val c = local(UNSIGNED8)
        val d = local(UNSIGNED8)
        val x = local(UNSIGNED8)

        state = in
        i = 0
        while (i < 16) {
            a = state(i + 0)
            b = state(i + 1)
            c = state(i + 2)
            d = state(i + 3)
            x = xtime(a ^ b ^ c ^ d)
            state(i + 0) = xtime(xtime(x ^ a ^ b) ^ a ^ b) ^ b ^ c ^ d
            state(i + 1) = xtime(xtime(x ^ b ^ d) ^ b ^ c) ^ a ^ c ^ d
            state(i + 2) = xtime(xtime(x ^ a ^ c) ^ c ^ d) ^ a ^ b ^ d
            state(i + 3) = xtime(xtime(x ^ b ^ d) ^ a ^ d) ^ a ^ b ^ c
            i += 4
        }
        out = state

    }

    val Source = new Kernel {

        val out = output(STATE)
        val state = local(STATE)

        val x = local(UNSIGNED32, 0)

        while (x < 1024 * 1024) {
            for (i <- 0 until 16) {
                state(i) = (i << 4) | i
            }
            out = state
            x += 1
        }
        stop

    }

    val Print = new Kernel {
        val in = input(STATE)
        val state = local(STATE)
        val x = local(UNSIGNED32, 0)

        state = in
        for (i <- 0 until 16) {
            stdio.printf("""%02x """, state(i))
        }
        stdio.printf("""\n""")
        while (x < 1024 * 1024 - 1) {
            state = in
            x += 1
        }
        stdio.exit(0)
    }

    val app = new Application {

        param('queueDepth, 4)
        param('profile, false)

        def crypt(data: Stream, keySchedule: StreamList): Stream = {
            def pipe(n: Int, data: Stream): Stream = {
                if (n < 10) {
                    val sub = SubBytes(data)
                    val shift = ShiftRows(sub)
                    val mix = MixColumns(shift)
                    val add = AddRoundKey(mix, keySchedule(n))
                    pipe(n + 1, add)
                } else {
                    data
                }
            }
            var rk = AddRoundKey(data, keySchedule(0))
            val last = pipe(1, rk)
            val sub = SubBytes(last)
            val shift = ShiftRows(sub)
            AddRoundKey(shift, keySchedule(10))
        }

        def decrypt(data: Stream, keySchedule: StreamList): Stream = {
            def pipe(n: Int, data: Stream): Stream = {
                if (n >= 1) {
                    val shift = InvShiftRows(data)
                    val sub = InvSubBytes(shift)
                    val add = AddRoundKey(sub, keySchedule(n))
                    val mix = InvMixColumns(add)
                    pipe(n - 1, mix)
                } else {
                    data
                }
            }
            val rk = AddRoundKey(data, keySchedule(10))
            val last = pipe(9, rk)
            val shift = InvShiftRows(last)
            val sub = InvSubBytes(shift)
            AddRoundKey(sub, keySchedule(0))
        }

        val schedule = SetKey()
        val data = Source()
        val encrypted = crypt(data, schedule)
        Print(encrypted)

/*
        map(Source -> ANY_KERNEL, CPU2FPGA())
        map(ANY_KERNEL -> Print, FPGA2CPU())
*/

    }
    app.emit("aes")

}

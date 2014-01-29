package scalapipe.kernels

import scalapipe._
import scalapipe.dsl._

object ZigguratNormal extends Kernel {

    val in  = input(UNSIGNED32)
    val out = output(SIGNED32)

    val state = local(SIGNED8, -1)
    val hz = local(SIGNED32)
    val iz = local(UNSIGNED64)
    val kn = local(new Vector(UNSIGNED64, 128))
    val wn = local(new Vector(FLOAT32, 128))
    val fn = local(new Vector(FLOAT32, 128))
    val u = local(new Vector(FLOAT32, 2))
    val x = local(FLOAT32)
    val y = local(FLOAT32)

    val r = 3.442620
    val uni = 0.2328306e-9
    val p = 268435456.0

    switch(state) {

        when(-1) {
            val m1 = 2147483648.0
            val vn = local(FLOAT32, 9.91256303526217e-3)
            val dn = local(FLOAT32, 3.442619855899)
            val tn = local(FLOAT32, 3.442619855899)
            val q  = local(FLOAT32)
            val i  = local(UNSIGNED32)
            q = vn / exp(dn * dn * -0.5)
            kn(0) = cast((dn / q) * m1, UNSIGNED64)
            kn(1) = 0

            wn(0) = q / m1
            wn(127) = dn / m1

            fn(0) = 1.0
            fn(127) = dn / m1 //d->fn[127] = exp(-0.5 * dn * dn);

            i = 126
            while (i >= 1) {
                dn = sqrt(log(vn / dn + exp(dn * dn * -0.5)) * -2.0)
                kn(i + 1) = cast((dn / tn) * m1, UNSIGNED64)
                tn = dn
                fn(i) = exp(dn * dn * -0.5)
                wn(i) = dn / m1
                i -= 1
            }
            state = 0
        }

        when(0) {
            hz = in //ival
            iz = hz & 0x7F
            if (abs(hz) < kn(iz)) {
                out = cast(hz * wn(iz) * p, SIGNED32)
            } else {
                if (iz == 0) {
                    state = 1
                } else {
                    x = hz * wn(iz)
                    state = 3
                }
            }
        }

        when(1) {
            u(0) = 0.5 + in * uni
            state = 2
        }

        when(2) {
            u(1) = 0.5 + in * uni
            x = -log(u(0)) / r
            y = -log(u(1))
            if (y + y >= x * x) {
                if (hz > 0) {
                    out = cast((x + r) * p, SIGNED32)
                } else {
                    out = cast((-x - r) * p, SIGNED32)
                }
                state = 0
            } else {
                state = 1
            }
        }
when(3) {
            u(0) = 0.5 + in * uni
            if (fn(iz) + u(0) * (fn(iz - 1) - fn(iz)) < exp(x * x * -0.5)) {
                out = cast(x * p, SIGNED32)
            }
            state = 0
        }

        others {
            state = 0
        }

    }

}

package examples

import scalapipe.kernels._
import scalapipe._
import scalapipe.dsl._

object NBody {

    // Total FLOPs: 18n^2 + 6n
    // For 1000: 18006000 FLOPs per frame.
    def main(args: Array[String]) {

        val maxParticles = 10000
        val useX = true
        val hw = true

        val VTYPE = FLOAT32
        val PARTICLE = new Struct {
            val x = VTYPE
            val y = VTYPE
            val z = VTYPE
            val mass = VTYPE
            val vx = VTYPE
            val vy = VTYPE
            val vz = VTYPE
        }
        val POINT = new Struct {
            val x = VTYPE
            val y = VTYPE
            val z = VTYPE
            val mass = VTYPE
        }

        val gravity = 0.0000000000667428

        // Compute the acceleration of a particle by streaming
        // all particles past this block.
        // 15n^2 FLOPs
        val Force = new Kernel("Force") {

            val pin = input(PARTICLE)
            val oin = input(POINT)
            val out = output(POINT)

            val p = local(PARTICLE)
            val other = local(POINT)
            val forces = local(POINT)
            val rsq = local(VTYPE)
            val mult = local(VTYPE)
            val dx = local(VTYPE)
            val dy = local(VTYPE)
            val dz = local(VTYPE)

            // Read the other particle.
            other = oin

            // If the mass of the other particle is less than zero, we
            // start processing a new particle.
            if (other.mass == -1) {
                p = pin
                out = other
                forces.mass = 0
            } else {
                if (other.x <> p.x || other.y <> p.y || other.z <> p.z) {
                    dx = other.x - p.x
                    dy = other.y - p.y
                    dz = other.z - p.z
                    rsq = dx * dx + dy * dy + dz * dz
                    mult = (gravity * other.mass) / (rsq * sqrt(rsq))
                    forces.x = dx * mult
                    forces.y = dy * mult
                    forces.z = dz * mult
                    out = forces
                }
            }

        }

        // Accumulate forces on a particle.
        // The result will be the total acceleration.
        // The result is sent when a negative command is received.
        // 3n^2 FLOPs
        val Accumulate = new Kernel("Accumulate") {

            val fin     = input(POINT)
            val fout    = output(POINT)

            val f       = local(POINT)
            val sum     = local(POINT)
            val first   = local(BOOL, 1)

            f = fin
            if (f.mass == -1) {
                if (first == 0) {
                    fout = sum
                }
                sum.x = 0
                sum.y = 0
                sum.z = 0
                first = 1
            } else {
                sum.x += f.x
                sum.y += f.y
                sum.z += f.z
                first = 0
            }

        }

        // Update a particle's state.
        // 6n FLOPs
        val Update = new Kernel("Update") {

            val pin   = input(PARTICLE)  // The particle to update.
            val fin   = input(POINT)     // The combined force on the particle.
            val pout1 = output(PARTICLE) // The updated particle.
            val pout2 = output(PARTICLE) // Another copy.

            val p = local(PARTICLE)
            val f = local(POINT)

            p = pin
            if (p.mass == -1) {
                pout1 = p
                pout2 = p
            } else {

                f = fin

                // Update position.
                p.x += p.vx
                p.y += p.vy
                p.z += p.vz

                // Update velocity.
                p.vx += f.x
                p.vy += f.y
                p.vz += f.z

                pout1 = p
                pout2 = p

            }

        }

        val Source = new Kernel("Source") {

            val pout = output(PARTICLE)

            val filename = config(STRING, 'filename, "particles.txt")

            val temp    = local(PARTICLE)
            val line    = local(Vector(FLOAT64, 7))
            val fd      = local(stdio.FILEPTR, 0)
            val rc      = local(SIGNED32)
            val count   = local(UNSIGNED32, 0)

            // Open the file.
            if (fd == 0) {
                fd = stdio.fopen(filename, "r")
                if (fd == 0) {
                    stdio.printf("Could not open %s\n", filename)
                    stdio.exit(-1)
                }
            }

            // Read the particle.
            rc = stdio.fscanf(fd, " %lf %lf %lf %lf %lf %lf %lf",
                              addr(line(0)), addr(line(1)), addr(line(2)),
                              addr(line(3)), addr(line(4)), addr(line(5)),
                              addr(line(6)))
            if (rc == 7 && count < maxParticles) {
                count += 1
                temp.mass   = line(0)
                temp.x      = line(1)
                temp.y      = line(2)
                temp.z      = line(3)
                temp.vx     = line(4)
                temp.vy     = line(5)
                temp.vz     = line(6)
                pout = temp
            } else {
                stdio.printf("Loaded %u particles\n", count)
                temp.mass = -1
                pout = temp
                stdio.fclose(fd)
                stop
            }

        }


        val Buffer = new Kernel("Buffer") {

            val fin = input(PARTICLE)
            val lin = input(PARTICLE)
            val pout = output(PARTICLE)

            val particles   = local(Vector(PARTICLE, maxParticles))
            val temp        = local(PARTICLE)
            val updateIndex = local(UNSIGNED32, 0)
            val sentIndex   = local(UNSIGNED32, 0)
            val count       = local(UNSIGNED32, 0)
            val loaded      = local(BOOL, false)

            if (!loaded) {
                temp = fin
                if (temp.mass == -1) {
                    loaded = true
                } else {
                    particles(count) = temp
                    count += 1
                }
            } else {
                if (sentIndex <> count) {
                    pout = particles(sentIndex)
                    sentIndex += 1
                    if (sentIndex == count) {
                        temp.mass = -1
                        pout = temp
                    }
                }
                if (updateIndex <> sentIndex) {
                    if (avail(lin)) {
                        temp = lin
                        if (temp.mass <> -1) {
                            particles(updateIndex) = temp
                            updateIndex += 1
                        }
                    }
                }
                if (updateIndex == count && sentIndex == count) {
                    temp = lin
                    updateIndex = 0
                    sentIndex = 0
                }
            }

        }

        val Streamer = new Kernel("Streamer") {

            val pin  = input(PARTICLE)
            val pout1 = output(PARTICLE)
            val pout2 = output(PARTICLE)
            val oout = output(POINT)

            val particles   = local(Vector(PARTICLE, maxParticles))
            val temp        = local(PARTICLE)
            val other       = local(POINT)
            val count       = local(UNSIGNED32, 0)
            val i           = local(UNSIGNED32, 0)
            val j           = local(UNSIGNED32, 0)
            val load        = local(BOOL, true)

            if (load) {
                temp = pin
                if (temp.mass == -1) {
                    load = false
                } else {
                    particles(count) = temp
                    count += 1
                }
            } else {
                if (i == 0) {
                    other.mass = -1
                    oout = other
                    temp = particles(j)
                    pout1 = temp
                    pout2 = temp
                    j += 1;
                }
                temp = particles(i)
                other.x = temp.x
                other.y = temp.y
                other.z = temp.z
                other.mass = temp.mass
                oout = other
                i += 1
                if (i == count) {
                    i = 0
                    if (j == count) {
                        other.mass = -1
                        oout = other
                        temp.mass = -1
                        pout1 = temp
                        pout2 = temp
                        j = 0
                        load = true
                        count = 0
                    }
                }
            }

        }

        val GetTime = new Func {
            val tv = local(stdio.TIMEVAL)
            val result = local(UNSIGNED64)
            stdio.gettimeofday(addr(tv), 0)
            result = UNSIGNED64(tv.tv_sec) * 1000000
            result += UNSIGNED64(tv.tv_usec)
            return result
        }

        val PrintText = new Kernel("Print") {

            val pin = input(PARTICLE)

            val p = local(PARTICLE)
            val i = local(SIGNED32)
            val j = local(SIGNED32, 0)
            val lastTime = local(UNSIGNED64, 0)
            val currentTime = local(UNSIGNED64)

            if (lastTime == 0) {
                lastTime = GetTime()
            }

            p = pin
            if (p.mass < 0) {
                currentTime = GetTime()
                stdio.printf("Frame time: %lu us\n",
                             currentTime - lastTime)
                lastTime = currentTime
                i = 0
                j += 1
            } else {
                i += 1
            }

        }

        val PrintX = new Kernel("Print") {

            val pin = input(PARTICLE)

            val p = local(PARTICLE)
            val i = local(SIGNED32)
            val j = local(SIGNED32, 0)
            val x = local(SIGNED32)
            val y = local(SIGNED32)
            val lastTime = local(UNSIGNED64)
            val currentTime = local(UNSIGNED64)

            val width = 640
            val height = 480
            val display = local(xlib.DISPLAYPTR, 0)
            val root = local(xlib.XID)
            val w = local(xlib.XID)
            val gc = local(xlib.GC)
            val event = local(xlib.XEVENT)
            val mapped = local(BOOL, false)

            if (display == 0) {
                display = xlib.XOpenDisplay(0)
                if (display == 0) {
                    stdio.printf("Could not open display\n")
                    stdio.exit(-1)
                }
                root = xlib.XDefaultRootWindow(display)
                w = xlib.XCreateSimpleWindow(display, root, 0, 0,
                                             width, height, 0, 0, 0)
                xlib.XSelectInput(display, w, xlib.ExposureMask)
                xlib.XMapWindow(display, w)
                while (!mapped) {
                    xlib.XNextEvent(display, addr(event))
                    if (event('type) == xlib.Expose) {
                        mapped = true
                    }
                }
                gc = xlib.XCreateGC(display, w, 0, 0)
                xlib.XSetForeground(display, gc, 0xFFFFFF)
                lastTime = GetTime()
            }

            p = pin
            if (p.mass < 0) {
                currentTime = GetTime()
                stdio.printf("Frame time: %lu us\n",
                             currentTime - lastTime)
                lastTime = currentTime
                i = 0
                j += 1
                while (xlib.XPending(display) > 0) {
                    xlib.XNextEvent(display, addr(event))
                }
                xlib.XClearWindow(display, w)
            } else {
                x = width / 2 + p.x
                y = height / 2 + p.y
                xlib.XDrawPoint(display, w, gc, x, y)
                i += 1
            }

        }

        val Print = if (useX) PrintX else PrintText

        val app = new Application {

            param('fpga, "Saturn")
            param('bram, false)

            val cycle = Cycle()
            val source = Source()
            val buffer = Buffer(source(0), cycle)
            val stream = Streamer(buffer(0))
            val force = Accumulate(Force(stream(0), stream(2)))
            val updated = Update(stream(1), force(0))
            cycle(updated(0))
            Print(updated(1))

            if (hw) {
                map(Source -> ANY_KERNEL, CPU2FPGA())
                map(ANY_KERNEL -> Print, FPGA2CPU())
            }

        }
        app.emit("nbody")

    }

}

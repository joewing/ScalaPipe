
package examples

import blocks._
import autopipe._
import autopipe.dsl._

object NBody {

   // Total FLOPs: 18n^2 + 6n
   // For 1000: 18006000 FLOPs per frame.
   def main(args: Array[String]) {

      val maxParticles = 1000
      val useX = false
      val hw = false

      val VTYPE = FLOAT32
      val PARTICLE = new AutoPipeArray(VTYPE, 7)   // x, y, z, m, vx, vy, vz
      val POINT = new AutoPipeArray(VTYPE, 4)      // x, y, z, command

      val gravity = 0.0000000000667428

      // Compute the acceleration of a particle by streaming
      // all particles past this block.
      // 15n^2 FLOPs
      val Force = new AutoPipeBlock("Force") {

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
         if (other(3) < 0) {
            p = pin
            out = other
            forces(3) = 0
         } else {
            if (other(0) <> p(0) || other(1) <> p(1) || other(2) <> p(2)) {
               dx = other(0) - p(0)                               // 1
               dy = other(1) - p(1)                               // 1
               dz = other(2) - p(2)                               // 1
               rsq = dx * dx + dy * dy + dz * dz                  // 5
               mult = (gravity * other(3)) / (rsq * sqrt(rsq))    // 4
               forces(0) = dx * mult                              // 1
               forces(1) = dy * mult                              // 1
               forces(2) = dz * mult                              // 1
               out = forces
            }
         }

      }

      val MDForce = new AutoPipeBlock("MDForce") {

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
         val sr2 = local(VTYPE)
         val sr6 = local(VTYPE)

         // Cutoff force.
         val cutoff = 100

         // Read the other particle.
         other = oin

         // If the mass of the other particle is less than zero, we
         // start processing a new particle.
         if (other(3) < 0) {
            p = pin
            out = other
            forces(3) = 0
         } else {
            if (other(0) <> p(0) || other(1) <> p(1) || other(2) <> p(2)) {
               // Lennard-Jones system
               dx = other(0) - p(0)
               dy = other(1) - p(1)
               dz = other(2) - p(2)
               rsq = dx * dx + dy * dy + dz * dz
               if (rsq < cutoff * cutoff) {
                  sr2 = 1.0 / rsq
                  sr6 = sr2 * sr2 * sr2
                  mult = sr6 * (sr6 - 0.5) * sr2
                  forces(0) = dx * mult
                  forces(1) = dy * mult
                  forces(2) = dz * mult
                  out = forces
               }
            }
         }

      }

      // Accumulate forces on a particle.
      // The result will be the total acceleration.
      // The result is sent when a negative command is received.
      // 3n^2 FLOPs
      val Accumulate = new AutoPipeBlock("Accumulate") {

         val fin  = input(POINT)
         val fout = output(POINT)

         val f     = local(POINT)
         val sum   = local(POINT)
         val count = local(UNSIGNED32, 0)

         f = fin
         if (f(3) < 0) {
            if (count > 0) {
               fout = sum
            }
            sum(0) = 0
            sum(1) = 0
            sum(2) = 0
            count = 0
         } else {
            sum(0) += f(0)
            sum(1) += f(1)
            sum(2) += f(2)
            count += 1
         }

      }

      // Update a particle's state.
      // 6n FLOPs
      val Update = new AutoPipeBlock("Update") {

         val pin  = input(PARTICLE)    // The particle to update.
         val fin  = input(POINT)       // The combined force on the particle.
         val pout = output(PARTICLE)   // The updated particle.

         val p = local(PARTICLE)
         val f = local(POINT)

         p = pin
         if (p(3) < 0) {
            pout = p
         } else {

            f = fin

            // Update position.
            p(0) += p(4)
            p(1) += p(5)
            p(2) += p(6)

            // Update velocity.
            p(4) += f(0)
            p(5) += f(1)
            p(6) += f(2)

            pout = p

         }

      }

      val Source = new AutoPipeBlock("Source") {

         val pout = output(PARTICLE)

         val filename = config(STRING, 'filename, "particles.txt")

         val temp = local(PARTICLE)
         val line = local(new AutoPipeArray(FLOAT64, 7))
         val fd   = local(stdio.FILEPTR, 0)
         val rc   = local(SIGNED32)
         val count = local(UNSIGNED32, 0)

         // Open the file.
         if (fd == 0) {
            fd = stdio.fopen(filename, "r")
            if (fd == 0) {
               stdio.printf("""Could not open %s\n""", filename)
               stdio.exit(-1)
            }
         }

         // Read the particle.
         rc = stdio.fscanf(fd, """ %lf %lf %lf %lf %lf %lf %lf""",
                           addr(line(0)), addr(line(1)), addr(line(2)),
                           addr(line(3)), addr(line(4)), addr(line(5)),
                           addr(line(6)))
         if (rc == 7 || count == maxParticles - 1) {
            count += 1
            temp(3) = line(0)
            temp(0) = line(1)
            temp(1) = line(2)
            temp(2) = line(3)
            temp(4) = line(4)
            temp(5) = line(5)
            temp(6) = line(6)
            pout = temp
         } else {
            stdio.printf("""Loaded %u particles\n""", count)
            temp(3) = -1
            pout = temp
            stdio.fclose(fd)
            stop
         }

      }

      val Loop = new AutoPipeLoopBack(PARTICLE)

      val Buffer = new AutoPipeBlock("Buffer") {

         val fin = input(PARTICLE)
         val lin = input(PARTICLE)
         val pout = output(PARTICLE)

         val arrayType = new AutoPipeArray(PARTICLE, maxParticles)
         val particles = local(arrayType)
         val temp = local(PARTICLE)
         val updateIndex = local(UNSIGNED32, 0)
         val sentIndex = local(UNSIGNED32, 0)
         val count = local(UNSIGNED32, 0)
         val loaded = local(BOOL, false)

         if (!loaded) {
            temp = fin
            if (temp(3) < 0) {
               loaded = true
            } else {
               particles(count) = temp
               count += 1
            }
         } else {
            if (sentIndex < count) {
               pout = particles(sentIndex)
               sentIndex += 1
               if (sentIndex == count) {
                  temp(3) = -1
                  pout = temp
               }
            }
            if (updateIndex < sentIndex) {
               if (avail(lin)) {
                  temp = lin
                  if (temp(3) >= 0) {
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

      val Streamer = new AutoPipeBlock("Streamer") {

         val pin  = input(PARTICLE)
         val pout = output(PARTICLE)
         val oout = output(POINT)

         val arrayType = new AutoPipeArray(PARTICLE, maxParticles)
         val particles = local(arrayType)
         val temp = local(PARTICLE)
         val other = local(POINT)
         val count = local(UNSIGNED32, 0)
         val i = local(UNSIGNED32, 0)
         val j = local(UNSIGNED32, 0)
         val load = local(BOOL, true)

         if (load) {
            temp = pin
            if (temp(3) < 0) {
               load = false
            } else {
               particles(count) = temp
               count += 1
            }
         } else {
            if (i == 0) {
               other(3) = -1
               oout = other
               pout = particles(j)
               j += 1;
            }
            temp = particles(i)
            other(0) = temp(0)
            other(1) = temp(1)
            other(2) = temp(2)
            other(3) = temp(3)
            oout = other
            i += 1
            if (i == count) {
               i = 0
               if (j == count) {
                  other(3) = -1
                  oout = other
                  temp(3) = -1
                  pout = temp
                  j = 0
                  load = true
                  count = 0
               }
            }
         }

      }

      val PrintText = new AutoPipeBlock("Print") {

         val pin = input(PARTICLE)

         val p = local(PARTICLE)
         val i = local(SIGNED32)
         val j = local(SIGNED32, 0)
         val lastTime = local(UNSIGNED64, 0)
         val currentTime = local(UNSIGNED64)

         def getTime() = {
            val tv = local(stdio.TIMEVAL)
            val result = local(UNSIGNED64)
            stdio.gettimeofday(addr(tv), 0)
            result = cast(tv('tv_sec), UNSIGNED64) * 1000000
            result += cast(tv('tv_usec), UNSIGNED64)
            result
         }

         if (lastTime == 0) {
            lastTime = getTime()
         }

         p = pin
         if (p(3) < 0) {
            currentTime = getTime()
            stdio.printf("""Frame time: %lu us\n""",
                         currentTime - lastTime)
            lastTime = currentTime
            i = 0
            j += 1
         } else {
/*
            stdio.printf("""%d %d %lg (%lg %lg %lg) (%lg %lg %lg)\n""", j, i,
                         cast(p(3), FLOAT64), cast(p(0), FLOAT64),
                         cast(p(1), FLOAT64), cast(p(2), FLOAT64),
                         cast(p(4), FLOAT64), cast(p(5), FLOAT64),
                         cast(p(6), FLOAT64))
*/
            i += 1
         }

      }

      val PrintX = new AutoPipeBlock("Print") {

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

         def getTime() = {
            val tv = local(stdio.TIMEVAL)
            val result = local(UNSIGNED64)
            stdio.gettimeofday(addr(tv), 0)
            result = cast(tv('tv_sec), UNSIGNED64) * 1000000
            result += cast(tv('tv_usec), UNSIGNED64)
            result
         }

         if (display == 0) {
            display = xlib.XOpenDisplay(0)
            if (display == 0) {
               stdio.printf("""Could not open display\n""")
               stdio.exit(-1)
            }
            root = xlib.XDefaultRootWindow(display)
            w = xlib.XCreateSimpleWindow(display, root, 0, 0, width, height,
                                         0, 0, 0)
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
            lastTime = getTime()
         }

         p = pin
         if (p(3) < 0) {
            currentTime = getTime()
            stdio.printf("""Frame time: %lu us\n""",
                         currentTime - lastTime)
            lastTime = currentTime
            i = 0
            j += 1
            while (xlib.XPending(display) > 0) {
               xlib.XNextEvent(display, addr(event))
            }
            xlib.XClearWindow(display, w)
         } else {
/*
            stdio.printf("""%d %d %lg (%lg %lg %lg) (%lg %lg %lg)\n""", j, i,
                         cast(p(3), FLOAT64), cast(p(0), FLOAT64),
                         cast(p(1), FLOAT64), cast(p(2), FLOAT64),
                         cast(p(4), FLOAT64), cast(p(5), FLOAT64),
                         cast(p(6), FLOAT64))
*/
            x = width / 2 + p(0)
            y = height / 2 + p(1)
            xlib.XDrawPoint(display, w, gc, x, y)
            i += 1
         }

      }

      val Print = if (useX) PrintX else PrintText
      val Dup = new DuplicateBlock(PARTICLE, 2)

      val app = new AutoPipeApp {

//         param('queueDepth, 4)
//         param('share, 1)
//         param('fpga, "SmartFusion")
         param('fpga, "Simulation")
         param('trace, true)

         val source = Source()
         val buffer = Buffer(source(0), Loop.output())
         val stream = Streamer(buffer(0))

         val oldParticle = Dup(stream(0))
         val force = Accumulate(Force(oldParticle(0), stream(1)))
         val newParticle = Update(oldParticle(1), force(0))()
         val updated = Dup(newParticle)

         Print(updated(0))
         Loop.input(updated(1))

         if (hw) {
            map(ANY_BLOCK -> Force, CPU2FPGA());
            map(Force -> ANY_BLOCK, FPGA2CPU());
         }


      }
      app.emit("nbody")

   }

}


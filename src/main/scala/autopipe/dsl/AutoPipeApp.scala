
package autopipe.dsl

import language.implicitConversions
import autopipe._
import scala.collection.mutable.HashSet

/** ScalaPipe Application DSL. */
class AutoPipeApp {

    implicit val ap = new AutoPipe

    def iteratedFold(inputs: Array[Stream],
                     combiner: AutoPipeBlock): Stream = {

        val size = inputs.size
        if (size > 1) {
            val result = Array.fill[StreamList](size - 1)(null)
            for (i <- Array.range(0, size / 2)) {
                val first = 2 * i
                val second = first + 1
                val dest = size / 2 + i - 1
                val kernel = ap.createKernel(combiner)
                result(dest) = kernel((null, inputs(first)),
                                      (null, inputs(second)))
            }
            for (i <- Array.range(size / 2 - 1, 0, -1)) {
                val left = i * 2 - 1
                val right = left + 1
                val dest = i - 1
                val kernel = ap.createKernel(combiner)
                result(dest) = kernel((null, result(left)(0)),
                                      (null, result(right)(0)))
            }
            result(0)(0)
        } else {
            inputs(0)
        }

    }

    implicit def apb2Block(apb: AutoPipeBlock) = ap.createKernel(apb)

    implicit def kernel2StreamList(k: Kernel): StreamList = k.apply()

    implicit def kernel2Arg(k: Kernel) = (null, k.apply().apply())

    implicit def stream2Arg(s: Stream) = (null, s)

    implicit def streamList2Arg(sl: StreamList) = (null, sl.apply())

    implicit def streamList2Stream(sl: StreamList) = sl.apply()

    implicit def objToEdge[T <: Edge](e: EdgeObject[T]): T = e.apply()

    def measure(streamList: StreamList, stat: Symbol, metric: Symbol) {
        streamList.addMeasure(stat, metric)
    }

    def measure(streamList: StreamList, metric: Symbol) {
        measure(streamList, 'trace, metric)
    }

    def measure(stream: Stream, stat: Symbol, metric: Symbol) {
        stream.addMeasure(stat, metric)
    }

    def measure(stream: Stream, metric: Symbol) {
        measure(stream, 'trace, metric)
    }

    def measure(edge: (AutoPipeBlock, AutoPipeBlock),
                    stat: Symbol,
                    metric: Symbol) {
        ap.addMeasure(new EdgeMeasurement(edge._1, edge._2, stat, metric))
    }

    def measure(edge: (AutoPipeBlock, AutoPipeBlock), metric: Symbol) {
        measure(edge, 'trace, metric)
    }

    def map(streamList: StreamList, edge: Edge) {
        streamList.setEdge(edge)
    }

    def map(stream: Stream, edge: Edge) {
        stream.edge = edge
    }

    def map(edge: (AutoPipeBlock, AutoPipeBlock), t: Edge) {
        ap.addEdge(new EdgeMapping(edge._1, edge._2, t))
    }

    def param(name: Symbol, value: Any) {
        ap.setParameter(name, value)
    }

    def emit(dirname: String) {
        ap.emit(dirname)
    }

}


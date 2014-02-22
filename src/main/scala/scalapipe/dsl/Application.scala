package scalapipe.dsl

import language.implicitConversions
import scalapipe._

/** ScalaPipe Application DSL. */
class Application {

    implicit val sp = new ScalaPipe

    def iteratedFold(inputs: Array[Stream],
                     combiner: Kernel): Stream = {

        val size = inputs.size
        if (size > 1) {
            val result = Array.fill[StreamList](size - 1)(null)
            for (i <- Array.range(0, size / 2)) {
                val first = 2 * i
                val second = first + 1
                val dest = size / 2 + i - 1
                val kernel = sp.createInstance(combiner)
                result(dest) = kernel((null, inputs(first)),
                                      (null, inputs(second)))
            }
            for (i <- Array.range(size / 2 - 1, 0, -1)) {
                val left = i * 2 - 1
                val right = left + 1
                val dest = i - 1
                val kernel = sp.createInstance(combiner)
                result(dest) = kernel((null, result(left)(0)),
                                      (null, result(right)(0)))
            }
            result(0)(0)
        } else {
            inputs(0)
        }

    }

    def config(name: Symbol, default: Any) = new Config(name, default)

    implicit def kernel2instance(k: Kernel) = sp.createInstance(k)

    implicit def instance2StreamList(k: KernelInstance): StreamList = k.apply()

    implicit def instance2Arg(k: KernelInstance) = (null, k.apply().apply())

    implicit def stream2Arg(s: Stream) = (null, s)

    implicit def streamList2Arg(sl: StreamList) = (null, sl.apply())

    implicit def streamList2Stream(sl: StreamList) = sl.apply()

    implicit def cycle2Arg(cycle: Cycle) = (null, cycle.output(sp))

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

    def measure(edge: (Kernel, Kernel),
                    stat: Symbol,
                    metric: Symbol) {
        sp.addMeasure(new EdgeMeasurement(edge._1, edge._2, stat, metric))
    }

    def measure(edge: (Kernel, Kernel), metric: Symbol) {
        measure(edge, 'trace, metric)
    }

    def map(streamList: StreamList, edge: Edge) {
        streamList.setEdge(edge)
    }

    def map(stream: Stream, edge: Edge) {
        stream.edge = edge
    }

    def map(edge: (Kernel, Kernel), t: Edge) {
        sp.addEdge(new EdgeMapping(edge._1, edge._2, t))
    }

    def param(name: Symbol, value: Any = true) {
        sp.setParameter(name, value)
    }

    def param(name: Symbol) {
        param(name, true)
    }

    def param(edge: (Kernel, Kernel), name: Symbol, value: Any) {
        sp.addParameter(new EdgeParameter(edge._1, edge._2, name, value))
    }

    def param(edge: (Kernel, Kernel), name: Symbol) {
        param(edge, name, true)
    }

    def param(edge: Stream, name: Symbol, value: Any) {
        edge.addParameter(name, value)
    }

    def param(edge: Stream, name: Symbol) {
        param(edge, name, true)
    }

    def param(edge: StreamList, name: Symbol, value: Any) {
        edge.addParameter(name, value)
    }

    def param(edge: StreamList, name: Symbol) {
        param(edge, name, true)
    }

    def emit(dirname: String) {
        sp.emit(dirname)
    }

}

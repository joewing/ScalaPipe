package examples

import scalapipe.kernels._
import scalapipe._
import scalapipe.dsl._

object Cluster {

    def main(args: Array[String]) {

        val clusterCount = 2
        val iterations = 10

        val MAX_WIDTH = 1024
        val MAX_HEIGHT = 1024
        val BUFFER_SIZE = MAX_WIDTH * MAX_HEIGHT

        val ValueType = UNSIGNED32
        val ClusterType = new Vector(ValueType, clusterCount)

        val LAST_INDEX = -1

        /** Source of cluster data. */
        val ClusterSource = new Kernel("ClusterSource") {

            val cluster_in     = input(ClusterType)
            val cluster_out    = output(ClusterType)

            val cluster         = local(ClusterType)
            val i                 = local(SIGNED32)
            val initialized    = local(BOOL, false)

            if (initialized) {
                cluster = cluster_in
                cluster_out = cluster
            } else {
                initialized = true
                i = 0
                while (i < clusterCount) {
                    cluster(i) = i * (((1 << 24) / clusterCount) - 1)
                    i += 1
                }
                cluster_out = cluster
            }

        }

        /** Source of value data. */
        val ValueSource = new Kernel("ValueSource") {

            val value_in        = input(ValueType)
            val width_in        = input(SIGNED32)
            val height_in      = input(SIGNED32)

            val value_out      = output(ValueType)
            val width_out      = output(SIGNED32)
            val height_out     = output(SIGNED32)

            val initialized    = local(BOOL, false)
            val imageReady     = local(BOOL, false)
            val cluster         = local(ClusterType)
            val width            = local(SIGNED32, 0)
            val height          = local(SIGNED32)
            val x                 = local(SIGNED32, 0)
            val y                 = local(SIGNED32, 0)
            val buffer          = local(Vector(ValueType, BUFFER_SIZE))

            if (initialized && imageReady) {
                value_out = buffer(y * width + x)
                x += 1
                if (x == width) {
                    x = 0
                    y += 1
                    if (y == height) {
                        y = 0
                    }
                }
            } else if (!initialized) {
                initialized = true
                width = width_in
                height = height_in
                width_out = width
                height_out = height
            } else {
                buffer(y * width + x) = value_in
                x += 1
                if (x == width) {
                    x = 0
                    y += 1
                    if (y == height) {
                        imageReady = true
                        x = 0
                        y = 0
                    }
                }
            }

        }

        val Distance = new Func {

            val a = input(ValueType)
            val b = input(ValueType)

            val temp = local(SIGNED32)
            val sum  = local(SIGNED32)

            sum = 0
            temp = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)
            sum += temp * temp
            temp = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)
            sum += temp * temp
            temp = ((a >> 0) & 0xFF) - ((b >> 0) & 0xFF)
            sum += temp * temp

            return sum

        }

        /** Block to assign values to clusters. */
        val Assign = new Kernel("Assign") {

            val value_in    = input(ValueType)
            val width_in    = input(SIGNED32)
            val height_in  = input(SIGNED32)
            val cluster_in = input(ClusterType)

            val value_out  = output(ValueType)
            val index_out  = output(SIGNED32)
            val width_out  = output(SIGNED32)
            val height_out = output(SIGNED32)
            val result_out = output(ValueType)

            val i             = local(SIGNED32)
            val value        = local(ValueType)
            val cluster     = local(ClusterType)
            val assignment = local(SIGNED32)
            val dist         = local(ValueType)
            val temp         = local(ValueType)
            val width        = local(SIGNED32, 0)
            val height      = local(SIGNED32)
            val x             = local(SIGNED32, 0)
            val y             = local(SIGNED32, 0)

            // Read the width/height if we don't know them yet.
            if (width == 0) {
                width = width_in
                height = height_in
                width_out = width
                height_out = height
                cluster = cluster_in
            }

            // Read a value.
            value = value_in

            // Assign the value to the nearest cluster.
            i = 1
            assignment = 0
            dist = Distance(value, cluster(0))
            while (i < clusterCount) {
                temp = Distance(value, cluster(i))
                if (temp < dist) {
                    dist = temp
                    assignment = i
                }
                i += 1
            }
            index_out = assignment
            value_out = value
            result_out = cluster(assignment)

            // Update the coordinates.
            x += 1
            if (x == width) {
                x = 0
                y += 1
                if (y == height) {
                    y = 0
                    index_out = LAST_INDEX
                    cluster = cluster_in
                }
            }

        }

        /** Block to update clusters. */
        val UpdateClusters = new Kernel("UpdateClusters") {

            val value_in        = input(ValueType)
            val index_in        = input(SIGNED32)
            val width_in        = input(SIGNED32)
            val height_in      = input(SIGNED32)

            val cluster_out    = output(ClusterType)
            val width_out      = output(SIGNED32)
            val height_out     = output(SIGNED32)

            val clusters    = local(ClusterType)
            val counts      = local(ClusterType)
            val i             = local(SIGNED32)
            val index        = local(SIGNED32)
            val width        = local(SIGNED32, 0)
            val height      = local(SIGNED32)
            val value        = local(ValueType)

            // Read the width/height if we don't know them yet.
            if (width == 0) {
                width = width_in
                height = height_in
                width_out = width
                height_out = height
            } else {

                index = index_in
                if (index == LAST_INDEX) {

                    // Got all of the updates.
                    // Send an updated cluster.
                    i = 0
                    while (i < clusterCount) {
                        if (counts(i) <> 0) {
                            clusters(i) /= counts(i)
                        }
                        i += 1
                    }
                    cluster_out = clusters

                    // Reset the clusters.
                    i = 0
                    while (i < clusterCount) {
                        clusters(i) = 0
                        counts(i) = 0
                        i += 1
                    }

                } else {

                    clusters(index) += value_in
                    counts(index) += 1

                }

            }

        }

        /** Block to output the cluster data. */
        val Output = new Kernel("Output") {

            val cluster_in  = input(ClusterType)
            val value_in    = input(ValueType)
            val width_in    = input(SIGNED32)
            val height_in   = input(SIGNED32)

            val cluster_out = output(ClusterType)
            val value_out   = output(ValueType)
            val width_out   = output(SIGNED32)
            val height_out  = output(SIGNED32)

            val cluster = local(ClusterType)
            val i       = local(SIGNED32, 0)
            val width   = local(SIGNED32, 0)
            val height  = local(SIGNED32)
            val value   = local(ValueType)
            val x       = local(SIGNED32, 0)
            val y       = local(SIGNED32, 0)

            if (width == 0) {
                width = width_in
                height = height_in
                width_out = width
                height_out = height
            }

            value = value_in
            if (i + 1 == iterations) {
                value_out = value
            }

            x += 1
            if (x == width) {
                x = 0
                y += 1
                if (y == height) {
                    i += 1
                    stdio.printf("Iteration %d / %d\n", i, iterations)
                    y = 0
                    cluster = cluster_in
                    if (i < iterations) {
                        cluster_out = cluster
                    }
                }
            }

        }

        val Cluster = new Application {
            val loop = Cycle(ClusterType)
            val csrc = ClusterSource(loop)
            val bmp = BMPReader('file -> "in.bmp")
            val vsrc = ValueSource(bmp(0), bmp(1), bmp(2))
            val assignments = Assign(vsrc(0), vsrc(1), vsrc(2), csrc(0))
            val updated = UpdateClusters(assignments(0), assignments(1),
                                         assignments(2), assignments(3))
            val out = Output(updated(0), assignments(4), updated(1), updated(2))
            loop(out(0))
            BMPWriter(out(1), out(2), out(3), 'file -> "out.bmp")
        }
        Cluster.emit("cluster")

    }

}


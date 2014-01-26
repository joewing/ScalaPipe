package autopipe

private[autopipe] class DeviceType(val platform: Platforms.Value) {

    private val startingIndex = 1 << 16
    private var devices = Map[(String, Int), Device]()

    def count = devices.size

    def get(host: String, index: Int): Device = {

        val newIndex =
            if (index < 0) {
                var i = startingIndex
                while (devices.contains((host, i))) {
                    i += 1
                }
                i
            } else {
                index
            }

        devices.get((host, newIndex)) match {
            case Some(d)    => d
            case None       =>
                val d = new Device(this, host, newIndex)
                devices += ((host, newIndex) -> d)
                d
        }
    }

    override def toString = platform.toString

    def reassignIndexes() {

        val validIndexes = devices.filterKeys(_._2 < startingIndex).map(_._1._2)
        val newStart = validIndexes.foldLeft(0)(math.max)
        val toReassign = devices.filter(_._1._2 >= startingIndex)

        var index = newStart + 1
        for (i <- toReassign) {
            val oldKey = i._1
            val device = i._2
            devices -= oldKey
            device.index = index
            devices += (((device.host, device.index), device))
            index += 1
        }

    }

}

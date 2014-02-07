package scalapipe

private[scalapipe] class DeviceType(val platform: Platforms.Value) {

    private var devices = Map[(String, Int), Device]()

    def count = devices.size

    def get(host: String, index: Int): Device = {
        val key = (host, index)
        devices.get(key) match {
            case Some(d)    => d
            case None       =>
                val d = new Device(this, host, index)
                devices += (key -> d)
                d
        }
    }

    override def toString = platform.toString

}

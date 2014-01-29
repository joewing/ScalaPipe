package scalapipe

private[scalapipe] class DeviceManager(param: Parameters) {

    private var deviceTypes = Map[Platforms.Value, DeviceType]()

    private def defaultHost = param.get[String]('defaultHost)
    private def defaultIndex = 0

    def get(platform: Platforms.Value, host: String, index: Int): Device = {
        deviceTypes.get(platform) match {
            case Some(dt)   => dt.get(host, index)
            case None       =>
                val dt = new DeviceType(platform)
                deviceTypes += (platform -> dt)
                dt.get(host, index)
        }
    }

    def getDefault(platform: Platforms.Value) = get(platform, defaultHost, 0)

    def create(spec: DeviceSpec): Device = {
        val host = if (spec.host == null) defaultHost else spec.host
        val index = if (spec.index == Int.MaxValue) defaultIndex else spec.index
        get(spec.platform, host, index)
    }

    def reassignIndexes() {
        deviceTypes.foreach { _._2.reassignIndexes }
    }

    def threadCount: Int =
        deviceTypes.filter(_._1 == Platforms.C).map(_._2.count).sum

}

package scalapipe

private[scalapipe] class DeviceManager(param: Parameters) {

    private var deviceTypes = Map[Platforms.Value, DeviceType]()

    private def defaultPlatform = Platforms.C
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

    def create(spec: DeviceSpec): Device = {
        if (spec == null) {
            return get(defaultPlatform, defaultHost, defaultIndex)
        }
        val platform = if (spec.platform == Platforms.ANY)
            defaultPlatform else spec.platform
        val host = if (spec.host == null) defaultHost else spec.host
        val index = if (spec.index == Int.MaxValue) defaultIndex else spec.index
        return get(platform, host, index)
    }

    def reassignIndexes() {
        deviceTypes.foreach { _._2.reassignIndexes }
    }

}

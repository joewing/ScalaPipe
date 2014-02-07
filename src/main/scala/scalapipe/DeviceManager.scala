package scalapipe

private[scalapipe] class DeviceManager(param: Parameters) {

    private var deviceTypes = Map[Platforms.Value, DeviceType]()

    private def defaultPlatform = Platforms.C
    private def defaultHost = param.get[String]('defaultHost)
    private def defaultIndex(platform: Platforms.Value) = platform match {
        case Platforms.C    => -1
        case _              => 0
    }

    def get(platform: Platforms.Value, host: String, index: Int): Device = {
        deviceTypes.get(platform) match {
            case Some(dt)   => dt.get(host, index)
            case None       =>
                val dt = new DeviceType(platform)
                deviceTypes += (platform -> dt)
                dt.get(host, index)
        }
    }

    private def getPlatform(spec: DeviceSpec): Platforms.Value = {
        if (spec == null || spec.platform == Platforms.ANY) {
            defaultPlatform
        } else {
            spec.platform
        }
    }

    private def getHost(spec: DeviceSpec): String = {
        if (spec == null || spec.host == null) {
            defaultHost
        } else {
            spec.host
        }
    }

    private def getIndex(spec: DeviceSpec): Int = {
        if (spec == null || spec.index < 0) {
            defaultIndex(getPlatform(spec))
        } else {
            spec.index
        }
    }

    def create(spec: DeviceSpec): Device = {
        val platform = getPlatform(spec)
        val host = getHost(spec)
        val index = getIndex(spec)
        return get(platform, host, index)
    }

}

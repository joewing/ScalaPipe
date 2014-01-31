package scalapipe

private[scalapipe] class DeviceSpec(
        val platform: Platforms.Value,
        val host: String,
        val index: Int
    ) {

    def canCombine(other: DeviceSpec): Boolean = {
        platform == other.platform &&
            (host == null || other.host == null || host == other.host) &&
            (index == Int.MaxValue || other.index == Int.MaxValue ||
                index == other.index)
    }

    def combine(other: DeviceSpec): DeviceSpec = {

        // This combine operation is assumed to be valid.
        if (other != null) {
            val newPlatform = platform
            val newHost = if (host != null) host else other.host
            val newIndex = if (index != Int.MaxValue) index else other.index
            new DeviceSpec(newPlatform, newHost, newIndex)
        } else {
            this
        }

    }

    override def toString =
        if (host != null && index != Int.MaxValue) {
            s"$platform($host, $index)"
        } else if (host != null) {
            s"$platform($host)"
        } else if (index != Int.MaxValue) {
            s"$platform($index)"
        } else {
            s"$platform"
        }

}

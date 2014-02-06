package scalapipe

private[scalapipe] class DeviceSpec(
        val platform: Platforms.Value,
        val host: String,
        val index: Int
    ) {

    private def canCombine(p1: Platforms.Value, p2: Platforms.Value) = {
        p1 == Platforms.ANY || p2 == Platforms.ANY || p1 == p2
    }

    private def canCombine(h1: String, h2: String) = {
        h1 == null || h2 == null || h1 == h2
    }

    private def canCombine(i1: Int, i2: Int) = {
        i1 == Int.MaxValue || i2 == Int.MaxValue || i1 == i2
    }

    def canCombine(other: DeviceSpec): Boolean = {
        canCombine(platform, other.platform) &&
            canCombine(host, other.host) &&
            canCombine(index, other.index)
    }

    private def combined(p1: Platforms.Value, p2: Platforms.Value) = {
        if (p1 != Platforms.ANY) p1 else p2
    }

    private def combined(h1: String, h2: String) = {
        if (h1 != null) h1 else h2
    }

    private def combined(i1: Int, i2: Int) = math.min(i1, i2)

    def combine(other: DeviceSpec): DeviceSpec = {

        // This combine operation is assumed to be valid.
        if (other != null) {
            val newPlatform = combined(platform, other.platform)
            val newHost = combined(host, other.host)
            val newIndex = combined(index, other.index)
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

private[scalapipe] object AnyDeviceSpec
    extends DeviceSpec(Platforms.ANY, null, Int.MaxValue)

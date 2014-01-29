package scalapipe

private[scalapipe] class Device(
        val deviceType: DeviceType,
        val host: String,
        var index: Int
    ) {

    private[scalapipe] val label = LabelMaker.getDeviceLabel
    private[scalapipe] val name: String = deviceType + "[" + index + "]"
    private[scalapipe] val platform = deviceType.platform

    override def toString =
        deviceType.toString + "(" + host + ", " + index + ")"

}

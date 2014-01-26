package autopipe

private[autopipe] class Device(
        val deviceType: DeviceType,
        val host: String,
        var index: Int
    ) {

    private[autopipe] val label = LabelMaker.getDeviceLabel
    private[autopipe] val name: String = deviceType + "[" + index + "]"
    private[autopipe] val platform = deviceType.platform

    override def toString =
        deviceType.toString + "(" + host + ", " + index + ")"

}

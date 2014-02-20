package scalapipe

private[scalapipe] class EdgeParameters(
        val defaults: ApplicationParameters
    ) extends Parameters {

    add('queueDepth, defaults.get[Int]('queueDepth))
    add('fpgaQueueDepth, defaults.get[Int]('fpgaQueueDepth))

}

package scalapipe.gen

import scalapipe._

private[gen] class ProfileIRContext(
        _kt: KernelType
    ) extends HDLIRContext(_kt) {

    override def eliminateVariables = false

}

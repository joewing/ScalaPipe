package autopipe.gen

import autopipe._

private[gen] class ProfileIRContext(
        _kt: KernelType
    ) extends HDLIRContext(_kt) {

    override def eliminateVariables = false

}

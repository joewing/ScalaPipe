
package autopipe.gen

import autopipe._

private[gen] class ProfileIRContext(_co: CodeObject) extends HDLIRContext(_co) {

    override def eliminateVariables = false

}


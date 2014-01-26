package autopipe.gen

private[gen] trait CGenerator extends Generator {

    protected override def enter {
        write("{")
        super.enter
    }

    protected override def leave {
        super.leave
        write("}")
    }

    protected def writeIf(cond: String) {
        write(s"if($cond)")
        enter
    }

    protected def writeElse {
        leave
        write("else")
        enter
    }

    protected def writeElseIf(cond: String) {
        leave
        write(s"else if($cond)")
        enter
    }

    protected def writeSwitch(cond: String) {
        write(s"switch($cond)")
        enter
    }

    protected def writeEnd = leave

    protected def writeReturn(value: String = null) {
        if (value != null) {
            write(s"return $value;")
        } else {
            write("return;")
        }
    }

}

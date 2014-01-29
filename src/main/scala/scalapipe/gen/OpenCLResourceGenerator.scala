
package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] class OpenCLResourceGenerator(val ap: AutoPipe,
                                                                val device: Device)
        extends ResourceGenerator {

    override def getRules: String = {
        """
OPENCL_DIR = /usr/local/cuda
OCLINC=-I$(OPENCL_DIR)/include
ifeq ($(shell uname),Darwin)
    OCLLIB=-framework OpenCl
else
    OCLLIB=-lOpenCL
endif
INCS += $(OCLINC)
LDFLAGS += $(OCLLIB)
"""
    }

    override def emit(dir: File) {
        // Nothing to do here.
    }

}


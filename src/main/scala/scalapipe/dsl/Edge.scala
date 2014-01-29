package scalapipe.dsl

import scalapipe._

object CPU2FPGA extends EdgeObject[CPU2FPGA](Platforms.HDL)

object FPGA2CPU extends EdgeObject[FPGA2CPU](Platforms.C)

object CPU2CPU extends EdgeObject[CPU2CPU](Platforms.C)

object CPU2GPU extends EdgeObject[CPU2GPU](Platforms.OpenCL)

object GPU2CPU extends EdgeObject[GPU2CPU](Platforms.C)

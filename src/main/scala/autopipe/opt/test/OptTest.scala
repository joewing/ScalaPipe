
package autopipe.opt.test

object OptTest extends App {

   if (TestExpandExpressions.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestCSE.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestDSE.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestDCE.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestStrengthReduction.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestCopyPropagation.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestStateCompression.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestContinuousAssignment.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestCombineVariables.run) {
      println("OK")
   } else {
      println("FAILED")
   }

   if (TestReassignStates.run) {
      println("OK")
   } else {
      println("FAILED")
   }

}



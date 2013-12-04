
package autopipe

private[autopipe] object BlockMode extends Enumeration {

   val MODE_AS    = Value("all-synchronous")
   val MODE_AA    = Value("all-asynchronous")
   val MODE_1S    = Value("single-synchronous")
   val MODE_1A    = Value("single-asynchronous")
   val MODE_NS    = Value("unknown-synchronous")
   val MODE_NA    = Value("unknown-asynchronous")

}


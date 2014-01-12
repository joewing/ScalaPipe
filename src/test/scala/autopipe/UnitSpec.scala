package autopipe

import org.scalatest._
import org.scalamock.scalatest.MockFactory

abstract class UnitSpec extends FlatSpec with
    MockFactory with PrivateMethodTester

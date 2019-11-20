package cheetsheet

import cheetsheet._
import org.scalacheck.Properties

class MonadSpec extends Properties("Monad") {
  def law: Monad.Law[Maybe] = new Monad.Law[Maybe] {
    override val M = Maybe.maybeInstances
  }

  def odd(i: Int): Maybe[Int] = if (i % 2!= 0)

}

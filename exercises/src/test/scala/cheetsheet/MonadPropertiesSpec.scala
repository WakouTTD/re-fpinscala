package cheetsheet

import cheetsheet._
import org.scalacheck.{Prop, Properties}

class MonadPropertiesSpec extends Properties("Monad") {
  def law: MonadLaw[Maybe] =
    new MonadLaw[Maybe] { override val M = Maybe.maybeInstances }

  def odd(i: Int): Maybe[Int]  = if (i % 2 != 0) Just(i) else Empty()
  def fizz(i: Int): Maybe[Int] = if (i % 3 == 0) Just(i) else Empty()
  def buzz(i: Int): Maybe[Int] = if (i % 5 == 0) Just(i) else Empty()

  //左単位元
  property("leftIdentity") = Prop.forAll { i: Int =>
    println(s"leftIdentityのi=$i")
    law.leftIdentity(i, fizz)
  }
  //右単位元
  property("rightIdentity") = Prop.forAll { i: Int =>
    println(s"rightIdentityのi=$i")
    law.rightIdentity(buzz(i))
  }
  // 結合律
  property("associativity") = Prop.forAll { i: Int =>
    println(s"associativityのi=$i")
    law.associativity(odd(i), fizz, buzz)
  }
}

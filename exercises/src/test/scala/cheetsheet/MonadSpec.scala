package cheetsheet

import org.scalatest.FlatSpec

class MonadSpec extends FlatSpec {

  "for式で合成するため" should "型クラスのインスタンスを定義しただけではfor式で合成できない" in {
//      for {
//        i <- Maybe.just(1)
//        j <- Maybe.just(2)
//      } yield i + j
  }

}

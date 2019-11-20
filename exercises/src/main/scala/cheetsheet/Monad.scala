package cheetsheet

trait Monad[F[_]] {
  def pure[A](a: A): F[A]
  def flatMap[A, B](fa: F[A])(f: A => F[B]):F[B]
}

object Monad {
  // implicity[Monad[F]]のエイリアス
  def apply[F[_]](implicit M: Monad[F]): Monad[F] = M
}

sealed abstract class Maybe[A]

case class Just[A](a: A) extends Maybe[A]
case class Empty[A]() extends Maybe[A]

object Maybe {

  // smart constructer
  def just[A](a: A): Maybe[A] = Just(a)
  def empty[A]: Maybe[A] = Empty()

}

object Main {

  implicit val maybeInstances: Monad[Maybe] =
   new Monad[Maybe] {
     def pure[A](a: A): Maybe[A] = Just(a)

     def flatMap[A, B](fa: Maybe[A])(f: A => Maybe[B]): Maybe[B] =
       fa match {
         case Just(a) => f(a)
         case Empty() => Empty()
       }
   }

  def main(args: Array[String])= {

    val n = Monad[Maybe].pure(42)
    val monad = Monad[Maybe].flatMap(n){ i =>
      if (i > 0) Maybe.just(i) else Maybe.empty[Int]
    }

    println(monad)
  }

}

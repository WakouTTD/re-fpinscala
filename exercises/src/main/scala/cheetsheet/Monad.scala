package cheetsheet

trait Monad[F[_]] {
  def pure[A](a: A): F[A]
  def flatMap[A, B](fa: F[A])(f: A => F[B]):F[B]

  def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => pure(f(a)))

  def M: Monad[F]

  def leftIdentity[A, B](a: A, f: A => F[B]): Boolean =
    M.flatMap(M.pure(a))(f) == f(a)

  def rightIdentity[A](fa: F[A]): Boolean =
    M.flatMap(fa)(M.pure) == fa

  def associativity[A, B, C](fa: F[A], f: A => F[B], g: B => F[C]): Boolean =
    M.flatMap(M.flatMap(fa)(f))(g) == M.flatMap(fa)(a => M.flatMap(f(a))(g))

}

object Monad {
  // implicity[Monad[F]]のエイリアス
  def apply[F[_]](implicit M: Monad[F]): Monad[F] = M
}

sealed abstract class Maybe[A] {
  def flatMap[B](f: A => Maybe[B]): Maybe[B] = this match {
    case Just(a) => f(a)
    case Empty() => Empty()
  }
  // デフォルト実装
  //def map[B](f: A => B): Maybe[B] = flatMap(a => Maybe.pure(f(a)))
  def map[B](f: A => B): Maybe[B] = flatMap(a => Maybe.just(f(a)))
}
case class Just[A](a: A) extends Maybe[A]
case class Empty[A]() extends Maybe[A]

//object Maybe extends MonadSyntax {
object Maybe {

  implicit val maybeInstances: Monad[Maybe] =
    new Monad[Maybe] {
      def pure[A](a: A): Maybe[A] = Just(a)

      def flatMap[A, B](fa: Maybe[A])(f: A => Maybe[B]): Maybe[B] =
        fa match {
          case Just(a) => f(a)
          case Empty() => Empty()
        }
    }

  // smart constructer
  def just[A](a: A): Maybe[A] = Just(a)
  def empty[A]: Maybe[A] = Empty()

}

trait MonadSyntax {
  implicit def toMonadOps[F[_], A](fa: F[A]): MonadOps[F, A] =
    new MonadOps(fa)
}

class MonadOps[F[_], A](val fa: F[A]) extends AnyVal {
  def map[B](f:A => B)(implicit M: Monad[F]): F[B] = M.map(fa)(f)
  def flatMap[B](f: A => F[B])(implicit M: Monad[F]): F[B] = M.flatMap(fa)(f)
}


object Main {

  def main(args: Array[String])= {

    val n = Monad[Maybe].pure(42)
    val monad = Monad[Maybe].flatMap(n){ i =>
      if (i > 0) Maybe.just(i) else Maybe.empty[Int]
    }
    println(monad)

    val toMonadOps = for {
      i <- Maybe.just(1)
      j <- Maybe.just(2)
    } yield {
      i + j
    }
    println(s"toMonadOps:$toMonadOps")


  }

}

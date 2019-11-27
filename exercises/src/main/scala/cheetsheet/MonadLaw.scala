package cheetsheet
/*
trait MonadLaw[F[_]] {
  def M: Monad[F]

  def leftIdentity[A, B](a: A, f: A => F[B]): Boolean =
    M.flatMap(M.pure(a))(f) == f(a)

  def rightIdentity[A](fa: F[A]): Boolean =
    M.flatMap(fa)(M.pure) == fa

  def associativity[A, B, C](fa: F[A], f: A => F[B], g: B => F[C]): Boolean =
    M.flatMap(M.flatMap(fa)(f))(g) == M.flatMap(fa)(a => M.flatMap(f(a))(g))

}
*/
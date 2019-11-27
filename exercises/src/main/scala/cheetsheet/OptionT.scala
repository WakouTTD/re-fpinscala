package cheetsheet

object OptionT {
  def pure[F[_], A](a: A)(implicit M: Monad[F]): OptionT[F, A] =
    OptionT(M.pure(Some(a)))
}

final case class OptionT[F[_], A] (value: F[Option[A]]) {
  def map[B](f: A => B)(implicit M: Monad[F]): OptionT[F, B] =
    OptionT(M.map(value)(_.map(f)))

}
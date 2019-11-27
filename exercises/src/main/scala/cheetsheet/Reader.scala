package cheetsheet

import scala.util.chaining._

object Reader {
  def pure[R, A](a: A): Reader[R, A] = Reader(_ => a)
}
final case class Reader[R, A](run: R => A) {

  def flatMap[B](f: A => Reader[R, B]): Reader[R, B] =
    Reader(r => f(run(r)).run(r))

  def map[B](f: A => B): Reader[R, B] = flatMap(a => Reader.pure(f(a)))
}

/*

import scala.util.chaining._

object Reader {
  def pure[R, A](a: A): Reader[R, A] = Reader(_ => a)
}
final case class Reader[R, A](run: R => A) {

  def flatMap[B](f: A => Reader[R, B]): Reader[R, B] =
    Reader(r => f(run(r)).run(r))

  def map[B](f: A => B): Reader[R, B] = flatMap(a => Reader.pure(f(a)))
}

for {
  i <- Reader[String, Int]{s => (s.toInt + 1).tap(println)}
  j <- Reader[String, Int]{s => (s.toInt * 10).tap(println)}
} yield {
  i + j
}


*/
package cheetsheet

object Cont {
  def pure[R, A](a: A): Cont[R, A] = Cont(ar => ar(a))
}
final case class Cont[R, A](run: (A => R) => R) {
  def flatMap[B](f: A => Cont[R, B]): Cont[R, B] =
    Cont(br => run(a => f(a).run(br)))

  def map[B](f: A => B): Cont[R, B] = flatMap(a => Cont.pure(f(a)))
}

/*

def add[R](i: Int, j: Int): Cont[R, Int] = Cont(ar => ar(i + j))
def mul[R](i: Int, j: Int): Cont[R, Int] = Cont(ar => ar(i * j))
def show[R](i: Int): Cont[R, String]     = Cont(ar => ar(s"num: $i"))
def prog[R]: Cont[R, String] =
  for {
    a <- add(1, 2)
    b <- mul(a, 3)
    s <- show(b)
  } yield {
    s.toUpperCase
  }

prog.run(s => s.toList)

prog.run(s => s.length)

prog.run(s => s)

 */
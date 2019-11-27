package cheetsheet

object IO {
  def pure[A](a: A): IO[A] = IO(a)
  def apply[A](a: => A): IO[A] = new IO[A] { def unsafeRun(): A = a}
}

sealed abstract class IO[A] {
  def unsafeRun(): A // 呼び出したタイミングで全ての計算が実行される

  def flatMap[B](f: A => IO[B]):IO[B] = IO { f(unsafeRun()).unsafeRun() }
  def map[B](f: A => B): IO[B] = flatMap(a => IO.pure(f(a)))
}

/*
import scala.io.StdIn

def readLine: IO[String]         = IO { StdIn.readLine }
def println(s: String): IO[Unit] = IO { Predef.println(s) }

val io = println("Hello!")

val app = for {
  _ <- println("Message:")
  s <- readLine
  _ <- println(s"Your message is $s")
} yield s

app: IO[String] = IO$$anon$1@54ef9698

app.unsafeRun()

 */


/*
def readInt: IO[Option[Int]] = IO { StdIn.readLine().toIntOption }

val prog: IO[Option[Int]] = for {
  // IO[A]の合成
  nOpt <- readInt
  mOpt <- readInt
} yield for {
  // Option[A] の合成
  n <- nOpt
  m <- mOpt
} yield n + m

prog.unsafeRun()

prog.unsafeRun()
 */

/*
scala> val prog: IO[Option[Int]] = for {
     |   // IO[A]の合成
     |   nOpt <- readInt
     |   mOpt <- readInt
     | } yield for {
     |   // Option[A] の合成
     |   n <- nOpt
     |   m <- mOpt
     | } yield n + m
prog: IO[Option[Int]] = IO$$anon$1@1fcef6f9

scala> prog.unsafeRun()
// 1と2を入力
res6: Option[Int] = Some(3)

scala> prog.unsafeRun()
// 1とaを入力
res3: Option[Int] = None

 */


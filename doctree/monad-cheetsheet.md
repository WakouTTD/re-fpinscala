#モナド

- 変な比喩としては「モナドはコンテナ」
- 実際には単なる型クラスの1つ
[怖いScala](http://xuwei-k.github.io/slides/kowai_scala)
--
型クラスのインスタンスの定義

型クラスのインスタンス定義
```scala=
trait IntOrderring extends Ordering[Int] {
  def compare(x: Int, y Int) =
    if (x < y) -1
	else if (x == y) 0
	else 1
}
implicit object Int extends IntOrdering
```
型クラスを使う側
```scala=
def max[B >: A](implicit cmp: Ordering[B]): A
```







以降の例は、ほぼ写経
[Scalaにおけるモナドって何だろう](https://speakerdeck.com/aoiroaoino/scala-niokerumonadotutehe-darou)

```scala=
// M[_]はモナドの型
def pure[A](a: A): M[A]
def flatMap[B](f: A => M[B]): M[B]

//モナド則
//左単位元
pure(a).flatMap(f) == f(a)

//右単位元
fa.flatMap(a => pure(a)) == fa

//結合律
fa.flatMap(f).flatMap(g) == fa.flatMap(a => f(a).flatMap(g))
```
```scala=
val xs: List[String] = List("foo", "bar")
val ys: List[String] = List("!","?")

for {
  x <- xs
  y <- ys
} yield x + y


xs.flatMap{ x =>
  ys.map{ y =>
    x + y
  }
}
```
----------------------
```shell=
scala> val xs: List[String] = List("foo", "bar")
xs: List[String] = List(foo, bar)

scala> val ys: List[String] = List("!","?")
ys: List[String] = List(!, ?)

scala> for {
     |   x <- xs
     |   y <- ys
     | } yield x + y
res0: List[String] = List(foo!, foo?, bar!, bar?)

scala> xs.flatMap{ x =>
     |   ys.map{ y =>
     |     x + y
     |   }
     | }
res1: List[String] = List(foo!, foo?, bar!, bar?)
```

---

Scalaにおけるモナドが示す２つの意味
抽象的なモナド
- 広義のインターフェース
- 抽象的なモナドを実装できるプログラミング言語は少ない
- 主に高階型パラメータ、型クラスを用いて実装する
- 型クラスによって既存のデータ型もモナドとして拡張できる
- 例：ScalazやCatsのtrait Monad[F[_]]{...}

具体的なモナド
- 広義のインターフェースに対するインスタンス、実装
- 具体的なモナドインスタンスを実装できるプログラミング言語は多い
- 「モナドを知らなくても使える/使ってる」は主に具体的なモナドの方
- 例：標準ライブラリのOption[A]、Either[E, A]、List[A]、etc・・・

---

抽象的なモナドの実装
- 型クラスで抽象的なモナドを実装
- Enrich my libraryパターンで拡張メソッドを提供

---

抽象的なモナドの実装--モナド型クラスの定義
```scala=
trait Monad[F[_]] {
  def pure[A](a: A): F[A]
  def flatMap[A, B](fa: F[A])
}

object Monad {
  // implicity[Monad[F]]のエイリアス
  def apply[F[_]](implicit M: Monad[F]): Monad[F] = M
}
```

---

抽象的なモナドの実装--データ型の例
```scala=
sealed abstract class Maybe[A]

case class Just[A](a: A) extends Maybe[A]
case class Empty[A]() extends Maybe[A]

object Maybe {
  // smart constructer
  def just[A](a: A): Maybe[A] = Just(a)
  def empty[A]: Maybe[A] = Empty()
}

```
抽象的なモナドの実装--インスタンスを定義
```scala=
implicit val maybeInstances: Monad[Maybe] = 
  Monad[Maybe] {
    def pure[A](a: A): Maybe[A] = Just(a)
    def flatMap[A, B](fa: Maybe[A])(f: A => Maybe[B]): Maybe[B] = 
      fa match {
        case Just(a) => f(a)
        case Empty() => Empty()
      }
  }
```
抽象的なモナドの実装--動作確認

```shell=
val n = Monad[Maybe].pure(42)
Monad[Maybe].flatMap(n){ i =>
  if (i > 0) Maybe.just(i) else Maybe.empty[Int]
}
```



抽象的なモナドの実装--モナド則を満たすか

```scala=
trait MonadLaw[F[_]] {
  def M: Monad[F]

  def leftIdentity[A, B](a: A, f: A => F[B]): Boolean =
    M.flatMap(M.pure(a))(f) == f(a)

  def rightIdentity[A](fa: F[A]): Boolean =
    M.flatMap(fa)(M.pure) == fa

  def associativity[A, B, C](fa: F[A], f: A => F[B], g: B => F[C]): Boolean =
    M.flatMap(M.flatMap(fa)(f))(g) == M.flatMap(fa)(a => M.flatMap(f(a))(g))
}
```

```scala=
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
```

```shell=
sbt:exercises> test
[info] + Monad.rightIdentity: OK, passed 100 tests.
[info] + Monad.leftIdentity: OK, passed 100 tests.
[info] + Monad.associativity: OK, passed 100 tests.
[info] ScalaCheck
[info] Passed: Total 3, Failed 0, Errors 0, Passed 3
```

抽象的なモナドの実装--for式で合成するために
```scala=
// コンパイル通らない
//  for {
//    i <- Maybe.just(1)
//    j <- Maybe.just(2)
//  } yield i + j
```

抽象的なモナドの実装--for式で合成するために
- 型クラスのインスタンスを定義しただけではfor式で合成できない
- for式が展開された後のシグネチャに合わせたmap/flatMapが必要

---
```scala=
// Monad[Maybe].map(fa)(f) をfa.map(f)と書けるようになる
trait MonadSyntax {
  implicit def toMonadOps[F[_], A](fa: F[A]): MonadOps[F, A] =
    new MonadOps(fa)
}
class MonadOps[F[_], A](val fa: F[A]) extends AnyVal {
  def map[B](f:A => B)(implicit M: Monad[F]): F[B] = M.map(fa)(f)
  def flatMap[B](f: A => F[B])(implicit M: Monad[F]): F[B] = M.flatMap(fa)(f)
}
```
--
```scala=
// toMonadOps がスコープに定義されてる状態で for 式で合成ができる
for {
  i <- Maybe.just(1)
  j <- Maybe.just(2)
} yield i + j
// res1: Maybe[Int] = Just(3)

```
-- 
具体的なモナドの実装--実装方法
- モナド則を満たすようpure/flatMapを直接メソッドとして定義する
```scala=
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
```
-- 

```scala=
// 特に暗黙の型変換をせずともfor式で合成できる
for {
  i <- Maybe.just(1)
  j <- Maybe.just(2)
} yield {
  i + j
}
// res0: Maybe[Int] = Just(3)
```
---
## モナドを活用するために
### モナドって何?(再掲)
- モナド則を満たすようにpure/flatMapを実装したもの
---
### Scalaにおける実用上のモナドの価値(再掲)
- データ型の「文脈」に依存しない汎用的な操作/合成を提供
---
Scalaにおける「モナド」が示す2つの意味(再掲)
- 抽象的なモナド
- 具体的なモナド
 ※　この区分の名称は一般的ではない
---
### 具体的なモナドの例
- Option[A]
- Either[E, A]
- List[A]
- Reader[R, A]
- Write[W, A]
- State[S, A]
- Cont[R, A]  // 継続(Continuation)
- IO[A]
- Free[F[_], A]
・・・etc
---
具体的なモナド--Readerモナド
```scala=
object Reader {
  def pure[R, A](a: A): Reader[R, A] = Reader(_ => a)
}
final case class Reader[R, A](run: R => A) {
  
  def flatMap[B](f: A => Reader[R, B]): Reader[R, B] =
    Reader(r => f(run(r)).run(r))

  def map[B](f: A => B): Reader[R, B] = flatMap(a => Reader.pure(f(a)))
}
```
```scala=
scala> import scala.util.chaining._
import scala.util.chaining._

scala> :paste
// Entering paste mode (ctrl-D to finish)

object Reader {
  def pure[R, A](a: A): Reader[R, A] = Reader(_ => a)
}
final case class Reader[R, A](run: R => A) {

  def flatMap[B](f: A => Reader[R, B]): Reader[R, B] =
    Reader(r => f(run(r)).run(r))

  def map[B](f: A => B): Reader[R, B] = flatMap(a => Reader.pure(f(a)))
}

// Exiting paste mode, now interpreting.

defined object Reader
defined class Reader

scala> for {
     |   i <- Reader[String, Int]{s => (s.toInt + 1).tap(println)}
     |   j <- Reader[String, Int]{s => (s.toInt * 10).tap(println)}
     | } yield {
     |   i + j
     | }
res0: Reader[String,Int] = Reader(Reader$$Lambda$1040/0x0000000800687840@2aa14ae6)

```
---
具体的なモナド--Contモナド(継続モナド)
```scala=
object Cont {
  def pure[R, A](a: A): Cont[R, A] = Cont(ar => ar(a))
}
final case class Cont[R, A](run: (A => R) => R) {
  def flatMap[B](f: A => Cont[R, B]): Cont[R, B] =
    Cont(br => run(a => f(a).run(br)))

  def map[B](f: A => B): Cont[R, B] = flatMap(a => Cont.pure(f(a)))
}
```

```scala=
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
```

```shell=
scala> prog.run(s => s.toList)
res0: List[Char] = List(N, U, M, :,  , 9)

scala> prog.run(s => s.length)
res1: Int = 6

scala> prog.run(s => s)
res2: String = NUM: 9
```
---
具体的なモナド--IOモナド
```scala=
object IO {
  def pure[A](a: A): IO[A] = IO(a)
  def apply[A](a: => A): IO[A] = new IO[A] { def unsafeRun(): A = a}
}

sealed abstract class IO[A] {
  def unsafeRun(): A // 呼び出したタイミングで全ての計算が実行される

  def flatMap[B](f: A => IO[B]):IO[B] = IO { f(unsafeRun()).unsafeRun() }
  def map[B](f: A => B): IO[B] = flatMap(a => IO.pure(f(a)))
}
```

```shell=
import scala.io.StdIn

def readLine: IO[String]         = IO { StdIn.readLine }
def println(s: String): IO[Unit] = IO { Predef.println(s) }

scala> val io = println("Hello!")
io: IO[Unit] = IO$$anon$1@b8142f4

scala> io.unsafeRun()
Hello!
```

```scala=
scala> val app = for {
     |   _ <- println("Message:")
     |   s <- readLine
     |   _ <- println(s"Your message is $s")
     | } yield s
app: IO[String] = IO$$anon$1@54ef9698

scala> app.unsafeRun()
Message:
Your message is Hello!
res2: String = Hello!
```
---
抽象的なモナドの活用
- MonadTransformer
- Effect Injection

---
抽象的なモナド--MonadTransformer
- モナドの合成を考える
- def readInt: IO[Option[Int]] = IO { StdIn.readLine().toIntOption }

---
```scala=
val prog: IO[Option[Int]] = for {
  // IO[A]の合成
  nOpt <- readInt
  mOpt <- readInt
} yield for {
  // Option[A] の合成
  n <- nOpt
  m <- mOpt
} yield n + m
```

```shell=
scala> prog.unsafeRun()
// 1と2を入力
res6: Option[Int] = Some(3)

scala> prog.unsafeRun()
// 1とaを入力
res3: Option[Int] = None

```
---
- F[_], G[_]がモナドのとき、F[G[_]]もモナドになる・・・とは限らない
- G[_]が特定の条件を満たす(具体的な)モナドの場合はF[G[_]]もモナドになる
  - F[Option[A]],F[Either[E, A]], F[Cont[R, A]]など
  - 特定の条件: F[G[_]]をG[F[_]]にできる
- 条件を満たす(具体的な)モナドに対応するデータ型を定義し、それもまたモナドのインスタンスにしてしまおう
  OptionT[F, A], EitherT[F, E, A], ContT[F, R, A]など


抽象的なモナドの実装--Effect Injection






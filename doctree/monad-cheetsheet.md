#モナド

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

```




```scala=
```




```scala=
```












抽象的なモナドの実装--Effect Injection






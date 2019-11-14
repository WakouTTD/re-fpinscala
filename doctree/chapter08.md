# fpinscala 輪読会#8

- 前章で学んだこと

  - APIを代数として形成すること
  
  - データ型、データ型に対する関数、関数の間の関係を表す法則・プロパティの集まりとしてAPIを構成すること

- 本章で学ぶこと

  - プロパティベースのテストを可能にするライブラリを作成する

---
## 8.1速習プロパティベースのテスト
ScalaCheck

- Sample Code
```scala=
class SampleSpec extends FlatSpec {

  val intList = Gen.listOf(Gen.choose(0, 100))
  val prop =
    forAll(intList)(ns => ns.reverse.reverse == ns) &&
    forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
  val failingProp = forAll(intList)(ns => ns.reverse == ns)

  prop.check
  failingProp.check
}
```

- terminal
```bash
+ OK, passed 100 tests.
! Falsified after 2 passed tests.
> ARG_0: List("0", "1")
> ARG_0_ORIGINAL: List("1", "15")
```

---
### EXERCISE8-1
Question
- `sum: List[Int] => Int` 関数の実装を指定するプロパティを考え出せ
  - リストをリバースしてから合計すると、元のリバースしていないリスト合計した場合と同じ結果になるはずである
  - リストの全ての要素が同じ値である場合、合計はどうなるか
  - 他のプログラムは思いつけるか

---
### EXERCISE8-2
Question
- List[Int]の最大値を検出する関数を指定するプロパティはどのようなものになるか

Answer

```scala=
val intListIntMaxRange = Gen.listOf(Gen.choose(Int.MinValue, Int.MaxValue))
```

---

プロパティベースのテストライブラリ
- テストケースの最小化
  - 閾値
- 包括的なテストケースの生成
  - Gen[A]によって生成される値の集まりをドメインという
    - 関数の定義域という意味
    - https://ja.wikipedia.org/wiki/%E5%AE%9A%E7%BE%A9%E5%9F%9F
  - サンプル値の生成ではなく、全ての値を検証することが可能

---

## 8.2 データ型と関数の選択
- ライブラリのデータ型と関数を発見する
- プロパティベースのテストをするためのライブラリを設計する
- ...ここで示すライブラリの設計方法と完成品が、自分で考えたものと同じであるとは限らない

---

### 8.2.1 API:最初のコード
テスト用のライブラリで使用するデータ型はどのようなものでしょうか？

Gen.listOfの例

- `Gen.listOf`は、`Gen[Int] => Gen[List[Int]]` というシグネチャを持つ関数
```scala=
val intList: Gen[Int] => Gen[List[Int]] = Gen.listOf(_)
// val intList = Gen.listOf(Gen.choose(0, 100))
```

- 多層関数（コンビネーターを別々に作成しないため）
```scala=
def liftOf[A](a: Gen[A]): Gen[List[A]]
```

- 生成するリストサイズを渡す
```scala=
def liftOfN[A](n: Int, a: Gen[A]): Gen[List[A]]
```

---

forAllの例

- `Gen[List[Int]]`に対応する
```scala=
val prop =
forAll(intList)(ns => ns.reverse.reverse == ns) &&
forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
```

- 型のよるポリモーフィズム
- `Gen[A]`をメソッドにバインドした結果として、`Prop`という新しい型を定義している
```scala=
def forAll[A](a: Gen[A])(f: A => Boolean): Prop
trait Prop {
  def &&(p: Prop): Prop
}
```

---

### 8.2.2 プロパティの意味とAPI
- 型と関数に意味を持たせる
- `Prop`
  - `forAll` : プロパティを作成する関数 
  - `&&` : プロパティを合成する演算子
  - `check` : プロパティを実行する関数（コンソール出力という副作用あり）

<br>

### EXERCISE8-3
```scala=
def &&(p: Prop): Prop = new Prop {
  def check: Boolean = Prop.this.check && p.check
}
```

---

- テストの失敗におけるトレーサビリティを意識して、Either`を返すように変更する
- どれだけのテストが失敗したのかの表現方法
  - プロパティを失敗させた値の型が必要？
  - `Prop[A]`を追加し、`Either[A,SuccessCount]`にする？
  - （採用）コンソール出力した方が便利？
- 成功したテストの件数の表現方法

```scala=
trait Prop {
  import Prop._
  
  // def check: Either[(String, Int), Int]
  def check: Either[(FailedCase, SuccessCount), SuccessCount]
}
object Prop {
  // 型エイリアス
  type FailedCase = String
  type SuccessCount = Int
}
```

---

### 8.2.3 ジェネレータの意味とAPI
- `Gen[A]`がA型の値を生成する方法として考えられる一つの例が、乱数ジェネレータ
- `Gen` : 乱数ジェネレーターの状態遷移をラッピングする型にしてみる

```scala=
case class Gen[A](sample: State[RNG, A])
```

> ### EXERCISE8-4
```scala=
import RNG
import State
case class Gen[A](sample: State[RNG, A])
def choose(start: Int, stopExclusive: Int): Gen[Int] = {
  Gen(State(RNG.nonNegativeInt).map(n => start + n % (stopExclusive - start)))
}
```


> ### EXERCISE8-5
```scala=
def unit[A](a: => A): Gen[A] = Gen(State.unit(a))

def boolean: Gen[Boolean] = Gen(State(RNG.boolean))

def liftOfN[A](n: Int, g: Gen[A]): Gen[List[A]] =
  Gen(State.sequence(List.fill(n)(g.sample)))
```

---

重要なこと
- どの演算がプリミティブで、どの演算が派生なのかを理解すること
- 小さいく最も表現豊かなプリミティブを見つけ出すこと

探求の方法
- パターンを見つけ出し、パターンを抜き出してコンビネーターにして、プリミティブを改良する

遊ぶこと
- 具体的な例や、重要な問題を解決、便利な機能をいきなり実装してはいけない
- 表現が豊かなプリミティブ・演算を試してみることで、設計上の疑問を抽出するようにする

---

### 8.2.4 生成された値に依存するジェネレーター
- 値を生成した後、その値に基づいて次に使用するジェネレーターを決定する

> ### EXERCISE8-6
```scala=
def flatMap[B](f: A => Gen[B]): Gen[B] =
  Gen(sample.flatMap(a => f(a).sample))

def listOfN(size: Gen[Int]): Gen[List[A]] =
  size flatMap(a => this.listOfN(a))

def listOfN(size: Int): Gen[List[A]] =
  Gen.liftOfN(size, this)

object Gen {
  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(g.sample)))
}
```

> ### EXERCISE8-7
```scala=
def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] =
  boolean.flatMap(b => if (b) g1 else g2)
```

> ### EXERCISE8-8
```scala=
def weighted[A](g1: (Gen[A], Double), g2: (Gen[A], Double)): Gen[A] = {
  val g1Threshold = g1._2.abs / (g1._2.abs + g2._2.abs)
  Gen(State(RNG.double).flatMap(d => if (d < g1Threshold) g1._1.sample else g2._1.sample))
}
```

### 8.2.5 Propデータ型の改良
```scala=
trait Prop {
  def check: Either[(FailedCase, SuccessCount), SuccessCount]
}
object Prop {
  type FailedCase = String
  type SuccessCount = Int
}
```

- 「テストケースをいくつ調べればプロパティがテストにパスしたとされるか」という観点が抜けている
- 依存関係を抽象化して定義する

```scala=
object Prop {
  type FailedCase = String
  type SuccessCount = Int
  type TestCases = Int
  type Result = Either[(FailedCase, SuccessCount), SuccessCount]

  def check: Either[(FailedCase, SuccessCount), SuccessCount] = ???
}
case class Prop(run: TestCases => Result)
```

- Eitherの両側で、成功するテスト数を記録してるが、テストが成功した場合の数は、テスト数と同じであるため、一旦、`Option`にしておく

```scala=
type Result = Option[(FailedCase, SuccessCount)]
```

- None
  - テストが全て成功したことを意味する
- Some
  - テストが失敗したことを意味する

**本来のOptionと意味が反転した...**

- 上記に相当する、新しい型を作成してみる


---

```scala=
object Prop {
  sealed trait Result {
    def isFalsified: Boolean
  }
  case object Passed extends Result {
    def isFalsified = false
  }
  case class Falsified(failure: FailedCase,
                       successes: SuccessCount) extends Result {
    def isFalsified = true
  }
  case object Proved extends Result {
    def isFalsified = false
  }
...
}
```

- `Passed` : 全てのテストをパスしたことを示す
- `Falsified` : 失敗したことを示す

`forAll` が実装できるか試してみる

```scala=
def forAll[A](a: Gen[A])(f: A => Prop): Prop
```

- テストケースをランダムに生成するために、依存関係としてRNGが必要
- 依存関係をPropに伝播させる

```scala=
case class Prop(run: (TestCases, RNG) => Result)
```


> ### EXERCISE8-9
```scala=
case class Prop(run: (MaxSize, TestCases, RNG) => Result) {
  def &&(p: Prop) = Prop {
    (max, n, rng) => run(max, n, rng) match {
      case Passed | Proved => p.run(max, n, rng)
      case x => x
    }
  }

  def ||(p: Prop) = Prop {
    (max,n,rng) => run(max,n,rng) match {
      // In case of failure, run the other prop.
      case Falsified(msg, _) => p.tag(msg).run(max,n,rng)
      case x => x
    }
  }

  def tag(msg: String) = Prop {
    (max,n,rng) => run(max,n,rng) match {
      case Falsified(e, c) => Falsified(msg + "\n" + e, c)
      case x => x
    }
  }
}
```

## 8.3 テストケースの最小化

- デバックを用意にするためにテストケースを最小にするのが理想
- アプローチ
  - 縮小
    - 失敗するテストケースを特定した後、テストケースを最小化するための別の手続きを実行する
  - サイズに基づく生成
    - サイズが小さいものからテストケースを生成する

#### サイズに基づく生成を行う型を導入

```scala=
case class SGen[+A](forSize: Int => Gen[A])
```

> ### EXERCISE8-10
```scala=
case class Gen[+A](sample: State[RNG, A]) {
  def unsized: SGen[A] = SGen(_ => this)
}
```

> ### EXERCISE8-11
```scala=
case class SGen[+A](g: Int => Gen[A]) {

  def apply(n: Int): Gen[A] = g(n)

  def map[B](f: A => B): SGen[B] = SGen{ g(_) map f }

  def flatMap[B](f: A => SGen[B]): SGen[B] = {
    val g2: Int => Gen[B] = n => {
      g(n) flatMap { f(_).g(n) }
    }
    SGen(g2)
  }

  def **[B](s2: SGen[B]): SGen[(A,B)] =
    SGen(n => apply(n) ** s2(n))

}
```

> ### EXERCISE8-12
```scala=
object Gen {
  def listOf[A](g: Gen[A]): SGen[List[A]] = SGen(n => listOfN(n, g))
}
```

---

- SGenとforAllの関係

  現状

  - SGenはサイズ指定を期待する

  - Prop.forAllは、サイズ情報を受け取らない

  対応

  - PropにSGenを対応させる

  - ジェネレータを様々なサイズで呼び出せるようにしたい

  - Propに最大サイズを受け取らせるようにする


```scala=
object Prop {
  type MaxSize = Int
}
case class Prop(run: (MaxSize, TestCases, RNG) => Result)
```

```scala=
  def forAll[A](g: SGen[A])(f: A => Boolean): Prop = forAll(g(_))(f)

  def forAll[A](g: Int => Gen[A])(f: A => Boolean): Prop = Prop {
    (max, n, rng) =>
      val casesPerSize = (n - 1) / max + 1
      val props: Stream[Prop] = Stream
          .from(0)
          .take((n min max) + 1)
          .map(i => forAll(g(i))(f))
      val prop: Prop = props
        .map(p => Prop {
          (max, n, rng) =>
          p.run(max, casesPerSize, rng)
        }).toList.reduce(_ && _)
      prop.run(max, n, rng)
  }
```

- `val casesPerSize` : サイズごとにランダムケースを生成
- `val props` : サイズごとにプロパティを1つ作成する
- `reduce` : ひとつのプロパティにまとめる

## 8.4 ライブラリの使用とユーザビリティの改善

### 8.4.1 単純な例

- ライブラリを使ってテストを生成し、問題がないか確認しておく

- Propに最大サイズを受け取らせることに関してListのmaxメソッドを例とする

```scala=
import Gen._
import Prop._

val smallInt = Gen.choose(-10, 10)
val maxProp = forAll(listOf(smallInt)) { ns =>
  val max = ns.max
  !ns.exists(_ > max)
}
```

- Propでrunを実行するのがやっかい
- Propコンパニオンオブジェクトにヘルパー関数を追加する

```scala=
def run(
    p: Prop,
    maxSize: Int = 100, 
    testCases: Int = 100, 
    rng: RNG = RNG.Simple(System.currentTimeMillis)): Unit =
    p.run(maxSize, testCases, rng) match {
      case Falsified(msg, n) =>
        println(s"! Falsified after $n passed tests: \n $msg")
      case Passed =>
        println(s"+ OK, passed $testCases tests.")
      case Proved =>
  }
```

> ### EXERCISE8-13
```scala=
val smallInt = Gen.choose(-10,10)
def listOf1[A](g: Gen[A]): SGen[List[A]] =
  SGen(n => g.listOfN(n max 1))
val maxProp1 = forAll(listOf1(smallInt)) { l =>
  val max = l.max
  !l.exists(_ > max)
}
```

> ### EXERCISE8-14
ex.) List(2,1,3).sorted => List(1,2,3)

```scala=
val sortedProp: Prop = forAll(listOf(smallInt)) { l =>
  val ls = l.sorted
  l.isEmpty ||
    ls.tail.isEmpty ||
    !ls.zip(ls.tail).exists { case (a,b) => a > b }
  }
```

### 8.4.2 並列処理のためのテストスイートの作成

- 前章で扱った並列処理のテスト

- 書いてみると...美しくないw
  
  - API設計の不備？

  - ヘルパー関数がないから？

プロパティの証明

- forAllが汎用すぎる

- ハードコーディングで良いのでPropコンパニオンオブジェクトに追加する

```scala=
def check[A](p: => Boolean): Prop = Prop { (_, _) =>
    if(p) Passed else Falsified("()", 0)
  }
```

- run(check(true)) が100回実行されてしまうことを懸念

```scala=
case object Proved extends Result
```

<br>

> ### EXERCISE8-15
https://github.com/fpinscala/fpinscala/blob/master/answers/src/main/scala/fpinscala/testing/Exhaustive.scala

---

Parのテスト

- `Par.map(Par.unit(1))(_ + 1)` と `Par.unit(2)` が等しいというプロパティの証明 

- Prop.checkプリミティブを使って意図を明確に表現する

```scala=
val ES: ExecutorService = Executors.newCachedThreadPool
def check(p: => Boolean): Prop =
    forAll(Gen.unit(()))(_ => p)

val p2 = Prop.check {
  val p = Par.map(Par.unit(1))(_ + 1)
  val p2 = Par.unit(2)
  p(ES).get == p2(ES).get
}
```

- map2を使って比較をParにリフトさせる

```scala=
def equal[A](p: Par[A], p2: Par[A]): Par[Boolean] =
  Par.map2(p, p2)(_ == _)

val p3 = check {
  equal(
    Par.map(Par.unit(1))(_ + 1),
    Par.unit(2)
  )(ES).get
}
```

- Parの実行を別の関数forAllへ移動する

```scala=
val S = weighted(
  choose(1,4).map(Executors.newFixedThreadPool) ->.75,
  Gen.unit(Executors.newCachedThreadPool) ->.25)
def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
  forAll(S.map2(g)((_,_))) { case (s, a) => f(a)(s).get }
```

- 固定サイズのスレッドプールエグゼキュータを75%の確率で作成
- 固定サイズではないスレッドプールエグゼキュータを25%の確率で作成

---

- S.map2(g)((_, _))は、2つのジェネレータを結合するため、手際が良くない
- 単純化するためのコンビネータを作成する

```scala=
case class Gen[+A](sample: State[RNG, A]) {
  def **[B](g: Gen[B]): Gen[(A, B)] =
　  (this map2 g)((_, _))
}

// （サンプルではforAllPar2という名称になってる可能性あり）
```

```scala=
def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
  forAll(S ** g) { case (s, a) => f(a)(s).get }
  // forAll(S ** g) { case s ** a => f(a)(s).get }

// （サンプルではforAllPar3という名称になってる可能性あり）
```

```scala=
object ** {
  def unapply[A,B](p: (A,B)) = Some(p) 
}
```

```scala=
def checkPar(p: Par[Boolean]): Prop =
  forAllPar(Gen.unit(()))(_ => p) // ここがforAllParに変わった

val p4 = checkPar {
  equal(
    Par.map(Par.unit(1))(_ + 1),
    Par.unit(2)
  )
}
```

---

- Parの他のプロパティも見てみる

```scala=
map(unit(x))(f) == unit(f(x))

val pint = Gen.choose(0,10) map Par.unit
val p5 = forAllPar(pint)(n => equal(Par.map(n)(y => y), n))
```

> ### EXERCISE8-16
```scala=
val pint2: Gen[Par[Int]] = choose(-100, 100)
    .listOfN(choose(0, 20))
    .map(l =>
      l.foldLeft(Par.unit(0))((p, i) =>
        Par.fork {
          Par.map2(p, Par.unit(i))(_ + _)
        })
```

> ### EXERCISE8-17
```scala=
val forkProp =
  Prop.forAllPar(pint2)(i => equal(Par.fork(i), i)) tag "fork"
```

## 8.5 高階関数のテストと今後の展望

- ジェネレータを使ってデータを生成する方法は用意されているが、関数を生成する良い方法がない

- ex.) List(1,2,3).takeWhile(_ < 3) => List(1, 2)

- takeWhile(<font color="red">ここの関数</font>)を生成したい...

<br>

> ### EXERCISE8-18
```scala=
l.takeWhile(f) ++ l.dropWhile(f) == l

// 空でない場合、残りのリストは、述語を「満たさない」要素で開始する必要がある
```

---

- 特定の引数だけを調べることはできるが、テストフレームワークに生成させたい

```scala=
// 仮の定義
val isEven = (i: Int => i % 2 == 0)
val takeWhileProp =
Prop.forAll(Gen.listOf(int))(ns.takeWhile(isEven).forall(isEven))
```

- Gen[String => Int]を生成する場合を考えてみる

  - 入力Stringを無視して、Intを生成するデリゲート関数 :flushed:

```scala=
def genStringIntFn(g: Gen[Int]): Gen[String => Int] =
  g map (i => (s => i))
```

> ### EXERCISE8-19
```scala=
def genStringInt(g: Gen[Int]): Gen[String => Int]
```
```scala=
def genStringFn[A](g: Gen[A]): Gen[String => A]
```
- 入力値(String)を使用して生成するAに影響を与えるため、内部的にRNGを使う
```scala=
def genStringFn[A](g: Gen[A]): Gen[String => A] =
  Gen {
    State { (rng: RNG) => ??? }
  }
```
- RNGを変更して生成した値と、入力(String)のハッシュ値を掛け合わせる
```scala=
def genStringFn[A](g: Gen[A]): Gen[String => A] = Gen {
  State { (rng: RNG) =>
    val (seed, rng2) = rng.nextInt
    val f = (s: String) => g.sample.run(RNG.Simple(seed.toLong ^ s.hashCode.toLong))._1
    (f, rng2)
  }
}
```

- 以下はより一般化したもの？
- 実装は演習としての課題
- 参考は「https://www.youtube.com/watch?v=CH8UQJiv9Q4」
```scala=
trait Cogen[-A] {
  def sample(a: A, rng: RNG): RNG
}
def fn[A,B](in: Cogen[A])(out: Gen[B]): Gen[A => B]
```


> ### EXERCISE8-20
### This is an open-ended exercise and we don't give an answer here.

---

## 8.6 ジェネレータの法則
- Genに実装してきた関数は、Par,List,Stream,Optionに定義したものと似ている
  
  - シグネチャが同じなのか？
  
  - 法則が同じなのか？

- Parの法則

  - map(x)(id) == x

  - Gen.mapの実装に対しても有効である（Stream, List, Option, Stateでも）

- 複数のドメインにまたがる基本的なパターンが明らかになった

- PartⅢでは、これらのパターンに名前を調べ、それらを制御する法則を発見していく

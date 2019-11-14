## fpinscala 輪読会#5

## 第5章 正確と遅延

---

#### 例えば...

トランプから奇数のカードを抜き取り、クイーンを裏返す

- 方法1

  - 奇数のカードを抜き取って、その後でクリーンを裏返す

- 方法2

  - 奇数のカードを抜き取る時に、クイーンなら裏返す

---

#### 方法1

```scala=
scala> List(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).map(_ * 3)
res0: List[Int] = List(36,42)
```

- `map(_ + 10)` で中間リストを生成し、`filter(_ % 2 == 0)` に渡される

- `filter(_ % 2 == 0)` で中間リストを生成し、`map(_ * 3)` に渡される

- `map(_ * 3)` で最終的なリストが生成される

---

評価のトレース

```scala=
List(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).map(_ * 3)

List(11,12,13,14).filter(_ % 2 == 0).map(_ * 3)

List(12,14).map(_ * 3)

List(36,42)
```

- 入力の走査(map)と、出力のリストの生成(filter)が別々に行われている

- 一時的なリストを生成せずに、一連の変換を1回の処理にまとめる方法の方がよくない？？？

---

#### 方法2...の前に正格と非正格

---

### 5.1 正格関数と非正格関数

---

- 非正格性(non-strictness)

  - 引数の1つ以上を評価しないという関数の特性

  - 遅延性(laziness) と言った方がピンとくる

- 正格性(strictness)

  - 引数を常に評価する
 
  - ほとんどのプログラミング言語の標準

---

特に明記しない場合、Scalaの関数定義はすべて正格である

```scala=
def square(x: Double): Double = x * x
```
- `square(41.0 + 1.0)`

  - 呼び出しの引数は計算済みの `42.0` という値が渡される

- `square(sys.error("failure"))`

  - 呼び出す前にエラーになる

---

非正格の例を紹介する

- 短絡論理関数
  - `&&` `||` は、引数を評価しない関数として考えることもできる
  
  - 以下の `{ println("!!"); true }` は評価されない

```scala=
scala> false && { println("!!"); true }
res0 Boolean = false
```

- if制御構造
  - `input` は、`if(input.isEmpty)` が `false` の場合のみ返却される

  - ただし、条件のパラメータは常に正格

```scala=
val result = if(input.isEmpty) sys.error("empty input") else input
```

---

非正格関数の定義

```scala=
def if2[A](cond: Boolean, onTrue: () => A, onFalse: () => A): A =
  if(cond) onTrue() else onFalse()
```

```scala=
val a = 0
if2(a < 22,
  () => println("a"),
  () => println("b")
)
```

- `() => A` 型の値は、0個の引数を受け取り、Aを返す関数である

- 評価されない形式の式のことをサンク(thunk)という 

---

- より便利な構文が以下

```scala=
def if2[A](cond: Boolean, onTrue: => A, onFalse: => A): A =
  if(cond) onTrue else onFalse
```

```scala=
scala> if2(false, sys.error("fall"), 3)
res0 Int = 3
```

- ==サンクは関数の本体で参照される場所ごとに1回だけ評価される==

---

- Scalaは、引数の評価結果を（デフォルトでは）キャッシュしない

```scala=
def maybeTwice(b: Boolean, i: => Int): Int =
  if(b) i + i else 0
```
```scala=
val x = maybeTwice(true, {println("hi"); 1+41 })
println(x)

hi
hi
84
```

- `i`は、関数本体で2回参照されているが、式`1+41`も2回計算されている

- 副作用`hi`が、2回コンソール出力されているのが証拠

---

- 結果を1回だけ評価したい場合は、`lazy`キーワードを使って、値を明示的にキャッシュする

```scala=
def maybeTwice(b: Boolean, i: => Int): Int = {
  lazy val j = i
  if(b) j + j else 0
}
```
```scala=
hi
84
```

- `val`宣言に、`lazy`キーワードを指定すると、右辺の評価が最初に参照されるまで先送りされる 

- その後の参照で再評価されないように、結果をキャッシュする

- Scalaの非正格関数の引数は、値渡しではなく、名前渡しで受け取る

---

### 5.2 遅延リストの例

---

- 遅延性をどのように利用するか、遅延リスト（ストリーム）を例にみていく

- 具体的には、ストリームでの一連の変換を遅延を使って1回の処理にまとめる方法

---

```scala=
trait Stream[+A]
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty[A]: Stream[A] = Empty

  def apply[A](as: A*): Stream[A] =
    if(as.isEmpty) empty
    else cons(as.head, apply(as.tail: _*))
}
```

---

- `Stream`は、先頭と末尾で構成される(どちらも非正格)

- 評価の繰り返しを避けるため、`head`、`tail`を遅延値としてキャッシュ

- `List`とよく似ているが、`Cons`データコンストラクタは、明示的なサンクを受け取る

---

#### 5.2.1 ストリームを記憶し、再計算を回避する

---

- スマートコンストラクタ(`def cons`、`def empty`)を使用している

  - 追加の不変条件を満たすデータ型、実際のコンストラクタと異なるデータ型を生成する関数

  - 慣例では、対応するデータコンストラクタの1文字目を小文字にしたものを使用する

- `Stream[A]` を調査、走査する場合は、これらのサンクを強制的に評価する必要がある

  - `Stream`の先頭要素を取り出す関数を例にみていく

```scala=
trait Stream[+A] {
  def headOption: Option[A] = this match {
    case Empty => None
    case Cons(h, _) => Some(h())
  }
}
```

- `h()`を使って、`h`サンク強制的に評価
- `Cons`の末尾を評価しない

---

#### 5.2.2 ストリームを検査するためのヘルパー関数

---

:::success
### EXERCISE5-1

```scala=
def toList: List[A] = this match {
  case Empty => Nil
  case Cons(h, t) => h() :: t().toList
}
```

```scala=
// 末尾再帰
def toList: List[A] = {
  @annotation.tailrec
  def go(s: Stream[A], l: List[A]): List[A] = s match {
    case Cons(h, t) => go(t(), h() :: l)
    case _ => l
  }
  go(this, List())
}
```


---

### EXERCISE5-2

```scala=
def take(n: Int): Stream[A] = {
  @annotation.tailrec
  def go(n: Int, s: Stream[A], ss: Stream[A]): Stream[A] = s match {
    case Cons(_, _) if n <= 0 => ss
    case Cons(h, t) => go(n - 1, t(), Cons(h, () => ss))
    case _ => Empty
  }
  go(n, this, Stream())
}
```

```scala=
def drop(n: Int): Stream[A] = {
  @annotation.tailrec
  def go(n: Int, s: Stream[A]): Stream[A] = s match {
    case Cons(_, t) if n <= 1 => t()
    case Cons(_, t) => go(n - 1, t())
    case _ => Empty
  }
  go(n, this)
}
```

---

### EXERCISE5-3

```scala=
def takeWhile(p: A => Boolean): Stream[A] = {
  @annotation.tailrec
  def go(s: Stream[A], ss: Stream[A]): Stream[A] = s match {
    case Cons(h, t) if p(h())  => go(t(), Cons(h, () => ss))
    case Cons(_, t) => go(t(), ss)
    case Empty => ss
  }
  go(this, Stream())
}
```

---

### 5.3 プログラムの記述と評価の切り分け

---

- `Stream`では、一連の要素を生成するための処理を組み立てることが可能であり、そうした処理のステップはそれらの要素が実際に必要になるまで実行されない

- 遅延を利用することで、式の記述をその式の評価から切り離すことが可能となる

- 例として、`exists`関数をみていく

```scala=
def exists(p: A => Boolean): Boolean = this match {
  case Cons(h, t) => p(h()) || t().exists(p)
  case _ => false
}
```

- `p(h())`がtrueを返した場合
  - 走査は終了する
  - `||` が第2引数に関して非正格であり、ストリームの`tail`が、`lazy val`定義されているため、ストリームの末尾は評価されない

---

- `foldRight`による汎用的な再帰

```scala=
def foldRight[B](z: => B)(f: (A, => B) => B): B = this match {
  case Cons(h, t) => println(h()); f(h(), t().foldRight(z)(f))
  case  _=> z
}
```
- `(f: (A, => B) => B)`は、関数`f`の第2引数を名前渡しで受け取るという意味

- ==評価しない選択があるという意味==


```scala=
def exists2(p: A => Boolean): Boolean =
  foldRight(false)((a, b) => p(a) || b)
```
- `b`は、ストリームの末尾を抱える評価されない再帰ステップ
- `p(a)`が、trueを返した場合、`b`は評価されず計算はそこで終了する
- ==非正格な`foldRight`は、走査を中断することができる==

```scala=
// Program Trace
Stream(1,2,3,4,5,6,7,8,9,10).exists2(_ == 5)
foldRight(false)((a, b) => a == 5 || b)

f(1, Stream(2,3,4,5,6,7,8,9,10) => 1 == 5 || Stream(2,3,4,5,6,7,8,9,10).foldRight(false)((a, b) => a == 5 || b))
f(2, Stream(3,4,5,6,7,8,9,10) => 2 == 5 || Stream(3,4,5,6,7,8,9,10).foldRight(false)((a, b) => a == 5 || b))
f(3, Stream(4,5,6,7,8,9,10) => 3 == 5 || Stream(4,5,6,7,8,9,10).foldRight(false)((a, b) => a == 5 || b))
f(4, Stream(5,6,7,8,9,10) => 4 == 5 || Stream(5,6,7,8,9,10).foldRight(false)((a, b) => a == 5 || b))
f(5, Stream(6,7,8,9,10) => 5 == 5 || Stream(6,7,8,9,10).foldRight(false)((a, b) => a == 5 || b))
```

---


### EXERCISE5-4

```scala=
def forAll(p: A => Boolean): Boolean =
  foldRight(true)((a, b) => p(a) && b)
```

```scala=
// Program Trace
Stream(1,2,3,4,5,6,7,8,9,10).forAll(_ < 5)
foldRight(true)((a, b) => a < 5 && b)

f(1, Stream(2,3,4,5,6,7,8,9,10) => 1 < 5 && Stream(2,3,4,5,6,7,8,9,10).foldRight(true)((a, b) => a < 5 || b))
f(2, Stream(3,4,5,6,7,8,9,10) => 2 < 5 && Stream(3,4,5,6,7,8,9,10).foldRight(true)((a, b) => a < 5 || b))
f(3, Stream(4,5,6,7,8,9,10) => 3 < 5 && Stream(4,5,6,7,8,9,10).foldRight(true)((a, b) => a < 5 || b))
f(4, Stream(5,6,7,8,9,10) => 4 < 5 && Stream(5,6,7,8,9,10).foldRight(true)((a, b) => a < 5 || b))
f(5, Stream(6,7,8,9,10) => 5 < 5 && Stream(6,7,8,9,10).foldRight(true)((a, b) => a < 5 || b))
```

---

### EXERCISE5-5

```scala=
// fpinの答えミスってる
import Stream._
def takeWhile2(p: A => Boolean): Stream[A] =
  foldRight(empty[A])((a, b) => if (p(a)) cons(a, b) else b)
```

```scala=
// Program Trace
Stream(1,2,3,4,5).takeWhile2(_ % 2 == 0)
foldRight(empty[A])((a, b) => if (f(a)) cons(a, b) else b)

f(1, (2,3,4,5))
f(1, (2,3,4,5)) f(2, (3,4,5))
f(1, (2,3,4,5)) f(2, (3,4,5)) f(3, (4,5))
f(1, (2,3,4,5)) f(2, (3,4,5)) f(3, (4,5)) f(4, (5,Empty))
f(1, (2,3,4,5)) f(2, (3,4,5)) f(3, (4,5)) f(4, (5,Empty)) f(5, (Empty))
(5, Empty) => if (5 % 2 == 0) Empty
(4, Empty) => if (4 % 2 == 0) Cons(4,Empty)
(3, (4,Empty)) => if (3 % 2 == 0) (4,Empty)
(2, (4,Empty)) => if (2 % 2 == 0) Cons(2, Cons(4,Empty))
(1, (2,4,Empty)) => if (1 % 2 == 0) (2,4,Empty)

```

---

### EXERCISE5-6

```scala=
def headOption2: Option[A] =
  foldRight(None: Option[A])((h,_) => Some(h))
```

- 途中（最初の要素の次）でループ抜けてる

---

### EXERCISE5-7

```scala=
def map[B](f: A => B): Stream[B] = 
  foldRight(empty[B])((a, b) => cons(f(a), b))

val s = Stream(1,2,3,4,5).map(_ * 2)
println(s.toList)
```

```scala=
def filter(p: A => Boolean): Stream[A] =
  foldRight(empty[A])((a, b) => if(p(a)) cons(a, b) else b)
```

```scala=
// NG
def append[E >: A](s: Stream[E]): Stream[E] =
  foldRight(s)((a, b) => cons(a, b))
  
val s = Stream(1,2,3,4,5).append(Stream(6,7,8,9,10))
println(s.toList)
```

```scala=
def flatMap[B](f: A => Stream[B]): Stream[B] =
  foldRight(empty[B])((a, b) => f(a).append(b))

import Stream._
val s = Stream(1,2,3,4,5).flatMap(a => cons(a * 2, empty))
println(s.toList)
```

---

- 完全なStreamの生成ではない

- 他の処理が調べるまでStreamを実際の生成する処理は実行されない

- 中間結果をインスタンス化せずに、関数を次々に呼び出すことができる

- Streamプログラムをトレースして確認していく （ リスト5-5 ）

  - この式を==List==に変換することで強制的に評価する
  
  - 空のストリームは、空のリストへ

  - mapとfilterの交換が交互に実行されている

    - mapの出力として要素を1つ生成する作業

    - その要素が2で割り切れるかどうか確認するfilter

  - ==mapの結果として得られる中間ストリームを完全にインスタンス化しないこと==

- マッチする要素が存在する場合に、その最初の要素だけを返すfindメソッドを定義する

  - findはマッチした時点で終了する

---

### 5.4 無限ストリーム

- 漸進的な関数は、無限ストリームに対応している

  - 1の無限ストリームの例をみていく
  
  - onesは無限だが、ストリーム要素のうち要求された出力を生成するのに必要な部分だけ調べる

```scala=
scala> val ones: Stream[Int] = Stream.cons(1, ones)
```

```scala=
scala> ones.take(5).toList
res0: List[Int] = List(1,1,1,1,1)

scala> ones.exists(_ % 2 != 0)
res1: Boolean = true
```

---

### EXERCISE5-8

```scala=
// This is more efficient than `cons(a, constant(a))` since it's just
def constant[A](a: A): Stream[A] = {
    lazy val tail: Stream[A] = Cons(() => a, () => tail)
    tail
}
```
---

### EXERCISE5-9

```scala=
def from(n: Int): Stream[Int] = {
  cons(n, from(n+1))
}
```
---

### EXERCISE5-10

```scala=
val fibs = {
  def go(f0: Int, f1: Int): Stream[Int] = 
    cons(f0, go(f1, f0+f1))
  go(0, 1)
}

```

---

### EXERCISE5-11

```scala=
def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = {
  f(z) match {
    case Some((h, s)) => cons(h, unfold(s)(f))
    case None => empty
  }
}
```

- unfold関数は、余再帰と呼ばれる関数の一例である

- データ構築の流れに沿って再帰。

- 再帰する際に引数に取っている遅延データのうち、後ろの残りのみを取る

- 最後のデータまで処理しきらなくても最初の方のデータを参照可能。

- 遅延構築可能なデータの構築に用いるとスタックやヒープの節約になる

- 正格データを生成するには末尾再帰

- 遅延データを生成するには普通の再帰

---

### EXERCISE5-12

```scala=
def unfold[B, S](z: S)(f: S => Option[(B, S)]): Stream[B] = {
  f(z) match {
    case Some((h, s)) => cons(h, unfold(s)(f))
    case None => empty
  }
}
```

```scala=
val fibsViaUnfold = unfold((0, 1)) { case (f0, f1) => Some((f0, (f1, f0 + f1))) }
// Program Trace
Some(0, (1, 0+1)) => Cons(0, unfold(((1, 0+1))(f)))
Some(1, (1, 1+1)) => Cons(0, Cons(1, unfold((1, 1+1)(f))))
Some(1, (2, 1+2)) => Cons(0, Cons(1, Cons(1, unfold((2, 1+2)(f)))))
Some(2, (3, 2+3)) => Cons(0, Cons(1, Cons(1, Cons(2, unfold((3, 2+3)(f))))))
Some(3, (5, 3+5)) => Cons(0, Cons(1, Cons(1, Cons(2, Cons(3, unfold((5, 3+5)(f)))))))
Some(5, (8, 5+8)) => Cons(0, Cons(1, Cons(1, Cons(2, Cons(3, Cons(5, unfold((8, 5+8)(f))))))))
・・・以降無限に生成していく
```

```scala=
def fromViaUnfold(n: Int): Stream[Int] = unfold(n)(n => Some((n, n + 1)))
// Program Trace
fromViaUnfold(0)
Some(0, 0+1) => Cons(0, unfold(0+1)(f))
Some(1, 1+1) => Cons(0, Cons(1, unfold(1+1)(f)))
Some(2, 2+1) => Cons(0, Cons(1, Cons(2, unfold(2+1)(f))))
Some(3, 3+1) => Cons(0, Cons(1, Cons(2, Cons(3, unfold(3+1)(f)))))
Some(4, 4+1) => Cons(0, Cons(1, Cons(2, Cons(3, Cons(4, unfold(4+1)(f))))))
・・・以降無限に生成していく
```

```scala=
def constantViaUnfold[A](a: A): Stream[A] = unfold(a)(_ => Some((a, a)))
// Program Trace
constantViaUnfold(1)
Some(1, 1) => Cons(1, unfold(1)(f))
Some(1, 1) => Cons(1, Cons(1, unfold(1)(f)))
Some(1, 1) => Cons(1, Cons(1, Cons(1, unfold(1)(f))))
Some(1, 1) => Cons(1, Cons(1, Cons(1, Cons(1, unfold(1)(f)))))
Some(1, 1) => Cons(1, Cons(1, Cons(1, Cons(1, Cons(1, unfold(1)(f))))))
・・・以降無限に生成していく
```

```scala=
val onesViaUnfold = unfold(1)(_ => Some((1, 1)))
// Program Trace・・・一緒になってしまったw
Some(1, 1) => Cons(1, unfold(1)(f))
Some(1, 1) => Cons(1, Cons(1, unfold(1)(f)))
Some(1, 1) => Cons(1, Cons(1, Cons(1, unfold(1)(f))))
Some(1, 1) => Cons(1, Cons(1, Cons(1, Cons(1, unfold(1)(f)))))
Some(1, 1) => Cons(1, Cons(1, Cons(1, Cons(1, Cons(1, unfold(1)(f))))))
・・・以降無限に生成していく
```


---

### EXERCISE5-13

```scala=
def mapViaUnfold[B](f: A => B): Stream[B] =
  unfold(this) {
    case Cons(h, t) => Some((f(h()), t()))
    case _ => None
  }
// Program Trace
Stream(1,2,3,4,5).mapViaUnfold(_ * 2)
unfold(Stream(1,2,3,4,5))
Cons(1, Stream(2,3,4,5)) => Some(f(1*2), Stream(2,3,4,5)) => Cons(2, unfold(Stream(2,3,4,5))(f))
Cons(2, Stream(3,4,5)) => Some(f(2*2), Stream(3,4,5)) => Cons(2, Cons(4, unfold(Stream(3,4,5))(f)))
Cons(3, Stream(4,5)) => Some(f(3*2), Stream(4,5)) => Cons(2, Cons(4, Cons(6, unfold(Stream(4,5))(f))))
Cons(4, Stream(5)) => Some(f(4*2), Stream(5)) => Cons(2, Cons(4, Cons(6, Cons(8, unfold(Stream(5))(f)))))
Cons(5, Stream(Empty)) => Some(f(5*2), Stream(Empty)) => Cons(2, Cons(4, Cons(6, Cons(8, Cons(10, unfold(Stream(Empty))(f))))))
None => None => Cons(2, Cons(4, Cons(6, Cons(8, Cons(10, Empty)))))
```

```scala=
def takeViaUnfold(n: Int): Stream[A] =
  unfold((this, n)) {
    case (Cons(h, t), 1) => Some((h(), (empty, 0)))
    case (Cons(h, t), n) if n > 1 => Some((h(), (t(), n - 1)))
    case _ => None
  }
// Program Trace
Stream(1,2,3,4,5).takeViaUnfold(3)
unfold(Stream(1,2,3,4,5), 3)
Cons(1, Stream(2,3,4,5)) => Some(1, (Stream(2,3,4,5), 3-1)) => Cons(1, unfold(Stream(2,3,4,5), 2))
Cons(2, Stream(3,4,5)) => Some(2, (Stream(3,4,5), 2-1)) => Cons(1, Cons(2, unfold(Stream(3,4,5), 1))
Cons(3, Stream(4,5)) => Some(3, (Emtpy, 0)) => Cons(1, Cons(2, Cons(3, unfold(Emtpy, 0))
None => None => Cons(1, Cons(2, Cons(3, Empty)
```

```scala=
def takeWhileViaUnfold(f: A => Boolean): Stream[A] =
  unfold(this) {
    case Cons(h, t) if f(h()) => Some((h(), t()))
    case _ => None
  }
// Program Trace
Stream(1,2,3,4,5).takeWhileViaUnfold(_ < 3)
unfold(Stream(1,2,3,4,5))
Cons(1, Stream(2,3,4,5)) => Some(1, Stream(2,3,4,5)) => Cons(1, unfold(Stream(2,3,4,5)))
Cons(2, Stream(3,4,5)) => Some(2, Stream(3,4,5)) => Cons(1, Cons(2, unfold(Stream(3,4,5)))
_ => None => Cons(1, Cons(2, Empty))
```

```scala=
def zipWith[B, C](s2: Stream[B])(f: (A, B) => C): Stream[C] =
  unfold((this, s2)) {
    case (Cons(h1, t1), Cons(h2, t2)) =>
      Some((f(h1(), h2()), (t1(), t2())))
    case _ => None
  }
// Program Trace
Stream(1,2,3).zipWith(Stream(4,5,6))(_+_)
unfold(Stream(1,2,3), Stream(4,5,6))
(Cons(1, Stream(2,3)), Cons(4, Stream(5,6))) => Some(1+4, (Stream(2,3),Stream(5,6))) => Cons(5, unfold((Stream(2,3),Stream(5,6))))
(Cons(2, Stream(3)), Cons(5, Stream(6))) => Some(2+5, (Stream(3), Stream(6))) => Cons(5, Cons(7, unfold((Stream(3), Stream(6)))))
(Cons(3, Empty), Cons(6, Empty)) => Some(3+6, (Empty, Empty)) => Cons(5, Cons(7, Cons(9, unfold((Empty, Empty)))))
_ => None => Cons(5, Cons(7, Cons(9, Empty)))
```

```scala=
def zip[B](s2: Stream[B]): Stream[(A, B)] =
  zipWith(s2)((_, _))
// Program Trace
Stream(1,2,3).zip(Stream(4,5,6))((_, _))
unfold(Stream(1,2,3), Stream(4,5,6))((_, _))
(Cons(1, Stream(2,3)), Cons(4, Stream(5,6))) => Some((1,4), (Stream(2,3),Stream(5,6))) => Cons((1,4), unfold((Stream(2,3),Stream(5,6))))
(Cons(2, Stream(3)), Cons(5, Stream(6))) => Some((2,5), (Stream(3), Stream(6))) => Cons((1,4), Cons((2,5), unfold((Stream(3), Stream(6)))))
(Cons(3, Empty), Cons(6, Empty)) => Some((3,6), (Empty, Empty)) => Cons((1,4), Cons((2,5), Cons((3,6), unfold((Empty, Empty)))))
_ => None => Cons((1,4), Cons((2,5), Cons((3,6), Empty)))
```

```scala=
def zipAll[B](s2: Stream[B]): Stream[(Option[A], Option[B])] =
  zipWithAll(s2)((_, _))

def zipWithAll[B, C](s2: Stream[B])(f: (Option[A], Option[B]) => C): Stream[C] =
  unfold((this, s2)) {
    case (Empty, Empty) => None
    case (Cons(h, t), Empty) => Some(f(Some(h()), Option.empty[B]) -> (t(), empty[B]))
    case (Empty, Cons(h, t)) => Some(f(Option.empty[A], Some(h())) -> (empty[A] -> t()))
    case (Cons(h1, t1), Cons(h2, t2)) => Some(f(Some(h1()), Some(h2())) -> (t1() -> t2()))
  }
// Program Trace

(Int, (Int, Int)) = 1 -> (2 -> 3)

Stream(1,2,3).zipAll(Stream(4,5,6,7))
unfold(Stream(1,2,3), Stream(4,5,6,7))
(Cons(1, Stream(2,3)), Cons(4, Stream(5,6,7))) => Some((Some(1), Some(4)), (Stream(2,3), Stream(5,6,7))) => Cons((Some(1), Some(4)), unfold((Stream(2,3), Stream(5,6,7))))
(Cons(2, Stream(3)), Cons(5, Stream(6,7))) => Some((Some(2), Some(5)), (Stream(3), Stream(6,7))) => Cons((Some(1), Some(4)), Cons((Some(2), Some(5)), unfold((Stream(2,3), Stream(5,6,7)))))
(Cons(3, Empty), Cons(6, Stream(7))) => Some((Some(3), Some(6)), (Empty, Stream(7))) => Cons((Some(1), Some(4)), Cons((Some(2), Some(5)), Cons((Some(3), Cons(6)), unfold((Empty, Stream(7))))))

(Empty, Empty) => None =>
  Cons((Some(1), Some(4)), Cons((Some(2), Some(5)), Cons((Some(3), Some(6)), Cons(None, Some(7)))))
```

---

### EXERCISE5-14

```scala=
def startsWith[A](s: Stream[A]): Boolean =
  zipAll(s).takeWhile(_._2.nonEmpty) forAll {
    case (h, h2) => h == h2
  }
// Program Trace
Stream(1,2,3).startsWith(Stream(1,2))

zipAll(Stream(1,2))
Cons((Some(1), Some(1)), Cons((Some(2), Some(2)), Cons((Some(3), None))))

takeWhile(Cons((Some(1), Some(1)), Cons((Some(2), Some(2)), Cons((Some(3), None)))))
Cons((Some(1), Some(1)), Cons((Some(2), Some(2))))

forAll(Cons((Some(1), Some(1)), Cons((Some(2), Some(2)))))
foldRight(true)()

foldRight(true)((h, h2) => h == h2)

Cons((Some(1), Some(1)), Cons((Some(2), Some(2)))) => Some(1) == Some(1)
...true
```


---

### EXERCISE5-15

```scala=
def unfold[B, S](z: S)(f: S => Option[(B, S)]): Stream[B] = {
  f(z) match {
    case Some((h, s)) => cons(h, unfold(s)(f))
    case None => empty
  }
}

def tails: Stream[Stream[A]] =
  unfold(this) {
    case Empty => None
    case s => Some((s, s drop 1))
  } append Stream(empty)

// Program Trace
Stream(1,2,3).tails
unfold(Stream(1,2,3))
Some((Stream(1,2,3), Stream(2,3))) => Cons(Stream(1,2,3), unfold(Stream(2,3))(f))
Some((Stream(2,3), Stream(3))) => Cons(Stream(1,2,3), Cons(Stream(2,3), unfold(Stream(3))(f)))
Some((Stream(3), Empty)) => Cons(Stream(1,2,3), Cons(Stream(2,3), Cons(Stream(3), unfold(Empty)(f))))
None => Cons(Stream(1,2,3), Cons(Stream(2,3), Cons(Stream(3), Empty)))

append Stream(empty)

Cons(Stream(1,2,3), Cons(Stream(2,3), Cons(Stream(3), Empty), Empty))


```

---

### EXERCISE5-16

```scala=
def foldRight[B](z: => B)(f: (A, => B) => B): B = this match {
  case Cons(h, t) => f(h(), t().foldRight(z)(f))
  case _ => z
}
```

```scala=
def scanRight[B](z: B)(f: (A, () => B) => B): Stream[B] =
  foldRight((z, Stream(z)))((a, p0) => {
    lazy val p1 = p0
    val b2 = f(a, p1._1)
    (b2, cons(b2, p1._2))
  })._2

// Program Trace
Stream(1,2,3).scanRight(0)((a, b) => a + b))

z=(0, Stream(0))
foldRight(z)

Cons(1, Stream(2,3)) => f(1, Stream(2,3).foldRight(z))(f)) => p1=(0, Stream(0)) (3, Cons(3, Stream(0)))
Cons(2, Stream(3)) => f(1, f(2, Stream(3).foldRight(z))(f))
Cons(3, Empty) => f(1, f(2, f(3, Empty.foldRight(z))(f)))
_ => f(1, f(2, f(3, (0, Stream(0)))(f)))

f(1, f(2, f(3, (0, Cons(0))))) => p1=(0, Cons(0)),b2=3+0 => f(1, f(2, (3, Cons(3, Cons(0)))))
f(1, f(2, Cons(3, Cons(0)))) => p1=(3, Cons(3, Cons(0))),b2=3+2 => f(1, (5, Cons(5, Cons(3, Cons(0)))))
f(1, (5, Cons(5, Cons(3, Stream(0))))) => p1=(1, Cons(5, Cons(3, Cons(0)))),b2=1+5 => Cons(6, Cons(5, Cons(5, Cons(3, Cons(0)))))
Cons(6, Cons(5, Cons(5, Cons(3, Cons(0)))))
(6, 5, 3, 0)
```

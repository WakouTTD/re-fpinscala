# fpinscala 輪読会 #3

Scala関数型デザイン&プログラミングScalazコントリビューターによる関数型徹底ガイド

# 第3章関数型プログラミングのデータ構造
関数型のプログラムでは、変数を更新することや、ミュータブルなデータ構造を変更することはありません。

差し迫った問題として挙げられるのは、関数型プログラミングではどのような種類のデータを使用できるのか、それらをScalaでどのように定義するのか、そしてそれらをどのように操作するのかです。本章では、関数型データ構造の概念と、それらを操作する方法について説明します。

###### Hさんがよく言ってた「代数的データ型」について触れる章

## 3.1 関数型データ構造の定義

関数型データ構造は、当然ながら、純粋関数のみを使って操作されます。
純粋関数では、データを直接変更したり、他の副作用を発生させてはならないことを思い出してください。

***関数型データ構造は本質的にイミュータブル*** です。

Scalaでは空のリストをList()またはNilと記述します。aとbの2つのリストを連結する構文はa++bになります。

データを余分にコピーすることになるのでしょうか。
答えはNO


可変コレクションおよび不変コレクション
https://docs.scala-lang.org/ja/overviews/collections/overview.html


## 3.2 パターンマッチング

### Scalaのコンパニオンオブジェクト
Scalaでは、コンパニオンオブジェクトを使用することがむしろ慣例となっています

だけど、case class有るから、タテダはcase class使っちゃう

## 3.3 関数型データ構造でのデータ共有
データがイミュータブルであるとしたら、たとえばリストの要素を追加または削除する関数をどのようにして記述するのでしょうか。簡単です。たとえばxsというリストがある場合、要素1をリストの先頭に追加したら、新しいリスト（Cons(1,xs)）を返します。この場合、リストはイミュータブルなので、xsを実際にコピーする必要はなく、それを再利用するだけで済みます。これをデータ共有（datasharing）と呼びます。イミュータブルなデータを共有すると、関数をより効率よく実装できることがよくあります。その後のコードによってデータが変更される心配をせずに、常にイミュータブルなデータ構造を返せるようになるからです。データの変更や破壊を避けるために、悲観的なコピーを作成する必要はありません※9。同様に、mylist=Cons(x,xs)リストから先頭の要素を削除したい場合は、そのxsを返すだけです。実際に削除が発生することはありません。元のリストであるmylistは依然として利用可能で、元のままです。関数型データ構造は永続的であり、既存の参照がデータ構造での操作によって変化することはありません（図33）。リストをさまざまな方法で変更する関数をいくつか実装してみましょう。この関数と、ここで記述する他の関数は、Listコンパニオンオブジェクトの中で記述できます。

## 3.4 リストの再帰と高階関数の一般化

### 3.4.1 リストを扱うその他の関数

### 3.4.2 より単純なコンポーネントからリスト関数を組み立てるときの非効率性

## 3.5 ツリー

#### 代数的データ型とカプセル化

代数的データ型に関しては、型の内部表現を公開することから、カプセル化に違反しているという異論があるかもしれません。関数型プログラミングでは、カプセル化の問題に別の方法でアプローチします。一般的には、公開された場合にバグや不変条件への違反につながるような、注意を要するミュータブル状態はありません。型のデータコンストラクタが公開されても問題がない場合が多く、データコンストラクタを公開するかどうかは、データ型のパブリックAPIの機能と同じように決定されます※24。代数的データ型は、一連のケースが閉じている（固定されている）状況で使用するのが一般的です。ListとTreeの場合は、データコンストラクタを変更すると、これらのデータ型の定義を大きく変更することになります。Listは本質的に単方向リストであり、NilとConsの2つのケースはその便利なパブリックAPIの一部を形成します。Listよりも抽象的なAPIに対処するコードを記述することはもちろん可能ですが、そうした情報の隠ぺいは、Listに直接埋め込むのではなく、別のレイヤとして処理するのが効果的です。

代数的データ型
 代数的データ型（英: Algebraic data type）とはプログラミング、特に関数型プログラミングや型システムにおいて使われるデータ型である。それぞれの代数的データ型の値には、1個以上のコンストラクタがあり、各コンストラクタには0個以上の引数がある。

代数的データ型の値（データ）の感覚的な説明としては、引数で与えられた他のデータ型の値を、コンストラクタで包んだようなもの、である。コンストラクタに引数がある代数データ型は複合型（他のデータ型を組み合わせて形成する型）である。

代数的データ型の特殊な例として、直積型（1つのコンストラクタだけを持つ）と列挙型（引数なしの多くのコンストラクタを持つ）がある。

集合論において代数的データ型と等価なものとして直和がある。この集合の各元はタグ（コンストラクタと等価）とそのタグに対応する型のオブジェクト（コンストラクタの引数と等価）で構成される。

一般に代数的データ型は直積型の総和であり、再帰的に定義されることもある。各コンストラクタは直積型のタグとなって他と区別されるか、1つしかコンストラクタがない場合は、そのデータ型自体が直積型となる。さらにコンストラクタの引数の型が直積型の要素となる。引数のないコンストラクタは空に対応する。データ型が再帰的であるなら、その直積型の総和は再帰データ型となり、各コンストラクタによって再帰データ型が構成される。

[代数的データ型](https://ja.wikipedia.org/wiki/%E4%BB%A3%E6%95%B0%E7%9A%84%E3%83%87%E3%83%BC%E3%82%BF%E5%9E%8B)


代数的データ型としてのケースクラス
ケースクラス (case class) は、代数的データ型 (algebraic data type) をエンコードする: ケースクラスは数多くのデータ構造をモデリングするのに役に立ち、強力な不変式を簡潔なコードとして提供する。ケースクラスは、パターンマッチと共に利用すると特に有用だ。パターンマッチの解析器は、さらに強力な静的保証を提供する包括的解析 (exhaustivity analysis) を実装している。

ケースクラスで代数的データ型をエンコードする際は、以下のパターンを使おう
```scala=
sealed trait Tree[T]
case class Node[T](left: Tree[T], right: Tree[T]) extends Tree[T]
case class Leaf[T](value: T) extends Tree[T]
```
Tree[T] 型には Node と Leaf の 2 つのコンストラクタがある。型を sealed として宣言するとソースファイルの外でコンストラクタを追加できなくなるので、コンパイラに包括的解析を行わせることができる。

パターンマッチと共に使うと、上記のモデリングは簡潔でありかつ”明らかに正しい”コードになる:
```scala=
def findMin[T <: Ordered[T]](tree: Tree[T]) = tree match {
  case Node(left, right) => Seq(findMin(left), findMin(right)).min
  case Leaf(value) => value
}
```
木構造のような再帰的構造は、代数的データ型の古典的な応用を占めるが、代数的データ型が有用な領域はそれよりずっと大きい。特に、状態機械によく現れる直和 (disjoint union) は、代数的データ型で容易にモデル化できる。

[effective scala「代数的データ型としてのケースクラス」](http://scalajp.github.io/effectivescala/index-ja.html#%E9%96%A2%E6%95%B0%E5%9E%8B%E3%83%97%E3%83%AD%E3%82%B0%E3%83%A9%E3%83%9F%E3%83%B3%E3%82%B0-%E4%BB%A3%E6%95%B0%E7%9A%84%E3%83%87%E3%83%BC%E3%82%BF%E5%9E%8B%E3%81%A8%E3%81%97%E3%81%A6%E3%81%AE%E3%82%B1%E3%83%BC%E3%82%B9%E3%82%AF%E3%83%A9%E3%82%B9)

## 3.6 まとめ 
- 代数的データ型とパターンマッチング、単方向リストを含め、純粋関数型のデータ構造を実装する方法
- 純粋関数の記述とそれらの一般化


```scala=
// リスト 3-1
package fpinscala.datastructures

sealed trait List[+A] // `List` data type, parameterized on a type, `A`
case object Nil extends List[Nothing] // A `List` data constructor representing the empty list
/* Another data constructor, representing nonempty lists. Note that `tail` is another `List[A]`,
which may be `Nil` or another `Cons`.
 */
case class Cons[+A](head: A, tail: List[A]) extends List[A]

//case class Gons[+A](head: List[A], tail: A) extends List[A]

object List { //extends App { // `List` companion object. Contains functions for creating and working with lists.

  def sum(ints: List[Int]): Int = ints match { // A function that uses pattern matching to add up a list of integers
    case Nil => 0 // The sum of the empty list is 0.
    case Cons(x,xs) => x + sum(xs) // The sum of a list starting with `x` is `x` plus the sum of the rest of the list.
  }

  def product(ds: List[Double]): Double = ds match {
    case Nil => 1.0
    case Cons(0.0, _) => 0.0
    case Cons(x,xs) => x * product(xs)
  }

  def apply[A](as: A*): List[A] = // Variadic function syntax
    if (as.isEmpty) Nil
    else Cons(as.head, apply(as.tail: _*))

  val x = List(1,2,3,4,5) match {
    case Cons(x, Cons(2, Cons(4, _))) => x
    case Nil => 42
    case Cons(x, Cons(y, Cons(3, Cons(4, _)))) => x + y
    case Cons(h, t) => h + sum(t)
    case _ => 101
  }

  // EXERCISE 3ー1
  def x2 = List(1,2,3,4,5) match {
    case Cons(x, Cons(2, Cons(4, _))) => {println("a"); x}
    case Nil => {println("b"); 42}
    case Cons(x, Cons(y, Cons(3, Cons(4, _)))) => {println("c"); x + y}
    case Cons(h, t) => {println(s"d:$h:$t"); h + sum(t)}
    case _ => {println("e");101}
  }

  // リスト 3-2
  def append[A](a1: List[A], a2: List[A]): List[A] =
    a1 match {
      case Nil => a2
      case Cons(h,t) => Cons(h, append(t, a2))
    }

  // リスト 3-3
  def foldRight[A,B](as: List[A], z: B)(f: (A, B) => B): B = // Utility functions
    as match {
      case Nil => z
      case Cons(x, xs) => f(x, foldRight(xs, z)(f))
    }



  def sum2(ns: List[Int]) =
    foldRight(ns, 0)((x,y) => x + y)

  def product2(ns: List[Double]) =
    foldRight(ns, 1.0)(_ * _) // `_ * _` is more concise notation for `(x,y) => x * y`; see sidebar

  // EXERCISE 3ー2
  def tail[A](l: List[A]): List[A] =
    l match {
      case Nil => sys.error("tail of empty list") // exercise ママ 俺が考えてたのはそのままNil返すか、throw new IllegalArgumentException
      case Cons(_,t) => t
    }

  // EXERCISE 3ー3
  def setHead[A](l: List[A], h: A): List[A] = {
    l match {
      case Nil => sys.error("setHead on empty list")
      case Cons(_,t) => Cons(h,t)
      //case Cons(_, t) => h :: t //俺が考えていた解答
    }
  }

  // EXERCISE 3ー4 お手上げ
  def drop[A](l: List[A], n: Int): List[A] =
    if (n < 1) l
    else l match {
      case Nil => Nil
      case Cons(_, t) => drop(t, n - 1)
    }

  // EXERCISE 3ー5 お手上げ
  def dropWhile[A](l: List[A], f: A => Boolean): List[A] = {
    l match {
      case Nil => sys.error ("todo") //exercise ママ
      case Cons(h, t) if f(h) => dropWhile(t, f)
      case _ => l // answer
    }
  }


  // EXERCISE 3ー6 お手上げ
  /*
  Note that we're copying the entire list up until the last element. Besides being inefficient, the natural recursive
  solution will use a stack frame for each element of the list, which can lead to stack overflows for
  large lists (can you see why?). With lists, it's common to use a temporary, mutable buffer internal to the
  function (with lazy lists or streams, which we discuss in chapter 5, we don't normally do this). So long as the
  buffer is allocated internal to the function, the mutation is not observable and RT is preserved.

  Another common convention is to accumulate the output list in reverse order, then reverse it at the end, which
  doesn't require even local mutation. We'll write a reverse function later in this chapter.


  最後の要素までリスト全体をコピーしていることに注意してください。
  非効率的であることに加えて、自然な再帰的な解決法はリストの各要素のためにスタックフレームを使うでしょう、
  そしてそれは大きいリストのためにスタックオーバーフローをもたらすことができます（あなたはなぜかわかりますか？）。
  リストでは、関数の内部に一時的で可変のバッファを使うのが一般的です（レイジーリストやストリームについては、5章で説明しますが、通常これは行いません）。
  バッファが関数の内部に割り当てられている限り、突然変異は観測できず、RTは保存されます。
  もう1つの一般的な規則は、出力リストを逆の順序で累積してから最後に逆にすることです。
  これには、ローカルの突然変異さえ必要としません。この章の後半で逆関数を書きます。
  */
  def init[A](l: List[A]): List[A] =
  l match {
    case Nil => sys.error("init of empty list")
    case Cons(_,Nil) => Nil
    case Cons(h,t) => Cons(h,init(t))
  }

  def init2[A](l: List[A]): List[A] = {
    import collection.mutable.ListBuffer
    val buf = new ListBuffer[A]
    @annotation.tailrec
    def go(cur: List[A]): List[A] = cur match {
      case Nil => sys.error("init of empty list")
      case Cons(_,Nil) => List(buf.toList: _*)
      case Cons(h,t) => buf += h; go(t)
    }
    go(l)
  }

  // EXERCISE 3ー7 お手上げ
  //https://dev.classmethod.jp/server-side/scala-foldright-foldleft/
  /*
  No, this is not possible! The reason is because _before_ we ever call our function, `f`, we evaluate its argument,
  which in the case of `foldRight` means traversing the list all the way to the end. We need _non-strict_ evaluation
  to support early termination---we discuss this in chapter 5.

  いいえ、できません。その理由は、前に私達がこれまで私達の関数を `f`と呼んでいて、その引数を評価するからです。それは` foldRight`の場合はリストを最後まで横断することを意味します。早期終了をサポートするには_厳密でない評価が必要です
  */

  // EXERCISE 3ー8 お手上げ
  /*
  foldRight(List(1, 2, 3), Nil: List[Int])(Cons(_,_))

  We get back the original list! Why is that? As we mentioned earlier, one way of thinking about what `foldRight` "does"
  is it replaces the `Nil` constructor of the list with the `z` argument, and it replaces the `Cons` constructor with
  the given function, `f`. If we just supply `Nil` for `z` and `Cons` for `f`, then we get back the input list.

  originalのListに戻ります。先に述べたように、`foldRight`が"何をするか "について考える一つの方法は、リストの` Nil`コンストラクタを `z`引数に置き換え、` Cons`コンストラクタを与えられた関数に置き換えることです。 。
   `z`に` Nil`を、 `f`に` Cons`を指定すれば、入力リストに戻ります。

  foldRight(Cons(1, Cons(2, Cons(3, Nil))), Nil:List[Int])(Cons(_,_))
  Cons(1, foldRight(Cons(2, Cons(3, Nil)), Nil:List[Int])(Cons(_,_)))
  Cons(1, Cons(2, foldRight(Cons(3, Nil), Nil:List[Int])(Cons(_,_))))
  Cons(1, Cons(2, Cons(3, foldRight(Nil, Nil:List[Int])(Cons(_,_)))))
  Cons(1, Cons(2, Cons(3, Nil)))
  */


  // EXERCISE 3ー9 お手上げ
  def length[A](l: List[A]): Int = {
    foldRight(l, 0)((_, acc) => acc + 1)
  }

  // EXERCISE 3ー10 お手上げ
  @annotation.tailrec
  def foldLeft[A,B](l: List[A], z: B)(f: (B, A) => B): B = l match {
    case Nil => z
    case Cons(h, t) => foldLeft(t, f(z, h))(f)
  }

// Program Trace
foldLeft(List(1,2,3,4,5), 0)(_+_)
foldLeft(Cons(1, Cons(2, Cons(3, Cons(4, Cons(5, Nil))))), 0)(_+_)
foldLeft(Cons(2, Cons(3, Cons(4, Cons(5, Nil))))), 0+1)(_+_)
foldLeft(Cons(3, Cons(4, Cons(5, Nil))), 1+2)(_+_)
foldLeft(Cons(4, Cons(5, Nil)), 3+3)(_+_)
foldLeft(Cons(5, Nil), 6+4)(_+_)
10+5

  // EXERCISE 3ー11 お手上げ
  // foldLeftを使ってsum、product、およびリストの長さを計算する関数を記述せよ。
  def sum3(l: List[Int])= foldLeft(l, 0)(_ + _)


  def sum[B >: A : Numeric]: B = foldLeft(implicitly[Numeric[B]].zero)(implicitly[Numeric[B]].plus(_, _))

  def product[B >: A : Numeric]: B = foldLeft(implicitly[Numeric[B]].zero)(implicitly[Numeric[B]].times(_,_))

  // sum3の回答見たらできた
  def product3(l: List[Int])= foldLeft(l, 0)(_ * _)

  def product4(l: List[Int])= foldLeft(l, 0)((a:Int, b:Int) => a * b)

  // できなかったからproduct4で書けるか試した
  def length3(l: List[Int]) = foldLeft(l, 0)((acc, _) => acc + 1)

  // EXERCISE 3ー12 お手上げ
  // 要素が逆に並んだリストを返す関数を記述せよ。List(1,2,3)が与えられた場合、この関数はList(3,2,1)を返す。
  // 畳み込みを使って記述できるかどうかを確認すること。
  def reverse [A](l: List[A]): List[A] = foldLeft(l, List[A]())((acc, h) => Cons(h, acc))
  // List[A]の後の'()'が不明(T T) apply()?

// Program Trace
reverse(List(1,2,3,4,5))
foldLeft(List(1,2,3,4,5), List())((b, a) => Cons(a, b))
foldLeft(Cons(1, Cons(2, Cons(3, Cons(4, Cons(5, Nil)))), List())((b, a) => Cons(a, b))
foldLeft(Cons(2, Cons(3, Cons(4, Cons(5, Nil)))), Cons(1, Nil))((b, a) => Cons(a, b))
foldLeft(Cons(3, Cons(4, Cons(5, Nil))), Cons(2, Cons(1, Nil)))((b, a) => Cons(a, b))
foldLeft(Cons(4, Cons(5, Nil)), Cons(3, Cons(2, Cons(1, Nil))))((b, a) => Cons(a, b))
foldLeft(Cons(5, Nil), Cons(4, Cons(3, Cons(2, Cons(1, Nil)))))((b, a) => Cons(a, b))
Cons(5, Cons(4, Cons(3, Cons(2, Cons(1, Nil)))))

  //def reverse2 [A](l: List[A]): List[A] = foldLeft(l, List[A]() ){ (acc, h) => Cons(h, acc) }

  // EXERCISE 3ー13 お手上げ
  // 難問：foldRightをベースとしてfoldLeftを記述することは可能か。その逆はどうか。
  // foldLeftを使ってfoldRightを実装すると、foldRightを末尾再帰的に実装することが可能となり、
  // 大きなリストでもスタックオーバーフローが発生しなくなるので便利である。
  /*
  The implementation of `foldRight` in terms of `reverse` and `foldLeft` is a common trick for avoiding stack overflows
  when implementing a strict `foldRight` function as we've done in this chapter. (We'll revisit this in a later chapter,
  when we discuss laziness).

  The other implementations build up a chain of functions which, when called, results in the operations being performed
  with the correct associativity. We are calling `foldRight` with the `B` type being instantiated to `B => B`, then
  calling the built up function with the `z` argument. Try expanding the definitions by substituting equals for equals
  using a simple example, like `foldLeft(List(1,2,3), 0)(_ + _)` if this isn't clear. Note these implementations are
  more of theoretical interest - they aren't stack-safe and won't work for large lists.

  `reverse`と` foldLeft`に関する `foldRight`の実装は、この章で行ったように厳密な` foldRight`関数を実装するときにスタックオーバーフローを
  避けるための一般的なトリックです。 （私達が怠惰について論じるとき、私達は後の章でこれを再検討します）。
  他の実装では、呼び出されると正しい結合性で操作が実行されるようになる一連の関数が構築されます。
   `B => B`にインスタンス化された` B`型で `foldRight`を呼び出してから、` z`引数でビルドアップ関数を呼び出します。
   明確でない場合は、 `foldLeft（List（1,2,3）、0）（_ + _）`のような簡単な例でequalsをequalsに置き換えて定義を拡張してみてください。
   これらの実装はより理論的に興味があることに注意してください - それらはスタックセーフではなく、大きなリストに対しては動作しません。

  */
  def foldRightViaFoldLeft[A,B](l: List[A], z: B)(f: (A,B) => B): B =
    foldLeft(reverse(l), z)((b,a) => f(a,b))

  def foldRightViaFoldLeft_1[A,B](l: List[A], z: B)(f: (A,B) => B): B =
    foldLeft(l, (b:B) => b)((g,a) => b => g(f(a,b)))(z)

- id: (b: B) => b

foldRightViaFoldLeft2(List(1,2,3,4,5), 0)(_+_)
foldLeft(Cons(2, Cons(3, Cons(4, Cons(5, Nil)))), id)((g, a) => b => g(f(a, b)))(z)
foldLeft(Cons(2, Cons(3, Cons(4, Cons(5, Nil)))), id)((id, 1) => b => id(1 + b))(z) // g=id, a=1, res b => b + 1
foldLeft(Cons(3, Cons(4, Cons(5, Nil))), id)((id, 1) => b => id(f(b + 1)))(0)

  def foldLeftViaFoldRight[A,B](l: List[A], z: B)(f: (B,A) => B): B =
    foldRight(l, (b:B) => b)((a,g) => b => g(f(b,a)))(z)


  // EXERCISE 3ー14 お手上げ
  // foldLeftまたはfoldRightをベースとしてappendを実装せよ。
  /*
  `append` simply replaces the `Nil` constructor of the first list with the second list, which is exactly the operation
  performed by `foldRight`.
  `append`は単に最初のリストの` Nil`コンストラクタを2番目のリストに置き換えます。これはまさに `foldRight`によって実行される操作です。
  */
  def appendViaFoldRight[A](l: List[A], r: List[A]): List[A] =
    foldRight(l, r)(Cons(_,_))

  def appendViaFoldRight2[A](l: List[A], r: List[A]): List[A] =
    foldRight(as=l, z=r)(f = Cons(_, _))

// Program Trace
append2(List(1,2,3,4,5), List(6,7,8,9,10))
foldRight(Cons(1, Cons(2, Cons(3, Cons(4, Cons(5, Nil))))), Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(Cons(_,_))
f(1, foldRight(Cons(2, Cons(3, Cons(4, Cons(5, Nil))))), Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(Cons(_,_))
f(1, f(2, foldRight(Cons(3, Cons(4, Cons(5, Nil))))), Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(Cons(_,_))
f(1, f(2, f(3, foldRight(Cons(4, Cons(5, Nil))))), Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(Cons(_,_))
f(1, f(2, f(3, f(4, foldRight(Cons(5, Nil))))), Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(Cons(_,_))
f(1, f(2, f(3, f(4, f(5, Nil))))),Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(Cons(_,_))
f(1, f(2, f(3, f(4, f(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(Cons(_,_))
f(1, f(2, f(3, f(4, Cons(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(Cons(_,_))
f(1, f(2, f(3, Cons(4, Cons(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(Cons(_,_))
f(1, f(2, Cons(3, Cons(4, Cons(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(Cons(_,_))
f(1, Cons(2, Cons(3, Cons(4, Cons(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(Cons(_,_))
Cons(1, Cons(2, Cons(3, Cons(4, Cons(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(Cons(_,_))

  // EXERCISE 3ー15 お手上げ
  // 難問：複数のリストからなるリストを1つのリストとして連結する関数を記述せよ。
  // この関数の実行時間はすべてのリストの長さの合計に対して線型になるはずである。すでに定義した関数を使ってみること。
//  def concat[A](ls: List[List[A]]): List[A] =
//    foldLeft(ls, List[A]())((acc,scc) => Cons(acc, acc))

  def concat[A](l: List[List[A]]): List[A] =
    foldRight(l, Nil:List[A])(append)

concat(List(List(1,2,3), List(4,5,6), List(7,8,9,10)))
foldRight(List(List(1,2,3), List(4,5,6), List(7,8,9,10)), List[A]())(append)
f(Cons(1, Cons(2, Cons(3, Nil))), foldRight(List(List(4,5,6), List(7,8,9,10)), List[A]())(append)
f(Cons(1, Cons(2, Cons(3, Nil))), f(Cons(4, Cons(5, Cons(6, Nil)))), foldRight(List(List(7,8,9,10))),List[A]())(append)
f(Cons(1, Cons(2, Cons(3, Nil))), f(Cons(4, Cons(5, Cons(6, Nil)))), f(Cons(7, Cons(8, Cons(9, Cons(10, Nil))))), List[A]())(append)
f(Cons(1, Cons(2, Cons(3, Nil))), f(Cons(4, Cons(5, Cons(6, Nil)))), append(Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))(append)
f(Cons(1, Cons(2, Cons(3, Nil))), append(Cons(4, Cons(5, Cons(6, (Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))(append)
append(Cons(1, Cons(2, Cons(3, (Cons(4, Cons(5, Cons(6, (Cons(7, Cons(8, Cons(9, Cons(10, Nil)))))))))))))(append)
Cons(1, Cons(2, Cons(3, (Cons(4, Cons(5, Cons(6, (Cons(7, Cons(8, Cons(9, Cons(10, Nil))))))))))))

```

- `acc`は`accumulate(蓄積する、ためる)`の略

- Oさんに教わった解き方で初めてimplicityの存在を知った
  - Predef.scalaに定義されてた
    - コップ本のP.86に出てくる

Scalaは、java.langとscalaパッケージのメンバーのほか、Predefというシングルトンオブジェクトのメンバーを、暗黙のうちにすべてのScalaソースファイルにインポートする。
Scalaソースファイルでprintlnと書くとき、実際に呼び出しているのは、通常Predefのprintlnである(Predef.printlnは、さらにConsole.printlnを呼び出しており、このメソッドが実際の仕事をしている)。assertと書くときも、Predef.assertを呼び出しているのである。

- わからなかったら、プログラムトレースして簡単にあきらめないようにしましょう。

```scala=
  def sum4[B >: A: Numeric]: B = foldLeft(this, implicitly[Numeric[B]].zero)(implicitly[Numeric[B]].plus(_, _))

  def product5[B >: A : Numeric]: B = foldLeft(this, implicitly[Numeric[B]].zero)(implicitly[Numeric[B]].times(_,_))
```

EXERCISE 3ー16 お手上げ
各要素に1を足すことで整数のリストを変換する関数を記述せよ。注意：これは新しいListを返す純粋関数になるはずである。

```scala=
  def add1(l: List[Int]): List[Int] =
    foldRight(l, Nil:List[Int])((h,t) => Cons(h+1, t))
```

EXERCISE 3ー17 3-16の回答を見た後ならできた　関数名がconverterだっただけ
List[Double]の各値をStringに変換する関数を記述せよ。d.toStringという式を使ってd:DoubleをStringに変換できる。

```scala=
  def doubleToString(l: List[Double]): List[String] =
    foldRight(l, Nil:List[String])((h,t) => Cons(h.toString,t))
```

  // EXERCISE 3ー18 16,17の回答を見た後ならできた
  // リストの各要素を変更し、かつリストの構造をそのまま保つ総称関数mapを記述せよ。

```scala=
  def map[A,B](l: List[A])(f: A => B): List[B] =
    foldRight(l, Nil:List[B])((h,t) => Cons(f(h),t))
```
  // EXERCISE 3ー19 お手上げ
  // 与えられた述語条件が満たされるまでリストから要素を削除するfilter関数を記述せよ。
  // この関数を使ってList[Int]から奇数をすべて削除せよ。

```scala=
  def filter[A](l: List[A])(f: A => Boolean): List[A] =
    foldRight(l, Nil:List[A])((h,t) => if (f(h)) Cons(h,t) else t)
```

```
filter(List(1,2,3,4,5,6,7,8,9,10))((x: Int) => x % 2 != 0)
foldRight(as, Nil: List[A])((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, f(5, f(6, f(7, f(8, f(9, f(10, Nil: List[A]))))))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, f(5, f(6, f(7, f(8, f(9, Nil: List[A])))))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, f(5, f(6, f(7, f(8, Cons(9, Nil: List[A])))))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, f(5, f(6, f(7, Cons(9, Nil: List[A]))))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, f(5, f(6, Cons(7, Cons(9, Nil: List[A]))))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, f(5, Cons(7, Cons(9, Nil: List[A])))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, f(4, Cons(5, Cons(7, Cons(9, Nil: List[A])))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, f(3, Cons(5, Cons(7, Cons(9, Nil: List[A]))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, f(2, Cons(3, Cons(5, Cons(7, Cons(9, Nil: List[A]))))))((a, b) => if(f(a)) Cons(a, b) else b)
f(1, Cons(3, Cons(5, Cons(7, Cons(9, Nil: List[A])))))((a, b) => if(f(a)) Cons(a, b) else b)
Cons(1, Cons(3, Cons(5, Cons(7, Cons(9, Nil: List[A])))))((a, b) => if(f(a)) Cons(a, b) else b)
```

> filter(List(1,2,3,4,5,6,7,8))(arg => (arg % 2) == 0)
res4: List[Int] = Cons(2,Cons(4,Cons(6,Cons(8,Nil))))



  // EXERCISE 3ー20 お手上げ flatMapは、flatten + mapだから、まず、flattenを作らなきゃいけないと思ってた
  // mapと同じような働きをするflatMap関数を記述せよ。この関数は単一の結果ではなくリスト
  // を返し、そのリストは最終的な結果のリストに挿入されなければならない。


```scala=
  def flatMap[A,B](l: List[A])(f: A => List[B]): List[B] =
    concat(map(l)(f))
```

```
flatMap(List(1,2,3,4,5))((i => List(i, i))
  concat(map(List(1,2,3,4,5))(i => List(i, i)))
    map(List(1,2,3,4,5))(i => List(i, i))
      foldRight(as, Nil: List[B])((a, b) => Cons(f(a), b))
      f(1, foldRight(Cons(2, Cons(3, Cons(4, Cons(5, Nil))))))((a, b) => Cons(f(a), b))
      f(1, f(2, foldRight(Cons(3, Cons(4, Cons(5, Nil))))))((a, b) => Cons(f(a), b))
      f(1, f(2, f(3, foldRight(Cons(4, Cons(5, Nil))))))((a, b) => Cons(f(a), b))
      f(1, f(2, f(3, f(4, foldRight(Cons(5, Nil))))))(((a, b) => Cons(f(a), b))
      f(1, f(2, f(3, f(4, f(5, Nil)))))((a, b) => Cons(f(a), b))
      // f = (i => List(i, i))
      f(1, f(2, f(3, f(4, Cons(Cons(5, Cons(5, Nil)), Nil)))))((a, b) => Cons(f(a), b))
      f(1, f(2, f(3, Cons(Cons(4, Cons(4, Nil)), Cons(Cons(5, Cons(5, Nil)), Nil)))))((a, b) => Cons(f(a), b))
      f(1, f(2, Cons(Cons(3, Cons(3, Nil)),Cons(Cons(4, Cons(4, Nil)), Cons(Cons(5, Cons(5, Nil)), Nil)))))((a, b) => Cons(f(a), b))
      f(1, Cons(Cons(2, Cons(2, Nil)), Cons(Cons(3, Cons(3, Nil)),Cons(Cons(4, Cons(4, Nil)), Cons(Cons(5, Cons(5, Nil)), Nil)))))((a, b) => Cons(f(a), b))
      Cons(Cons(1, Cons(1, Nil)), Cons(Cons(2, Cons(2, Nil)), Cons(Cons(3, Cons(3, Nil)),Cons(Cons(4, Cons(4, Nil)), Cons(Cons(5, Cons(5, Nil)), Nil)))))((a, b) => Cons(f(a), b))
  foldRight(Cons(Cons(1, Cons(1, Nil)), Cons(Cons(2, Cons(2, Nil)), Cons(Cons(3, Cons(3, Nil)),Cons(Cons(4, Cons(4, Nil)), Cons(Cons(5, Cons(5, Nil)), Nil))))))(append)
f(Cons(Cons(1, Cons(1, Nil))), f(Cons(Cons(2, Cons(2, Nil)))), f(Cons(Cons(3, Cons(3, Nil)))), f(Cons(Cons(4, Cons(4, Nil)))), f(Cons(Cons(5, Cons(5, Nil))), Nil))
f(Cons(Cons(1, Cons(1, Nil))), f(Cons(Cons(2, Cons(2, Nil)))), f(Cons(Cons(3, Cons(3, Nil)))), f(Cons(Cons(4, Cons(4, Nil)))), Cons(5, Cons(5, Nil)))
f(Cons(Cons(1, Cons(1, Nil))), f(Cons(Cons(2, Cons(2, Nil)))), f(Cons(Cons(3, Cons(3, Nil)))), Cons(4, Cons(4, Cons(5, Cons(5, Nil)))))
f(Cons(Cons(1, Cons(1, Nil))), f(Cons(Cons(2, Cons(2, Nil)))), Cons(3, Cons(3, Cons(4, Cons(4, Cons(5, Cons(5, Nil)))))))
f(Cons(Cons(1, Cons(1, Nil))), Cons(2, Cons(2, Cons(3, Cons(3, Cons(4, Cons(4, Cons(5, Cons(5, Nil)))))))))
Cons(1, Cons(1, Cons(2, Cons(2, Cons(3, Cons(3, Cons(4, Cons(4, Cons(5, Cons(5, Nil))))))))))
```

EXERCISE 3ー21 お手上げ
flatMapを使ってfilterを実装せよ。
  
```scala=
  def filterViaFlatMap[A](l: List[A])(f: A => Boolean): List[A] =
    flatMap(l)(a => if (f(a)) List(a) else Nil)
```


  // EXERCISE 3ー22 お手上げ
  // リストを2つ受け取り、対応する要素どうしを足し合わせて新しいリストを生成する関数を記述せよ。
  // たとえばList(1,2,3)とList(4,5,6)はList(5,7,9)になる。
  /*
  To match on multiple values, we can put the values into a pair and match on the pair, as shown next, and the same
  syntax extends to matching on N values (see sidebar "Pairs and tuples in Scala" for more about pair and tuple
  objects). You can also (somewhat less conveniently, but a bit more efficiently) nest pattern matches: on the
  right hand side of the `=>`, simply begin another `match` expression. The inner `match` will have access to all the
  variables introduced in the outer `match`.

  The discussion about stack usage from the explanation of `map` also applies here.

  次に示すように、複数の値で一致させるには、値をペアにしてペアで一致させることができます。
  同じ構文がN値での一致にも適用できます（ペアとタプルの詳細についてはサイドバー「Scalaのペアとタプル」を参照）オブジェクト）。
  （やや便利ではないが、もう少し効率的に）パターンマッチをネストすることもできます
  ： `=>`の右側で、単に別の `match`式を始めるだけです。
  内側の `match`は外側の` match`に導入されたすべての変数にアクセスするでしょう。
   `map`の説明からのスタック使用法に関する議論はここでも適用されます。
  */

```scala=
  def addPairwise(a: List[Int], b: List[Int]): List[Int] = (a,b) match {
    case (Nil, _) => Nil
    case (_, Nil) => Nil
    case (Cons(h1,t1), Cons(h2,t2)) => Cons(h1+h2, addPairwise(t1,t2))
  }
```


#### EXERCISE 3ー23 3-22の回答を見た後ならできた
EXERCISE3.22で作成した関数を、整数または加算に限定されないように一般化せよ。一般化された関数にはzipWithという名前を付けること。
  This function is usually called `zipWith`. The discussion about stack usage from the explanation of `map` also
  applies here. By putting the `f` in the second argument list, Scala can infer its type from the previous argument list.

  この関数は通常 `zipWith`と呼ばれています。 `map`の説明からのスタック使用法に関する議論はここでも適用されます。
  2番目の引数リストに `f`を入れることで、Scalaは前の引数リストから型を推論することができます。

```scala=
  def zipWith[A,B,C](a: List[A], b: List[B])(f:(A,B) => C): List[C] =
    (a,b) match {
      case (Nil, _) => Nil
      case (_, Nil) => Nil
      case (Cons(h1,t1), Cons(h2,t2)) => Cons(f(h1,h2), zipWith(t1,t2)(f))
    }
```

    def hasSubsequence2[A](sup: List[A], sub: List[A]): Boolean = {
      def go[B](n: Int, list: List[B] ,acc: List[List[B]]): List[List[B]] = if(n > list.length) acc else
        append(Cons(take(list, n), acc), go(n+1, list, acc))

      def go2(list: List[A], acc: List[List[A]]): List[List[A]] = list match {
        case Nil => Nil
        case Cons(_, t) => append(go(1, list, Nil), go2(t, Nil))
      }
      val sublist = go2(sup, Nil)
      foldLeft(sublist, false)((acc, e)=> acc || (e==sub))
    }
    
    def take[A](list:List[A], n:Int): List[A] = if(n<=0) Nil else list match {
      case Nil => Nil
      case Cons(h, t) => Cons(h, take(t, n-1))
    }

#### EXERCISE 3ー24 お手上げ
難問：例として、Listに別のListがサブシーケンスとして含まれているかどうかを調べるhasSubsequenceを実装せよ。
たとえばList(1,2,3,4)には、List(1,2)、List(2,3)、List(4)などがサブシーケンスとして含まれている。
純粋関数型で、コンパクトで、かつ効率的な実装を見つけ出すのは難しいかもしれない。
その場合は、それでかまわない。どのようなものであれ、最も自然な関数を実装すること。
この実装については、第5章で改めて取り上げ、改良する予定である。なおScalaでは、任意の値xおよびyに対し、x==yという式を使って等しいかどうかを比較できる。

　
  この実装には特に悪いことは何もありませんが、それはややモノリシックで誤解しやすいという点が異なります。
  可能であれば、他の機能の組み合わせを使用してこのような機能を組み立てることをお勧めします。
  それはコードをより明らかに正しくそして読みやすくそして理解しやすくする。
  この実装では、ループから早く抜け出すための特別な目的のロジックが必要です。
  第5章では、結果として得られる関数をデータの1パスで機能させる効率を犠牲にすることなく、
  このような関数をより単純なコンポーネントから構成する方法について説明します。
  これらの関数についていくつかのプロパティを指定するのは良いことです。
  例えば、あなたはこれらの表現が真実であることを期待しますか？

  (xs append ys) startsWith xs
  xs startsWith Nil
  (xs append ys append zs) hasSubsequence ys
  xs hasSubsequence Nil

```scala=
  @annotation.tailrec
  def startsWith[A](l: List[A], prefix: List[A]): Boolean = (l,prefix) match {
    case (_,Nil) => true
    case (Cons(h,t),Cons(h2,t2)) if h == h2 => startsWith(t, t2)
    case _ => false
  }
  @annotation.tailrec
  def hasSubsequence[A](sup: List[A], sub: List[A]): Boolean = sup match {
    case Nil => sub == Nil
    case _ if startsWith(sup, sub) => true
    case Cons(h,t) => hasSubsequence(t, sub)
  }
```


代数的データ型は、他のデータ構造の定義に使用できます。例として、単純な2分木データ構造を定義してみましょう（図34）。
![](https://i.imgur.com/hUVHJmy.png)


リスト3-4
```scala=
sealed trait Tree[+A]

case class Leaf[A](value:A) extends Tree[A]
case class Branch[A](left: Tree[A], right:Tree[A]) extends Tree[A]
```

:::info
変位指定アノテーション (Variance Annotations
変位には以下の3つがあります。

+T: 共変（covariant）：その型もしくはそのサブ型を受け入れる
-T: 反変（contravariant）：その型もしくはその親の型を受け入れる
 T: 非変（nonvariant）：その型のみを受け入れる
:::



#### EXERCISE 3ー25 お手上げ
2分木のノード（LeafとBranch）の数を数えるsize関数を記述せよ。
```scala=
def size[A](t: Tree[A]): Int = t match {
  case Leaf(_) => 1
  case Branch(l,r) => 1 + size(l) + size(r)
}
```


#### EXERCISE 3ー26 お手上げ
Tree[Int]の最大の要素を返すmaximum関数を記述せよ。なおScalaでは、x.max(y)またはx max yを使って2つの整数xとyの最大値を計算できる。
```scala=
def maximum(t: Tree[Int]): Int = t match {
  case Leaf(n) => n
  case Branch(l,r) => maximum(l) max maximum(r)
}

def maximum[A: Ordering](tree: Tree[A]): A = tree match {
  case Leaf(v) => v
  case Branch(l, r) => implicitly[Ordering[A]].max(maximum(l), maximum(r))
}
```



#### EXERCISE 3ー27 お手上げ
2分木のルートから任意のLeafまでの最長パスを返すdepth関数を記述せよ。
```scala=
def depth[A](t: Tree[A]): Int = t match {
  case Leaf(_) => 0
  case Branch(l,r) => 1 + (depth(l) max depth(r))
}
```

#### EXERCISE 3ー28 お手上げ
2分木の各要素を特定の関数を使って変更するmap関数を記述せよ。この関数はListの同じ名前のメソッドに類似している。
```scala=
def map[A,B](t: Tree[A])(f: A => B): Tree[B] = t match {
  case Leaf(a) => Leaf(f(a))
  case Branch(l,r) => Branch(map(l)(f), map(r)(f))
}

```

#### EXERCISE 3ー29 お手上げ
size、maximum、depth、mapを一般化し、それらの類似点を抽象化する新しいfold関数を記述せよ。そして、このより汎用的なfold関数を使ってそれらを再実装せよ。このfold関数とListの左畳み込みおよび右畳み込みの間にある類似性を抽出することは可能か。
```scala=
def fold[A,B](t: Tree[A])(f: A => B)(g: (B,B) => B): B = t match {
  case Leaf(a) => f(a)
  case Branch(l,r) => g(fold(l)(f)(g), fold(r)(f)(g))
}

def sizeViaFold[A](t: Tree[A]): Int = 
  fold(t)(a => 1)(1 + _ + _)

def maximumViaFold(t: Tree[Int]): Int = 
  fold(t)(a => a)(_ max _)

def depthViaFold[A](t: Tree[A]): Int = 
  fold(t)(a => 0)((d1,d2) => 1 + (d1 max d2))

/*
Note the type annotation required on the expression `Leaf(f(a))`. Without this annotation, we get an error like this: 

type mismatch;
  found   : fpinscala.datastructures.Branch[B]
  required: fpinscala.datastructures.Leaf[B]
     fold(t)(a => Leaf(f(a)))(Branch(_,_))
                                    ^  

This error is an unfortunate consequence of Scala using subtyping to encode algebraic data types. Without the annotation, the result type of the fold gets inferred as `Leaf[B]` and it is then expected that the second argument to `fold` will return `Leaf[B]`, which it doesn't (it returns `Branch[B]`). Really, we'd prefer Scala to infer `Tree[B]` as the result type in both cases. When working with algebraic data types in Scala, it's somewhat common to define helper functions that simply call the corresponding data constructors but give the less specific result type:  
  
  def leaf[A](a: A): Tree[A] = Leaf(a)
  def branch[A](l: Tree[A], r: Tree[A]): Tree[A] = Branch(l, r)
*/
def mapViaFold[A,B](t: Tree[A])(f: A => B): Tree[B] = 
  fold(t)(a => Leaf(f(a)): Tree[B])(Branch(_,_))
```


-----------------------

### 型パラメータには以下の5つの境界が設定できる。
- 上限
- 下限
- Generalized type Constraints
- View Bounds 可視境界
- Context Bounds


可視境界について「Scala逆引きレシピ」より
![](https://i.imgur.com/FSq0Bde.png)

![](https://i.imgur.com/4XRyf93.png)


#### 上限境界 upper bound
Javaのextends
```
hoge[A <: T]
```
A が T のサブタイプでなければならない


#### 下限境界 lower bound
JavaのSuper
```
hoge[A >: T]
```
A は T のスーパータイプでなければならない



[Generalized type Constraintsについてゆるよろさんの日記](https://yuroyoro.hatenablog.com/entry/20100914/1284471301)

#### Context Bounds

コップ本　p.423
21.6 コンテキスト境界
Orderingも書いてある

#### 可視境界(view bound)
```
hoge[A <% T]
```
A が T として扱える　(暗黙の型変換により変換可能または A が T のサブタイプ)

#### 畳み込み (convolution)
foldLeft、foldRight、/:、:\、reduceLeft、reduceRight。

連続する要素に二項演算を適用する


2つの異なる関数f(t)とg(t)から、新しい関数h(t)=∫f(τ)g(t-τ)dτを作る操作


#### 型限定の畳み込み

sum、product、min、max。特定の型(数値型、互換性のある型など)の要素に二項演算を適用する


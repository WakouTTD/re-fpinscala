# fpinscala 輪読会 #6

## 6章 純粋関数型の状態

例として乱数の生成
標準ライブラリのscala.util.Random
- 典型的な命令型のAPI
- 副作用に依存

[manning](https://livebook.manning.com/#!/book/functional-programming-in-scala/chapter-6/6)


[サイコロを振るのは簡単である、しかし、ゲームでサイコロを実装するにはサイコロを知らなければならない](https://qiita.com/isonami/items/1cc278cbf2093d2d6abd)

本来、コンピュータには「ランダムな数」というものは存在しません。
なぜかといえば、アルゴリズムは決定的なものなので、
同じパラメータを与えれば出力されるものも同じになります。
これは絶対不変のもので、ソフトウェアでは解決できないものです。
要するにソフトウェアにおける「乱数」とは必ず「疑似乱数」なのです。
（ハードウェアで本物の乱数を実装しているものはあります）
そうなんです。
ソフトウェアにおける「乱数」とははランダムということではなく、ランダムに見える数列を生成するだけなんですね。


## 6.1 副作用を使った乱数の生成

```bash=
scala> val rng = scala.util.Random
rng: util.Random.type = scala.util.Random$@33f98231

scala> rng.nextDouble
res0: Double = 0.9153414029378363

scala> rng.nextDouble
res1: Double = 0.6440117358938048

scala> rng.nextInt
res2: Int = 1017396041

scala> rng.nextInt(10)
res3: Int = 2

```
上記nextDoubleの呼び出しで、rng内部状態が更新されていることが想像される。
そうでなければ複数回呼び出した都度、同じ結果になるはず。

状態の更新は副作用として実行されるため、これらのメソッドは参照透過ではない。

テスト、合成、並列化が容易ではなく、モジュール性に乏しい


- 6面サイコロを振るシミュレーション
```scala=
def rollDie: Int = {
  val rng = new scala.util.Random
  rng.nextInt(6)	
}
```
上記メソッドには「1つ違いのエラー」(off-by-one error)がある。
1〜6の値を返すはずが、実際には0〜5の値を返す。
しかし、このメソッドをテストした時には6回テストした内、5回は仕様を満たすことになります。
かつ、テストが失敗した場合に、その失敗を確実に再現することが理想的です。

乱数ジェネレータを渡してみるのはどうでしょうか。
```scala=
def rollDie(rng: scala.util.Random): Int = rng.nextInt(6)
```
この方法でも「同じ」ジェネレータにするには、同じシードで作成するだけではなく、同じ状態にする必要があるため、問題が残る。

## 6.2 純粋関数型の乱数の生成

参照透過性を取り戻すための鍵は、状態の更新を明示的なものにすること。

状態を副作用として更新するのではなく、生成された値とともに新しい状態を返すようにするのです。

乱数ジェネレータ(Random Number Generation)
インターフェイスの1例

#### リスト 6.2
```scala=
trait RNG {
  def nextInt: (Int, RNG)
}
```

上記nextIntメソッドはランダムな整数を生成するはずです。
後ほど、nextIntをベースとして他の関数も定義します。scala.util.Randomのように、生成された乱数だけを返し、内部状態がそこで変化して更新されるままにするのではなく、乱数として新しい状態の両方を返し、古い状態はそのままにしておきます。実質的には、次の状態の計算を、プログラムの他の部分に対する新しい状態の通知から切り離すことになります。

グローバルに変更可能なメモリがいっさい使用されず、呼び出し元に次に状態を返すだけであることに注意してください。

これによりnextIntの呼び出し元が新しい状態の処理を完全に制御できるようになります。

このAPIのユーザーが乱数ジェネレータそのものの実装について何も知らないという点では、状態を<b>カプセル化</b>していると言えるでしょう。(図 6-1)

#### 図 6-1 関数型RNG
![](https://i.imgur.com/DyBYRcS.jpg)

ですが、実装が必要であることに変わりはないので、簡単なものを見てみましょう。リスト6-3は、scala.util.Randomと同じアルゴリズムを使用する純粋関数型のシンプルな乱数ジェネレータを示しています。奇しくも、このジェネレータは<b>線型合同法</b>と呼ばれるものです。この実装の詳細はそれほど重要ではありませんが、nextIntが生成された値と呼ばれるものです。
この実装の詳細はそれほど重要ではありませんが、nextIntが生成された値と新しいRNGを返すことに注目してください。

この新しいRNGは次の値の生成に使用されます。

[線形合同法（せんけいごうどうほう、英: Linear congruential generators, LCGs）](https://ja.wikipedia.org/wiki/%E7%B7%9A%E5%BD%A2%E5%90%88%E5%90%8C%E6%B3%95)


#### リスト 6.3

```scala=
case class SimpleRNG(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL	
    val nextRNG = SimpleRNG(newSeed)	
    val n = (newSeed >>> 16).toInt	
    (n, nextRNG)	
  }
}
```
3行目　&はビット論理積。現在のシードを使って新しいシードを生成。
`0x5DEECE66DL` は10進数では `25214903917` で素因数分解は `7 * 443 * 739 * 11003` である。

4行目　次の状態(新しいシードから作成されたRNGインスタンス)
5行目　>>> は0埋め右バイナリシフト。値nは新しい擬似乱数(整数)。
6行目　戻り値は擬似乱数(整数)とRNGの次の状態からなるタプル。

参考:  [java.util.Randomの抜粋](http://www.kmonos.net/wlog/97.html#_2206090523)


```scala=
scala> val rng = SimpleRNG(42)	
rng: SimpleRNG = SimpleRNG(42)

scala> val (n1, rng2) = rng.nextInt	
n1: Int = 16159453
rng2: RNG = SimpleRNG(1059025964525)

scala> val (n2, rng3) = rng2.nextInt
n2: Int = -1281479697
rng3: RNG = SimpleRNG(197491923327988)

```

この一連のステートメントは何回実行しても常に同じ値を返します。rng.nextIntを呼び出すと常に16159453と新しいRNGが返され、そのnextIntは常に-1281479696を返します。つまりこのAPIは純粋関数です。


[java – ランダムな長さで、同じ数値を2回続けて入れることができる](https://codeday.me/jp/qa/20190217/247095.html)

#### 6.3 ステートフルAPIの純粋化

ステートフルなAPIを純粋関数化するという問題とその解決策 -- APIに実際に何かを変化させるのではなく、次の状態に<b>計算</b>させる -- は乱数の生成に限ったことではありません。これはよく発生する問題であり、常にこれと同じ方法で対処できます。


例
```scala=
class Foo {
  private var s: FooState = ...
  def bar: Bar
  def baz: Int
}
```

sが状態を持っており、barとbazで、状態から状態への遷移を明確にできる。
```scala=
trait Foo {
  def bar: (Bar, Foo)
  def baz: (Int, Foo)
}
```
このパターンを利用すると、計算された次の状態をプログラムの他の部分に渡す責任は呼び出し元にあります。先ほどの純粋関数型のRNGの例では、前のRNGを再利用すると常に以前と同じ値が生成されます。


```scala=
def randomPair(rng: RNG): (Int, Int) = {
  val (i1: Int, _: RNG) = rng.nextInt
  val (i2: Int, _: RNG) = rng.nextInt
  (i1, i2)
}
```

上記の場合は、i1とi2は同じ値になる。

```scala=
def randomPair(rng: RNG): ((Int,Int), RNG) = {
  val (i1: Int, ,rng2: RNG) = rng.nextInt
  val (i2: Int, ,rng3: RNG) = rng2.nextInt	
   ((i1,i2), rng3)	
}
```

#### EXCERCISE 6.1
RNG.nextIntを使って0〜Int.maxValue(0とInt.maxValueを含む)のランダムな整数を生成する関数を記述せよ。なお、nextIntがInt.MinValueを返すときには、対応する自然数がない。
この特異なケースにも対処する必要がある。


```scala=
trait RNG {
  def nextInt: (Int, RNG)
  def noneNegative(rng: RNG): (Int, RNG)
}

case class SimpleRNG(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL	
    val nextRNG = SimpleRNG(newSeed)	
    val n = (newSeed >>> 16).toInt	
    (n, nextRNG)	
  }
  
  def noneNegative(rng: RNG): (Int, RNG) = {
    val (i, nr) = nextInt()
    val i2 = i match {
      case (i == Int.MinValue) -1 * (i+1)
      case (i < 0) => -1 * i
      case _ => _
    }
    (i2, nr)
  }
}
```


解答
```scala=
  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (i, r) = rng.nextInt
    (if (i < 0) -(i + 1) else i, r)
  }
```


#### EXCERCISE 6.2
0〜1(1を含まない)のDouble型の値を生成する関数を記述せよ。
Int.MaxValueを使って正の整数の最大値を取得できることと、x.toDoubleを使ってx: IntをDoubleに変換できることに注意。

```scala=
trait RNG {
  def nextInt: (Int, RNG)
  def noneNegative(rng: RNG): (Int, RNG)
}

case class SimpleRNG(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL	
    val nextRNG = SimpleRNG(newSeed)	
    val n = (newSeed >>> 16).toInt	
    (n, nextRNG)	
  }
  

def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (i, r) = rng.nextInt
    (if (i < 0) -(i + 1) else i, r)
  }

  def double(rng: RNG): (Double, RNG)
}
```


解答
```scala=
  def double(rng: RNG): (Double, RNG) = {
    val (i, r) = nonNegativeInt(rng)
    (i / (Int.MaxValue.toDouble + 1), r)
  }
  //Int.MaxValueは、2147483647
  //Int.MaxValue.toDoubleは、2.147483647E9 
```


#### EXCERCISE 6.3
ペア(Int, Double)、ペア(Double, Int)、および3要素のタプル(Double, Double, Double)を生成する関数を記述せよ。すでに作成済みの関数を再利用できるはずだ。


```scala=
def intDouble(rng: RNG): ((Int, Double), RNG) = {
  val (i: Int, r: RNG) = nonNegativeInt(rng)
  val (d: Double, r2: RNG) = double(rng)
  ((i, d), r2)
}

def doubleInt(rng: RNG): ((Double, Int), RNG) = {
  val (i: Int, r: RNG) = nonNegativeInt(rng)
  val (d: Double, r2: RNG) = double(rng)
  ((d, i), r2)  
}

def double3(rng: RNG): ((Double, Double, Double), RNG) = {
  val (d1: Double, r2: RNG) = double(rng)
  val (d2: Double, r3: RNG) = double(r2)
  val (d3: Double, r4: RNG) = double(r3)
  ((d1, d2, d3), r4)
}
```

#### EXCERCISE 6.4
ランダムな整数のリストを生成する関数を記述せよ。


```scala=
  def ints(count: Int)(rng: RNG): (List[Int], RNG) = {

    def loop(cnt: Int, rng: RNG, l: List[Int]): (List[Int], RNG) = {
      cnt match {
        case i if(i < 1) => (l, rng)
        case i => {
          val (i2: Int, r2: RNG) = nonNegativeInt(rng)
          loop(i-1, r2, l:+i2)
        }
      }
    }

    loop(count, rng, List.empty[Int])
  }
```

一応動く
```scala=
defined class SimpleRNG

scala> val ss = SimpleRNG(5L)
ss: SimpleRNG = SimpleRNG(5)

scala> val (i,r) = ss.ne
ne   nextInt

scala> val (i,r) = ss.nextInt
i: Int = 1923744
r: RNG = SimpleRNG(126074519596)

scala> ss.ints(4)(r)
res0: (List[Int], RNG) = (List(1478223345, 832832900, 735168418, 2091154546),SimpleRNG(144429072360432))
```


解答
```scala=
// A simple recursive solution
def ints(count: Int)(rng: RNG): (List[Int], RNG) =
  if (count == 0)
    (List(), rng)
  else {
    val (x, r1)  = rng.nextInt
    val (xs, r2) = ints(count - 1)(r1)
    (x :: xs, r2)
  }

// A tail-recursive solution
def ints2(count: Int)(rng: RNG): (List[Int], RNG) = {
  def go(count: Int, r: RNG, xs: List[Int]): (List[Int], RNG) =
    if (count == 0)
      (xs, r)
    else {
      val (x, r2) = r.nextInt
      go(count - 1, r2, x :: xs)
    }
  go(count, rng, List())
}


```




##### 関数型プログラミングでのやっかいな状況への対処

関数型プログラミングでプログラムを記述するようになると、プログラムを関数方式で表現することが不自然である、あるいは手間がかかるように感じるケースに遭遇することがあります。
純粋性とは、[E]の文字を使わずに小説を書き上げようとするのと同じ、ということなのでしょうか。
もちろん、そうではありません。
そうした不自然さは、たいていまだ発見されてない抽象性がどこにあるかという兆候です。
こうした状況に遭遇した場合は思い切ってリファクタリング可能な共通のパターンを探してみることをお勧めします。
ほとんどの場合は同じ問題に遭遇している人が他にもいるはずです。そして苦労して見つけたソリューションが「標準」のソリューションだった、ということになるかもしれません。
行き詰まった場合は元のソリューションの仕組みを解明してみると、他の誰かが同じような問題にどのように対処したのかを理解するのに役立つでしょう。
練習と経験を積み本書に含まれているイディオムに慣れてくると、プログラムを関数方式ですらすら表現できるようになります。
もちろん、設計は一筋縄ではいきませんが、純粋関数を使ったプログラミングにより、設計空間は大幅に単純化されます。

### 6.4 状態の処理に適したAPI

これまでの実装の共通パターン

どの関数も、ある型Aに対して `RNG => (A, RNG)` 形式の型が使用されています。この種の関数は、RNGの状態を遷移させることから、<b>状態アクション</b>または<b>状態遷移</b>と呼ばれます。これらの状態アクションは<b>コンビネータ</b>を使って組み合わせることができます。

コンビネータとは、後ほど定義する高階関数のことです。

状態を明示的に受け渡すのはかなり手間のかかる繰り返しの多い作業なので、アクション間での状態の受け渡しをコンビネータに任せて自動化することにしましょう。
アクションの型について説明するときにややこしく考えずに済むよう、ここでは状態アクションデータ型であるRNGの型エイリアスを作成することにします。

##### 補足

コンビネータ(combinator):組合せ子
[不動点コンビネータ](https://ja.wikipedia.org/wiki/%E4%B8%8D%E5%8B%95%E7%82%B9%E3%82%B3%E3%83%B3%E3%83%93%E3%83%8D%E3%83%BC%E3%82%BF)



#### リスト 6-4
```scala=
type Rand[+A] = RNG => (A, RNG)
```

> ※ 復習
> 何も指定しないデフォルトの型パラメータ指定を「非変」
>  「+」の指定を「共変」といいます。
[color=#907bf7]


Rand[A]型の値については「ランダムに生成されたA」として考えることができますが、それでは正確さを欠きます。正確にはそれは状態アクションです。つまりRNGを使ってAを生成し、あとから別のアクションに利用できる新しい状態へRNGを遷移させる、RNGに依存するプログラムです。
これでRNGのnextIntなどのメソッドをこの新しい型の値に変換できます。


#### リスト 6-5
```scala=
val int: Rand[Int] = _.nextInt
```

次にRNGの状態を明示的にやり取りするのを回避した上でRandのアクションを結合するためのコンビネータを記述してみましょう。
これは受け渡しのすべてを自動的に行うドメイン固有の言語のようなものになりますし、たとえばunitアクションはRNGの単純な状態遷移であり、RNGの状態を未使用のまま渡し、常に乱数値ではなく定数値を返します。

###### これ、後々EXERCISE6-7で使う

#### リスト 6-6
```scala=
//リスト6-4持ってきた
type Rand[+A] = RNG => (A, RNG)

def unit[A](a: A): Rand[A] =
  rng => (a, rng)
```

```bash
scala> RNG.unit(1)
res5: RNG.Rand[Int] = RNG$$$Lambda$1492/1932740085@5add7380

scala> RNG.unit("a")
res6: RNG.Rand[String] = RNG$$$Lambda$1492/1932740085@1a3272e0

scala> RNG.unit('c')
res7: RNG.Rand[Char] = RNG$$$Lambda$1492/1932740085@4c682826
```


状態そのものを変化させずに状態アクションの出力を変換するmapもあります。Rand[A]が関数型 `RNG => (A, RNG)` の型エイリアスであることを考えると、これは関数合成のようなものです。

#### リスト 6-7
```scala=
def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
  rng => {
    val (a, rng2) = s(rng)
    (f(a), rng2)
  }
```

型を明示すると
```scala=
def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
  rng: RNG => {
    val (a: A, rng2: RNG) = s(rng: RNG)
    (f(a), rng2)
  }
```

mapの使用例は以下。
このnonNegativeEvenは、nonNegativeIntを再利用し、
2で割り切れる0以上のIntを生成します。

```scala=
//nonNegativeIntはEXERCISE6-1から持ってきた
def nonNegativeInt(rng: RNG): (Int, RNG) = {
  val (i, r) = rng.nextInt
  (if (i < 0) -(i + 1) else i, r)
}

def nonNegativeEven: Rand[Int] =
  map(nonNegativeInt)(i => i - i % 2)
```


###### よくわからんから試した
```scala=
scala> RNG.nonNegativeEven
res1: RNG.Rand[Int] = RNG$$$Lambda$1454/369237870@17931cc0

scala> def tostring(a: Int) = a.toString
tostring: (a: Int)String

scala> RNG.map(RNG.unit(11))(tostring(_))
res2: RNG.Rand[String] = RNG$$$Lambda$1454/369237870@24afdf18

scala> def plusOne(i: Int): Int = i + 1
plusOne: (i: Int)Int

scala> RNG.map(RNG.unit(11))(plusOne(_))
res3: RNG.Rand[Int] = RNG$$$Lambda$1454/369237870@3b035d0c
```

#### EXCERCISE 6.5
mapを使ってdoubleをもう少し要領よく実装し直せ。EXERCISE6.2を参照
 
```scala=
// EXCERCISE6-2の解答持ってきた
//def double(rng: RNG): (Double, RNG) = {
//  val (i, r) = nonNegativeInt(rng)
//  (i / (Int.MaxValue.toDouble + 1), r)
//}

def double(rng: RNG): (Double, RNG) = {
  map(nonNegativeInt){(i, r) => 
   (i / (Int.MaxValue.toDouble + 1), r)
  }
}
```

解答
```scala=
val _double: Rand[Double] = 
  map(nonNegativeInt)(_ / (Int.MaxValue.toDouble + 1))
```
間違ってたのでリスト6-4の再掲
```scala=
  type Rand[+A] = RNG => (A, RNG)
```

### 6.4.1 状態アクションの結合
上記のmapはEXERCISE6.3のintDoubleとdoubleIntを実装できるほど高性能ではありません。
そこで必要となるのが、新しいコンビネータmap2です。map2では単項関数ではなく2項関数を使って2つのRNGアクションを1つにまとめることができます。


#### EXCERCISE 6.6
以下のシグネチャに基づいてmap2を実装せよ。この関数は、raとrbの2つのアクションと、それらの結果を結合する関数fを受け取り、それらを結合する新しいアクションを返す。


:::danger
真剣に書いたが、コンパイルすら通らなかった

```scala=
def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B)): Rand[(A,B)] = {
  for{
   a <- ra
   b <- rb
  } yield {
   f(a, b)
  }
}
```

解答
```scala=
def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
  rng => {
    val (a, r1) = ra(rng)
    val (b, r2) = rb(r1)
    (f(a, b), r2)
  }

```

型書いてみた
```scala=
def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
  rng: RNG => {
    val (a: A, r1: RNG) = ra(rng)
    val (b: B, r2: RNG) = rb(r1)
    (f(a, b), r2)
  }

```
再度、リスト6-4を見て、何となく納得
```scala=
  type Rand[+A] = RNG => (A, RNG)
```


---

p.106辺り

map2コンビネータを一度記述すれば、任意のRNG状態アクションの結合に利用できるようになります。たとえば、A型の値を生成するアクションとB型の値を生成するアクションがある場合は、それらを組み合わせてAとBのペアを生成する1つのアクションにまとめることができます。

```scala=
def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
  map2(ra, rb)((_, _))
```

これを使ってEXERCISE 6.3のintDoubleとdoubleIntをもう少し簡潔に書き直してみましょう。

```scala=
//リスト6.5のint
// val int: Rand[Int] = _.nextInt
// EXERCISE 6.5のdouble

val randIntDouble: Rand[(Int, Double)] =
  both(int, double)

val randDoubleInt: Rand[(Double, Int)] =
  both(double, int)
```


#### EXCERCISE 6.7
難問: 2つのRNG遷移の組み合わせが可能であるとしたら、それらのリスト全体を結合することも可能であるはずだ。遷移のListを1つの遷移にまとめるためのsequenceを実装せよ。それを使って、以前に記述したints関数を再実装せよ。その際には、標準ライブラリのList.fill(n)(x)関数を使ってxをn回繰り返すリストを作成できる。

#### fill復習
```scala=
scala> List.fill(5)("A")
res0: List[String] = List(A, A, A, A, A)
```
##### hint
リストを再帰的に繰り返す必要があります。
再帰的な定義を記述する代わりに、「foldLeft」または「foldRight」を使用できることを忘れないでください。
書いたばかりの `map2`関数を再利用することもできます。
実装のテストケースとして、
`List(1, 2, 3)`を返すためには
`sequence(List(unit(1), unit(2), unit(3)))(r)._1` 
と記述する必要があります。

#### foldRight復習
```scala=
scala> List(1,2,3).foldLeft(0)((ac, z) => ac - z)
res2: Int = -6
//  ((0-1)-2)-3


scala> List(1,2,3).foldRight(0)((z, ac) => z - ac)
res4: Int = 2 
//  1-(2-(3-0))
```

お手上げ・解答
```scala=
  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
    fs.foldRight(unit(List[A]()))((f, acc) => map2(f, acc)(_ :: _))
```
動くが、`hint`の `(r)._1` がわからない
```scala=
scala> RNG.sequence(List(RNG.unit(1), RNG.unit(2), RNG.unit(3)))
res3: RNG.Rand[List[Int]] = RNG$$$Lambda$1452/1193577032@72557746
```

### 6.4.2 入れ子の状態アクション
mapコンビネータとmap2コンビネータを利用することで、書くのが面倒な上に書き間違いをしやすい関数をかなり簡潔に要領よく実装することができました。
しかし、mapとmap2を利用してもうまく記述できない関数がいくつか存在します。

この関数は0〜n(0を含み、nを含まない)の整数を生成します。

```scala=
def nonNegativeLessThan(n: Int): Rand[Int]
```

実装上の最初の課題はnを法とする自然数を生成することかもしれません。

```scala=
def nonNegativeLessThan(n: Int): Rand[Int] =
  map(nonNegativeInt) { _ % n }
```

このようにすれば、確かに範囲内の数は生成されますが、Int.MaxValueはnで割り切れないことがあるため、その除算の余りよりも小さい数が頻繁に発生するという歪みが生じることになります。
nonNegativeIntが生成した数が、32ビットの整数に収まり、かつnの最大の倍数よりも大きい数である場合は、より小さい数が得られることを期待してジェネレータをリトライする必要があります。


```scala=
def nonNegativeLessThan(n: Int): Rand[Int] =
  map(nonNegativeInt) { i =>
    val mod = i % n
    if (i + (n-1) - mod >= 0) mod else nonNegativeLessThan(n)(???)	
  }

```
###### 4行目 取得したIntが32ビットのIntに収まるnの最大の倍数よりも大きい場合は、再帰的にリトライ。

正しい方向に向かっていることは確かですが、nonNegativeLessThan(n)で使用されている型が正しくありません。本来ならばRand[Int]が返されるはずで、それはRNGを期待する関数です。ですが、そのようにはなっていません。そこでnonNegativeIntから返されたRNGがnonNegativeLessThanの再帰呼び出しに渡されるようにつなぎ合わせてしまいましょう。
以下に示すようにmapを使用せずに明示的に渡すことが可能です。


```scala=
def nonNegativeLessThan(n: Int): Rand[Int] = { rng =>
  val (i, rng2) = nonNegativeInt(rng)
  val mod = i % n
  if (i + (n-1) - mod >= 0)
    (mod, rng2)
  else nonNegativeLessThan(n)(rng2)
}
```
###### 書籍で7行目が`(rng)` になってるのは `(rng2)` の誤植


#### EXCERCISE 6.8
flatMapを実装し、それを使ってnonNegativeLessThanを実装せよ

flatMapを利用すれば、Rand[A]を使って乱数を生成し、そのAの値に基づいてRand[B]を選択できます。flatMapをnonNegativeLessThanで利用し、nonNegativeIntが生成した値に基づいてリトライするかどうかを選択します。

```scala=
def flatMap[A, B](f: Rand[A])(g: A => Rand[B]): Rand[B]
```

###### ちなみにscala標準コレクションに `nonNegativeLessThan` なんてメソッドは無い
```scala=
scala> List(1,2,3,4).n
ne   nonEmpty   notify   notifyAll
```
・・・お手上げ


解答

```scala=
  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, r1) = f(rng)
      g(a)(r1)
    }

  def nonNegativeLessThan(n: Int): Rand[Int] = {
    flatMap(nonNegativeInt) { i =>
      val mod = i % n
      if (i + (n-1) - mod >= 0) unit(mod) else nonNegativeLessThan(n)
    }
  }
```




##### 上のRand typeとmapとmap2もう一度持ってきて、解答に型宣言もつけてみる
```scala=
  type Rand[+A] = RNG => (A, RNG)

  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng: RNG => {
      val (a: A, rng2: RNG) = s(rng)
      (f(a), rng2)
    }

  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng: RNG => {
      val (a: A, r1: RNG) = ra(rng)
      val (b: B, r2: RNG) = rb(r1)
      (f(a, b), r2)
    }
    
  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a: A, r1: RNG) = f(rng)
      g(a)(r1) // Rand[B]とRNGつまりBとRNG
    }

```

#### EXCERCISE 6.9
flatMapを使ってmapとmap2を再実装せよ。これが可能であることは、flatMapがmapとmap2よりも強力であると述べていることから明らかである。

本章の冒頭で示したサイコロを振る例について、ここで改めて考えてみましょう。純粋関数型のAPIを利用すれば、テスタビリティを改善できるのでしょうか。
nonNegativeLessThanを使ったrollDieの実装は以下のようになります。これには「1つ違い」エラーが含まれていました。

解答
```scala=
  def _map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(a => unit(f(a)))

  def _map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => map(rb)(b => f(a, b)))
```

###### わからないからflatMapとunit持ってきて考えてみる

```scala=
//def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] 
//def unit[A](a: A): Rand[A]

  def _map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(a => unit(f(a)))

  def _map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => map(rb)(b => f(a, b)))
```

本章の冒頭で示したサイコロを示した例を改めて考える。
純粋関数型のAPIを利用すればテスタビリティを改善できるか
nonNegativeLessThanを使ったrollDieの実装は以下のようになります。


```scala=
def rollDie: Rand[Int] = nonNegativeLessThan(6)
```

この関数をさまざまなRNG状態でテストすれば、この関数が0を返すようなRNGがすぐに見つかるはずです。

```scala=
scala> val zero = rollDie(SimpleRNG(5))._1
zero: Int = 0
```

同じ乱数ジェネレータSimpleRNG(5)を使って、これを正確に再現できます。その状態が使用された後に削除される心配はありません。このバグを修正するのは簡単です。


```scala=
def rollDie: Rand[Int] = map(nonNegativeLessThan(6))(_ + 1)
```

## 6.5 状態アクションデータ型の一般化

これまでのunit,map,map2,flatMap,sequenceの5つの関数はどの角度からも乱数ジェネレータに特化しているとは言えません。これらは状態アクションを処理するための汎用目的の関数であり、状態の型を特別扱いしません。たとえばmapには、RNGの状態アクションを処理しているという認識はありません。このため、より汎用的な型が必要です。


```scala=
def map[S,A,B](a: S => (A,S))(f: A => B): S => (B,S)
```

シグネチャをこのように変更したからといってmapの実装を変更する必要はありません。
より汎用的なシグネチャはずっと存在していましたが、私たちの目に入らなかっただけです。
任意の型の状態を処理するには、Randよりも汎用的な型が必要です。


```scala=
type State[S,+A] = S => (A,S)
```

このStateは、<b>何らかの状態を扱う計算</b>、あるいは<b>状態アクション</b>、<b>状態遷移</b>、さらには<b>ステートメント</b>の省略形です。
リスト 6-8に示すように、これをクラスとして独立させ、関数を追加するとよいかもしれません。

#### リスト 6-8
```scala=
case class State[S,+A](run: S => (A,S))
```

重要なのはコードの表現ではなく、汎用目的のただ1つの型が定義されていることです。この型を使って、ステートフルプログラムの共通パターンを表現する汎用目的の関数を記述すればよいわけです。
あとは、RandをStateの型エイリアスにするだけです。

#### リスト 6-9
```scala=
type Rand[A] = State[RNG, A]
```
#### EXCERCISE 6.10
unit,map,map2,flatMap,sequenceの5つの関数を一般化せよ。可能であればそれらをStateケースクラスのメソッドとして追加せよ。それが不可能であれば、Stateコンパニオンオブジェクトに配置せよ。

```scala=
case class State[S,+A](run: S => (A,S)){

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)
  
  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }
  
  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng => {
      val (a, r1) = ra(rng)
      val (b, r2) = rb(r1)
      (f(a, b), r2)
    }
  
 def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, r1) = f(rng)
      g(a)(r1)
    }
}  
```

解答
```scala=
case class State[S, +A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =
    flatMap(a => unit(f(a)))
  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap(a => sb.map(b => f(a, b)))
  def flatMap[B](f: A => State[S, B]): State[S, B] = State(s => {
    val (a, s1) = run(s)
    f(a).run(s1)
  })
}


```

###### fpinscalaのgit持ってきた
```scala=
case class State[S,+A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =
    ???
  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    ???
  def flatMap[B](f: A => State[S, B]): State[S, B] =
    ???
}

```

解答
```scala=
trait RNG {
  def nextInt: (Int, RNG) // Should generate a random `Int`. We'll later define other functions in terms of `nextInt`.
}

case class State[S, +A](run: S => (A, S)) {

  // 今までのRand 
  // type Rand[+A] = RNG => (A, RNG)
  type Rand[A] = State[RNG, A]

  def unit[S, A](a: A): State[S, A] =
    State(s => (a, s))

  def map[B](f: A => B): State[S, B] =
    flatMap(a => unit(f(a)))

  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap(a => sb.map(b => f(a, b)))

  def flatMap[B](f: A => State[S, B]): State[S, B] = State(s => {
    val (a, s1) = run(s)
    f(a).run(s1)
  })

  def sequence[S, A](sas: List[State[S, A]]): State[S, List[A]] = {
    def go(s: S, actions: List[State[S, A]], acc: List[A]): (List[A], S) =
      actions match {
        case Nil => (acc.reverse, s)
        case h :: t => h.run(s) match {
          case (a, s2) => go(s2, t, a :: acc)
        }
      }

    State((s: S) => go(s, sas, List()))
  }
}
```

###### RNGのflatMapとStateのflatMapの違いだけを見るとなんとなくわかるような気がする・・・

###### 問題文の `可能であればそれらをStateケースクラスのメソッドとして追加せよ。それが不可能であれば、Stateコンパニオンオブジェクトに配置せよ。` が謎のまま

ここで記述した関数は、最も一般的なパターンのごく一部を表現しているだけです。関数型プログラミングのコードを記述するようになれば、他のパターンに気づいて、それらを表現する関数を発見するようになるでしょう。

## 6.6 純粋関数型の命令型プログラミング

ここまでの部分では明確なパターンに従う関数を記述してきました。それらは状態アクションを実行し、その結果をvalに代入し、そのvalを利用する別の状態アクションを実行し、その結果を別のvalに代入する、といったものでした。これは命令型プログラミング(imperative programming)によく似ています。

命令型プログラミングのパラダイムでは、プログラムとはずらりと並んだステートメントのことであり、ステートメントはそれぞれプログラムの状態を変化させる可能性があります。

それはまさに本章で行ってきたことですが、ここでの「ステートメント」はStateのアクションであり、それらは実際には関数です。それらは関数として、引数として渡されたプログラムの現在の状態を読み取り、単に値を返すことでプログラムの状態を書き出します。

<b>命令型プログラミングの反対は関数型プログラミングか</b>
そのようなことは絶対にありません。関数型プログラミングが副作用のないプログラミングにすぎないことを思い出してください。

命令型プログラミングとはステートメントを使ってプログラムの状態を変更するプログラミングのことです。
ここまで見てきたように副作用を伴わずに状態を管理することは完全に理にかなっています。

関数型プログラミングは、命令型プログラムの記述を見事にサポートするだけではありません。そうしたプログラムは参照透過であるため、それらの等式推論が可能であるという付加価値もあります。

プログラムの等式推論についてはPartⅡで説明します。
命令型プログラムについてはPartⅢとPartⅣで説明します。

ここではステートメントからステートメントへの状態の伝達を処理するために、map、map2、そして最終的にflatMapなどのコンビネータを実装してきました。
しかし、その過程で命令型のスタイルが少し失われてしまったように思えます。

例
```scala=
val ns: Rand[List[Int]] =
  int.flatMap(x =>	
    int.flatMap(y =>
     ints(x).map(xs =>	
       xs.map(_ % y))))	

```
###### 2行目 intはランダムな整数を1つ生成Rand[Int]型
###### 3行目 ints(x)は長さxのリストを生成
###### 4行目 リストないの全ての要素をyで割った余りと置き換える

これでは何が行われているかよくわかりません。
ただし、mapとflatMapは定義済みなので、for内包表記を使って命令型のスタイルを取り戻すことができます。

```scala=
val ns: Rand[List[Int]] = for {
  x <- int	
  y <- int	
  xs <- ints(x)	
} yield xs.map(_ % y)	

```
###### 2行目 整数xを生成
###### 3行目 別の整数yを生成
###### 4行目 長さxのリストxsを生成
###### 5行目 各要素をyで割った余りと置き換えたリストxsを返す。

このコードの方がはるかに読み書きしやすく、書かれているとおりのものに見えます。
これは状態を管理する命令型のプログラムです。ただし、これは同じコードです。次の整数(Int)を取得し、それをxに代入し、その次の整数を取得し、それをyに代入し、長さのxのリストを生成し、最後にyを法とするすべての要素のリストを返しています。

`法として`?

Oさんの解説によるとn進数のような意味だった


この種の命令型プログラミングでfor内包表記(またはflatMap)を利用するために必要なのは、Stateの2つのプリミティブコンビネータだけです。1つは状態を読み取るためのもので、もう1つは状態を書き出すためのものです。

現在の状態を取得するためのgetコンビネータと、新しい状態を設定するためのsetコンビネータがあれば、任意の方法で状態を変更することが可能なコンビネータを実装できるはずです。


State 復習1
```scala=
type Rand[A] = State[RNG, A]

case class State[S, +A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =
    flatMap(a => unit(f(a)))
  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap(a => sb.map(b => f(a, b)))
  def flatMap[B](f: A => State[S, B]): State[S, B] = State(s => {
    val (a, s1) = run(s)
    f(a).run(s1)
  })
}
```

#### リスト 6-10
```scala=
def modify[S](f: S => S): State[S, Unit] = for {
  s <- get	
  _ <- set(f(s))
} yield ()
```
2行目 現在の状態を取得し、sに代入
3行目 sに適用されるfに新しい状態を設定

このメソッドは関数fから渡された状態を変更するStateアクションを返します。このアクションは、状態以外に戻り値がないことを示すUnitを生成します。
getアクションとsetアクションはどのようなものになるのでしょうか。これらは非常に単純です。getアクションは渡された状態を値として返すだけです。


#### リスト 6-11
```scala=
def get[S]: State[S, S] = State(s => (s, s))
```

setアクションは新しい状態sを使って構築されます。
結果として得られるアクションは、渡された状態を無視し、それを新しい状態へと置き換え、意味のある値ではなく()を返します。


#### リスト 6-12
```scala=
def set[S](s: S): State[S, Unit] = State(_ => ((), s))
```

State 復習2
```scala=
type Rand[A] = State[RNG, A]

object State {
  type Rand[A] = State[RNG, A]

  def unit[S, A](a: A): State[S, A] =
    State(s => (a, s))

  def modify[S](f: S => S): State[S, Unit] = for {
    s <- get
    _ <- set(f(s))
  } yield ()

  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => ((), s))
}
```

#### EXERCISE 6.11
難問:Stateの使用になれるために単純なスナックの自動販売機をモデリングする有限状態オートマトンを実装せよ。この自動販売機では、2種類の入力を使用する。すなわち、硬貨を投入することができ、ハンドルを回してスナックを取り出すことができる。

自動販売機はロックされた状態とロックが解除された状態のどちらかになる。また、残りのスナックの数と自動販売機に投入された硬貨の数も追跡する。


```scala=
sealed trait Input
case object Coin extends Input
case object Turn extends Input

case class Machine(locked: Boolean, candies: Int, coins: Int)
```

自動販売機のルールは以下のとおり。

- ロックされた状態の自動販売機に硬貨を投入すると、スナックが残っている場合はロックが解除される。
- ロックが解除された状態の自動販売機のハンドルを回すと、スナックが出てきてロックがかかる。
- ロックされた状態でハンドルを回したり、ロックが解除された状態で硬貨を投入したりしても何も起こらない
- スナックが売り切れた自動販売機が入力をすべて無視する。

simulateMachineメソッドは、入力のリストに基づいて自動販売機を操作し、最後に自動販売機の中にある硬貨とスナックの数を返す。たとえば、入力であるMachineに硬貨が10枚、スナックが5個入っていて、合計で4個のスナックが正常に購入された場合、出力は(14,1)になるはずだ。

```scala=
def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)]
```

やってみようとした

```scala=
/*
ロックされた状態の自動販売機に硬貨を投入すると、スナックが残っている場合はロックが解除される。
Machine(locked: True, candies: 40, coins: 0)
　　↓  simulateMachine(inputs: List(Coin))
State(Machine(locked: False, candies: 40, coins: 1), (Coin数:0, Candies:0 ))

ロックが解除された状態の自動販売機のハンドルを回すと、スナックが出てきてロックがかかる。
Machine(locked: False, candies: 40, coins: 1)
　　↓  simulateMachine(inputs: List(Turn))
State(Machine(locked: True, candies: 39, coins: 0), (Coin数:0, Candies:1 ))

ロックされた状態でハンドルを回したり、ロックが解除された状態で硬貨を投入したりしても何も起こらない
Machine(locked: True, candies: 39, coins: 0)
　　↓  simulateMachine(inputs: List(Turn))
State(Machine(locked: True, candies: 39, coins: 0), (Coin数:0, Candies:0 ))

Machine(locked: False, candies: 39, coins: 1)
　　↓  simulateMachine(inputs: List(Coin,Coin))
State(Machine(locked: False, candies: 39, coins: 3), (Coin数:0, Candies:0 ))

スナックが売り切れた自動販売機が入力をすべて無視する。

Machine(locked: True, candies: 0, coins: 5)
　　↓  simulateMachine(inputs: List(Turn,Coin))
Machine(locked: True, candies: 0, coins: 5), (Coin数:0, Candies:0 ))


simulateMachineメソッドは、入力のリストに基づいて自動販売機を操作し、
最後に自動販売機の中にある硬貨とスナックの数を返す。

たとえば、入力であるMachineに硬貨が10枚、スナックが5個入っていて、
合計で4個のスナックが正常に購入された場合、出力は(14,1)になるはずだ。

Machine(locked: True, candies: 5, coins: 10)
　　↓  simulateMachine(inputs: List(Coin,Turn,Coin,Turn,Coin,Turn,Coin,Turn))
Machine(locked: True, candies: 1, coins: 14), (Coin数:0, Candies:4 ))

 */

もうinputにMachineが無いからお手上げ
def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = {

  val coinCount = inputs.filter(_== Coin).count
  val turnCount = inputs.filter(_== Turn).count
  
  if(coinCount > 0) this.machine.coin + coinCount
  if(turnCount > 0) this
  
  // inputs.map{ input => 
  //   input match {
  //     case Coin => 
  //     case Turn => 
  //     case _ =>       
  //   }
  // }  
}

```


解答
```scala=
import State._

case class State[S, +A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =
    flatMap(a => unit(f(a)))
  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap(a => sb.map(b => f(a, b)))
  def flatMap[B](f: A => State[S, B]): State[S, B] = State(s => {
    val (a, s1) = run(s)
    f(a).run(s1)
  })
}

object State {
  type Rand[A] = State[RNG, A]

  def unit[S, A](a: A): State[S, A] =
    State(s => (a, s))

  // The idiomatic solution is expressed via foldRight
  def sequenceViaFoldRight[S,A](sas: List[State[S, A]]): State[S, List[A]] =
    sas.foldRight(unit[S, List[A]](List()))((f, acc) => f.map2(acc)(_ :: _))


/*
この実装は内部的にループを使用し、左折りと同じ再帰パターンです。左の折り目では、リストを逆の順序で作成し、最後にリストを逆にするのが一般的です。 （内部でcollection.mutable.ListBufferを使用することもできます。）
*/
  def sequence[S, A](sas: List[State[S, A]]): State[S, List[A]] = {
    def go(s: S, actions: List[State[S,A]], acc: List[A]): (List[A],S) =
      actions match {
        case Nil => (acc.reverse,s)
        case h :: t => h.run(s) match { case (a,s2) => go(s2, t, a :: acc) }
      }
    State((s: S) => go(s,sas,List()))
  }


/*
左折りを使用してループを記述することもできます。
これは、前のソリューションと同様に末尾再帰ですが、
リストを反転し、後でではなく、折り返す前にリストします。
リストを2回検索するため、これは `foldRight`ソリューションよりも遅いと思うかもしれませんが、実際にはもっと速いです！ 
`foldRight`ソリューションは、末尾再帰ではなく呼び出しスタックを解く必要があるため、技術的にもリストを2回歩く必要があります。
そして、呼び出しスタックはリストが長いほど高くなります。
 */ 
  def sequenceViaFoldLeft[S,A](l: List[State[S, A]]): State[S, List[A]] =
    l.reverse.foldLeft(unit[S, List[A]](List()))((acc, f) => f.map2(acc)( _ :: _ ))

  def modify[S](f: S => S): State[S, Unit] = for {
    s <- get // Gets the current state and assigns it to `s`.
    _ <- set(f(s)) // Sets the new state to `f` applied to `s`.
  } yield ()

  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => ((), s))
}

sealed trait Input
case object Coin extends Input
case object Turn extends Input

case class Machine(locked: Boolean, candies: Int, coins: Int)

object Candy {
  def update = (i: Input) => (s: Machine) =>
    (i, s) match {
      case (_, Machine(_, 0, _)) => s
      case (Coin, Machine(false, _, _)) => s
      case (Turn, Machine(true, _, _)) => s
      case (Coin, Machine(true, candy, coin)) =>
        Machine(false, candy, coin + 1)
      case (Turn, Machine(false, candy, coin)) =>
        Machine(true, candy - 1, coin)
    }

  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = for {
    _ <- sequence(inputs map (modify[Machine] _ compose update))
    s <- get
  } yield (s.coins, s.candies)
}
```
:::

```scala=
object Candy {
  def update = (i: Input) => (s: Machine) =>
    (i, s) match {
      case (_, Machine(_, 0, _)) => s
      case (Coin, Machine(false, _, _)) => s
      case (Turn, Machine(true, _, _)) => s
      case (Coin, Machine(true, candy, coin)) =>
        Machine(false, candy, coin + 1)
      case (Turn, Machine(false, candy, coin)) =>
        Machine(true, candy - 1, coin)
    }

  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = for {
    _ <- sequence(inputs map (modify[Machine] _ compose update))
    s <- get
  } yield (s.coins, s.candies)
}

```
intelliJ上では
```scala=
def update: Input => Machine => Machine = (i: Input) => (s: Machine) =>
```

復習
```scala=
def sequence[S, A](sas: List[State[S, A]]): State[S, List[A]]

composeはFunction1の下記メソッド
def compose[A](g: A => T1): A => R = { x => apply(g(x)) }


  def modify[S](f: S => S): State[S, Unit] = for {
    s <- get // Gets the current state and assigns it to `s`.
    _ <- set(f(s)) // Sets the new state to `f` applied to `s`.
  } yield ()

case class State[S, +A](run: S => (A, S))

  def get[S]: State[S, S] = State(s => (s, s))
  def set[S](s: S): State[S, Unit] = State(_ => ((), s))

```
#### まとめ


本章では、状態を保つ純数関数型のプログラムを記述する方法に話題がおよびました。
考え方は単純で、引数として状態を受け取る純粋関数を使用し、結果とともに新しい状態を返します。

次回、副作用に依存する命令型APIに遭遇したときには、その純粋関数型バージョンを作成し、本章で記述した関数を使ってその処理をもっと便利なものにできるかどうかを考えてみてください。



---------------


```scala=
class Creature
class Animal extends Creature
class Cat extends Animal

class Container[-T] {
  //def foo(x: Animal): Animal = x
  def foo[T1 <: T](x: T1): T1 = x
}

val cre: Container[Animal] = new Container[Creature]
cre.foo(new Creature)
cre.foo(new Animal)
cre.foo(new Cat)
cre.foo(AnyRef)
//cre.foo(nothing)
cre.foo("hello") // これもOK

val ani: Container[Animal] = new Container[Animal]


//val cat: Container[Animal] = new Container[Cat]
```


Scalaでは、何も指定しなかった型パラメータは通常は非変（invariant）
Javaの組み込み配列クラスは標準で非変ではなく共変であるという設計ミスを犯している

```
  不変（invariant）：List<Object> と List<String> には関係性がない
  共変（covariant）：List<Object> は List<String> のスーパータイプ
  反変（contravariant）：List<String> は List<Object> のスーパータイプ
```
[【Java】ジェネリックス型の不変、共変、反変とは何か](https://www.thekingsmuseum.info/entry/2016/02/07/235454)

[Oさんが参考に教えてくれたジェネリクスについての記事](
https://apiumhub.com/tech-blog-barcelona/scala-generics-generalized-type-constraints/)

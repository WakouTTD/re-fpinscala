# fpinscala 輪読会 #10

# Part Ⅲ

このチャプターを終わる頃には「モナドのように見える」と発言するようになります。

---

### 第10章 モノイド

- 代数とは、データ型がサポートする演算と制御する法則
- 純粋な代数構造の世界に足を踏み入れ、monoidについて考えます。
- モノイドとはその代数によってのみ定義される単純な構造のこと。
- モノイドインターフェースのインスタンスどうしは、同じ法則を満たすこと以外、ほぼ繋がりがありません。
- とはいえ、この代数構造さえあれば、有効な多相関数を記述するのに十分であることがわかるでしょう。

- 日々のプログラミングはモノイドだらけです。
  - モノイドという名前は数学に由来するが、圏論では1つの対象を持つ圏(category)を意味する。
  - この数学との関連性は、本章の目的にとって重要ではない。

- モノイドがいかに有益であるかは2つの点
  - 問題を並列計算が可能なブロックに分割できるようにすることでモノイドが並列化を促進できる
  - 単純な要素から複雑な計算を組み立てるためにモノイドを合成できる

---

### 10.1 モノイドとは

>"foo" + "bar"を演算すると"foobar"となり、この演算の単位元(identity element)は空の文字列です。
つまり、(s + "")または("" + s)の結果は常にsになります。
さらに(r + s + t)を使って3つの文字列を結合する場合、その演算は結合的(associative)です。
つまり、((r + s) + t)または(r + (s + t))のように括弧で囲んだとしても結果は同じです。

>整数の加算のルールも全く同じです。
(x + y) + zは常にx + (y + z)に等しいことから結合的であり、その単位元は別の整数に足しても何の影響も与えない0です。
乗算についても同じで、単位元は1になります。
論理演算子である&&と||も同様に結合的であり、単位元はそれぞれtrueとfalseです。


モノイドはこうした代数を指す用語
結合律と同一律はモノイド則と総称される。

モノイドは以下の要素で構成される
- 何らかの型A
- A型の2つの値を受け取り、それらを1つにまとめる2項連想演算op。  <br>任意の x:A, y:A, z:Aに対し、op(op(x,y), z) == op(x, op(y,z))が成り立つ
- この演算の単位元であるzero: Aの値。任意のx: Aに対し、  <br>
op(x, zero) == xとop(zero, x)　== xが成り立つ



**モノイド**
- 2項演算が存在する
- その演算に対して結合法則が成り立つ
- 単位元が存在する

**結合法則**
(A+B)+C = A+(B+C)

**単位元**
X+0 = 0+X = X

```scala=

trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}


Stringモノイドの例
val stringMonoid = new Monoid[String] {
  def op(a1: String, a2: String) = a1 + a2
  
  def zero: String = ""
}

Listモノイドの例
val listMonoid = new Monoid[List[A]] {
  def op(a1: List[A], a2: List[A]) = a1 ++ a2
  
  def zero = Nil
}

```

#### EXERCISE 10.1
```scala=

val intAddition: Monoid[Int] = new Monoid[Int] {
  def op(a1:Int, a2:Int): Int = a1 + a2

  def zero: Int = 0
}

val intMultiplication: Monoid[Int] = new Monoid[Int] {
  def op(a1:Int, a2:Int): Int = a1 * a2

  def zero: Int = 1
}

val booleanOr: Monoid[Boolean] = new Monoid[Boolean] {
  def op(a1: Boolean, a2:Boolean): Boolean = a1 || a2

  def zero: Boolean = false
}

val booleanAnd: Monoid[Boolean] = new Monoid[Boolean] {
  def op(a1: Boolean, a2:Boolean): Boolean = a1 && a2

  def zero: Boolean = true
}
```


#### EXERCISE 10.2

```scala=

def optionMonoid[A]: Monoid[Option[A]] = new Monoid[Option[A]] {
  def op(a1: Option[A], a2:Option[A]): Option[A] = (a1, a2) match {
    case (None, None) => None
    case (Some(_), Some(_)) => a1
  }

  def zero: Option[A] = None
}

```

#### EXERCISE 10.3
```scala=
def endoMonoid[A]: Monoid[A=>A] = new Monoid[A=>A] {
  def op(a1:A=>A, a2:A=>A): A=>A = a => a2(a1(a))

  def zero = identity
}

```

#### EXERCISE 10.4

```scala=

```



---

### 10.2 モノイドによるリストの畳み込み

---

モノイドはリストと深い関わりがある。
リストの強力な演算である畳み込みを考えてみる

```scala=

def foldRight[B](z: B)(f: (A,B)=>B): B
def foldLeft[B](z: B)(f: (B,A)=>B): B

ここでAとBが同じかたの場合は

def foldRight(z: A)(f: (A,A)=>A): A
def foldLeft(z: A)(f: (A,A)=>A): A

z → 単位元
f → 二項演算

モノイドにぴったり一致する

val words = List("Hic", "Est", "Index")
val s = words.foldRight(stringMonoid.zero)(stringMonoid.op)
val t = words.foldLeft(stringMonoid.zero)(stringMonoid.op)

モノイドは結合法則を満たすので右からでも左からでも結果は変わらない
op(a, op(b, c)) = op(op(a,b),c)


モノイドでリストを畳み込む総称関数
def concatenate[A](as: List[A], m: Monoid[A]): A = as.foldLeft(m.zero)(m.op)

```


#### EXERCISE 10.5
```scala=

def foldMap[A,B](as: List[A], m: Monoid[B])(f: A=>B): B = 
    as.foldLeft(m.zero)((acc,e) => m.op(acc, f(e)))

```


#### EXERCISE 10.6
```scala=

def foldMap[A,B](as: List[A], m: Monoid[B])(f: A=>B): B = 
    as.foldLeft(m.zero)((acc,e) => m.op(acc, f(e)))


def foldRight[A,B](as: List[A], z:B)(f: (A,B) => B): B = 
    foldMap(as.map(f.curried(_)).reverse, endoMonoid[B])(identity)(z)

def foldLeft[A,B](as: List[A], z: B)(f: (B, A)=>B): B = 
    foldMap(as.map(x => {b:B => f.curried(b)(x)}), endoMonoid[B])(identity)(z)

```


---

### 10.3 結合性と並列性

>モノイドの演算が結合的であるということは、リストなどのデータ構造を畳み込む方法を選択できるということ
foldLeftまたはfoldRightを使って演算を左または右に結合すれば、リストを徐々に畳み込むことができます。

>しかし、モノイドを利用できる場合は、平衡畳み込み(balanced fold)を使ってリストを畳み込むことができます。
演算によっては、この方が効率的であり、並列化も可能となります。

---
モノイドは結合法則が成り立ちます。
これは演算の順番が計算結果に影響を与えないということなので、畳み込む方法を選択できるということになります。

```scala=
a,b,c,dを畳み込む例

右畳み込み
op(a, op(b, op(c,d)))

左畳み込み
op(op(op(a,b),c),d)

平衡畳み込み
op(op(a,b), op(c,d))

```

- 上の三つは演算の順序が違うが計算結果は同じになる（結合法則のおかげで）
  - → 計算結果が同じになることが保証されるので好きな方法を選べるということ
  - → 平衡畳み込みを使えばop(a,b)とop(c,d)を並列化できるので早くなりそう!!
  
**交換法則が成り立つわけではない!!!**

交換法則
A+B=B+A

結合法則と交換法則は別物である。

上のa,b,c,dの畳み込みの例でいうと
```scala=

op(op(b,a), op(d,c))

が等しくなるわけではない。
a,b,c,dの順番がb,a,d,cに変わってしまっているため。

```



 ※ ここでの+演算は数字の+演算ではなく一般的な演算記号として使用している




#### EXERCISE 10.7

```scala=

```

#### EXERCISE 10.8

```scala=

```

#### EXERCISE 10.9

```scala=

```

---

### 10.4 並列解析の例

---

#### EXERCISE 10.10

```scala=

```

#### EXERCISE 10.11

```scala=

```


## モノイド準同型写像

二つのモノイドがある時に、それらの構造をうまく保つような変換（関数とかメソッド）がモノイド準同型写像という

例として文字モノイドと整数モノイドをあげる
```scala=

//Stringモノイド
val stringMonoid = new Monoid[String] {
  def op(a1: String, a2: String) = a1 + a2
  
  def zero: String = ""
}

//整数モノイド
val intMonoid: Monoid[Int] = new Monoid[Int] {
  def op(a1:Int, a2:Int): Int = a1 + a2

  def zero: Int = 0
}

```

この場合、二つの構造をうまく保つような変換がlength（文字の長さを返す）関数になる

```scala=
"Scala".length + "Enginner".length = ("Scala" + "Enginner").length

↓

intMonoid.op("Scala".length, "Enginner".length) = stringMonoid.op("Scala", "Enginner").length


ポイントは+足し算がモノイドのopメソッドに抽象化されているのを感じること
```


この例ではString→Intの準同型写像(length)でした。

ここで仮に、
1. A→Bの準同型写像fがある
2. B→Aの準同型写像gがある
3. f andThen gが恒等関数
4. g andThen fが恒等関数

の4条件を満たした場合はfとgを**モノイド同型写像**と呼ぶ

モノイド同型写像になる例
・(String, +)モノイドと(List[Char], ++)モノイド
・(false, ||)モノイドと(true, &&)モノイド

---

### 10.5 畳み込み可能なデータ構造

---
### 10.6 モノイドの合成

>モノイドの真価は、それらを合成できることにある。
型Aと型Bがモノイドである場合、タプル型(A, B)もモノイドとなり、それらの積(product)と呼ばれます。



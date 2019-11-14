# fpinscala 輪読会 #10

# Part III

---

このチャプターを終わる頃には「モナドのように見える」と発言するようになります。

---

### 第10章 モノイド

---

日々のプログラミングはモノイドだらけです。

---

### 10.1 モノイドとは

---

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

:::success
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

:::


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

上の三つは演算の順序が違うが計算結果は同じになる（結合法則のおかげで）
→計算結果が同じになることが保証されるので好きな方法を選べるということ
→平衡畳み込みを使えばop(a,b)とop(c,d)を並列化できるので早くなりそう!!
  


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



※ここでの+演算は数字の+演算ではなく一般的な演算記号として使用している




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



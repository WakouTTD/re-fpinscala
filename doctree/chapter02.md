# fpinscala 輪読会 #2

## 第2章 Scala関数型プログラミングの準備

---

- Scala言語の構文を理解する
  - 末尾再帰関数を使ったループ
  - 高階関数
  (他の関数を引数として受け取り、出力として関数を返すことができる関数)
- 型に基づいて実装を導く多相高階関数

---

## 2.1 速習：Scala言語

---

```scala=
object MyModule {
  def abs(n: Int): Int =
    if(n < 0) -n
    else n

  private def formatAbs(x: Int) = {
    val msg = "The absolute value of %d is %d"
    msg.format(x, abs(x))
  }

  def main(args: Array[String]): Unit = {
    println(formatAbs(-42))
  }
}
```

---

- `MyModule`
  - このようなオブジェクトをモジュールという
  - コードに場所と名前を与えて、あとから参照できるようにした
  - Scalaのコードは、`object`、`class`に含まれていなければならない

- `def`
  - `def`を使って、オブジェクト || クラスで定義された関数・フィールドをメソッドという

- `=`
  - 単一の等号(=)よりも左辺の部分をシグネチャ、右辺は定義という
  - 右辺の定義は式(評価の結果を返すもの)であるため`return`キーワードは明示的に指定されていない

- `formatAbs`
  - `private`なメソッドであるため、結果型が定義されていない

- `main`
  - コンソール出力という副作用を持っているため、プロシージャーまたは非純粋関数と呼ばれることがある

---

## 2.2 プログラムの実行

---

### sbtなどのビルドツールを使って実行するのが一般的

---

## 2.3 モジュール、オブジェクト、名前空間

---

- オブジェクト
  - Scalaでは、すべての値はオブジェクトである
  - 各オブジェクトは、メンバーを1つ以上定義できるが、1つも定義しなくてもいい
  - メンバーに名前空間を割り当てることを主な目的とするオブジェクトは、モジュールと呼ばれる

- メンバー
  - `def`キーワード使ってメソッドとして宣言するか、`val`、`object`キーワード使って別のオブジェクトとして宣言する

- オブジェクトの参照
  - 名前空間の後に`.`(ドット)とメンバー名を指定する
  - `42.toString`
  - `2 + 1`は、オブジェクト`2`の`+`メンバーを呼び出している
  
- 単一の引数で呼び出す場合は、中置形式で使用できる
  - `MyModule.abs(42)` = `MyModule abs 42`

- オブジェクトのメンバーをスコープに含めたい場合は、インポートを利用する

---

```scala=
scala> import MyModule.abs
import MyModule.abs

scala> abs(42)
res1:  Int = 42
```

```scala=
// オブジェクトのメンバーをすべて含めたい場合は、`_`アンダースコア構文を使用する
scala> import MyModule._
```

---

## 2.4 高階関数:関数に関数を渡す

> 関数は値である

---

### 2.4.1 関数型のループ

---

```scala=
def factorial(n: Int): Int = {
  @annotation.tailrec
  def go(n: Int, acc: Int): Int = // ローカル定義
    if (n <= 0) acc
    else go(n-1, n*acc)
  
  go(n, 1)
}
```

---

- ループ変数を使用せず、再帰関数を使用する

- 再帰ヘルパー関数`go`を定義(慣例では`loop`という名称でも良い)

- `go`関数は、`factorial`関数の本体からのみ参照できる

- `go`関数の引数は、残余値`n`と現在の累積階乗`acc`である

- ループを抜けるには、再帰呼び出しを行う代わりに値を返す
  - ここでは、`n <= 0`の場合に`acc`を返す

- 再帰呼び出しが末尾再帰である限り、`While`ループに対して生成されるものと同じ種類のバイトコードとしてコンパイルされる

---

**Scalaの末尾再帰**

- コールスタックフレーム
  - 関数を呼び出すときに生成されるローカル変数などのメモリを保持するスタック領域のこと
  - スタックフレーム（アクティベーションレコード）ともいう

- 問題点
  - 関数の呼び出し階層が深くなると、スタックフレームに保持するメモリ領域が増大する
  - 末尾再帰にすることで、このスタックフレームを再利用することができる
  - ポイントは関数の演算結果を引数として渡すこと

- 結果として、コールスタックフレームを消費しないループとして自動的にコンパイルされる

- `@annotation.tailrec`を使用することで、末尾再帰になっているかの確認ができる

---

### EXERCISE2-1

```scala=
def fib(n: Int): Int = {
  @annotation.tailrec
  def go(n: Int, a: Int, b: Int): Int = {
    if(n <= 0) a else go(n-1, b, a+b)
  }
  go(n, 0, 1)
}
```

---

### 2.4.2 高階関数の作成

---

```scala=
def factorial(n: Int): Int = {
  @annotation.tailrec
  def go(n: Int, acc: Int): Int =
    if (n <= 0) acc
    else go(n-1, n*acc)
  
  go(n, 1)
}

def formatFactorial(n: Int) = {
  val msg = "The factorial of %d is %d."
  msg.format(n, factorial(n))
}
```

一般化
```scala=
def formatResult(name: String, n: Int, f: Int => Int) = {
  val msg = "The %s of %d is %d"
  msg.format(name, n, f(n))
}
```

---

- `formatResult`
  - 引数として`f`という別の関数を受け取る高階関数
  - `f`に、`Int => Int` という型を割り当てている
  - `Int` と受け取って `Int` を返す関数を型としているため、以下のように`factorial`を渡して呼び出すことができる


```scala=
formatResult("test", 10, factorial)
```

---

## 2.5 多相関数：型の抽象化

---

- 単相関数(mmonomorphic)
  - 1つの型のデータだけを操作する関数のこと
  - 既に出てきている `abs`、`factorial` は、 `Int`型、`Int => Int`型に、限定されている

- 多相関数(polymorphic function)
  - ・・・例を見てくwww

---

### 2.5.1 多相関数の例

---

単相関数の例
```scala=
def findFirst(ss : Array[String], key: String): Int = {
  @annotation.tailrec
  def loop(n :Int): Int =
    if(n >= ss.length) -1
    else if(ss(n) == key) n
    else loop(n + 1)  

  loop(0)
}
```

---

多相関数の例
```scala=
def findFirst[A](as : Array[A], p: A => Boolean): Int = {
  @annotation.tailrec
  def loop(n :Int): Int =
    if(n >= as.length) -1
    else if(p(as(n))) n
    else loop(n + 1)

  loop(0)
}
```

---

- `def findFirst[A]`
  - 配列と、その検索に使用する型を抽象化している
  - 型パラメータ名は、一般的に、`[A,B,C]`のように大文字1つの名前をつけるの慣例となっている

- `(as : Array[A], p: A => Boolean)`
  - ここでは、型変数`A`が、2つの場所で参照されている
  - どちらの`A`も、同じ型でなければならない
  - コンパイル時に、コンパイラが型のチェックを行う

```scala=
/** 型引数Aの型が以下の通り異なるため、コンパイルエラー
 * - Array[String]
 * - Int
 */
findFirst(Array("1", "2"), (n: Int) => n.toString == "1")
```

---

### EXERCISE2-2
```scala=
def isSorted[A](as: List[A], ordered: (A, A) => Boolean): Boolean = {
  def go(n: Int): Boolean =
    if(n == 0) true
    else if(ordered(as(n - 1), as(n))) go(n - 1)
    else false
  go(as.length - 1)
}
```
```scala=
isSorted(List(1, 2, 4, 4, 5), (a: Int, b: Int) => a <= b)
```

---

### 2.5.2 無名関数を使った高階関数の呼び出し

---

- 無名関数(anonymous function)、関数リテラル(function letral)

- 高階関数を使用する場合には、以下のように呼び出せる
  - `Array(7, 9, 13)` は、配列リテラル
  - `(x: Int) => x == 9` は、関数リテラルであり、無名関数とも呼ばれる

```scala=
findFirst(Array(7, 9, 13), (x: Int) => x == 9)
```

---

Scalaでの値としての関数

- 関数リテラルを定義する際、Scalaで実際に定義されるのは、`apply` というメソッドを持つオブジェクトである

- `(a, b) => a == b` のような関数リテラルの定義は、実際にはオブジェクトを作成するためのシンタックスシュガーである

```scala=
val lessThan = new Function2[Int, Int, Boolean] {
  def apply(a: Int, b: Int) = a == b
}
```

- `Function2` は、引数を2つ受け取る関数オブジェクトを表す
- Scalaでは、`Function22` まで定義されている

---

## 2.6 型に従う実装

---

- 多相関数を実装する場合は、可能な実装の幅が制限される

- 部分適用(partial application)
  - 関数が一部の引数にのみ適用されることから、部分適用と呼ばれる

---

例として
- `parrial1`関数
  - 引数として値と関数を受け取り、関数を返す
  - コンパイルが通るのは、以下の実装(L2)のみ

- 補足の説明
  - リンゴ(A)と、バナナ(B)をくれたら、人参(C)をあげよう(=f: (A, B) => C)
  - リンゴ(a: A)は、もうもらったから、バナナをくれたら人参をあげる

```scala=
def partial1[A,B,C](a: A, f: (A, B) => C): B => C =
  b => f(a, b)
```

- つまり、型シグネチャに従う実装に限定されている
- また、実装が限定されているため、Scalaの推論により `b` の型アノテーション( `b: B` )が不要

---

### EXERCISE2-3
```scala=
def curry[A,B,C](f: (A,B) => C): A => B => C =
  a => b => f(a, b)
```
<small></small>

---

### EXERCISE2-4
```scala=
def uncurry[A,B,C](f: A => B => C): (A, B) => C =
  (a, b) => f(a)(b)
```

---

### EXERCISE2-5
```scala=
def compose[A,B,C](f: B => C, g: A => B): A => C =
  a => f(g(a))
```

---

- EXERCISE2-5は、関数合成の例であるが、Scalaの標準ライブラリでは、`compose`が`Funciton1`のメソッドてして定義されている

- 関数`f`と、関数`g`を合成するには、`f compose g`とする

- `f andThen g`は、上記と同じ意味

---


## 2.7 まとめ

---

- 再帰によるループ処理
- 高階関数
  - 他の関数を引数として受け取り、出力として関数を返すことができる関数
- 多相関数
  - 型に従うことで正しい実装を導き出せる
  - その分、実装が制限される

おわり

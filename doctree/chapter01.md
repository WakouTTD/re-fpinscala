# fpinscala 輪読会 #1
---

### 第1章 関数型プログラミングとは

---

- 純粋関数を使ってプログラムを構築すること
- 純粋関数とは副作用のない関数のこと
- 副作用とは結果を返すこと以外に何かをすること
  > 変数の変更、データ構造の変更、オブジェクトのフィールド設定、例外の送出、コンソール・ファイルI/O、画面描画、...etc`

- 関数型プログラミングはプログラミングの書き方を制約するものだが、プログラムの種類を制限するものではない
- 純粋関数を使ったプログラムは、モジュール性が向上し、テスト、再利用、並列化、一般化、推論が用意になり、バグが発生しにくくなる

---

## 1.1 関数型プログラミングの利点

---

:::
- コーヒーショップの注文処理プログラムの例

- 副作用があるとテスタビリティに問題がある

- テストがしにくいため設計を変更した方が良い
:::
```scala=
class Cafe {
  def buyCoffee(cc: CreditCard): Coffee = {
    val cup = new Coffee()
    cc.charge(cup.price) // 外部I/O 副作用
    cup
  }
}
```

---

### :innocent: 改善1

:::warning
- `CreditCard`に、`charge`処理を組み込むべきではない

- `buyCoffee`に`Payments`オブジェクトを渡す修正を行うことで、コードのモジュール性とテスタビリティを向上させる
:::
```scala=
class Cafe {
  def buyCoffee(cc: CreditCard, p: Payments): Coffee = {
    val cup = new Coffee()
    p.charge(cc, cup.price) // 副作用
    cup
  }
}
```

---

#### :imp: 残課題 :smiling_imp: 
- 複数杯Coffeeを注文する場合に、`buyCoffee`の再利用性が難しくなる

- ループ処理で`buyCoffee`を呼び出す場合、CreditCardの決済を複数回行う必要がある

---

### :innocent: 改善2

- 元々メソッドであった`Charge`を、`Coffee`と共に値として返すように修正する
- `case class`により、イミュータブルなパブリックフィールド保持するプライマリコンストラクタを定義
- 同じ`CreditCard`に対する`Charge`をまとめる`combine`という便利な関数を定義

```scala=
class Cafe {
  def buyCoffee(cc: CreditCard): (Coffee, Charge) = {
    val cup = new Coffee()
    (cup, Charge(cc, cup.price))
  }
}
```

```scala=
case class Charge(cc: CreditCard, amount: Double) {
  def combine(other: Charge): Charge =
    if(cc == other.cc)
      Charge(cc, amount + other.amount)
    else
      throw new Exception("Cant't combine charges to different cards.")
}
```

---

### 改善3

- 複数の`Coffee`を購入するための`buyCoffees`を定義する

- `List.fill(n)(x)`は、`x`のコピーを`n`個含んだリストを作成する

- `unzip`は、複数のペアからなる1つのリストを2つのリストのペアに分割する

- `reduce`は、`List[Charge]`のすべての要素に対して、引数の関数を適用する
  (`combine`関数により、同じ`CreditCard`をもつ`Charge`の`Amount`が合算される)

```scala=
def buyCoffees(cc: CreditCard, n: Int): (List[Coffee], Charge) = {
  val purchases: List[(Coffee, Charge)] = List.fill(n)(buyCoffee(cc))
  val (coffees, charges) = purchases.unzip
  (coffees, charges.reduce((c1, c2) => c1.combine(c2)))
}
```

---

### 改善のまとめ

- `Payments`インターフェースとモックを定義しなくても、どの関数も簡単にテストすることができる

- `Charge`値がどのように処理されるのかについて、`Cafe`は関知しない

- `Charge`を処理するためのビジネスロジックが組み立てやすくなる

- 例）`CreditCard`でグループ化し、`Charge`を集計する関数

```scala=
def coalesce(charges: List[Charge]): List[Charge] =
  charges.groupBy(_.cc).values.map(_.reduce(_ combine _)).toList
```

---


## 1.2 関数とはいったい何か
参照透過性
置換モデル

---


- 入力に基づいて計算結果を出力すること以外何もしない

- これは一般の**式**の特性でもある
  - **式**: 1つの結果として評価できるプログラムの任意の部分

  - 例えば`2+3`は、純粋関数`+`を`2`と`3`の値に適用する式であり、`2`と`3`の値も式である

  - この式の評価は、常に`5`であり、プログラム内の`2+3`を`5`に置き換えても意味は変わらない

  - 参照透過な式とは、プログラムの意味を変えることなく、式とその結果に置き換えることができるもの


---

## 1.3 参照透過性、純粋性、置換モデル

---


以下は`x`を、`x`が参照している式と置き換えても結果が変わらない例

```scala=
scala> val x = "Hello World"
x: String = Hello World

scala> val r1 = x.reverse
r1: String = dlroW olleH

scala> val r2 = x.reverse
r2: String = dlroW olleH
```
```scala=
scala> val r1 = "Hello World".reverse
r1: String = dlroW olleH

scala> val r2 = "Hello World".reverse
r2: String = dlroW olleH
```

---
以下は参照透過ではない関数の例

```scala=
scala> val x = new StringBuilder("Hello")
x: StringBuilder = Hello

scala> val r1 = x.append(" World").toString
r1: String = Hello World

scala> val r2 = x.append(" World").toString
r2: String = Hello World World
```

---

## 1.4 まとめ

- 関数型プログラミングはモジュール性が高い

  - プログラム全体から切り離されたコンポーネントで構成される

  - プログラム全体が何を意味するかは、コンポーネントの意味と、それらの合成を決定するルールによって決まる
  - つまり関数型プログラミングは、モジュラーで合成可能(composable)である
  - 入力は関数への引数、出力は計算結果のみというシンプルなロジックになる

- 参照透過性と置換モデルによって、プログラムの推論が容易になる

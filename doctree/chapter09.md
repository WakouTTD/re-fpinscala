# FPinScala輪読会


# 第9章 パーサーコンビネータ

パーサーを作成するためのコンビネータライブラリの設計に取り組む
ユースケースとしてJSON構文解析を使用



パーサーとは、テキストまたは記号、数字、トークンのストリームといった非構造化データを入力として受け取り、そのデータを構造化して出力する、特殊なプログラムのことです。

- たとえば、パーサーを使ってカンマ区切りのファイルをリストのリストに変換
その際、外側のリストはレコードを表し、内側の各リストの要素は各レコードのカンマ区切りのフィールドを表します。

- XMLまたはJSONドキュメントを受け取り、それをツリー形式のデータ構造に変換するパーサーもあります。

本章で作成するものもそうですが、パーサーコンビネータライブラリに含まれているパーサーは、それほど複雑であるとは限らず、ドキュメント全体を解析するとも限りません。
入力の一文字を識別するだけの初歩的なものかもしれません。
コンビネータを使って初歩的なパーサーから合成パーサーを組み立て、そこからさらに複雑なパーサーを作成することもできます。


個人のイメージだと

- パーサー:構文解析器、抽象構文木（abstract syntax tree、AST）とか具象構文木とか

    - 文章を読んで(文字とか単語に区切る≒ツリー構造を作る)解釈する
    - 人間が普段文章読んでやってること
       - 文字列読んで、文法に従って単語にバラして解釈
    - x-liftのscraperもやってた
    - コンパイラがやってること4段階の2番目
      - 1.字句解析（Lexical Analysis）
        - ソースコードを読み込んで、トークン（字句）に分解する工程
      - 2.構文解析（Syntactic Analysis）
        - 分解したトークン列をもとに構文木を構築する工程
      - 3.最適化（Optimization）
        - 構築した構文木を効率の良いものに変換する工程
      - 4.コード生成（Code Generation）
        - 構文木からオブジェクトコードを生成する工程

- コンビーネータ:関数合成演算関数

構文解析器を生成するプログラムを「パーサジェネレータ（Parser Generator）」と言います。パーサジェネレータそのものは構文解析器ではありません。また、パーサジェネレータの中でも、演算子と関数を組み合わせて構文解析器を生成できるプログラムを「パーサコンビネータ（Parser Combinator）」と呼びます。
[参考：パーサコンビネータで構文解析をより身近なものにする](https://ceblog.mediba.jp/post/159559336252/ハ-ーサコンヒ-ネータて-構文解析をより身近なものにする)




本書では `代数的設計(algebraic design)` と呼んでいるアプローチについて説明します。

[Yaccってyet another compiler compiler(またもう一つのコンパイラ　コンパイラ)って意味らしい](https://ja.wikipedia.org/wiki/Yacc)

[ANTLR（ANother Tool for Language Recognition）](https://ja.wikipedia.org/wiki/ANTLR)
[ANTLRの字句解析器・構文解析器・走査器は、Javaがメイン](http://threeprogramming.lolipop.jp/blog/?p=348)

---

## 9.1 代数の設計から始める

:::success
`代数` とは、
- 1つ以上のデータ型を操作する関数の集まり
- そうした関数の間の関係を指定する一連の法則 
- 法則が含まれている代数から作業を開始し、表現をあとから決定する
- このアプローチを `代数的設計` と呼ぶ
:::

最初は"abracadabra"や"abba"といった文字の繰り返しや意味不明な単語の解析から始めるのが簡単で好都合

'a'という1文字の入力する最も単純なパーサー
```scala=
def char(c: Char): Parser[Char]
```
このコードはParserという型を作り出し、Parserの結果型を指定するパラメータを1つ定義している。
つまり成功した場合は、有効な型の結果を返し、失敗した場合は `失敗に関する情報`  を返す

char('a')が成功するのは入力が'a'の場合のみであり、結果としては
Parser('a')が返るはずだが、失敗した場合に、どうなる？
代数を拡張する必要がある


```scala=
def run[A](p: Parser[A])(input: String): Either[ParseError,A]
```
ParseErrorはここで初めて作り出した型

では、traitを使ってこれを明示的に指定してみる
```scala=
trait Parsers[ParseError, Parser[+_]] {	

  def run[A](p: Parser[A])(input: String): Either[ParseError,A]
  def char(c: Char): Parser[Char]	
}
```
- 1行目：Parserは型パラメータであり、それ自体が共変の型コンストラクタ(型構築子)
- 4行目:ここでParser型コンストラクタがCharに適用される
- `Parser[+_]`という型引数は、これ自体が型コンストラクタである型パラメータに対するScala構文(あとの章で説明)
※第10章 p.225で出てきます


ParseErrorを型引数にすると、ParsersインターフェイスがParseErrorのあらゆる表現に対応するようになる。

Parser[+_]を型パラメータにすると、ParsersインターフェイスがParserのあらゆる表現に対応するようになる。


アンダースコア(_)は、Parserが何であれ、Parser[Char]のように結果の型を表す型引数を1つ期待することを意味します。


満たさなければならない明白な法則
任意のCharであるcに対して、以下の式が成り立つ
```scala=
run(char(c))(c.toString) == Right(c)
```


'a'の1文字は認識できるとして、文字列"abracadabra"を認識したい場合
```scala=
def string(s: String): Parser[String]
```
この関数でも満たさなければならない法則
任意のStringであるsに対して、以下の式が成り立つ

```scala=
run(string(s))(s) == Right(s)
```

"abra"もしくは"cadabra"のどちらかの文字列を認識したい場合はどうするのか。
具体的なコンビネータを追加する手段がある

```scala=
def orString(s1: String, s2: String): Parser[String]
```

しかし、2つのパーサーから選択する方が結果型に関係なく、より汎用的であるため、多相(polymorphic)にする
```scala=
def or[A](s1: Parser[A], s2: Parser[A]): Parser[A]
```

下記の `or(string("abra"),string("cadabra"))` は、どちらかのstringパーサーが成功した場合に成功するはず
```scala=
run(or(string("abra"), string("cadabra")))("abra") == Right("abra")
run(or(string("abra"), string("cadabra")))("cadabra") == Right("cadabra")

```

第7章で行ったように、implicitを使ってorコンビネータにs1 | s2や、s1 or s2のような便利な中置構文を割り当てることができます。
### リスト 9-1　パーサーへの中置構文の追加
```scala=
trait Parsers[ParseError, Parser[+_]] { self =>	
  ...
  def or[A](s1: Parser[A], s2: Parser[A]): Parser[A]
  implicit def string(s: String): Parser[String]
  implicit def operators[A](p: Parser[A]) = ParserOps[A](p)
  implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]):
    ParserOps[String] = ParserOps(f(a))

  case class ParserOps[A](p: Parser[A]) {
    def |[B>:A](p2: Parser[B]): Parser[B] = self.or(p,p2)	
    def or[B>:A](p2: Parser[B]): Parser[B] = self.or(p,p2)
  }
}

```
- 1行目:Parsersインスタンスをselfという名称で参照。ParserOpsで使用される
- 4行目:stringをimplicitの変換関数として定義
implicitのasStringParserも追加。
これら2つの関数によってStringがParserに自動的に昇格されるため、Parser[String]への変換が可能な任意の型の中置演算子が得られる。
したがって、val P: Parsersが定義されているとすれば、import P._を使って"abra" | "cadabra" のような式を記述することでパーサーを作成できる。

ここでは主要な定義をParsersに直接配置し、それらの定義にParsersOpsからデリゲートするというルール

本章ではこれ以降 or(a,b) という意味で a | b 構文を使用

- 11行目: selfを使ってtraitのorメソッドを明示的かつ明確に参照。


ここまでで様々な文字列を認識できるようになったが、繰り返しを指定する方法がない。
たとえば、"abra" | "cadabra"パーサーの3つの繰り返しをどのようにして認識するのか。
※ 前章でも同じような関数を記述した(p.158)
```scala=
def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]]
```

ここは正誤表を見てください
```scala=
run(listOfN(3, "ab" | "cad"))("ababcad") == Right(List("ab", "ab", "cad"))
run(listOfN(3, "ab" | "cad"))("cadabab") == Right(List("cad","ab", "ab"))
run(listOfN(3, "ab" | "cad"))("ababab") == Right(List("ab", "ab", "ab"))
```

ここまでで必要なコンビネータが揃ったが、代数を最小限のプリミティブに絞り込むことができておらず、より汎用的な法則について語られていない。

:::info
##### プリミティブとは
[Wikipediaの「プリミティブ型」](https://ja.wikipedia.org/wiki/%E3%83%97%E3%83%AA%E3%83%9F%E3%83%86%E3%82%A3%E3%83%96%E5%9E%8B)
プリミティブ型（primitive data type、プリミティブデータ型）は、データ型の1分類である。理論計算機科学的に代数的データ型によって考えれば「そのデータ型の定義の中に部分として他の型を含まないような型」がプリミティブ型であるが、Javaにおいて型が「primitive types（プリミティブ型）とreference type（参照型）」に二分されることにひきずられたと思われる2分法などが信じられていることも多い。

この記事におけるここから先の説明は、最初に説明した理論的な分類に従ったものではなく、よく信じられているらしい「プログラミング言語によって提供されるデータ型であり基本的な要素である。対する語は複合型という。言語やその実装に依存して、プリミティブ型がコンピュータメモリ上のオブジェクトと一対一対応のときもあれば、そうでないときもある。組み込み型もしくは基本型とも呼ぶ。」という解釈に従ったものである。
:::


Parserのより単純なユースケース

##### 'a'の文字を0個以上認識するParser[Int]
結果として得られる値は、検出された'a'の文字数
"aa"が与えられた場合、パーサーは2を生成
"b123"が与えられた場合、パーサーは0を生成

##### 'a'の文字を1個以上認識するParser[Int]
結果として得られる値は、検出された'a'の文字数
(これを上記の'a'の文字を0個以上認識するParserに基づいて定義)
'a'で始まってない文字列が与えられると失敗するはず
失敗した場合、"Expected one or more 'a'"のような明示的なメッセージをAPIに提供されることは可能か

##### 0個以上の'a'に続いて1個以上の'b'を認識するパーサー
結果として得られる値は、検出された文字数のペア
(これを上記の'a'の文字を0個以上認識するParserに基づいて定義)
"bbb"が与えられた場合は(0,3)、"aaaab"が与えられた場合は(4,1)が生成されます。

##### さらに
- 0個以上の'a'からなるシーケンスを解析しようとしていて、検出された文字の数にのみ関心があるとしたら、長さを取り出して削除するためだけに(たとえば)List[Char]を構築するのは効率が悪い

- さまざまな形式の繰り返しは本書の代数のプリミティブでしょうか。もっと単純化することは可能でしょうか？

- 本章ではParseError型を追加しましたが、今のところ、ParseErrorのAPIに対する関数は選択していません。
本書の代数には、報告されるエラーの種類をプログラマに制御させる方法がありません。
パーサーから意味のあるエラーメッセージを受け取りたいという趣旨からすると、これは制限のように思えます。
これをどうにかできないか

- `a | b` は `b | a` と同じ意味か？
はい、と言いたい

- `a | (b | c)` は `(a | b) | c` と同じ意味か。はい、と答えた場合、それは代数のプリミティブな法則でしょうか。それとも何かもっと単純なものによって定義されるのでしょうか。

- 代数を定義するための一連の法則
法則は必ずしも完全なものでなくてもかまいません。
任意のParsers実装に対して有効であるべきものと想定される法則を書き出すだけ


:::info
代数的設計の利点
ライブラリの代数を最初に設計する際には、代数のデータ型はそれほど重要ではありません。
必要な法則と関数をサポートしている限り、表現を明らかにする必要すらありません。
ここでの目標は、型の内部表現ではなく、他の型との関係に基づいて型に意味を持たせることにあります。
型は一連の関数とそれらの法則によって定義されます。この見解は圏論(category theory)という数学の一分野と関連しています。
:::


## 9.2 代数の例

'a’の文字を0個以上の繰り返しを認識し、検出された文字の数を返すパーサー

しかし、実際には検出された文字のListを返してる

```scala=
def many[A](p: Parser[A]): Parser[List[A]]
```
必要なのは、要素の数を数えるParser[Int]
Parser[Int]を返すようにできないこともないが、それでは具体的すぎるので、リストの長さ以外にも関心を持つ場合があることは間違いありません。
今ではすっかり見慣れてしまったコンビネータmapを追加

```scala=
def map[A,B](a: Parser[A])(f: A => B): Parser[B]

```
パーサーを以下のように定義できる
```scala=
map(many(char('a')))(_.size)
　↓
map(Parser('a'))(_.size)
　↓
Parser(1)
```

ParserOpsにmapとmanyをメソッドとして追加し、同じ内容をもう少し便利な構文で記述できる
```scala=
val numA: Parser[Int] = char('a').many.map(_.size)
```

期待してるのは
```
run(numA)("aaa") が Right(3) を返す
run(numA)("b")   が Right(0) を返す
```
mapはParserが成功した場合、mapが結果の値を変換するのみ
ParやGenと同様に、mapにも `構造を維持する` ことが期待される

###### すっかり見慣れた法則
```scala=
map(p)(a => a) == p
```
前章で法則を実行可能にする方法を開発したライブラリ


#### リスト 9-2 Parser と map の結合

```scala=
import fpinscala.testing._

trait Parsers[ParseError, Parser[+_]]
  ...
  object Laws {
    def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
      forAll(in)(s => run(p1)(s)== run(p2)(s))

    def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop =
      equal(p, p.map(a => a))(in)
  }
}
```
後ほどParsersの実装が期待どおりに動くかテストする


map使えるようになったので、charをstringに基づいて実装
```scala=
def char(c: Char): Parser[Char] =
  string(c.toString) map (_.charAt(0))
```


同様に、別のコンビネータsucceedもstringとmapを使って定義できる
```scala=
def succeed[A](a: A): Parser[A] =
  string("") map (_ => a)
```


string("")は入力が空であったとしても常に成功するため、このパーサーは入力文字列に関係なく、常にaの値で成功。
このコンビネータの振る舞いは法則を使って指定可能
```scala=
run(succeed(a))(s) == Right(a)
```

### 9.2.1 スライスと空ではない繰り返し

さらに改善
長さを取り出して値を捨ててしまうだけのためにList[Char]を構築するのは効率が悪い。
入力文字列のどの部分を調べているのかを確認する目的でのみParserを実行できれば良い

```scala=
def slice[A](p: Parser[A]): Parser[String]
```
なぜ`slice`か
run(slice(('a'|'b').many))("aabb")の結果はRight("aabb")


"aabbcc"の場合は？
manyの実装がなされてないため不明だが、
filerのように"aabb"になるのでは？
```scala=
def many[A](p: Parser[A]): Parser[List[A]]
```

- sliceを定義すると下記のように記述できるようになる
char('a').many.slice.map(_.size)
- sliceのエイリアスをParserOps(さっき出てきたcase class)に追加する前提
- 上記の `_size` 関数は、Listのsizeメソッドではなく、Stringのsizeメソッド 

※ この時点ではまだ実装が存在していない
`p.many.map(_.size)` は中間リストを作成するとしても
`slice(p.many).map(_.size)` は中間リストを作成しない

意味わからん、たぶん下記の違い
- List[a].sizeか
- Parser[String].sizeか


sliceはパーサーの内部表現にアクセスできなければならない

文字'a'を1つ以上認識するためのコンビネータ
```scala=
def many1[A](p: Parser[A]): Parser[List[A]]
```
※　前述のmanyの定義と同じ
many1はプリミティブではないはずだが、manyをベースとして定義できるはず

 `many1(p)` は、pの後に `many(p)` が続くだけ

1つ目のパーサーの後に続くもの
```scala=
def product[A,B](p: Parser[A], p2: Parser[B]): Parser[(A,B)]
```
`**` と `product` は、ParserOpsのメソッドとして追加可能
`a ** b` と `a product b` は、どちらも `product(a,b)`にデリゲートする。


#### EXERCISE 9.1 

productを使ってコンビネータmap2を実装し、manyを使ってmany1を実装せよ。
```scala=
def map2[A,B,C](p: Parser[A], p2: Parser[B])(f: (A,B) => C): Parser[C]
```

##### answer
```scala=
def map[A,B](a: Parser[A])(f: A => B): Parser[B]

def product[A,B](p: Parser[A], p2: Parser[B]): Parser[(A,B)]

def map2[A,B,C](p: Parser[A], p2: Parser[B])(f: (A,B) => C): Parser[C] =
    map(product(p, p2))(f.tupled)

def many1[A](p: Parser[A]): Parser[List[A]] = 
  map2(p, many(p))(_ :: _)
```


many1を使って、0個以上の'a'に続いて1個以上の'b'を解析するためのパーサーを以下のように実装できます。

```scala=
char('a').many.slice.map(_.size) ** char('b').many1.slice.map(_.size)
```


#### EXERCISE 9.2
productの振る舞いを定義する法則を考えだせ

##### answer
```scala=
  (a ** b) ** c
  a ** (b ** c)
```
唯一の違いは、ペアのネスト方法です。 `（a ** b）** c`パーサーは`（（A、B）、C） `を返しますが、` a **（b ** c） `は`（A、（B、C ）） `。これらのネストされたタプルをフラットな3タプルに変換する関数 `unbiasL`と` unbiasR`を定義できます。
```scala=
  def unbiasL[A,B,C](p: ((A,B), C)): (A,B,C) = (p._1._1, p._1._2, p._2)
  def unbiasR[A,B,C](p: (A, (B,C))): (A,B,C) = (p._1, p._2._1, p._2._2)
```
```scala=
(a ** b) ** c map (unbiasL) == a ** (b ** c) map (unbiasR)
```

###### トレース
`**` はproductの中置
```scala=

def product[A,B](p: Parser[A], p2: Parser[B]): Parser[(A,B)]
//左右同時に
(a ** b)      ** c map (unbiasL) == a ** (b ** c)      map (unbiasR)
Parser((A,B)) ** c map (unbiasL) == a ** Parser((B,C)) map (unbiasR)
Parser(((A,B),C)   map (unbiasL) == Parser((A,(B,C))   map (unbiasR)
Parser(((A,B),C)   map (unbiasL) == Parser((A,(B,C))   map (unbiasR)
//1行が長いので片辺づつ
//左辺
Parser(((A,B),C)   map (p._1._1, p._1._2, p._2) == Parser((A,(B,C))   map (unbiasR)
//右辺
Parser(p._1._1, p._1._2, p._2) == Parser((A,(B,C))   map (p._1, p._2._1, p._2._2)

Parser(p._1._1, p._1._2, p._2) == Parser(p._1, p._2._1, p._2._2)
```

##### 10月23日　ここまで
----- 

###### anseweより
両側に明らかな全単射が存在する場合は、単に `〜=`を使用することがあります
[全単射のwiki](https://ja.wikipedia.org/wiki/%E5%85%A8%E5%8D%98%E5%B0%84)
```scala=
  (a ** b) ** c ~= a ** (b ** c)
```
mapのことを写像という(単射ではない)

`map`と` product`にも興味深い関係があります。振る舞いに影響を与えることなく、2つのパーサーの積を取る前でも後でも `map`できます

```scala=
  a.map(f) ** b.map(g) == (a ** b) map { case (a,b) => (f(a), g(b)) }
```
たとえば、「a」と「b」の両方が「Parser [String]」であり、「f」と「g」の両方が文字列の長さを計算した場合、「a」の結果をマッピングしてもかまいません`その長さを計算するか、または製品の後にそれを行うかどうか。




#### EXERCISE 9.3 
or,map2,succeedをベースとしてmanyを定義できるか

manyは9-2の冒頭に出てきた 
`’a’の文字を0個以上の繰り返しを認識し、検出された文字の数を返すパーサー`
`def many[A](p: Parser[A]): Parser[List[A]]`

`succeedはリスト9-2に出てきた`
```scala=
  def succeed[A](a: A): Parser[A] =
    string("") map (_ => a)
```
###### ※stringは、リスト9-1にでてきた
`implicit def string(s: String): Parser[String]`


##### answer
```scala=
  def many[A](p: Parser[A]): Parser[List[A]] =
    map2(p, many(p))(_ :: _) or succeed(List())
```

:::info
scala> 3::List(4)
res11: List[Int] = List(3, 4)

scala> 3::4::List()
res7: List[Int] = List(3, 4)

scala> 3::4::List().empty
res8: List[Int] = List(3, 4)

scala> 3::4::Nil
res9: List[Int] = List(3, 4)
:::


#### EXERCISE 9.4 
map2とsucceedを使って先のlistOfNコンビネータを実装せよ

`listOfNは、リスト9-1で出てきたNの繰り返しを記述するためのコンビネータ`
```scala=
def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]]
```

`map2はEXERCISE 9.1に出てきた`
```scala=
def map2[A,B,C](p: Parser[A], p2: Parser[B])(f: (A,B) => C): Parser[C] =
    map(product(p, p2))(f.tupled)
```

##### 9.4 answer
```scala=
def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]] =
  if (n <= 0) succeed(List())
  else map2(p, listOfN(n-1, p))(_ :: _)
```


```
// トレースしたかったけど、listOfNの実装が無いからトレースできない
listOfN(3, parser("abc"))
map2(p: Parser("abc"), listOfN(2, p： Parser("abc")))(_ :: _)
map2(p: Parser("abc"), listOfN(1, p： Parser("abc")))(_ :: _)
map2(p: Parser("abc"), listOfN(0, p： Parser("abc")))(_ :: _)
Parser(List("a","b","c"))
```

or,map2,succeedをベースとしてmanyを実装すると以下のようになる。
manyは、`’a’の文字を0個以上の繰り返しを認識し、検出された文字の数を返すパーサー`

--------

manyの実装に入る
```scala=
// 復習
def succeed[A](a: A): Parser[A] =
  string("") map (_ => a)

def many[A](p: Parser[A]): Parser[List[A]] =
  map2(p, many(p))(_ :: _) or succeed(List())
```

上記manyの実装にも問題がある。
manyがmap2の第2引数として再帰的に呼び出され、map2が第2引数の評価において`正格`であることが問題


```scala=
many(p)
map2(p, many(p))(_ :: _)
map2(p, map2(p, many(p))(_ :: _))(_ :: _)
map2(p, map2(p, map2(p, many(p))(_ :: _))(_ :: _))(_ :: _)
...
```

map2の呼び出しでは常にその第2引数が評価されるため、many関数はいつまでも終了しない
※ `(_ :: _))(_ :: _))(_ :: List())`って状態にいつまでもならない

これはproductとmap2の第2引数に対して非正格にする必要があることを意味する

```scala=
def product[A,B](p: Parser[A], p2: => Parser[B]): Parser[(A,B)]

def map2[A,B,C](p:Parser[A], p2: =>Parser[B])(f:(A,B) => C): Parser[C] =
  product(p, p2) map (f.tupled)
```

#### EXERCISE 9.5 
第7章で示したように、非正格に別のコンビネータで対処することも可能。
ここでもそれを試して既存のコンビネータに必要な変更を追加せよ
このアプローチをどう思うか

※　７章は「純粋関数型の並列処理」

##### ansewer 9.5
```scala=
def wrap[A](p: => Parser[A]): Parser[A]

def many[A](p: Parser[A]): Parser[List[A]] =
    map2(p, wrap(many(p)))(_ :: _) or succeed(List())

```

並列処理の章では、対応するシリアル(直列、逐次)計算と同じくらい多くの時間とスペースを必要とする `Par`オブジェクトを避けることに特に興味があり、遅延コンビネータはこれをより慎重に制御できます。ここでは、これはそれほど重要なことではなく、「map2」を呼び出すたびに「wrap」を呼び出す必要性を検討しません。

----

productについては1つ目のParserが失敗した場合は2つ目のParserを調べることすらしないため、概念的には第2引数についても非正格になるはず
`同じ種類のものを連続して解析するための効果的なコンビネータ`ができた。
そもそもコンビネータが非正格であるべきかどうか
orについても考える

```scala=
def or[A](p1: Parser[A], p2: Parser[A]): Parser[A]
```

orが左結合であると想定すると、入力でp1を試し、p1に失敗した場合にのみp2を試すことになる
調査されるかどうかもあやしい第2引数は非正格にすべき

```scala=
def or[A](p1: Parser[A], p2: => Parser[A]): Parser[A]
```

p.193
## 9.3 文脈依存への対処 

これまでに作成したプリミティブ一覧

- `string(s)` :Stringを1つ認識して返す

- `slice(p)` :成功した場合はpが調べた部分の入力を返す

- `succeed(a)` :常にaの値で成功する

- `map(p)(f)` :成功した場合はpの結果に関数fを適用する

- `product(p1, p2)` :2つのパーサを逐次化してp1を実行した後にp2を実行して、両方が成功した場合にそれらの結果をペアで返す

- `or(p1, p2)` :2つのパーサのどちらかを選択し、最初にp1を試した後、p1が失敗した場合にp2を試す


これらのプリミティブを利用することでJSONを含めあらゆる文脈自由構文を解析するのにこれらのプリミティブで十分であるとしたら？

まだ表現できないものは何か？
たとえば'4'のような1桁の数字に続いて、文字'a'をその数だけ解析したいとする

"0", "1a", "2aa", "4aaa"など

2つ目のパーサの選択が1つ目のパーサの結果に`依存`する
つまり、2つ目のパーサがそのコンテキストに依存するため、productでは表現できません。逐次だから。

1つ目のパーサを実行した後、1つ目のパーサの結果から取り出した数値を使ってlistOfNを実行したくとも、これをproductでは表現できない

これまでの章でも出た、表現力の制限に遭遇時のflatMap
ここでも、このプリミティブを追加する。

ParserOpsエイリアスも追加して、for内包表記を使ってパーサーを記述できるようにします。


```scala=
def flatMap[A,B](p: Parser[A])(f: A => Parser[B]): Parser[B]
```

#### EXERCISE 9.6 
flatMapと他のコンビネータを使って、先ほど表現できなかった文脈依存パーサを記述せよ。
数字の解析には、正規表現をParserへ昇格させる新しいプリミティブregexを使用できる
Scalaでは文字列sをRegexオブジェクトに処理させるにはs.rを"[a-zA-Z][a-zA-Z0-9_]*".rのように使用すれば良い

```scala=
implicit def regex(x: Regex): Parser[String]
```

##### hint
数字の文字列 `s`を指定すると、` s.toInt`を使用してそれを `Int`に変換できます。

##### answer 9.6
```scala=
for {
  digit <- "[0-9]+".r //Regexがimplicit def regexでStringになる
  val n = digit.toInt
  _ <- listOfN(n, char('a'))
} yield n
```

- このパーサーに、読み取られた「"a"」文字の数を返すだけです。 for-comprehension内で通常の値を宣言できることに注意してください。

- toIntによってスローされた例外を実際にキャッチし、解析失敗に変換する必要があります。

#### EXERCISE 9.7 
flatMapをベースとしてproductとmap2を実装せよ

productは、EXERCISE 9.4の解説に出てきた
```scala=
def product[A,B](p: Parser[A], p2: => Parser[B]): Parser[(A,B)]
```

##### hint
`flatMap` と `succeed`を使う

##### answer 9.7
```scala=
def product[A,B](p: Parser[A], p2: => Parser[B]): Parser[(A,B)] = 
  flatMap(p)(a => map(p2)(b => (a,b)))

def map2[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] = 
  for { a <- p; b <- p2 } yield f(a,b)
```

#### EXERCISE 9.8 
mapはもはやプリミティブではない。flatMapや他のコンビネータをベースとして表現せよ

##### answer 9.8
```scala=
def map[A,B](p: Parser[A])(f: A => B): Parser[B] =
  p.flatMap(a => succeed(f(a)))
```

##### 再掲
```scala=
def flatMap[A,B](p: Parser[A])(f: A => Parser[B]): Parser[B]

def succeed[A](a: A): Parser[A] =
  string("") map (_ => a)
```

---

この時点において、プリミティブは下記の6つ
- string
- regex
- slice
- succeed
- or
- flatMap

あまり汎用的ではないmapとproductの代わりにflatMapを使用することで、JSONのような恣意的な文脈自由構文だけではなく、C++やREPLのようなきわめて複雑なものを含む文脈依存文法の解析も可能になります。


## 9.4 JSONパーサーの作成

JSONパーサの実装する
以下は後回し
- 代数の実装
- エラーを報告するのに適したコンビネータ
- このJSONパーサーに各パーサーの内部表現を認識させる

JSONパーサの何らかの結果型に対して、以下のような関数を記述します。JSONの書式とパーサーの結果型については、後ほど説明します。

以下のような関数を記述してゆく
```scala=
def jsonParser[Err,Parser[+_]](P: Parsers[Err,Parser]): Parser[JSON] = {
  import P._	
  val spaces = char(' ').many.slice
  ...
}
```

関数型プログラミングでは、代数を定義し、具体的な実装がないままの状態でその表現力を調べるのが一般的

### 9.4.1 JSONの書式


##### JSONドキュメント例
```json=
{
  "Company name" : "Microsoft Corporation",
  "Ticker" : "MSFT",
  "Active" : true,
  "Price" : 30.66,
  "Shares outstanding" : 8.38e9,
  "Related companies" :
    [ "HPQ", "IBM", "YHOO", "DELL", "GOOG" ]
}
```
- 値はさまざまな型を持つ
- JSONオブジェクトは、カンマ区切りのキーと値のペアを並べてもの
- 中括弧で囲まれています
- キーは"Ticker"や、"Price"のような文字列
- 値は別のオブジェクトか配列、もしくはリテラル

JSONドキュメントを表現するデータ型
#### リスト 9-3 parsing/JSON.scala
```scala=
trait JSON
object JSON {
  case object JNull extends JSON
  case class JNumber(get: Double) extends JSON
  case class JString(get: String) extends JSON
  case class JBool(get: Boolean) extends JSON
  case class JArray(get: IndexedSeq[JSON]) extends JSON
  case class JObject(get: Map[String, JSON]) extends JSON
}
```

### 9.4.2 JSONパーサー

- `string(s)`: Stringを1つ認識して返す
- `regex(s)`: 正規表現sを認識する
- `slice(p)`: 成功した場合はpが調べた部分の入力を返す
- `succeed(a)`: 常にaの値で成功する
- `flatMap(p)(f)`:パーサーを実行し、その結果を使って、次に実行するパーサーを順番に選択する。
- or(p1,p2):2つのパーサのどちらかを選択し、最初にp1を試したあと、p1が失敗した場合にp2を試す

p.198
#### EXERCISE 9.9 

これまでに定義したプリミティブを使って
Parser[JSON]を一から作成せよ

##### hint
- 文法のトークンについては、多くの場合、末尾の空白をスキップして、文法のあらゆる場所で空白を処理する必要がないようにすることをお勧めします。このためにコンビネーターを導入してみてください。 
- `**`でパーサーをシーケンスする場合、シーケンス内のパーサーの1つを無視するのが一般的であり、おそらくこのためのコンビネーターを導入する必要があります。

##### answer 9.9

```scala=
trait JSON

object JSON {
  case object JNull extends JSON
  case class JNumber(get: Double) extends JSON
  case class JString(get: String) extends JSON
  case class JBool(get: Boolean) extends JSON
  case class JArray(get: IndexedSeq[JSON]) extends JSON
  case class JObject(get: Map[String, JSON]) extends JSON

  def jsonParser[Parser[+_]](P: Parsers[Parser]): Parser[JSON] = {
    // 文字列の暗黙的な変換を非表示にし、
    // 代わりに文字列をトークンに昇格させます。
    // これはどこにでもトークンを書くよりも少しいいです
    import P.{string => _, _}
    implicit def tok(s: String) = token(P.string(s))

    def array = surround("[","]")(
      value sep "," map (vs => JArray(vs.toIndexedSeq))) scope "array"
    def obj = surround("{","}")(
      keyval sep "," map (kvs => JObject(kvs.toMap))) scope "object"
    def keyval = escapedQuoted ** (":" *> value)
    def lit = scope("literal") {
      "null".as(JNull) |
      double.map(JNumber(_)) |
      escapedQuoted.map(JString(_)) |
      "true".as(JBool(true)) |
      "false".as(JBool(false))
    }
    def value: Parser[JSON] = lit | obj | array
    root(whitespace *> (obj | array))
  }
}
```

## 9.5 エラー報告

ここまでエラー報告についてまったく説明しなかったが、
パーサーが想定外の入力を受け取ったときの対処法についても決定できるようにしておきたい

これまで定義してきたコンビネータには、失敗した場合に報告すべきエラーメッセージやParseErrorに追加すべき情報については何も規定していない


#### EXERCISE 9.10 

Parserから報告されるエラーを表現するために便利なコンビネータがまだ見つかっていない場合は、ここでそれらを見つけ出せ

コンビネータごとにその振る舞いを指定する法則を定義すること

これは自由回答方式の設計タスクであり、以下にガイドラインとなる質問をまとめておく


- "abra".**(" ".many).**("cadabra")というパーサーがあると仮定した場合、入力"abra cAdabra"に対してどのようなエラーを報告すればよいか(大文字の'A'に注意)

Expected 'a'のようなものだけで良いか、それともExpected "cadabra"がよいか

"Magic word incorrect, try again!"のような別のエラーメッセージを選択したい場合はどうすれば良いか


- a or b であると仮定した場合、aが入力で失敗したら必ずbを実行するのか、それとも、そうしたくない状況があるか。そうした状況がある場合、orが2つ目のパーサーを考慮すべきであることをプログラマが指定できる追加のコンビネータを思いつけるか。

-　エラーの場所を報告するにはどのように対処したいか

- a or bであると仮定した場合、aとbの両方で入力が失敗したら両方のエラーを報告するのか、それとも、2つのエラーのどちらを報告するかをプログラマが指定できるようにするのか

ansewr無し


#### 9.5.1 設計の例

エラー報告コンビネータの一つの設計候補

```scala=
def label[A](msg: String)(p: Parser[A]): Parser[A]
```

labelの目的は、pが失敗した場合にそのParseErrorにmsgを組み込むことです。

type ParseError = Stringと、返されるParseErrorがlabelと等しいことを想定すれば良い

解析エラーが発生した部位も明らかにしたい

これを代数に追加してみましょう


```scala=
case class Location(input: String, offset: Int = 0) {
 lazy val line = input.slice(0,offset+1).count(_ == '\n') + 1
  lazy val col = input.slice(0,offset+1).lastIndexOf('\n') match {
    case -1 => offset + 1
    case lineStart => offset - lineStart
  }
}

def errorLocation(e: ParseError): Location
def errorMessage(e: ParseError): String
```

完全な入力、この入力へのオフセット、そして完全な入力とオフセットから遅延計算できる行番号と列番号を含んだLocationの具体的な表現を選択

[オフセット](https://it-words.jp/w/E382AAE38395E382BBE38383E38388.html)
オフセット(offset)とは、基準となるある点からの相対的な位置のことである。offsetは英語で「差し引き計算する」という意味で、そこから必要なデータの位置を基準点からの差(距離)で表した値をオフセットと呼ぶようになった。

これによりlabelに期待できるものをより正確に表現できるようになる


##### 11/6 ここまで進んだ

`9.3 文脈依存への対処` で`文脈自由文法`、`文脈依存文法`という用語が出てきたがこれらはチョムスキー階層、4タイプのうちの2つの文法に当たる

| 句構造文法階層 | 文法 | 言語 |オートマトン|
|---------|----|----|----------------|
| タイプ-0 | -- |帰納的可算| チューリングマシン |
| タイプ-1 | 文脈依存 |文脈依存 |線形拘束オートマトン|
| タイプ-2 | 文脈自由 |文脈自由 |プッシュダウンオートマトン|
| タイプ-3 | 正規 |正規 |線形拘束オートマトン|

0が最も強く、3が最も弱い

Oさんの説明では、flatMapは図でのタイプ1レベル

[チョムスキー階層](https://ja.wikipedia.org/wiki/%E3%83%81%E3%83%A7%E3%83%A0%E3%82%B9%E3%82%AD%E3%83%BC%E9%9A%8E%E5%B1%A4)

[計算理論](http://ocw.kyushu-u.ac.jp/menu/faculty/05/3.html)


上記実装により
Left(e)で失敗した場合、errorMessage(e)はlabelによって設定されたメッセージと等しくなります。

これをPropを使って指定する

```scala=
def labelLaw[A](p: Parser[A], inputs: SGen[String]): Prop =
  forAll(inputs ** Gen.string) { case (input, msg) =>
    run(label(msg)(p))(input) match {
      case Left(e) => errorMessage(e) == msg
      case _ => true
    }
  }
```
##### 復習
** はproductの中置
```scala=
def product[A,B](p: Parser[A], p2: Parser[B]): Parser[(A,B)]

def label[A](msg: String)(p: Parser[A]): Parser[A]

//次で出てくるrun
def run[A](p: Parser[A])(input: String): Either[ParseError,A]
```


エラーが発生した部位が設定されるようにしたいが、その意図するところも曖昧

a or bであり、両方のパーサーが入力で失敗した場合、どの場所が報告され、どのラベルが使用されるのでしょうか
(文章意味不明だが、aかbのどちらかが成功すれば、エラーメッセージが無いということか)


#### 9.5.2 入れ子のエラー (p.201)

labelコンビネータは、エラー報告のニーズを全て満たすのに十分とは言えない。例を見てみる。

```scala=
val p = label("first magic word")("abra") **
        " ".many **	
        label("second magic word")("cadabra")
```
- 2行目でホワイトスペースをスキップしている


`run(p)("abra cAdabra")`からどのようなParseErrorを返せば良いか
Aが大文字
小文字のaが期待されている部分に大文字のA
- このエラーには「部分」の情報があるため、それを報告できれば便利
- もう少し知っていると役に立ちそうな情報はParserで直接発生した"second magic word"というラベルのエラー
- 理想的には"socond magic word"の解析中に想定外の大文字の'A'が検出されたことをエラーメッセージで知らせるべき。"parsing magic spell"などの情報が提供されれば、それも参考になるはず

つまり、1階層のエラー報告で常に十分と考えるのは間違い
ラベルを`入れ子`にする方法を定義する

```scala=
def scope[A](msg: String)(p: Parser[A]): Parser[A]
```

labelとは異なり、scopeはpに関連付けられている1つ以上のラベルを削除するのではなく、pが失敗した場合に補足情報を追加します。

まずParseErrorから情報を取り出す関数を修正
Location/Stringメッセージを1つだけ取得するのではなく、List[(Location, String)]を取得すべきです。

```scala=
case class ParseError(stack: List[(Location, String)])
```

これは、Parserが失敗時に実行していた処理を示すエラーメッセージのスタックです。
※スタックなのでたぶんLIFO(LastInFirstOut)

これでscopeの動作を指定できるようになる
`run(p)(s)` がLeft(e1)の場合、`run(scope(msg)(p))`は、Left(e2)

`e2.stack.head`は、msg
`e2.stack.tail`は、e1



後ほど、ParseError値の生成と操作をより便利にし、それらをユーザーが使いやすいように書式設定するためのヘルパー関数を作成します。

今のところは、エラーを報告するときに関連する情報が全て含まれるようにしたいだけ

Parsersから抽象的な型パラメータを削除してみる

```scala=
//元は、trait Parsers[ParseError, Parser[+_]] {

trait Parsers[Parser[+_]] {
  def run[A](p: Parser[A])(input: String): Either[ParseError, A]
  ...
}
```

階層式のエラーを生成することにしたため、そのために必要な情報を全てParsersの実装に与えることになる

Parsersライブラリのユーザである私たちは、解析エラーが発生したときにParsersの実装で使用できるlabelとscopeの呼び出しを抜かりなく文法に散りばめるはずです。

なお、Parsersの実装では、ParseErrorの能力を全て利用するのではなく、エラーの原因と場所についての基本情報のみを残したとしてもまったく妥当


#### 9.5.3 分岐とバックトラックの制御 p.201

エラー処理についてまだ1つ
or コンビネータ中でエラーが発生した場合に、1つ以上のエラーのうち、どれを報告するのかを判断する方法が必要

これに関してグルーバルな規約しかないという状況は避けたいところであり、この選択をプログラマが制御できるようにしたいこともあります。

```scala=
//def scope[A](msg: String)(p: Parser[A]): Parser[A]

val spaces = " ".many
val p1 = scope("magic spell") {
  "abra" ** spaces ** "cadabra"
}
val p2 = scope("gibberish") {
  "abba" ** spaces ** "babba"
}
val p = p1 or p2
```

`run(p)("abra cAdabra")`からどのようなParseErrorが返されるようにしたいか

orのどちらの分岐でも入力エラーが生成される
p2の"gibberish"というラベルが指定されたパーサーは
最初の単語が"abba"であることを想定しているため、エラーを報告します。

"magic spell"というラベルのパーサーは、"cAdabra"に想定外の大文字が含まれているため、エラーを報告します。


今回は、"magic spell"を報告する

"abra"という単語を正常に解析した後は、orの"magic spell"分岐にコミットする

つまり、解析エラーが発生した場合は、orの次の分岐を調べません。

どうやら特定の解析分岐にコミットするケースをプログラマに指定されるためのプリミティブが必要

入力でp1を実行後、p1で失敗した場合は、同じ入力でp2を実行

つまり、
p1実行がコミットされてない状態で失敗した場合は、同じ入力でp2を実行し、それ以外の場合は失敗を報告するように変更できる

一般的な解決の方法の一つとして、結果を生成するためにパーサーが文字を1つでも調べる場合は、それらすべてのパーサーをデフォルトでコミットさせる

解析へのコミットを先送りさせるattemptコンビネータを追加

```scala=
def attempt[A](p: Parser[A]): Parser[A]
```
※attempt:試みる

以下の要件を満たすはず
```scala=
attempt(p flatMap (_ => fail)) or p2 == p2
```

この`fail` は常に失敗するパーサー

望むならこれをプリミティブコンビネータとして定義することも可能

つまりpが入力を調べてる途中で失敗したとしても,
attemptはその解析へのコミットを取り消し、p2を実行できるようにします。


文法が不明確なため、複数のトークンを調査しなければならないケースも考えられる

※ここでの「token」は、おそらく「文字列」

```scala=
(attempt("abra" ** spaces ** "abra") ** "cadabra") or (
 "abra" ** spaces "cadabra!")
```

このパーサーが"abra cadabra!"で実行されるとして、
最初の"abra"を解析した後、1つ目の分岐である別の"abra"と2つ目の分岐である"cadabra!"のどちらを期待すれば良いのかが不明。

attemptに`"abra" ** spaces ** "abra"`を含めることによって、2つ目の"abra"の解析が終わるまで2つ目の分岐を調査の対象にできる。


そして、解析が終わった時点でその分岐にコミットする

#### EXERCISE 9.11 
or で連結されている(1つ以上の)エラーのうちどれを報告するかをプログラマが指定できるようにする上で役立つプリミティブを他に思いつけるか

##### hint 
2つのオプションがあります。`or`チェーン内の最新のエラーを返すか、入力文字列に最も遠く入った後に発生したエラーを返します。

##### answer

```scala=
//エラーが発生した場合、
//最も多くの文字を消費した後に発生したエラーを返します。
def furthest[A](p: Parser[A]): Parser[A]

//エラーが発生した場合、
//最後に発生したエラーを返します。
def latest[A](p: Parser[A]): Parser[A]
```
最も遠い↔︎最新の

ここに至ってもまだ代数を実装してないことに注目してください。
ここまでの情報を、定義されている法則を満たすような方法でどのように利用するのかを判断するのは、実装の役目です。


## 9.6 代数の実装

これまでは代数を肉付けしてParser[JSON]を定義してきました。
復習した後、試してみましょう

- `string(s)`: Stringを1つ認識して返す
- `regex(s)`: 正規表現sを認識する
- `slice(p)`: 成功した場合はpが調べた部分の入力を返す
- `label(e)(p)`:失敗した場合は割り当てられたメッセージをeと置き換える
- `scope(e)(p)`:失敗した場合はpから返されたエラースタックにeを追加する
- `flatMap(p)(f)`:パーサーを実行し、その結果を使って、次に実行するパーサーを順番に選択する。
- `attempt(p)`成功するまでpへのコミットを遅らせる
- `or(p1,p2)`:2つのパーサのどちらかを選択し、最初にp1を試したあと、p1が失敗した場合にp2を試す

#### EXERCISE 9.12 p.205
次からParserの表現を使ってParserインターフェースを実装するが、その前に自力で実装せよ。
ここまで設計してきた代数により表現として考えられるものに大きな制約が課される。
Parsersインターフェースを実装するために使用できる、純粋関数型の単純なParser表現を思いつけるはずだ。

```scala=
class MyParser[+A](...){...}

object MyParsers extends Parsers[MyParser]{
 // プリミティブの実装
}
```

MyParserは、パーサーを表現するために使用するデータ型と置き換える。

※Parsersの実装が完成した後、JSONパーサーを実行した場合に、スタックオーバーフローが発生するかもしれない。

#### 9.6.1 実装の例　p.206

Parserの最終的な表現に最初から取り組むのではなく、この代数のプリミティブを調べて、それぞれのプリミティブをサポートするのに必要な情報を推測することで、徐々に完成させていく。

まずstringコンビネータ
```scala=
def string(s: String): Parser[A]
```

関数runをサポートする必要あり
```scala=
def run[A](p: Parser[A])(input: String): Either[ParseError,A]
```

Parserがrun関数の実装であると想定できる
```scala=
type Parser[+A] = String => Either[ParseError,A]
```

これを使ってstringプリミティブを実装できます。
```scala=
def string(s: String): Parser[A] =
  (input: String) =>
    if (input.startsWith(s))
      Right(s)
    else
      Left(Location(input).toError("Expected: " + s))	
```
6行目：後ほど定義するtoErrorを使ってParseErrorを生成

else分岐でParseErrorを生成する必要があります。これらをそこで生成するのは少し都合が悪いため、Locationにヘルパー関数toErrorを追加しています。

```scala=
def toError(msg: String): ParseError =
  ParseError(List((this, msg)))
```

#### 9.6.2 パーサーの逐次化　p.207

次はパーサーの逐次化
残念ながら、"abra" ** "cadabra"のようなパーサーを表現するには、既存の表現では不十分です。
"abra"の解析が成功した場合は、それらの文字を消費されたものと見なして、残りの文字で"cadabra"パーサーを実行させる必要があります。
したがって、逐次化を可能にするには、消費された文字の数をParserに通知させる方法が必要です。

逐次化を可能にするには、消費された文字の数をParserに通知させる方法が必要。

```scala=
type Parser[+A] = Location => Result[A]	

trait Result[+A]
case class Success[+A](get: A, charsConsumed: Int) extends Result[A]	
case class Failure(get: ParseError) extends Result[Nothing]
```
1行目:パーサーが成功または失敗を示すResultを返すようになる
4行目:成功の場合は、パーサーによって消費された文字の数を返す

Eitherを使用するのではなく、Resultという新しい型を使用している。
成功した場合は、A型の値と、消費された入力文字の数を返します。
呼び出し元はこれを使ってLocationの状態を更新できます。
※(A, Location)を返せば、Locationに格納されているinputをParserが変更できるようになるが、それでは権限を与えすぎ

Resultは、Parserの本質に近づき始めています。
それは第6章(純粋関数型の状態：rngの章)で構築したような、失敗する可能性がある状態アクションの一種です。

成功した場合は、値に加えて、状態をどのように更新すべきかを制御するのに十分な情報が返されます。

Parserが単なる状態アクションであると考えれば、ここで入念に定義してきたコンビネータや法則を全てサポートする表現をどのように組み立てれば良いか見えてくる

各プリミティブが状態を表す型で何を追跡するか、(Locationだけでは不十分かもしれない)各コンビネータがこの状態を変換する方法に取り組めば良いわけです。


#### EXERCISE 9.13 p.207 

このParserの最初の表現に対するstring、regex、succeed、sliceを実装せよ。
sliceでは依然として削除するだけの値を生成しなければならないため、本来よりも非効率であることに注意。
これについては後ほど改めて取り組む。

##### answer
`parsing/instances/Reference.scala`には` Parsers`の完全なリファレンス実装がありますが、表現を改良しているので、それを見る前に読み続けることをお勧めします。


#### 9.6.3 パーサーのラベル付け p.208

プリミティブリストの次にscopeについて見て行く
失敗した場合はParseErrorスタックに新しいメッセージをプッシュしたいため、そのためのpushというヘルパー関数をParseErrorで定義する

```scala=
def push(loc: Location, msg: String): ParseError =
  copy(stack = (loc,msg) :: stack)
```
※ここのcopyはcase classのcopyメソッド

pushを使うことでscopeを実装

```scala=
def scope[A](msg: String)(p: Parser[A]): Parser[A] =
  s => p(s).mapError(_.push(s.loc,msg))	
```
失敗した場合は、msgをエラースタックにプッシュ
mapError関数はResultに定義し、失敗した場合に関数を適用

```scala=
def mapError(f: ParseError => ParseError): Result[A] = this match {
  case Failure(e) => Failure(f(e))
  case _ => this
}
```

スタックにプッシュするのは内側のパーサーから制御が戻った後なので、スタックの底には、解析の後の段階で発生した、より詳細なメッセージが含まれることになります。
たとえば、bの解析中に`scope(msg1)(a ** scope(msg)(b))`が失敗した場合、スタックの最初のエラーはmsg1になり、その後にaによって生成されたエラー、さらにmsg2、そして最後にbによって生成されたエラーが続きます。

labelも同じように実装できますが、エラースタックにプッシュするのではなく、すでにスタックに含まれているものを置き換えています。
これもmapErrorを使って記述できます。

```scala=
def label[A](msg: String)(p: Parser[A]): Parser[A] =
  s => p(s).mapError(_.label(msg))	
```
2行目でParseErrorのヘルパーメソッドlabelを呼び出している

ParseErrorに同じlabelという名前のヘルパー関数が追加されています。

設計上の決断として、labelが



ParseErrorに同じlabelという名前のヘルパー関数が追加されています。
設計上の決断として、labelがエラースタックを切り捨て、内側のスコープからのより詳細なメッセージを削除し、スタックの底にある最後のエラーだけを使用することに

```scala=
def label[A](s: String): ParseError =
  ParseError(latestLoc.map((_,s)).toList)

def latestLoc: Option[Location] =
  latest map (_._1)

def latest: Option[(Location,String)] =
  stack.lastOption	
```
※lastOptionは、スタックの最後の要素を取得する。スタックが空の場合はNone

#### EXERCISE 9.14 p.209 
stringの実装を見直し、scopeやlabelを使って、エラーが発生した場合に意味のあるメッセージを提供するように変更せよ。

##### hint
`string`は、解析された文字列全体だけでなく、障害の直接的な原因（一致しない文字）を報告することもできます。

##### answer

`parsing/instances/Reference.scala`を見る

```scala=
  def string(w: String): Parser[String] = {
    val msg = "'" + w + "'"
    s => {
      val i = firstNonmatchingIndex(s.loc.input, w, s.loc.offset)
      if (i == -1) // they matched
        Success(w, w.length)
      else
        Failure(s.loc.advanceBy(i).toError(msg), i != 0)
    }
  }
```

#### 9.6.4 フェイルオーバーとバックトラック

次はorとattemptです。orに期待される振る舞いは以下のとおり。
1つ目のパーサーを実行し、それがコミットされてない状態で失敗した場合、orは同じ入力で2つ目のパーサーを実行する。
先に述べたように文字を1つでも消費すれば解析はコミットされることになり、`attempt(p)`によってコミットされたpの失敗がコミットされていない失敗は変換されます。

ResultのFailureケースに情報をもう1つ追加すれば、ここで求めている振る舞いをサポートできます。
その情報とは、パーサーがコミットされた状態で失敗したかどうかを示すBoolean値です。

```scala=
case class Failure(get: ParseError,
                  isCommitted: Boolean) extends Result[Nothing]
```

attemptの実装では、発生した失敗のコミットを取り消すだけです。
これにはヘルパー関数uncommitを使用しますが、この関数はResultで定義できます。

```scala=
def attempt[A](p: Parser[A]): Parser[A] =
  s => p(s).uncommit

def uncommit: Result[A] = this match {
  case Failure(e,true) => Failure(e,false)
  case _ => this
}
```

これにより、orの実装では、2つ目のパーサーを実行する前にisCommitedフラグをチェックすればよいことになります。
パーサーx or yでは、xが成功すれば、全体が成功します。

xがコミットされた状態で失敗した場合は、途中で失敗したことになり、yの実行は省略されます。


```scala=
def or[A](x: Parser[A], y: => Parser[A]): Parser[A] =
  s => x(s) match {
    case Failure(e,false) => y(s)
    case r => r	
}
```
※コミットされた失敗または成功の場合はyの事項をスキップ

#### 9.6.5 文脈依存の解析

次のリストは最後のプリミティブであるflatMapです。
flatMapは、2つ目のパーサーの選択を1つ目のパーサーの結果に基づいて決定できるようにすることで、文脈依存のパーサーを実現します。
実装は単純で、2つ目のパーサーを呼び出す前にソースの位置を前進させます。
この場合、Locationのヘルパー関数advanceByを使用します。

1つ目のパーサーが文字を1つでも消費したら、ParseErrorのヘルパー関数addCommitを使って、2つ目のパーサーがコミットされるようにすること。


##### リスト 9-4 `parsing/instances/Reference.scala`
```scala=
def flatMap[A,B](f: Parser[A])(g: A => Parser[B]): Parser[B] =
  s => f(s) match {
    case Success(a,n) => g(a)(s.advanceBy(n))	
                         .addCommit(n != 0)	
                         .advanceSuccess(n)	
    case e@Failure(_,_) => e
  }
```
3行目: 2つ目のパーサーを呼び出す前にソースの位置を前進させる
4行目: 1つ目のパーサーが文字を1つでも消費した場合はコミット
5行目: 成功した場合は、fによってすでに消費された文字を考慮するために、消費された文字の数にnを足す

advanceByの実装は明白で、オフセットを増やすだけです。
```scala=
def advanceBy(n: Int): Location =
  copy(offset = offset+n)
```

同様に、ParseErrorで定義されるaddCommitも簡単です。
```scala=
def addCommit(isCommitted: Boolean): Result[A] = this match {
  case Failure(e,c) => Failure(e, c || isCommitted)
  case _=> this
}
```

最後に、advanceSuccessで成功した場合の結果の消費文字数を増やします。flatMapによって消費された文字の合計数を、パーサーfとgによって生成されたパーサーの消費文字数の合計にする必要があります。

そのためには、gの結果でadvanceSuccessを使用します。
```scala=
def advanceSuccess(n: Int): Result[A] = this match {
  case Success(a,m) => Success(a,n+m)
  case _ => this	
}
```
3行目: 不成功の場合は、結果をそのまま

#### EXERCISE 9.15 

Parserのこの表現を使って、runを含む残りのプリミティブを実装し、さまざまな入力でJSONパーサーを実行せよ。

※[1,2,3,・・・・10000]のような大きな入力ではスタックオーバーフローが発生する。この問題はmanyの特別な実装を用意することで、簡単で解決できる。
この実装は、生成されるリストの要素ごとにスタックフレームを使用しない。したがって、繰り返し実行されるコンビネータがmanyをベースとして定義されていれば、これで問題が解決する。
より汎用的な方法については、ダウンロードサンプルのanswersを参照

##### answer

`parsing/instances/Reference.scala`を見る

#### EXERCISE 9.16 

ParseErrorをユーザが利用するのに適した書式にする方法を考え出せ。
選択肢はいろいろあるが、エラーをStringとして表示する際、ユーザーが同じ場所に関連づけられてるラベルを結合する、またはグループ化したいと考えることが鍵となる。

```scala=
case class ParseError(stack: List[(Location,String)] = List()) {
  def push(loc: Location, msg: String): ParseError =
    copy(stack = (loc,msg) :: stack)

  def label[A](s: String): ParseError =
    ParseError(latestLoc.map((_,s)).toList)

  def latest: Option[(Location,String)] =
    stack.lastOption

  def latestLoc: Option[Location] =
    latest map (_._1)

  /**
  折りたたまれたエラースタックを表示
  -同じ場所にある隣接するスタック要素はすべて1行に結合されます。
  一番下のエラーについては、エラーの列を指すキャレット付きの
  完全な行を表示します。
  Example:

  1.1 file 'companies.json'; array
  5.1 object
  5.2 key-value
  5.10 ':'

  { "MSFT" ; 24,
           ^
  */
  override def toString =
    if (stack.isEmpty) "no error message"
    else {
      val collapsed = collapseStack(stack)
      val context =
        collapsed.lastOption.map("\n\n" + _._1.currentLine).getOrElse("") +
        collapsed.lastOption.map("\n" + _._1.columnCaret).getOrElse("")
      collapsed.map { case (loc,msg) => loc.line.toString + "." + loc.col + " " + msg }.mkString("\n") +
      context
    }

  /* 指定されたエラースタックメッセージの折りたたみバージョンを
   * 同じ場所に構築し、それらのメッセージをセミコロンで区切って
   * マージします 
  */
  def collapseStack(s: List[(Location,String)]): List[(Location,String)] =
    s.groupBy(_._1).
      mapValues(_.map(_._2).mkString("; ")).
      toList.sortBy(_._1.offset)

  def formatLoc(l: Location): String = l.line + "." + l.col
}
```

#### EXERCISE 9.17 

sliceコンビネータには効率化の余地がある。たとえば、many(char('a')).sliceには依然として削除されるだけのList[Char]を生成する。Parserの表現を修正してsliceを効率化する方法を思いつけるか。

##### hint
別の状態を「Location」に追加してみてください、「isSliced」。
単なる場所ではないので、 `Location`の名前を` ParseState`に変更することもできます！

##### answer
`instances/Sliceable.scala`を見る

##### 復習
`many(char('a')`は、9.2で出てきた記述

#### EXERCISE 9.18 

パーサーをorコンビネータで結合すると失われる情報がある。パーサーが2つとも失敗した場合は、2つ目のパーサーのエラーだけが保持される。

しかし、エラーメッセージを両方とも表示したい場合や、2つの分岐のうち、長くもちこたえたほうのエラーを選択したい場合があるかもしれない。ParseErrorの表現を変更し、パーサーのもう一方の分岐で発生したエラーを追跡できるようにせよ。

##### hint
属性 `otherFailures：List [ParseError]`を `ParseError`自体に追加できます。これは、パーサーの他のブランチで発生した解析エラーのリストになります。

##### answer
```scala=
// ここでスケッチをします。
// 基本的な考え方は、追加のフィールドを「ParseError」に
// 追加することです

case class ParseError(stack: List[(Location,String)] = List(),
                      otherFailures: List[ParseError] = List()) {

  def addFailure(e: ParseError): ParseError =
    this.copy(otherFailures = e :: this.otherFailures)
  ...
}

// 次に、「or」の実装でこれを設定することを確認する必要があります
  def or[A](p: Parser[A], p2: => Parser[A]): Parser[A] =
    s => p(s) match {
      case Failure(e,　false) => p2(s).mapError(_.addFailure(e))
      case r => r // committed failure or success skips running `p2`
    }
```
もちろん、人間が消費するために `ParseError`を出力する方法を決定する必要があります。また、` a | b | c`のチェーンが失敗した場合に報告されるエラーを選択するためのコンビネーターを公開することもできます。
3つのパーサーのそれぞれについてすべてのエラーを収集するか、失敗する前に入力から最も遠いパーサーのみを表示するかを選択します。

## 9.7 まとめ

コンビネータライブラリを記述するための代数的設計手法を紹介し、
JSONパーサーを実装しました。

PARTⅢでは、こうしたライブラリどうしの関係がどのような性質のものであるかを理解し、それらに共通する構造を抽象化する方法について説明します。

パターンと抽象化の世界があなたを待っています。


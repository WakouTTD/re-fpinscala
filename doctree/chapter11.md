# fpinscala 輪読会 #11

# 第11章 モナド

- `map`はendo functor
- `flatMap`は、monad

※　Haskellでは、単にFunctorと言うが、圏論ではendo Functorが正確

### 11.1 ファンクタ:map関数の一般化

```scala=
def map[A,B](ga: Gen[A])(f: A => B): Gen[B]

def map[A,B](pa: Parser[A])(f: A => B): Parser[B]

def map[A,B](oa: Option[A])(f: A => B): Option[B]
```

型シグネチャの違いは、具体的なデータ型(Gen,Parser,Option)のみ

この「mapを実装するデータ型」という発送をScalaのトレイトとして表せる

#### リスト 11-1
```scala=
trait Functor[F[_]] {
  def map[A,B](fa: F[A])(f: A => B): F[B]
}
```
型コンストラクタF[_]でmapをパラメータ化


#### リスト 11-2
```scala=
val listFunctor = new Functor[List] {
  def map[A,B](as: List[A])(f: A => B): List[B] = as map f
}
```



#### 11.1.1 ファンクタ則

### 11.2 モナド:flatMapと単位関数の一般化


### 11.2.1 モナドトレイト

### 11.3 モナドコンビネータ

### 11.4 モナド則

#### 11.4.1 結合律

#### 11.4.2 特定のモナドに対する結合律の証明

##### クライスリ合成:結合律より明確な表現

#### 11.4.3 同一律

- モノイドにおいてゼロがappendの単位元
- モナドでは`unit`がcomposeの単位元<br>
  - unit(単元)という名前は、数学において何らかの演算の単位元を表すためによく利用される。

```scala=
def unit[A](a: => A): F[A]
```
この関数にはcomposeに引数として渡すのにふさわしい型が指定されています。
ここでは非正格なAから`F[A]((=> A)=> F[A]))`が指定されており、　　<br>
Scalaでは、この型は通常のA => F[A]とは異なる。


左単位元(left identity)と、右単位元(right identity)という2つの法則で表されます。
```scala=
compose(f, unit) == f
compose(unit, f) == f
```
これらの法則をflatMapを使って表現することもできますが、以下に示すようにあまり明白では
ありません。
```scala=
flatMap(x)(unit) == x
flatMap(unit(y))(f) == f(y)
```
#### 11.5 モナドとはいったい何か

##### 11.5.1 単位元モナド

##### 11.5.2 Stateモナドと部分的な型の適用

##### まとめ

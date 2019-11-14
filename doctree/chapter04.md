# fpinscala 輪読会 #4
---

### 第4章 例外を使わないエラー処理

---

- 「エラーを値で返す」というのが基本的なアイディア
- 参照透過が維持される

try/catchでのエラー処理は参照透過ではないということ

- エラー値を表現するのに代数的データ型を用いる
- ScalaにおいてはOption,Eitherなどがエラー処理に使われる型である


---

### 4.1 例外の光と影

---

例外によって参照透過が損なわれてしまうのはなぜか？
実例をみる

```scala=
def failingFn(i: Int): Int = {
  val y: Int = throw new Exception("fail")
  try {
    val x = 42 + 5
    x + y
  }
  catch { case e: Exception => 43}
}
```
実行すると
```scala=
scala>failingFn(12)
java.lang.Exception
```


```scala=
def failingFn(i: Int): Int = {
  try {
    val x = 42 + 5
    x + ((throw new Exception("fail")): Int)
  }
  catch { case e: Exception => 43}
}
```
実行すると
```scala=
scala>failingFn(12)
43
```

構造は同じだが結果が異なっている
これは例外(throw文)がコンテキストに依存しているから

この場合のコンテキストとはtry~catch文のことです

### コンテキスト
コンテキストとは文脈、周囲の状況のこと。
コンテキストに依存するプログラムとはつまり、周りの状況によって結果が変わりうるという事を意味している。
関数型プログラミングでの関数とは、どのような状況で使われても結果が変わらないものを指している。
関数は引数によってのみ決定され、周囲の状況によって計算結果が変わるようなことがあってはならない。



---

### 4.2 例外に代わる手法

---

- 処理結果の型の偽の値を返す
- 計算できな場合の結果をあらかじめ引数で受け取る

  
**このどちらも良い方法とは言えない**

---

### 4.3 Optionデータ型

---

## Answer
### エラーを値で返すこと

計算結果を型で表現する→**Option**
その結果のパターンを代数的データ型で分類する
計算が成功する→**Some**
計算が失敗する→**None**
  
  
以下がScalaプログラムでの実装
```scala=
sealed trait Option[+A]
case class Some[+A](get: A) extends Option[A]
case object None extends Option[Nothing]


//Optionが付いていることによって返り血を見るだけで
//このメソッドの処理が失敗する可能性があると認識できる
def mean(xs: Seq[Double]): Option[Double] = ??? 

//比較用
def mean(xs: Seq[Double]): Double = ??? 


/*
def mean(xs: Seq[Double]): Option[Double] =
  if(xs.isEmpty) None
  else Some(xs.sum / xs.length)
*/

```

エラーを値で返すというのは新しいアイディアではない
Linuxのコマンド、Cの処理などでもプログラムが正常に終了したかどうかを表現するために値を返却していた。
exit 0  など
   
   
## Optionの基本関数



## EXERCISe4-1
```scala=
trait Option[+A] {self =>

  def map[B](f:A=>B):Option[B] = this match {
    case Some(v) = Some(f(v))
    case None => None
  }
  
  def getOrElse[B>:A](default: =>B):B = this match {
    case Some(v) => v
    case None => defult
  }
  
  
  def flatMap[B](f:A=>Option[B]):Option[B] = this.map(f).getOrElse(None)
  
  def orElse[B>:A](ob: =>Option[B]): Option[B] = this.map(a=>Some(a)).getOrElse(ob)
  
  def filter(f:A=>Boolean): Option[A] = this.flatMap{a=> if(f(a)) Some(a) else None}
}
```
mapとgetOrElseの二つを使用して他が実装されている
  
  

[Scala共変・反変](https://qiita.com/mtoyoshi/items/bd0ad545935225419327)
  

mapとflatMapの形は非常に似ている

mapの受け取る処理は A=>B
flatMapの受け取る処理は A=>Option[B]
  
flatMapの受け取る処理の方が表現力が高いことが感じ取れる

## EXERCISE4-2

[分散](https://sci-pursuit.com/math/statistics/variance.html)
```scala=
def variance(xs: Seq[Double]): Option[Double] = {
  val m: Option[Double] = mean(xs)
  m.flatMap(ave => mean(xs.map(e => Math.pow(e-ave, 2))))
}

def mean(xs: Seq[Double]): Option[Double] =
  if (xs.isEmpty) None else Some(xs.sum / xs.length)
```


### Option例
名前の文字列から従業員を探して、その人の部門を取得する
```scala=

case class Employee(name:String,department:String)

def lookupByName(name: String):Option[Employee]

//Joeの部門をOptionで取得
//Joeという社員の取得に失敗している場合はNoneになている
def joeDepartment: Option[String] = lookupByName("Joe").map(_.department)

val dept: String = 
  lookupByName("Joe").
  map(_.department).
  filter(_ != "Accounting").
  getOrElse("Default Dept")
```


## EXERCISE4-3
```scala=

def map2[A,B,C](a: Option[A], b:Option[B])(f:(A,B) => C): Option[C] = 
  for{
    a1 <- a
    b1 <- b
  } yield f(a1, b1)
```

## EXERCISE4-4
```scala=

def sequence[A](a: List[Option[A]]): Option[List[A]] = 
  a.foldRight(Some(Nil): Option[List[A]]){(e, acc) => map2(e, acc)(_ :: _)}
  
```

## EXERCISE4-5
```scala=
def traverse[A,B](a: List[A])(f: A=>Option[B]): Option[List[B]] =
  a.foldRight(Some(Nil): Option[List[B]]){(e, acc) => map2(f(e), acc)(_ :: _)}
      
```

---

### 4.4 Either データ型

---


### Optionで出来ないこと
何が原因で失敗したのかを知ることができない
  
None → これを見ると失敗したことは分かるが、ここから単純に値が存在しなかったのか、データベースの接続時にエラーが起きたからなのか、もしくわ他の何かなのか判断することはできない

**Eitherなら失敗の原因を伝えることが可能**

```scala=

sealed trait Either[+E, +A]
case class Left[+E](value: E) extends Either[E,Nothing]
case class Right[+A](value: A) extends Either[Nothing, A]

```

  
### Either例
```scala=
def mean(xs: IndexedSeq[Double]): Either[String,Double] =
  if(xs.isEmpty) Left("mean of empty lsit!")
  else Right(xs.sum / xs.length)


def safeDiv(x:Int, y:Int): Either[Exception, Int] =
  try Right(x/y)
  catch { case e: Exception => Left(e)}

//　※  =>A でないといけない
def Try[A](a: => A): Either[Exception, A] = 
  try Right(a)
  catch { case e: Exception => Left(e)}



case class Person(name: Name, age: Age)
sealed class Name(val value: String)
sealed class Age(val value: Int)

def mkName(name: String): Either[String, Name] =
  if(name == "" || name == null) Left("name if empty")
  else Right(new Name(name))
  
def mkAge(age: Int): Either[String, Age] = 
  if(age < 0) Left("Age is out of rage")
  else Right(new Age(age))
  
def mkPerson(name: String, age: Int): Either[String, Person] = 
  mkName(name).map2(maAge(age))(Person(_,_))

```


## EXERCISE4-6
```scala=
trait Either[+E, +A] {

  def map[B](f: A=>B): Either[E,B] = this.flatMap(e => Right(f(e)))
  
  def flatMap[EE>:E, B](f:A=>Either[EE,B]): Either[EE,B] = this match {
    case Left(_) => this
    case Right(v) => f(v)
  }
  
  def orElse[EE>:E, B>:A](b: =>Either[EE,B]): Either[EE,B] = this match {
    case Left(_) => b
    case Right(_) => this
  }
  
  def map2[EE>:E, B, C](b: Either[EE, B])(f: (A,B)=>C): Either[EE, C] =
    for{
      e1 <- this
      e2 <- b
    } yield f(e1, e2)

}
```

## EXERCISE4-7
```scala=

def sequence[E, A](es: List[Either[E,A]]): Either[E, List[A]] =
  es.foldRight(Right(Nil): Either[E, List[A]]){(e, acc) => e.map2(acc)(_ :: _)}

def traverse[E, A, B](as: List[A])(f: A=>Either[E,B]): Either[E, List[B]] =
  as.foldRight(Right(Nil): Either[E, List[B]])((e, acc) => f(e).map2(acc)(_ :: _))

```

## EXERCISE4-8
```scala=
def map2[EE>:E,B,C](b: Either[EE, B])(f: (A,B)=>C): Either[List[EE],C] = this match {
  case Left(l1) => b match {
      case Left(l2) => Left(List(l1, l2))
      case Right(_) => Left(List(l1))
  }
  case Right(r1) => b match {
      case Left(l3) => Left(List(l3))
      case Right(r2) => Right(f(r1, r2))
  }
}

def mkPerson(name: String, age:Int): Either[List[String], Person] = 
  mkName(name).map2(mkAge(age))(Person(_,_))
  

trait Partial[+A,+B] {
  def map[C](f: B=>C):Partial[A,C] = this match {
    case Errors(e) => Errors(e)
    case Success(s) => Success(f(s))
  }
  
  def map2[AA>:A, C, D](other:Partial[AA, C])(f:(B,C)=>D): Partial[AA,D] = this match {
    case Errors(es1) => other match {
      case Errors(es2) => Errors(es1 ++ es2)
      case Success(_) => Errors(es1)
    }
    case Success(s1) => other match {
      case Errors(es) => Errors(es)
      case Success(s2) => Success(f(s1,s2))
    }
  }
}
case class Errors[+A](get: Seq[A]) extends Partial[A,Nothing]
case class Success[+B](get: B) extends Partial[Nothing,B]

```
[Validated](https://typelevel.org/cats/datatypes/validated.html)


---

### 4.5 まとめ

---

Option,Eitherを使うことでエラー処理を共通のパターンで書ける

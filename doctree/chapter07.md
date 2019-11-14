# fpinscala 輪読会 #7

# Part II

Part Iでは関数型プログラミングでのループ、データ構造、例外を見てきた。
Part IIではライブラリを作成していく過程で**関数型の設計スキル**を磨く事を目標とする。

---

### 第7章 純粋関数型の並列処理

---

合成可能でモジュール性に優れた並列処理ライブラリを目指す

ポイントは**計算の表現をその実際の実行から切り離す**こと  
  
本章で作成する関数の具体例
```scala=
コレクション内の全ての要素に関数fを同時に適用する
val outputList = parMap(inputList)(f)
```
#### ライブラリ設計の方針  
- インタフェースを考える（実装は後回し）
- 代数的データ型でAPIを表現する

---

### 7.1 データ型と関数の選択

---

#### 難所その① アイディアを実現可能にするデータ型を見つけること  

まずは「並列計算を可能にする」とはどういうことかを考える必要がある。

```scala=
///具体例　整数リストの合計
//この処理は左から順番に（言うならば逐次的に）処理をしていく
def sum(ints: Seq[Int]): Int = ints. foldleft(0)((a,b)=>a+b)

```

```scala=
//IndexedSeqはランダムアクセスが得意なコレクション
//特定の箇所で効率よく分割する(splitAt)できる
//問題を分割している→部分ごとに並列化が可能!?
def sum(ints: IndexedSeq[Int]): Int =
  if(ints.size <=1)
    ints.headOption getOrElse 0
  else {
    val (l, r) = ints.splitAt(ints.length/2)
    sum(l) + sum(r)
  }

```

並列計算をするとしたら
```scala=
sum(l) + sum(r)
```
の部分で並列に計算が行われる事が分かる。←ここを直列に行ったら全体が並列ではなくなる

つまり、並列計算を表すデータ型は、その計算結果を保持していなければいけない。

ひとまずこのコンテナ型をPar[A]とする。

```scala=
意図：評価されていないAを受け取り、それを別のスレッドで実行する
def unit[A](a: =>A): Par[A]

意図：並列計算の結果を取得する
def get[A](a: Par[A]): A

```

Par型をもとに先ほどの例を改良してみる
```scala=
def sum(ints: IndexedSeq[Int]): Int = 
  if(ints.size<=1)
    ints headOption getOrElse 0
  else {
    val (l,r) = ints.splitAt(ints.length/2)
    val sumL: Par[Int] = Par.unit(sum(l))  //左半分を並行して計算
    val sumR: Par[Int] = Par.unit(sum(r))  //右半分を並行して計算
    Par.get(sumL) + Par.get(sumR)  //両方の結果を取得して足す
  }

```
しかし、このunitには副作用が存在する
```scala=
これはsumLとsumRが並行して計算される
val sumL: Par[Int] = Par.unit(sum(l))  //左半分を並行して計算
val sumR: Par[Int] = Par.unit(sum(r))  //右半分を並行して計算
Par.get(sumL) + Par.get(sumR)  //両方の結果を取得して足す

①sum(l)を並列で実行
②sum(r)を並列で実行
⓷結果を足し合わせる

sumLとsumRの部分を全て展開する（参照透過で副作用がないならばインライン展開しても意味は変わらないはずchapter2
Par.get(Par.unit(sum(l))) + Par.get(Par.unit(sum(r))

①この場合はPar.unit(sum(l))を並列で実行したあとgetですぐに結果を待つ
②Par.unit(sum(r))を並列で実行したあとgetですぐに結果を待つ

```

**getを呼び出すと計算待ちをする** これがunitの副作用を露見している

#### 対処案
- getを呼び出さない
- getを呼ぶのは一番最後にする


### 結論
- 並列処理をPar[A]で表す
- get,unitという計算には副作用がある


---

### 7.1.2 並列計算の結合

---

前回の対策案でgetを直ちに呼ぶのはやめた方が良いという話でした。
つまり、並列計算Par[Int]の中から計算結果を取り出さないということなのでsumメソッド自体もPar[Int]を返すことになる。

```scala=
def sum(ints: IndexedSeq[Int]): Par[Int] =
  if(ints.size<=1)
    Par.unit(ints.headOption getOrElse 0)
  else {
    val (l,r) = ints.splitAt(ints.length/2)
    Par.map2(sum(l), sum(r))(_ + _)  //計算結果を取得してたすのではなくParのまま足し算を行うことを記述する
  }
```

### EXERCISE7.1
```scala=
def map2[A,B,C](a: Par[A], b:Par[B])(f:(A,B)=>C):Par[C]
```

Parのmap2の意図としては、二つの計算が独立していて同時に実行できるようにした


map2は本質的に独立した二つになっている。
map2をflatMapで実装すればそれが確かめられる

現在のmap2の問題点として、左側"map2(a,b)(f)のaの方"が先に全部展開され実行開始されてから右側が展開される（並列に処理されてるとは言い難い）  

#### 対策案
map2では計算を行わない（引数の受け取りは正格のまま

  
つまりmap2では展開は行うが計算自体は行わないようにするということ  

```scala=
//展開しきった時
map2(
  map2(
    unit(1),
    unit(2))(_ + _),
  map2(
    unit(3),
    unit(4))(_ + _))(_ + _)
```

つまりsum関数ではIndexedSeq[Int]を入れてPar[Int]上記が出てくることになる。  
map2の計算とは受け取るf:(A,B=>C)のこと


### 結論
- getは最後まで呼ばない
- 計算はmap2によって記述する（この時に実行される訳ではなく、計算の構造をmap2によって定義している
- map2で受け取る値は同時に計算を開始した方がいい
- map2の引数が左から順に展開されてしまうという問題点は残ったままである


---

### 7.1.3 明示的なフォーク

---

前節ではmap2で受け取る値は常に同時に評価した方が良いとなったがそれについてもう少し検討する
```scala=
Par.map2(Par.unit(1), Par.unit(1))(_ + _)
```

この例の場合受け取るParが早く終わることが想像できるので、わざわざ別のスレッドで計算する意味がない。むしろコンテキストスイッチ等で遅くなる。  

この例から**並列で処理するかしないかは選べる方が良いのではないか**ということ

```scala=
//この案を受けて別のスレッドで処理を行うforkを導入する
def fork[A](a: =>Par[A]):Par[A]

def sum(ints: IndexedSeq[Int]): Par[Int] =
  if(ints.size<=1)
    //わざわざ別のスレッドで処理をしない
    Par.unit(ints.headOption getOrElse 0)
  else {
    val (l,r) = ints.splitAt(ints.length/2)
    //forkによってsum(l)とsum(r)は別のスレッドで並列処理することが明記されている
    Par.map2(Par.fork(sum(l)), Par.fork(sum(r)))(_ + _)
  }
```

#### Q. forkを利用しても「map2の引数が左から順に展開されてしまうという問題点は残ったままである」という問題点は解決していないのでは？

forkメソッドの引数を見てみると =>Par[A]というように名前渡しになっている。つまりsum(l)を渡したらさっさと次に行っているので解決している。

今までunitは非正格だったけど、forkを手に入れたのでunitは正格にする。今までの非正格なunitは新しいunitとforkで実装できる
```scala=
def unit[A](a: A):Par[A]

def lazyUnit[A](a: =>A): fork(unit(a))
```

次の問題は**評価をforkとgetのどちらに任せるのか？**

#### 仮定：Forkしたタイミングですぐに評価を始める
forkが引数の評価をすぐに始めるということはスレッドなりスレッドプールなりの情報をforkが知っていなければいけないということ（forkした時に別スレッドで実行されるので  
forkのたびにその情報を渡さなければいけない、という点で良くない  
背理法で**評価はgetに任せる**のが良さそうだ

#### Q. 評価はgetに任せると決めたは良いけど、forkでスレッドが切り替わるなら結局forkはスレッドなりの情報を知っている必要があるのでは？

評価はgetに任せる、という設計とはgetが呼ばれるまでは並列計算は始まらないということ。
つまりunit,fork,map2によって出来上がるのは計算の構造だけになる。7.1.2での展開で計算構造を作るということが目的で、これにforkが加わったことで展開しないで持っておくということが可能になった。
forkがスレッドなりの情報を持つ必要がないのは計算の構造を定義するだけの役割しかないから

### 結論
- 別スレッドでの処理を明示するforkメソッドを追加
- unitは正格に
- 実際の評価はrunメソッドで行う

---

### 7.2 表現の選択

---

**APIまとめ**

```scala=
//値をParの中に入れる
def unit[A](a: A):Par[A]

//2つのParを結合する
def map2[A,B,C](a:Par[A], b:Par[B])(f:(A,B)=>C): Par[C]

//並列評価としてマーク
def fork[A](a: =>Par[A]): Par[A]

//式aを並列評価
def lazyUnit[A](a: =>A): Par[A] = fork(unit(a))

//Parを実行して結果を取得
def run[A](a: =>Par[A]): A
```

### EXERCISE7.2

type Par[A] = ExecutorService => Future[A]

---

### 7.3 APIの改良

---

**ここまでの構想を一度実装してみる**
```scala=

type Par[A] = ExececuterService => Future[A]

object Par{
  def unit[A](a: A): Par[A] = (es: ExecuterService) => UnitFuture(a)

  private case class UnitFuture[A](get: A) extends Future[A] {
    def isDone = true
    def get(timeout: Long, units: TimeUnit): get
    def isCancelled = false
    def cancel(enventIfRunning: Boolean): Boolean = false
  }
  
  //map2の計算を並列かする場合はfork(map2 ...)
  def map2[A,B,C](a:Par[A], b:Par[B])(f:(A,B)=>C):Par[C] =
    (es: ExecuterService) => {
      val af = a(es)
      val bf = b(es)
      UnitFuture(f(af.get, bf.get)) //getしているので呼び出し元をブロック
    }
    
  def fork[A](a: =>Par[A]): Par[A] = 
    es => es.submit(new Callable[A]{
      def call = a(es).get
    })
}

```

java.util.concurrent.Futureインタフェースは純粋関数ではない
ここで重要なのが**Futureは純粋ではないがParのAPI全体は純粋なままである**ということ

その理由は、ユーザがFutureを手にするのがrunを呼び出した後だからです。
ユーザはunit,fork,map2等を使って純粋なAPIに対してプログラミングをしていることになる。

### EXERCISE7.3
```scala=

1. //当初私が実装したver
def map2[A,B,C](a:Par[A], b:Par[B])(f:(A,B)=>C)(timeout: Long): Par[C]  = 
    (es: ExecuterService) => {
      val af = a(es)
      val bf = b(es)
      val startTime = System.currentTimeMillis
      val resultA = af.get(timeout , java.util.concurrent.TimeUnit.MICROSECONDS)
      val finishA = System.currentTimeMillis - startTime
      val resultB = bf.get(timeout-resultA, java.util.concurrent.TimeUnit.MICROSECONDS)
      UnitFuture(f(resultA, resultB))
    }


/* This version respects timeouts. See `Map2Future` below. */
def map2[A,B,C](a: Par[A], b: Par[B])(f: (A,B) => C): Par[C] =
  es => {
    val (af, bf) = (a(es), b(es))
    Map2Future(af, bf, f)
  }

/*
Note: this implementation will not prevent repeated evaluation if multiple threads call `get` in parallel. We could prevent this using synchronization, but it isn't needed for our purposes here (also, repeated evaluation of pure values won't affect results).
*/
case class Map2Future[A,B,C](a: Future[A], b: Future[B],
                             f: (A,B) => C) extends Future[C] {
  @volatile var cache: Option[C] = None
  def isDone = cache.isDefined
  def isCancelled = a.isCancelled || b.isCancelled
  def cancel(evenIfRunning: Boolean) =
    a.cancel(evenIfRunning) || b.cancel(evenIfRunning)
  def get = compute(Long.MaxValue)
  def get(timeout: Long, units: TimeUnit): C =
    compute(TimeUnit.NANOSECONDS.convert(timeout, units))

  private def compute(timeoutInNanos: Long): C = cache match {
    case Some(c) => c
    case None =>
      val start = System.nanoTime
      val ar = a.get(timeoutInNanos, TimeUnit.NANOSECONDS)
      val stop = System.nanoTime;val aTime = stop-start
      val br = b.get(timeoutInNanos - aTime, TimeUnit.NANOSECONDS)
      val ret = f(ar, br)
      cache = Some(ret)
      ret
  }
}
```


### EXERCISE7.4
```scala=
def asyncF[A,B](f:A=>B): A=>Par[B] = a => lazyUnit(f(a))
```


```scala=

def map[A,B](pa:Par[A])(f: A=>B):Par[B] = map2(pa, ())((a, _)=> f(a))

def sortPar(parList: Par[List[Int]]) = map(parList)(_.sorted)
```

map2はmapよりも強力なので、map2でmapを実装する事ができる（その逆はできない）  

次にN個の並列計算を結合できるか試す。
ポイントは**asyncFによってA=>BをA=>Par[B]にする事ができる**こと

```scala=

def parMap[A,B](ps: List[A])(f: A=>B):Par[List[B]]

f:A=>BはasyncFによってA=>Par[B]にできる
List[A]と合わせるとList[Par[B]]まで辿り着く
すると、List[Par[B]]=>Par[List[B]]にするような処理があればparMapを実装できる

```

### EXERCISE7.5
```scala=
def sequence[A](ps: List[Par[A]]): Par[List[A]] =
  ps.foldRight(Par.unit(Nil): Par[List[A]])((e, acc)=> map2(e, acc)(_::_))
```


```scala=
def parMap[A,B](ps:List[A])(f:A=>B):Par[List[B]] = fork{
  val fbs: List[Par[B]] = ps.map(asyncF(f))
  sequence(fbs)
}
```
parMapはforkによって囲まれているのですぐに制御が戻る

### EXERCISE7.6
```scala=
//foldRightの中でf(e)を使って計算してしまっているのが良くない
def parFilter[A](as: List[A])(f: A=>Boolean): Par[List[A]] = fork {
  val fbs: List[Par[A]] = as.foldRigth(Nil: List[Par[A]])((e,acc) => if f(e) lazyUnit(e)::acc else acc)
  sequence(fbs)
}


//模範解答
def parFilter[A](l: List[A])(f: A => Boolean): Par[List[A]] = {
  val pars: List[Par[List[A]]] = 
    l map (asyncF((a: A) => if (f(a)) List(a) else List())) 
  map(sequence(pars))(_.flatten) // convenience method on `List` for concatenating a list of lists
}

```

---

### 7.4 APIの代数

---

**APIを代数として扱う**
- 演算の集まり
- 法則

---

### 7.4.1 マッピングの法則

---

```scala=

map(unit(x))(f) == unit(f(x))

```

map・・・文脈の中の値に関数を適用する
unit・・・値を文脈の中に入れる

何かしらの値xと関数fがあって、「xを文脈に入れてからfを適用する」のと「xにfを適用してから文脈に入れる」のでは常に等しくなるという法則

```scala=

map(unit(x))(f) == unit(f(x))
map(unit(x))(id) == unit(id(x))   //関数fが恒等関数idの場合
map(unit(x))(id) == unit(x)　　　　//id(x) == x
map(y)(id) == y                  //unit(x)　を yと置き換える

```

```scala=
map(x)(id) == x
```

レシーバ方式で x.map(id) == x と書くと分かりやすいかも？

「恒等関数を適用しても何も変わらない」

仮定：map(y)(id) == y
map(unit(x))(f) == unit(f(x))は真である

上記展開過程の逆

### EXERCISE7.7
仮定：map(y)(id) == y  

仮定より①map(unit(x))(f) == unit(f(x))が導かれる。

y = unit(x)とする

map(y)(g)
⇩
map(unit(x))(g)
⇩①より
unit(g(x))
よりmap(y)(g)=unit(g(x))を得る

map(map(y)(g))(f)
⇩
map(unit(g(x)))(f)
⇩①より
unit(f(g(x)))を得る

f(g(x))
⇩
(f compose g)(x)
よりunit((f compose g)(x))

①より
map(unit(x))(f compose g)
⇩y=unit(x)より
map(y)(f conpose g)


---

### 7.4.2 フォークの法則

---

```scala=
fork(x) == x
```

どのスレッドで計算しようが同じ結果になる

仮にこの法則が成り立たないとすると、同じ計算なのに実行するスレッドが違うだけで異なる結果になる事になる


---

### 7.4.3 法則違反へのバグ

---

### EXERCISE7.8

[Executors java doc](https://docs.oracle.com/javase/jp/8/docs/api/java/util/concurrent/Executors.html)
staticクラス
- callabel Callableオブジェクトを作る
- newCachedThreadPool スレッドプールを作成
- newFixedThreadPool 固定数のスレッドを利用するスレッドプールを作成
- newScheduledThreadPool コマンド実行をスケジュールできる

などなど、①様々な実装のスレッドプール(ExecutorService)を作成してくれるメソッド②スレッドプールで実行されるCallableを作成するメソッド③スレッドを作成するThreadFactoryを取得できるメソッド  

---
fork(x) == x
を満たさない判例  

```scala=
val a = lazyUnit(42 + 1)
val S = Executors.newFixedThreadPool(1)
println(Par.equal(S)(a, fork(a)))
```

法則通りならtrueだが、永遠に実行は終了しない→法則を満たしていない


fork実装
```scala=
def fork[A](a: => Par[A]): Par[A] =
  es => es.submit(new Callable[A] {
    def call = a(es).get
  })
```

Callabelを実行している時に内側でParを実行する
この時互いに相手が完了するのを待つためデッドロックになる


### EXERCISE7.9

```scala=
任意のスレッド数nのスレッドプールにて

n+1回のfork
fork(fork(fork(...fork(x)...))

```


---

### 7.4.4 アクターを使ったParの完全なノンブロッキング実装

---

固定サイズのスレッドプールでもデッドロックにならない（forkの法則を満たす）ような実装をする

#### 考え方
結果が準備できた時に呼び出されるコールバックを登録する

```scala=

trait Future[A] {
  private[parallelism] def apply(k: A=>Unit): Unit
  
  type Par[+A] = ExecutorService => Future[A]
}

//Parを実行
def run[A](es: ExecutorService)(p: Par[A]): A = {
  val ref = new AtomicReference[A]
  val latch = new CountDownLatch(1)
  p(es) {a => ref.set(a); latch.countDown}
  latch.await
  ref.get
}

```
runを実行すると呼び出し元はブロックされる
**ブロックしないrunは実装できない**

```scala=

def unit[A](a: A): Par[A] =
  es => new Future[A] {
    //unitはParに入れるだけなので別スレッドで実行する必要がない
    //ExecutorServiceを使用していない
    def apply(cb: A=>Unit): Unit = cb(a)
  }


def fork[A](a: => Par[A]): Par[A] =
  es => new Future[A] {
    //forkは別スレッドでの実行を示しているのでesが使用される
    def apply(cd: A=>Unit): Unit = eval(es)(a(es)(cb))
  }


def eval(es: ExecutorService)(r: => Unit): Unit = 
  es.submit(new Callable[Unit]{def call = r})
```
java.uti.concurrentだけではmap2の実装が凄く難しい。。。


#### アクター

並列タスクをキューに保存しておいて、スレッドがキューからタスクを取って行って実行していく

アクターによるmap2の実装
```scala=

def map2[A,B,C](p:Par[A], p2:Par[B])(f:(A,B)=>C): Par[C] =
  es => new Future[C] {
    def apply(cb: C=>Unit): Unit = {
      var ar: Option[A] = None
      var br: Option[B] = None
      val combiner = Actor[Either[A,B]](es){
        //Par[A]の呼び出しの場合
        case Left(a) => br match {
          case None => ar = Some(a)
          case Some(b) => eval(es)(cb(f(a,b)))
        }
        //Par[B]の呼び出しの場合
        case Right(b) => ar match {
          case None = br = Some(b)
          case Some(a) => eval(es)(cb(f(a,b)))
        }
      }
    }
  }

//再掲
def eval(es: ExecutorService)(r: => Unit): Unit = 
  es.submit(new Callable[Unit]{def call = r})
```
### EXERCISE7.10


ここからActor版ではなくjava.concurrent.Futureを用いた最初の実装での回答

### EXERCISE7.11

```scala=

def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] = 
  es => if(run(es)(cond).get) t(es) else f(es)
  //最初のPar実装なのでgetが出てくる

def choiceN[A](n: Par[Int])(choices: List[Par[A]]): Par[A] = 
  es => {
    val index = run(es)(n).get
    run(es)(choices(index))
  }

def choiceByChoiceN[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] = 
  choiceN(map(cond)(if (_) 0 else 1))(List(t, f))
```

### EXERCISE7.12

```scala=
def choiceMap[K,V](key: Par[K])(choices: Map[K, Par[V]]): Par[V] =
  es => {
    val rowKey = run(es)(key).get
    run(es)(choices(rowKey))
  }

```
7.11のchoiceNの実装とほぼ同じになっている。抽象化できる合図
より本質的なコンビネータを考える

### EXERCISE7.13
```scala=

def chooser[A,B](p: Par[A])(choices: A=>Par[B]): Par[B] =
  es => {
    val a = run(es)(p).get
    run(es)(choices(a))
  }

chooserは一般的にflatMapと呼ばれる

def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] = 
  chooser(cond)(bool => if (bool) t else f)

def choiceN[A](n: Par[Int])(choices: List[Par[A]]): Par[A] = 
  chooser(n)(num => choices(num))

```

**flatMapはmap➕join**

### EXERCISE7.14

```scala=
//再掲
type Par[A] = ExecutorService => Future[A]

def join[A](a: Par[Par[A]]): Par[A] = 
  es => run(es)(run(es)(a).get)

def flatMap[A,B](a: Par[A])(f: A=>Par[B]): Par[B] = join(map(a)(f))

def joinByFlatMap[A](a: Par[Par[A]]): Par[A] = flatMap(a)(identity)

```

---

### 7.6 まとめ

---
本章では並列処理を題材として取り上げたが、目的はあくまでも**関数型の設計プロセス**を感じることにある。

関数型の設計で重要なことは以下の2点
1. 問題を表現できるデータ型を見つけること
2. データ型の法則を考えること

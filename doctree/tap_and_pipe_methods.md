scala 2.13で導入されたtap/pipe

```Scala=
import scala.util.chaining._
val xs = List(1, 2, 3).tap(ys => println("debug " + ys.toString))
val times6 = (_: Int) * 6
(1 + 2 + 3).pipe(times6)
(1 - 2 - 3).pipe(times6).pipe(scala.math.abs)
```


```shell=
scala> import scala.util.chaining._
import scala.util.chaining._

scala> val xs = List(1, 2, 3).tap(ys => println("debug " + ys.toString))
debug List(1, 2, 3)
xs: List[Int] = List(1, 2, 3)

scala> val times6 = (_: Int) * 6
times6: Int => Int = $$Lambda$1727/1479800269@10fbbdb

scala> (1 + 2 + 3).pipe(times6)
res0: Int = 36

scala> (1 - 2 - 3).pipe(times6).pipe(scala.math.abs)
res1: Int = 24
```
可視境界
Generalized type Constraints
上限
下限
Context Bounds



型パラメータには以下の5つの境界が設定できる。

上限境界 upper bound
```
hoge[A <: T]
```
A が T のサブタイプでなければならない
下限境界 lower bound
```
hoge[A >: T]
```
A は T のスーパータイプでなければならない
可視境界(view bound)
```
hoge[A <% T]
```
A が T として扱える　(暗黙の型変換により変換可能または A が T のサブタイプ)



GeneralizedTypeConstraints



case class GeneralizedTypeConstraints[T](v:T) {
  
  // TがInt型の場合
  def increments(implicit ev:T =:= Int ):GeneralizedTypeConstraints[Int] = GeneralizedTypeConstraints( v + 1 )

  // TがString型の場合
  def concat(implicit ev:T =:= String ):GeneralizedTypeConstraints[String] = GeneralizedTypeConstraints( v + "1" )

  // TがDouble型の場合
  def culculate(implicit ev:T =:= Double ):GeneralizedTypeConstraints[Double] = GeneralizedTypeConstraints( v + 1D )

}

---

## 共変の定義
- ある座標系から別の座標系へ変換する際、直接変換によって変換される対象を「共変」と呼ぶ。

## 
- ある座標系から別の座標系へ変換する際、逆変換によって変換される対象を「反変」と呼ぶ。









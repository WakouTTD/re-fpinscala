import sbt._

object Dependencies {

  // Versions
  val scalaV = "2.13.0"

  // testing
  val scalatestV  = "3.0.5"
  val scalacheckV = "1.14.0"

  lazy val testingDependencies = Seq(
    "org.scalatest"  %% "scalatest"  % scalatestV,
    "org.scalacheck" %% "scalacheck" % scalacheckV
  ).map(_ % Test)

}


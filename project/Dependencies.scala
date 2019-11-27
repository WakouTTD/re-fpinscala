import sbt._

object Dependencies {

  // Versions
  val scalaV = "2.13.1"

  // testing
  val scalatestV  = "3.0.8"
  val scalacheckV = "1.14.0"

  lazy val testingDependencies = Seq(
    "org.scalatest"  %% "scalatest"  % scalatestV % "test",
    "org.scalacheck" %% "scalacheck" % scalacheckV % "test"
  ).map(_ % Test)

}


import Dependencies._

//name := "cheetsheet"

lazy val commonSettings = Seq[Setting[_]](
  scalaVersion := scalaV,
  fork in Test := false,
  parallelExecution in Test := false
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "wakou-re-fpinscala"
  )

lazy val exercises = (project in file("exercises"))
  .settings(commonSettings: _*)
  .settings(
    name := "exercises",
    libraryDependencies ++= testingDependencies
  )

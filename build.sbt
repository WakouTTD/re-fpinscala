import Dependencies._

//name := "cheetsheet"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  fork in Test := false,
  parallelExecution in Test := false
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "wakou-re-fpinscala"
  )

lazy val exercises = (project in file("exercises"))
  .settings(
    name := "exercises",
    libraryDependencies ++= testingDependencies
  )

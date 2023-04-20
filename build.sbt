ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

libraryDependencies += "com.github.losizm" %% "grapple" % "13.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "Grain"
  )

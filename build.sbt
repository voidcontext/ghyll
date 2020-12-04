ThisBuild / name := "json-stream"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.3"

val circeVersion = "0.13.0"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++=
      Seq(
        "org.typelevel"       %% "cats-core"     % "2.3.0",
        "org.typelevel"       %% "cats-effect"   % "2.3.0",
        "org.typelevel"       %% "mouse"         % "0.26.2",
        "co.fs2"              %% "fs2-core"      % "2.4.6",
        "com.google.code.gson" % "gson"          % "2.8.6",
        "io.circe"            %% "circe-core"    % circeVersion,
        "org.scalatest"       %% "scalatest"     % "3.2.3"      % Test,
        "io.circe"            %% "circe-generic" % circeVersion % Test
      ),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
    addCompilerPlugin(scalafixSemanticdb)
  )

addCommandAlias("fmt", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt")
addCommandAlias("prePush", ";fmt ;clean ;reload ;test")

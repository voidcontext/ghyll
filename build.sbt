ThisBuild / name := "json-stream"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.3"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

val circeVersion = "0.13.0"
val catsVersion = "2.3.0"
val catsEffectVersion = catsVersion
val fs2Version = "2.4.6"
val gsonVersion = "2.8.6"
val scalaTestVersion = "3.2.3"
val declineVersion = "1.3.0"

lazy val defaultSettings = Seq(
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  addCompilerPlugin(scalafixSemanticdb)
)

lazy val core = (project in file("modules/core"))
  .settings(
    defaultSettings,
    libraryDependencies ++=
      Seq(
        "org.typelevel"       %% "cats-core"     % catsVersion,
        "org.typelevel"       %% "cats-effect"   % catsEffectVersion,
        "org.typelevel"       %% "mouse"         % "0.26.2",
        "co.fs2"              %% "fs2-core"      % fs2Version,
        "com.google.code.gson" % "gson"          % gsonVersion,
        "io.circe"            %% "circe-core"    % circeVersion,
        "org.scalatest"       %% "scalatest"     % scalaTestVersion % Test,
        "io.circe"            %% "circe-generic" % circeVersion     % Test
      )
  )

lazy val benchmark = (project in file("benchmark"))
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline"        % "1.3.0",
      "com.monovore" %% "decline-effect" % declineVersion,
      "io.circe"     %% "circe-generic"  % circeVersion,
      "io.circe"     %% "circe-parser"   % circeVersion
    )
  )
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)

addCommandAlias("fmt", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt")
addCommandAlias("prePush", ";fmt ;clean ;reload ;test")

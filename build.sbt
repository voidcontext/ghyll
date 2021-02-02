import xerial.sbt.Sonatype._

val libraryName = "ghyll"
val website = "https://github.com/voidcontext/ghyll"

ThisBuild / name := libraryName
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.3"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / homepage := Some(url(website))
ThisBuild / publishTo := sonatypePublishToBundle.value
// Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

val circeVersion = "0.13.0"
val catsVersion = "2.3.0"
val catsEffectVersion = catsVersion
val fs2Version = "2.4.6"
val gsonVersion = "2.8.6"
val scalaTestVersion = "3.2.3"
val declineVersion = "1.3.0"
val scalatestScalacheckVersion = "3.1.1.1"
val scalaCheckVersion = "1.14.3"

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", libraryName, "gabor.pihaj@gmail.com"))
)

lazy val defaultSettings = Seq(
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.3" cross CrossVersion.full),
  addCompilerPlugin(scalafixSemanticdb),
  // // Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

lazy val core = (project in file("modules/core"))
  .settings(
    defaultSettings,
    publishSettings,
    name := libraryName,
    libraryDependencies ++=
      Seq(
        "org.typelevel"       %% "cats-core"       % catsVersion,
        "org.typelevel"       %% "cats-effect"     % catsEffectVersion,
        "co.fs2"              %% "fs2-core"        % fs2Version,
        "com.google.code.gson" % "gson"            % gsonVersion,
        "com.chuusai"          % "shapeless_2.13"  % "2.3.3",
        "io.circe"            %% "circe-core"      % circeVersion,
        "io.circe"            %% "circe-generic"   % circeVersion,
        "org.scalatest"       %% "scalatest"       % scalaTestVersion           % Test,
        "org.scalatestplus"   %% "scalacheck-1-14" % scalatestScalacheckVersion % Test,
        "org.scalacheck"      %% "scalacheck"      % scalaCheckVersion          % Test,
        "org.typelevel"       %% "claimant"        % "0.1.3"                    % Test
      )
  )

lazy val benchmark = (project in file("benchmark"))
  .settings(
    defaultSettings,
    skip in publish := true,
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline"        % "1.3.0",
      "com.monovore" %% "decline-effect" % declineVersion,
      "io.circe"     %% "circe-generic"  % circeVersion,
      "io.circe"     %% "circe-parser"   % circeVersion
    )
  )
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(core, benchmark)

addCommandAlias("fmt", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt")
addCommandAlias("prePush", ";fmt ;clean ;reload ;test")

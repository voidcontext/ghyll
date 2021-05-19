import xerial.sbt.Sonatype._

val libraryName = "ghyll"
val website = "https://github.com/voidcontext/ghyll"

val scala2 = "2.13.4"
val scala3 = "3.0.0"
val supportedScalaVersions = List(scala2, scala3)

ThisBuild / name := libraryName
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / scalaVersion := scala2
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.3"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / homepage := Some(url(website))
ThisBuild / publishTo := sonatypePublishToBundle.value
// Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

val circeVersion = "0.14.0-M4"
val catsVersion = "2.6.1"
val catsEffectVersion = "2.5.1"
val fs2Version = "2.5.6"
val gsonVersion = "2.8.6"
val scalaTestVersion = "3.2.9"
val declineVersion = "1.3.0"
val scalatestScalacheckVersion = "3.2.9.0"
val scalaCheckVersion = "1.15.4"

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", libraryName, "gabor.pihaj@gmail.com"))
)

lazy val defaultSettings = Seq(
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  // // Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

val coreSettings = Seq(
  crossScalaVersions := supportedScalaVersions,
  libraryDependencies ++= {
    Seq(
      "org.typelevel"       %% "cats-core"       % catsVersion,
      "org.typelevel"       %% "cats-effect"     % catsEffectVersion,
      "co.fs2"              %% "fs2-core"        % fs2Version,
      "com.google.code.gson" % "gson"            % gsonVersion,
      "org.scalatest"       %% "scalatest"       % scalaTestVersion           % Test,
      "org.scalatestplus"   %% "scalacheck-1-15" % scalatestScalacheckVersion % Test,
      "org.scalacheck"      %% "scalacheck"      % scalaCheckVersion          % Test
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          Seq(
            "com.chuusai" %% "shapeless" % "2.3.3",
            compilerPlugin(scalafixSemanticdb)
          )
        case _            => Nil
      })
  }
)

lazy val core = (project in file("modules/core"))
  .settings(
    defaultSettings,
    publishSettings,
    coreSettings,
    name := libraryName
  )

// This "phantom" module is for scala metals so that we can get code
// completion / navigation in the scala 3 source too.
lazy val core3 = project
  .in(file(".core"))
  .settings(
    coreSettings,
    scalaVersion := scala3,
    skip in publish := true,
    unmanagedSources.in(Compile) ++=
      (baseDirectory.in(ThisBuild).value / "modules" / "core" / "src" / "main" / "scala") ::
        (baseDirectory.in(ThisBuild).value / "modules" / "core" / "src" / "main" / "scala-3") ::
        Nil,
    unmanagedSources.in(Test) ++=
      (baseDirectory.in(ThisBuild).value / "modules" / "core" / "src" / "test" / "scala") ::
        (baseDirectory.in(ThisBuild).value / "modules" / "core" / "src" / "test" / "scala-3") ::
        Nil
  )

lazy val benchmark = (project in file("benchmark"))
  .settings(
    crossScalaVersions := List(scala2),
    defaultSettings,
    skip in publish := true,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline"        % declineVersion,
      "com.monovore" %% "decline-effect" % declineVersion,
      "io.circe"     %% "circe-generic"  % circeVersion,
      "io.circe"     %% "circe-parser"   % circeVersion
    )
  )
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .settings(
    skip in publish := true,
    crossScalaVersions := Nil
  )
  .aggregate(core)

addCommandAlias("fmt", s"++ $scala2 ;scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt")
addCommandAlias("fmtCheck", s"++ $scala2 ;scalafixAll --check ;scalafmtCheckAll; scalafmtSbtCheck")
addCommandAlias("prePush", ";fmt ;+ clean ;reload ;+ test")

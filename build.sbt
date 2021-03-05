import xerial.sbt.Sonatype._

val libraryName = "ghyll"
val website = "https://github.com/voidcontext/ghyll"

val supportedScalaVersions = List("3.0.0-RC1", "2.13.4")

ThisBuild / name := libraryName
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.3"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / homepage := Some(url(website))
ThisBuild / publishTo := sonatypePublishToBundle.value
// Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

val circeVersion = "0.14.0-M4"
val catsVersion = "2.4.2"
val catsEffectVersion = "2.3.2"
val fs2Version = "2.5.3"
val gsonVersion = "2.8.6"
val scalaTestVersion = "3.2.5"
val declineVersion = "1.3.0"
val scalatestScalacheckVersion = "3.2.5.0"
val scalaCheckVersion = "1.15.3"

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

lazy val core = (project in file("modules/core"))
  .settings(
    defaultSettings,
    publishSettings,
    name := libraryName,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= {
      Seq(
        "org.typelevel"       %% "cats-core"       % catsVersion,
        "org.typelevel"       %% "cats-effect"     % catsEffectVersion,
        "co.fs2"              %% "fs2-core"        % fs2Version,
        "com.google.code.gson" % "gson"            % gsonVersion,
        "io.circe"            %% "circe-core"      % circeVersion               % Test,
        "io.circe"            %% "circe-generic"   % circeVersion               % Test,
        "org.scalatest"       %% "scalatest"       % scalaTestVersion           % Test,
        "org.scalatestplus"   %% "scalacheck-1-15" % scalatestScalacheckVersion % Test,
        "org.scalacheck"      %% "scalacheck"      % scalaCheckVersion          % Test,
      ) ++
        (CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, _)) =>
            Seq(
              "com.chuusai"          %% "shapeless"  % "2.3.3",
              compilerPlugin(scalafixSemanticdb)
            )
          case _ => Nil
        })
    }
  )

lazy val benchmark = (project in file("benchmark"))
  .settings(
    crossScalaVersions := List("2.13.4"),
    defaultSettings,
    skip in publish := true,
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
  .aggregate(core, benchmark)

addCommandAlias("fmt", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt")
addCommandAlias("fmtCheck", ";scalafixAll --check ;scalafmtCheckAll; scalafmtSbtCheck")
addCommandAlias("prePush", ";fmt ;clean ;reload ;test")

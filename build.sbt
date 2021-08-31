import sbt._
import sbt.Keys._

addCommandAlias("ci-all",  ";+clean ;+test:compile ;+test ;+package")
addCommandAlias("release", ";+clean ;+publishSigned")

ThisBuild / scalaVersion       := "2.13.5"
ThisBuild / crossScalaVersions := Seq("2.12.13", "2.13.5", "3.0.0")
ThisBuild / organization       := "io.monix"
ThisBuild / organizationName   := "monix"

ThisBuild / scalacOptions ++= Seq(
  // Note, this is used by the doc-source-url feature to determine the
  // relative path of a given source file. If it's not a prefix of a the
  // absolute path of the source file, the absolute path of that file
  // will be put into the FILE_SOURCE variable, which is
  // definitely not what we want.
  "-sourcepath", file(".").getAbsolutePath.replaceAll("[.]$", "")
)

inThisBuild(List(
  organization := "io.monix",
  homepage := Some(url("https://monix.io")),
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  developers := List(
    Developer(
      id="Avasil",
      name="Piotr Gawrys",
      email="pgawrys2@gmail.com",
      url=url("https://github.com/Avasil")
    ))
))

val isDotty = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
)

lazy val sharedSettings = Seq(
  // Version specific options
  scalacOptions ++= (
    if (isDotty.value)
      Seq()
    else
      Seq(
        "-unchecked", "-deprecation", "-feature", "-Xlint",
        "-Ywarn-dead-code",
        "-Xlog-free-terms",
        // Enables linter options
        "-Xlint:adapted-args", // warn if an argument list is modified to match the receiver
        "-Xlint:nullary-unit", // warn when nullary methods return Unit
        "-Xlint:inaccessible", // warn about inaccessible types in method signatures
        "-Xlint:nullary-override", // warn when non-nullary `def f()' overrides nullary `def f'
        "-Xlint:infer-any", // warn when a type argument is inferred to be `Any`
        "-Xlint:missing-interpolator", // a string literal appears to be missing an interpolator id
        "-Xlint:doc-detached", // a ScalaDoc comment appears to be detached from its element
        "-Xlint:private-shadow", // a private field (or class parameter) shadows a superclass field
        "-Xlint:type-parameter-shadow", // a local type parameter shadows a type already in scope
        "-Xlint:poly-implicit-overload", // parameterized overloaded implicit methods are not visible as view bounds
        "-Xlint:option-implicit", // Option.apply used implicit view
        "-Xlint:delayedinit-select", // Selecting member of DelayedInit
        "-Xlint:package-object-classes" // Class or object defined in package object
      )

  ),

  unmanagedSourceDirectories in Compile += {
    (baseDirectory in LocalRootProject).value / "shared/src/main/scala"
  },

  Compile / unmanagedSourceDirectories += (
    if (isDotty.value)
      (ThisBuild / baseDirectory).value / "shared/src/main/scala-3"
    else
      (ThisBuild / baseDirectory).value / "shared/src/main/scala-2"
  ),
  Compile / doc / sources := {
    val old = (Compile / doc / sources).value
    if (isDotty.value)
      Seq()
    else
      old
  },

  libraryDependencies += "io.monix" %%% "minitest" % "2.9.6" % "test",
  testFrameworks += new TestFramework("minitest.runner.Framework"),

  headerLicense := Some(HeaderLicense.Custom(
    """|Copyright (c) 2014-2021 by The Monix Project Developers.
       |See the project homepage at: https://monix.io
       |
       |Licensed under the Apache License, Version 2.0 (the "License");
       |you may not use this file except in compliance with the License.
       |You may obtain a copy of the License at
       |
       |    http://www.apache.org/licenses/LICENSE-2.0
       |
       |Unless required by applicable law or agreed to in writing, software
       |distributed under the License is distributed on an "AS IS" BASIS,
       |WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       |See the License for the specific language governing permissions and
       |limitations under the License."""
      .stripMargin))
)

lazy val scalaJSSettings = Seq(
  scalaJSStage in Test := FastOptStage
)

val ReleaseTag = """^v(\d+\.\d+\.\d+(?:[-.]\w+)?)$""".r
lazy val publishSettings = Seq(
  ThisBuild / organization := "io.monix",
  ThisBuild / licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  ThisBuild / homepage := Some(url("https://github.com/monix/implicitbox")),

  ThisBuild / scmInfo := Some(
    ScmInfo(
      url("https://github.com/monix/implicitbox"),
      "scm:git@github.com:monix/implicitbox.git"
    )),

  ThisBuild / developers := List(
    Developer(
      id="alexelcu",
      name="Alexandru Nedelcu",
      email="noreply@alexn.org",
      url=url("https://alexn.org")
    )),

  // -- Settings meant for deployment on oss.sonatype.org
  //ThisBuild / sonatypeProfileName := (ThisBuild / organization).value
  ThisBuild / publishMavenStyle := true,
  ThisBuild / publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  ThisBuild / isSnapshot := {
    (ThisBuild / version).value endsWith "SNAPSHOT"
  },
  ThisBuild / Test / publishArtifact := false,
  ThisBuild / pomIncludeRepository := { _ => false }, // removes optional dependencies
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),

  /* The BaseVersion setting represents the in-development (upcoming) version,
   * as an alternative to SNAPSHOTS.
   */
  git.baseVersion := "0.0.1",

  git.gitTagToVersionNumber := {
    case ReleaseTag(v) => Some(v)
    case _ => None
  },

  git.formattedShaVersion := {
    val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

    git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
      git.baseVersion.value + "-" + sha + suffix
    }
  }
)

lazy val implicitboxRoot = project.in(file("."))
  .aggregate(implicitboxJVM, implicitboxJS)
  .settings(
    name := "implicitbox root",
    Compile / sources := Nil,
    skip in publish := true,
  )

lazy val implicitbox = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(GitVersioning)
  .jsSettings(scalaJSSettings)
  .settings(
    name := "implicitbox",
    sharedSettings,
    publishSettings
  )

lazy val implicitboxJVM    = implicitbox.jvm
lazy val implicitboxJS     = implicitbox.js

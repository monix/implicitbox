import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbt.Keys._
import scala.xml.Elem
import scala.xml.transform.{RewriteRule, RuleTransformer}

addCommandAlias("ci-all",  ";+clean ;+test:compile ;+test ;+package")
addCommandAlias("release", ";+clean ;+implicitsNative/clean ;+publishSigned ;+implicitsNative/publishSigned")

val Scala211 = "2.11.12"

ThisBuild / scalaVersion       := "2.12.8"
ThisBuild / crossScalaVersions := Seq(Scala211, "2.12.8", "2.13.0")
ThisBuild / organization       := "io.monix"
ThisBuild / organizationName   := "monix"

def scalaPartV = Def setting (CrossVersion partialVersion scalaVersion.value)
lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (unmanagedSourceDirectories in sc) ++= {
      (unmanagedSourceDirectories in sc).value.map { dir =>
        scalaPartV.value match {
          case Some((major, minor)) =>
            new File(dir.getPath + s"_$major.$minor")
          case None =>
            throw new NoSuchElementException("Scala version")
        }
      }
    }
  }

lazy val scalaLinterOptions =
  Seq(
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

lazy val scalaTwoTwelvePlusOptions =
  Seq(
    // Options available from Scala 2.12 and up
    "-Ywarn-unused:-implicits"
  )

lazy val scalaTwoTwelveDeprecatedOptions =
  Seq(
    // Deprecated in 2.12, removed in 2.13
    "-Ywarn-inaccessible",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit"
  )

lazy val sharedSettings = Seq(
  scalacOptions in ThisBuild ++= Seq(
    // Note, this is used by the doc-source-url feature to determine the
    // relative path of a given source file. If it's not a prefix of a the
    // absolute path of the source file, the absolute path of that file
    // will be put into the FILE_SOURCE variable, which is
    // definitely not what we want.
    "-sourcepath", file(".").getAbsolutePath.replaceAll("[.]$", "")
  ),

  scalacOptions ++= Seq(
    "-unchecked", "-deprecation", "-feature", "-Xlint",
    "-Ywarn-dead-code",
    "-Xlog-free-terms"
  ),

  // Version specific options
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v > 12 =>
      scalaLinterOptions ++ scalaTwoTwelvePlusOptions
    case Some((2, 12)) =>
      scalaLinterOptions ++ scalaTwoTwelvePlusOptions ++ scalaTwoTwelveDeprecatedOptions
    case Some((2, 11)) =>
      scalaLinterOptions ++ Seq("-target:jvm-1.6") ++ scalaTwoTwelveDeprecatedOptions
    case _ =>
      Seq("-target:jvm-1.6")
  }),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11 | 12)) =>
      Seq(
        "-Xlint:unsound-match", // Pattern match may not be typesafe
        "-Xlint:by-name-right-associative", // By-name parameter of right associative operator
        "-Ywarn-adapted-args"
      )
    case _ =>
      Nil
  }),

  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
    Resolver.sonatypeRepo("releases")
  ),

  unmanagedSourceDirectories in Compile += {
    (baseDirectory in LocalRootProject).value / "shared/src/main/scala"
  },

  libraryDependencies += "io.monix" %%% "minitest" % "2.6.0" % "test",
  testFrameworks += new TestFramework("minitest.runner.Framework"),

  headerLicense := Some(HeaderLicense.Custom(
    """|Copyright (c) 2014-2019 by The Monix Project Developers.
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

lazy val nativeSettings = Seq(
  scalaVersion := Scala211,
  crossScalaVersions := Seq(Scala211),
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

lazy val needsScalaParadise = settingKey[Boolean]("Needs Scala Paradise")

lazy val requiredMacroCompatDeps = Seq(
  needsScalaParadise := {
    val sv = scalaVersion.value
    (sv startsWith "2.11.") || (sv startsWith "2.12.") || (sv == "2.13.0-M3")
  },
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % Compile,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
  ),
  libraryDependencies ++= {
    if (needsScalaParadise.value) Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch))
    else Nil
  },
  scalacOptions ++= {
    if (needsScalaParadise.value) Nil
    else Seq("-Ymacro-annotations")
  }
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

  // For evicting Scoverage out of the generated POM
  // See: https://github.com/scoverage/sbt-scoverage/issues/153
  ThisBuild / pomPostProcess := { (node: xml.Node) =>
    new RuleTransformer(new RewriteRule {
      override def transform(node: xml.Node): Seq[xml.Node] = node match {
        case e: Elem
          if e.label == "dependency" && e.child.exists(child => child.label == "groupId" && child.text == "org.scoverage") => Nil
        case _ => Seq(node)
      }
    }).transform(node).head
  },

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

lazy val implicitsRoot = project.in(file("."))
  .aggregate(implicitboxJVM, implicitboxJS, implicitboxNative)
  .settings(
    name := "implicitbox root",
    Compile / sources := Nil,
    skip in publish := true,
  )

lazy val implicitbox = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(GitVersioning)
  .nativeSettings(nativeSettings)
  .jsSettings(scalaJSSettings)
  .settings(
    name := "implicitbox",
    sharedSettings,
    crossVersionSharedSources,
    requiredMacroCompatDeps,
    publishSettings
  )

lazy val implicitboxJVM    = implicitbox.jvm
lazy val implicitboxJS     = implicitbox.js
lazy val implicitboxNative = implicitbox.native

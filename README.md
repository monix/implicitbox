# Implicit Box

Scala micro-library providing utilities for capturing implicits.

[![CircleCI](https://circleci.com/gh/monix/implicitbox.svg?style=svg)](https://circleci.com/gh/monix/implicitbox)

### Compiler support

- Scala 2.11, 2.12, 2.13
- [Scala.js](https://www.scala-js.org/) 0.6.x
- [Scala Native](https://github.com/scala-native/scala-native) 0.3.x

## Usage in SBT

For `build.sbt` (use the `%%%` operator for Scala.js):

```scala
// use the %%% operator for Scala.js
libraryDependencies += "io.monix" %% "implicitbox" % "0.1.0"
```

/*
 * Copyright (c) 2014-2021 by The Monix Project Developers.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package implicitbox

import minitest.SimpleTestSuite

object PrioritySuite extends SimpleTestSuite {
  test("fallback") {
    val ev = implicitly[Priority[MyType[Int], MyType[String]]]

    assert(ev.isFallback, "ev.isFallback")
    assert(!ev.isPreferred, "!ev.isPreferred")

    assertEquals(ev.getFallback, Some(MyType.forString))
    assertEquals(ev.fold(x => x.upcast[Any])(x => x.upcast[Any]), MyType.forString)
    assertEquals(ev.join, MyType.forString)
    assertEquals(ev.getPreferred, None)
    assertEquals(ev.bimap(_ => "int")(_ => "string").toEither, Right("string"))
  }

  test("preferred") {
    import MyType.Implicits.forAny
    val ev = implicitly[Priority[MyType[Int], MyType[String]]]

    assert(!ev.isFallback, "!ev.isFallback")
    assert(ev.isPreferred, "ev.isPreferred")

    assertEquals(ev.getPreferred, Some(MyType.forAny))
    assertEquals(ev.getFallback, None)

    assertEquals(ev.fold(x => x.upcast[Any])(x => x.upcast[Any]), MyType.forAny)
    assertEquals(ev.join, MyType.forAny)
    assertEquals(ev.bimap(_ => "int")(_ => "string").toEither, Left("int"))
  }
}

/*
 * Copyright (c) 2014-2019 by The Monix Project Developers.
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

object NotSuite extends SimpleTestSuite {
  test("default implicit resolution") {
    import MyType.Implicits.forAny

    val forString = implicitly[MyType[String]]
    val forInt = implicitly[MyType[Int]]

    assertEquals(forString, MyType.forString)
    assertEquals(forInt, MyType.forAny[Int])
  }

  test("should resolve") {
    val ev = implicitly[Not[MyType[Int]]]
    assert(ev.isInstanceOf[Not[_]])
  }

  test("should fail if implicit is in scope") {
    assertDoesNotCompile("implicitly[Not[MyType[String]]]", ".*?exists in scope.*?")
  }
}

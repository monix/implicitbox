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

class MyType[A] {
  def upcast[B >: A]: MyType[B] =
    this.asInstanceOf[MyType[B]]
}

object MyType {
  /** For strings. */
  implicit val forString: MyType[String] =
    new MyType[String]

  def forAny[A]: MyType[A] =
    forAnyInst.asInstanceOf[MyType[A]]

  object Implicits {
    implicit def forAny[A](implicit ev: Not[MyType[A]]): MyType[A] =
      MyType.forAny
  }

  private[this] val forAnyInst: MyType[Any] =
    new MyType[Any]
}

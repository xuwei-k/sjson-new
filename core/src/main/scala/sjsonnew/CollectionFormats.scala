/*
 * Original implementation (C) 2009-2011 Debasish Ghosh
 * Adapted and extended in 2011 by Mathias Doenitz
 * Adapted and extended in 2016 by Eugene Yokota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sjsonnew

import scala.reflect.ClassTag

trait CollectionFormats {
  /**
    * Supplies the JsonFormat for Lists.
   */
  implicit def listFormat[A: JsonFormat] = new RootJsonFormat[List[A]] {
    lazy val elemFormat = implicitly[JsonFormat[A]]
    def write[J](list: List[A], builder: Builder[J]): Unit =
      {
        builder.beginArray()
        list foreach { x => elemFormat.write(x, builder) }
        builder.endArray()
      }
    override def addField[J](name: String, xs: List[A], builder: Builder[J]): Unit =
      if (xs.isEmpty) ()
      else {
        builder.addFieldName(name)
        write(xs, builder)
      }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): List[A] =
      jsOpt match {
        case Some(js) =>
          val size = unbuilder.beginArray(js)
          val xs = (1 to size).toList map { x =>
            val elem = unbuilder.nextElement
            elemFormat.read(Some(elem), unbuilder)
          }
          unbuilder.endArray
          xs
        case None => Nil
      }
  }

  /**
    * Supplies the JsonFormat for Arrays.
   */
  implicit def arrayFormat[A: JsonFormat: ClassTag] = new RootJsonFormat[Array[A]] {
    lazy val elemFormat = implicitly[JsonFormat[A]]
    def write[J](array: Array[A], builder: Builder[J]): Unit =
      {
        builder.beginArray()
        array foreach { x => elemFormat.write(x, builder) }
        builder.endArray()
      }
    override def addField[J](name: String, xs: Array[A], builder: Builder[J]): Unit =
      if (xs.isEmpty) ()
      else {
        builder.addFieldName(name)
        write(xs, builder)
      }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Array[A] =
      jsOpt match {
        case Some(js) =>
          val size = unbuilder.beginArray(js)
          val xs = (1 to size).toList map { x =>
            val elem = unbuilder.nextElement
            elemFormat.read(Some(elem), unbuilder)
          }
          unbuilder.endArray
          xs.toArray
        case None => Array()
      }
  }

  /**
    * Supplies the JsonFormat for Maps. The implicitly available JsonFormat for the key type K must
    * always write JsStrings, otherwise a [[sjsonnew.SerializationException]] will be thrown.
   */
  implicit def mapFormat[K: JsonFormat, V: JsonFormat] = new RootJsonFormat[Map[K, V]] {
    lazy val keyFormat = implicitly[JsonFormat[K]]
    lazy val valueFormat = implicitly[JsonFormat[V]]
    def write[J](m: Map[K, V], builder: Builder[J]): Unit =
      {
        builder.beginObject()
        m foreach {
          case (k, v) =>
            keyFormat.write(k, builder)
            valueFormat.write(v, builder)
        }
        builder.endObject()
      }
    override def addField[J](name: String, m: Map[K, V], builder: Builder[J]): Unit =
      if (m.isEmpty) ()
      else {
        builder.addFieldName(name)
        write(m, builder)
      }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Map[K, V] =
      jsOpt match {
        case Some(js) =>
          val size = unbuilder.beginObject(js)
          val xs = (1 to size).toList map { x =>
            val (k, v) = unbuilder.nextFieldWithJString
            keyFormat.read(Some(k), unbuilder) -> valueFormat.read(Some(v), unbuilder)
          }
          unbuilder.endObject
          Map(xs: _*)
        case None => Map()
      }
  }

  import collection.{immutable => imm}

  implicit def immIterableFormat[T :JsonFormat]   = viaSeq[imm.Iterable[T], T](seq => imm.Iterable(seq :_*))
  implicit def immSeqFormat[T :JsonFormat]        = viaSeq[imm.Seq[T], T](seq => imm.Seq(seq :_*))
  implicit def immIndexedSeqFormat[T :JsonFormat] = viaSeq[imm.IndexedSeq[T], T](seq => imm.IndexedSeq(seq :_*))
  implicit def immLinearSeqFormat[T :JsonFormat]  = viaSeq[imm.LinearSeq[T], T](seq => imm.LinearSeq(seq :_*))
  implicit def immSetFormat[T :JsonFormat]        = viaSeq[imm.Set[T], T](seq => imm.Set(seq :_*))
  implicit def vectorFormat[T :JsonFormat]        = viaSeq[Vector[T], T](seq => Vector(seq :_*))

  import collection._

  implicit def iterableFormat[T :JsonFormat]   = viaSeq[Iterable[T], T](seq => Iterable(seq :_*))
  implicit def seqFormat[T :JsonFormat]        = viaSeq[Seq[T], T](seq => Seq(seq :_*))
  implicit def indexedSeqFormat[T :JsonFormat] = viaSeq[IndexedSeq[T], T](seq => IndexedSeq(seq :_*))
  implicit def linearSeqFormat[T :JsonFormat]  = viaSeq[LinearSeq[T], T](seq => LinearSeq(seq :_*))
  implicit def setFormat[T :JsonFormat]        = viaSeq[Set[T], T](seq => Set(seq :_*))

  /**
    * A JsonFormat construction helper that creates a JsonFormat for an Iterable type I from a builder function
    * Seq => I.
   */
  def viaSeq[I <: Iterable[A], A: JsonFormat](f: imm.Seq[A] => I): RootJsonFormat[I] = new RootJsonFormat[I] {
    lazy val elemFormat = implicitly[JsonFormat[A]]
    def write[J](iterable: I, builder: Builder[J]): Unit =
      {
        builder.beginArray()
        iterable foreach { x => elemFormat.write(x, builder) }
        builder.endArray()
      }
    override def addField[J](name: String, xs: I, builder: Builder[J]): Unit =
      if (xs.isEmpty) ()
      else {
        builder.addFieldName(name)
        write(xs, builder)
      }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): I =
      jsOpt match {
        case Some(js) =>
          val size = unbuilder.beginArray(js)
          val xs = (1 to size).toList map { x =>
            val elem = unbuilder.nextElement
            elemFormat.read(Some(elem), unbuilder)
          }
          unbuilder.endArray
          f(xs)
        case None => f(Nil)
      }
  }
}

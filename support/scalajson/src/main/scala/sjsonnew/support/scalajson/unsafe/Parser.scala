package sjsonnew
package support.scalajson.unsafe

import shaded.scalajson.ast.unsafe._
import scala.collection.mutable.ArrayBuffer
import org.typelevel.jawn.{ Facade, FContext, SupportParser }

object Parser extends SupportParser[JValue] {
  implicit val facade: Facade[JValue] =
    new Facade[JValue] {
      def jnum(s: CharSequence, decIndex: Int, expIndex: Int) = JNumber(s.toString)
      def jstring(s: CharSequence) = JString(s.toString)
      def jnull() = JNull
      def jfalse() = JFalse
      def jtrue() = JTrue
      def jnum(s: String) = JNumber(s)
      def jint(s: String) = JNumber(s)
      def jstring(s: String) = JString(s)
      def singleContext() = new FContext[JValue] {
        var value: JValue = _
        def add(s: CharSequence) = add(s.toString)
        def add(s: String) = value = jstring(s)
        def add(v: JValue) = value = v
        def finish: JValue = value
        def isObj: Boolean = false
      }
      def arrayContext() = new FContext[JValue] {
        private val vs = ArrayBuffer.empty[JValue]
        def add(s: CharSequence) = add(s.toString)
        def add(s: String) = vs += jstring(s)
        def add(v: JValue) = vs += v
        def finish: JValue = JArray(vs.toArray)
        def isObj: Boolean = false
      }
      def objectContext() = new FContext[JValue] {
        private var key: String = _
        private var vs = ArrayBuffer.empty[JField]
        private def andNullKey[A](t: => Unit): Unit = { t; key = null }
        def add(s: CharSequence) = add(s.toString)
        def add(s: String) = if (key == null) key = s else andNullKey(vs += JField(key, jstring(s)))
        def add(v: JValue) = andNullKey(vs += JField(key, v))
        def finish: JValue = JObject(vs.toArray)
        def isObj: Boolean = true
      }
    }
}

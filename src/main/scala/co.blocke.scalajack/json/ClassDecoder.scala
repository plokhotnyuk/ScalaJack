package co.blocke.scalajack
package json

import scala.annotation.*

object ClassDecoder:
  // def apply[A: scala.reflect.ClassTag](fields: Array[String], fieldDecoders: Array[sj[_]], instantiator: Array[?] => A) = new sj[A] {
  def apply[A](
      fields: Array[String],
      fieldDecoders: Array[sj[_]],
      instantiator: Array[?] => A,
      fieldValues: Array[Any]
  ) = new sj[A] {

    val fieldMatrix = new StringMatrix(fields)

    def unsafeDecode(in: JsonSource): A =
      JsonParser.charWithWS(in, '{')
      if JsonParser.firstField(in) then
        var done = false
        while !done do
          val fieldIdx = JsonParser.parseField(in, fieldMatrix)
          if fieldIdx < 0 then JsonParser.skipValue(in)
          else
            val dec = fieldDecoders(fieldIdx)
            fieldValues(fieldIdx) = dec.unsafeDecode(in)
            if !JsonParser.nextField(in) then done = true
      else throw new JsonParseError("Expected fields!", in)

      // Construct the new object
      instantiator(fieldValues)
  }

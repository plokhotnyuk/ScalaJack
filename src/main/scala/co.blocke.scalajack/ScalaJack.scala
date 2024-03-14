package co.blocke.scalajack

import co.blocke.scala_reflection.{RTypeRef, TypedName}
import co.blocke.scala_reflection.reflect.ReflectOnType
import scala.quoted.*

import quoted.Quotes
import json.*

case class ScalaJack[T](jsonCodec: JsonCodec[T]): // extends JsonCodec[T] //with YamlCodec with MsgPackCodec
  def fromJson(js: String): T = // Either[JsonParseError, T] =
    jsonCodec.decodeValue(reading.JsonSource(js))

  val out = writing.JsonOutput() // let's clear & re-use JsonOutput--avoid re-allocating all the internal buffer space
  def toJson(a: T): String =
    jsonCodec.encodeValue(a, out.clear())
    out.result

// ---------------------------------------

object ScalaJack:

  def apply[A](implicit a: ScalaJack[A]): ScalaJack[A] = a

  // ----- Use default JsonConfig
  inline def sj[T]: ScalaJack[T] = ${ sjImpl[T] }
  def sjImpl[T: Type](using Quotes): Expr[ScalaJack[T]] =
    import quotes.reflect.*
    val classRef = ReflectOnType[T](quotes)(TypeRepr.of[T], true)(using scala.collection.mutable.Map.empty[TypedName, Boolean])
    // val jsonDecoder = reading.JsonReader.refRead2(classRef)
    // println(s"Decoder: ${jsonDecoder.show}")
    val jsonCodec = JsonCodecMaker.generateCodecFor(classRef, JsonConfig)

    '{ ScalaJack($jsonCodec) }

  // ----- Use given JsonConfig
  inline def sj[T](inline cfg: JsonConfig): ScalaJack[T] = ${ sjImplWithConfig[T]('cfg) }
  def sjImplWithConfig[T: Type](cfgE: Expr[JsonConfig])(using Quotes): Expr[ScalaJack[T]] =
    import quotes.reflect.*
    val cfg = summon[FromExpr[JsonConfig]].unapply(cfgE)
    val classRef = ReflectOnType[T](quotes)(TypeRepr.of[T], true)(using scala.collection.mutable.Map.empty[TypedName, Boolean])
    // val jsonDecoder = reading.JsonReader.refRead2(classRef)
    val jsonCodec = JsonCodecMaker.generateCodecFor(classRef, cfg.getOrElse(JsonConfig))
    '{ ScalaJack($jsonCodec) }

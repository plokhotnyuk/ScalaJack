package co.blocke.scalajack
package model

import util.Path

import scala.collection.mutable.Builder

case class LazyTypeAdapter[T](context: Context, tpe: Type) extends TypeAdapter[T] {

  var resolvedTypeAdapter: TypeAdapter[T] = _

  override def resolved: TypeAdapter[T] = {
    var typeAdapter = resolvedTypeAdapter

    // $COVERAGE-OFF$Can't really test as this is triggered by race condition, if it can happen at all.
    if (typeAdapter == null) {
      typeAdapter = context.typeAdapter(tpe).resolved.asInstanceOf[TypeAdapter[T]]
      if (typeAdapter.isInstanceOf[LazyTypeAdapter[_]]) {
        throw new IllegalStateException(s"Type adapter for $tpe is still being built")
      }
      resolvedTypeAdapter = typeAdapter
    }
    // $COVERAGE-ON$

    typeAdapter
  }

  def read(path: Path, reader: Transceiver, isMapKey: Boolean = false): T = resolved.read(path, reader, isMapKey)
  def write(t: T, writer: Transceiver)(out: Builder[Any, writer.WIRE]): Unit = {}
}

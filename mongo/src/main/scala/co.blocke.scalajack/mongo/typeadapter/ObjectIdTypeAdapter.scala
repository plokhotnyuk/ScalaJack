package co.blocke.scalajack
package mongo

import util.Path
import model._

import scala.collection.mutable.Builder
import org.bson.types.ObjectId
import org.bson._

object ObjectIdTypeAdapterFactory extends TypeAdapter.===[ObjectId] {

  def read[WIRE](path: Path, reader: Reader[WIRE]): ObjectId =
    reader.head.input match {
      case null =>
        reader.next
        null
      case i if i.asInstanceOf[BsonValue].isNull() =>
        reader.next
        null
      case _ =>
        reader.asInstanceOf[MongoReader].readObjectId(path)
    }

  def write[WIRE](t: ObjectId, writer: Writer[WIRE], out: Builder[WIRE, WIRE], isMapKey: Boolean): Unit =
    t match {
      case null => out += new BsonNull().asInstanceOf[WIRE]
      case _ =>
        out += (new BsonObjectId(t)).asInstanceOf[WIRE]
    }
}

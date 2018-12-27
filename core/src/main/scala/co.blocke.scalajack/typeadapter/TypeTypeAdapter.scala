package co.blocke.scalajack
package typeadapter

import util.Path
import model._

/*
object TypeTypeIRTransceiver {
  def typeNameToType(typeName: String): Type =
    try {
      staticClass(typeName).toType
    } catch {
      case e: ScalaReflectionException =>
        throw new ClassNotFoundException(s"""Unable to find class named "$typeName"\n""", e)
    }

  def typeToTypeName(tpe: Type): String = tpe.typeSymbol.fullName
}

class TypeTypeIRTransceiver(
                             typeToTypeName: Type => String = TypeTypeIRTransceiver.typeToTypeName,
                             typeNameToType: String => Type = TypeTypeIRTransceiver.typeNameToType) extends IRTransceiver[Type] {

  private val TypeType: Type = typeOf[Type]

  override def read[IR, WIRE](path: Path, ir: IR)(implicit ops: Ops[IR, WIRE], guidance: SerializationGuidance): ReadResult[Type] =
    ir match {
      // --- Not sure null is a thing related to type serialization...
      //      case AstNull()           => DeserializationSuccess(nullTypeTagged)
      case IRString(typeName) => ReadSuccess(TypeTagged(typeNameToType(typeName), TypeType))
    }

  override def write[IR, WIRE](tagged: TypeTagged[Type])(implicit ops: Ops[IR, WIRE], guidance: SerializationGuidance): WriteResult[IR] =
    tagged match {
      // Is there such a thing as a null type?  Not sure Scala lets you set a type to null...
      //      case TypeTagged(null) => SerializationSuccess(AstNull())
      case TypeTagged(tpe) => WriteSuccess(IRString(typeToTypeName(tpe)))
    }
}
*/

object TypeTypeAdapterFactory extends TypeAdapterFactory {

  override def typeAdapterOf[T](next: TypeAdapterFactory)(implicit context: Context, tt: TypeTag[T]): TypeAdapter[T] = {
    if (tt.tpe =:= typeOf[Type]) {
      TypeTypeAdapter(tt.mirror).asInstanceOf[TypeAdapter[T]]
    } else {
      next.typeAdapterOf[T]
    }
  }

}

trait HintModifier

case class TypeTypeAdapter(mirror: Mirror, typeModifier: Option[HintModifier] = None) extends TypeAdapter[Type] {

  def typeNameToType(typeName: String): Type =
    try {
      staticClass(typeName).toType
    } catch {
      case e: ScalaReflectionException =>
        throw new ClassNotFoundException(s"""Unable to find class named "$typeName"\n""", e)
    }

  def read(path: Path, reader: Reader, isMapKey: Boolean = false): Type = reader.readString(path) match {
    case s: String => typeNameToType(s)
    case null      => null
  }
}
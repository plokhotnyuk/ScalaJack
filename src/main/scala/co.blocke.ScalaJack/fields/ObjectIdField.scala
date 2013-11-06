package co.blocke.scalajack
package fields

import com.fasterxml.jackson.core._
import org.bson.types.ObjectId

/** 
 * Support for MongoDB's ObjectId data type
 */

case class ObjectIdField( name:String ) extends Field {
	override private[scalajack] def render[T]( sb:StringBuilder, target:T, label:Option[String], ext:Boolean, hint:String, withHint:Boolean=false ) : Boolean = {
		val oid = "{\"$oid\":\""+target.asInstanceOf[ObjectId].toString+"\"}"
		label.fold( {
				sb.append('"')
				sb.append(oid)
				sb.append('"')
			})((labelStr) => {
				sb.append('"')
				sb.append( labelStr )
				sb.append("\":"+oid+",")
			})
		true
	}
	override private[scalajack] def renderDB[T]( target:T, label:Option[String], hint:String, withHint:Boolean = false ) : Any = {
		target.asInstanceOf[ObjectId]
	}
	override private[scalajack] def readValue[T]( jp:JsonParser, ext:Boolean, hint:String )(implicit m:Manifest[T]) : Any = {
		jp.nextToken
		jp.nextToken
		val v = jp.getValueAsString
		jp.nextToken
		jp.nextToken
		new ObjectId( v )
	}
	override private[scalajack] def readValueDB[T]( src:Any, hint:String )(implicit m:Manifest[T]) : Any = src
}
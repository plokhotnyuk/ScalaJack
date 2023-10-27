package co.blocke.scalajack
package json

import co.blocke.scala_reflection.reflect.rtypeRefs.*
import co.blocke.scala_reflection.reflect.*
import co.blocke.scala_reflection.{RTypeRef, TypedName}
import co.blocke.scala_reflection.rtypes.*
import co.blocke.scala_reflection.Liftables.TypedNameToExpr
import scala.quoted.*
import co.blocke.scala_reflection.RType
import scala.jdk.CollectionConverters.*
import java.util.concurrent.ConcurrentHashMap
import scala.util.{Failure, Success}

object JsonWriter:

  // Tests whether we should write something or not--mainly in the case of Option, or wrapped Option
  def isOkToWrite(a: Any, cfg: JsonConfig) =
    a match
      case None if !cfg.noneAsNull                                    => false
      case Left(None) if !cfg.noneAsNull                              => false
      case Right(None) if !cfg.noneAsNull                             => false
      case Failure(_) if cfg.tryFailureHandling == TryOption.NO_WRITE => false
      case _                                                          => true

  val refCache = new ConcurrentHashMap[TypedName, (?, StringBuilder, JsonConfig) => StringBuilder]

  def writeJsonFn[T](rtRef: RTypeRef[T], isMapKey: Boolean = false)(using tt: Type[T], q: Quotes): Expr[(T, StringBuilder, JsonConfig) => StringBuilder] =
    import quotes.reflect.*

    rtRef match
      case rt: PrimitiveRef[?] if rt.family == PrimFamily.Stringish =>
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          sb.append('"')
          sb.append(a.toString)
          sb.append('"')
        }
      case rt: PrimitiveRef[?] =>
        if isMapKey then
          '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
            sb.append('"')
            sb.append(a.toString)
            sb.append('"')
          }
        else '{ (a: T, sb: StringBuilder, cfg: JsonConfig) => sb.append(a.toString) }

      case rt: AliasRef[?] =>
        val fn = writeJsonFn[rt.T](rt.unwrappedType.asInstanceOf[RTypeRef[rt.T]], isMapKey)(using Type.of[rt.T])
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) => $fn(a, sb, cfg) }

      case rt: ArrayRef[?] =>
        if isMapKey then throw new JsonError("Arrays cannot be map keys.")
        rt.elementRef.refType match
          case '[t] =>
            val elementFn = writeJsonFn[t](rt.elementRef.asInstanceOf[RTypeRef[t]])
            '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
              sb.append('[')
              val sbLen = sb.length
              a.asInstanceOf[Array[t]].foreach { e =>
                if isOkToWrite(e, cfg) then
                  $elementFn(e.asInstanceOf[t], sb, cfg)
                  sb.append(',')
              }
              if sbLen == sb.length then sb.append(']')
              else sb.setCharAt(sb.length() - 1, ']')
            }

      case rt: ClassRef[?] =>
        if isMapKey then throw new JsonError("Classes cannot be map keys.")
        val fieldFns = rt.fields.map { f =>
          f.fieldRef.refType match
            case '[t] => (writeJsonFn[t](f.fieldRef.asInstanceOf[RTypeRef[t]]), f)
        }
        val typedName = Expr(rt.typedName)
        '{
          val classFn = (a: T, sb: StringBuilder, cfg: JsonConfig) =>
            sb.append('{')
            val sbLen = sb.length
            ${
              val hintStmt = (rt match
                case s: ScalaClassRef[?] if s.renderTrait.isDefined =>
                  val traitName = Expr(s.renderTrait.get)
                  '{
                    sb.append('"')
                    sb.append(cfg.typeHintLabelByTrait.getOrElse($traitName, cfg.typeHintLabel))
                    sb.append('"')
                    sb.append(':')
                    sb.append('"')
                    val hint = cfg.typeHintTransformer.get(a.getClass.getName) match
                      case Some(xform) => xform(a)
                      case None        => cfg.typeHintDefaultTransformer(a.getClass.getName)
                    sb.append(hint)
                    sb.append('"')
                    sb.append(',')
                    ()
                  }
                case _ => '{ () }
              ) // .asInstanceOf[Expr[Unit]]
              val stmts = hintStmt :: fieldFns.map { case (fn, field) =>
                '{
                  val fieldValue = ${
                    Select.unique('{ a }.asTerm, field.name).asExpr
                  }
                  if isOkToWrite(fieldValue, cfg) then
                    ${
                      field.fieldRef.refType match
                        case '[t] =>
                          '{
                            sb.append('"')
                            sb.append(${ Expr(field.name) })
                            sb.append('"')
                            sb.append(':')
                            val fn2 = $fn.asInstanceOf[(t, StringBuilder, JsonConfig) => StringBuilder]
                            fn2(fieldValue.asInstanceOf[t], sb, cfg)
                            sb.append(',')
                          }
                    }
                }
              }
              Expr.ofList(stmts)
            }
            if sbLen == sb.length then sb.append('}')
            else sb.setCharAt(sb.length() - 1, '}')
          refCache.put($typedName, classFn)
          classFn
        }

      case rt: TraitRef[?] =>
        if isMapKey then throw new JsonError("Traits cannot be map keys.")
        val typedName = Expr(rt.typedName)
        val traitType = rt.expr
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          val comboName = ($traitType.typedName.toString + "::" + a.getClass.getName).asInstanceOf[TypedName]
          Option(refCache.get(comboName)) match
            case Some(writerFn) =>
              writerFn.asInstanceOf[(T, StringBuilder, JsonConfig) => StringBuilder](a, sb, cfg)
            case None =>
              val writerFn = ReflectUtil.inTermsOf[T]($traitType, a, sb, cfg)
              refCache.put(comboName, writerFn)
              writerFn(a, sb, cfg)
        }

      case rt: SeqRef[?] =>
        if isMapKey then throw new JsonError("Seq instances cannot be map keys.")
        rt.elementRef.refType match
          case '[t] =>
            val elementFn = writeJsonFn[t](rt.elementRef.asInstanceOf[RTypeRef[t]])
            '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
              sb.append('[')
              val sbLen = sb.length
              a.asInstanceOf[Seq[?]].foreach { e =>
                if isOkToWrite(e, cfg) then
                  $elementFn(e.asInstanceOf[t], sb, cfg)
                  sb.append(',')
              }
              if sbLen == sb.length then sb.append(']')
              else sb.setCharAt(sb.length() - 1, ']')
            }

      case rt: OptionRef[?] =>
        if isMapKey then throw new JsonError("Options cannot be map keys.")
        rt.optionParamType.refType match
          case '[t] =>
            val fn = writeJsonFn[t](rt.optionParamType.asInstanceOf[RTypeRef[t]])
            '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
              a match
                case None    => sb.append("null")
                case Some(v) => $fn(v.asInstanceOf[t], sb, cfg)
            }

      case rt: TryRef[?] =>
        if isMapKey then throw new JsonError("Try cannot be map keys.")
        rt.tryRef.refType match
          case '[t] =>
            val fn = writeJsonFn[t](rt.tryRef.asInstanceOf[RTypeRef[t]])
            '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
              a match
                case Success(v)                                                => $fn(v.asInstanceOf[t], sb, cfg)
                case Failure(_) if cfg.tryFailureHandling == TryOption.AS_NULL => sb.append("null")
                case Failure(v) =>
                  sb.append('"')
                  sb.append(v.getMessage)
                  sb.append('"')
            }

      case rt: MapRef[?] =>
        if isMapKey then throw new JsonError("Maps cannot be map keys.")
        rt.elementRef.refType match
          case '[k] =>
            rt.elementRef2.refType match
              case '[v] =>
                val keyFn = writeJsonFn[k](rt.elementRef.asInstanceOf[RTypeRef[k]], true)
                val valueFn = writeJsonFn[v](rt.elementRef2.asInstanceOf[RTypeRef[v]])
                '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
                  sb.append('{')
                  val sbLen = sb.length
                  a.asInstanceOf[Map[?, ?]].foreach { case (key, value) =>
                    if isOkToWrite(value, cfg) then
                      $keyFn(key.asInstanceOf[k], sb, cfg)
                      sb.append(':')
                      $valueFn(value.asInstanceOf[v], sb, cfg)
                      sb.append(',')
                  }
                  if sbLen == sb.length then sb.append('}')
                  else sb.setCharAt(sb.length() - 1, '}')
                }

      case rt: LeftRightRef[?] =>
        if isMapKey then throw new JsonError("Union, intersection, or Either cannot be map keys.")
        rt.leftRef.refType match
          case '[lt] =>
            val leftFn = writeJsonFn[lt](rt.leftRef.asInstanceOf[RTypeRef[lt]])
            rt.rightRef.refType match
              case '[rt] =>
                val rightFn = writeJsonFn[rt](rt.rightRef.asInstanceOf[RTypeRef[rt]])
                val rtypeExpr = rt.expr
                '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
                  $rtypeExpr match
                    case r if r.clazz == classOf[Either[_, _]] =>
                      a match
                        case Left(v) =>
                          $leftFn(v.asInstanceOf[lt], sb, cfg)
                        case Right(v) =>
                          $rightFn(v.asInstanceOf[rt], sb, cfg)
                    case _: RType[?] => // Intersection & Union types.... take your best shot!  It's all we've got.  No definitive info here.
                      val trial = new StringBuilder()
                      if scala.util.Try($rightFn(a.asInstanceOf[rt], trial, cfg)).isFailure then $leftFn(a.asInstanceOf[lt], sb, cfg)
                      else sb ++= trial
                }

      case rt: EnumRef[?] =>
        val expr = rt.expr
        val isMapKeyExpr = Expr(isMapKey)
        '{
          val rtype = $expr.asInstanceOf[EnumRType[?]]
          (a: T, sb: StringBuilder, cfg: JsonConfig) =>
            val enumAsId = cfg.enumsAsIds match
              case '*'                                               => true
              case aList: List[String] if aList.contains(rtype.name) => true
              case _                                                 => false
            if enumAsId then
              val enumVal = rtype.ordinal(a.toString).getOrElse(throw new JsonError(s"Value $a is not a valid enum value for ${rtype.name}"))
              if $isMapKeyExpr then
                sb.append('"')
                sb.append(enumVal.toString)
                sb.append('"')
              else sb.append(enumVal.toString)
            else
              sb.append('"')
              sb.append(a.toString)
              sb.append('"')
        }

      // TODO:  Not sure this is right!
      case rt: SealedTraitRef[?] =>
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          sb.append('"')
          a.toString
          sb.append('"')
        }

      case rt: TupleRef[?] =>
        if isMapKey then throw new JsonError("Tuples cannot be map keys.")
        val elementFns = rt.tupleRefs.map { f =>
          f.refType match
            case '[t] => (writeJsonFn[t](f.asInstanceOf[RTypeRef[t]]), f)
        }
        val numElementsExpr = Expr(elementFns.size)
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          sb.append('[')
          val sbLen = sb.length
          ${
            val stmts = elementFns.zipWithIndex.map { case ((fn, e), i) =>
              '{
                val fieldValue = ${
                  Select.unique('{ a }.asTerm, "_" + (i + 1)).asExpr
                }
                ${
                  e.refType match
                    case '[t] =>
                      '{
                        val fn2 = $fn.asInstanceOf[(t, StringBuilder, JsonConfig) => StringBuilder]
                        fn2(fieldValue.asInstanceOf[t], sb, cfg)
                        sb.append(',')
                      }
                }
              }
            }
            Expr.ofList(stmts)
          }
          if sbLen == sb.length then sb.append(']')
          else sb.setCharAt(sb.length() - 1, ']')
        }

      case rt: SelfRefRef[?] =>
        if isMapKey then throw new JsonError("Classes or traits cannot be map keys.")
        val e = rt.expr
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          val fn = refCache.get($e.typedName).asInstanceOf[(T, StringBuilder, JsonConfig) => StringBuilder]
          fn(a, sb, cfg)
        }

      case rt: JavaCollectionRef[?] =>
        if isMapKey then throw new JsonError("Collections cannot be map keys.")
        rt.elementRef.refType match
          case '[t] =>
            val elementFn = writeJsonFn[t](rt.elementRef.asInstanceOf[RTypeRef[t]])
            '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
              sb.append('[')
              val sbLen = sb.length
              a.asInstanceOf[java.util.Collection[?]].toArray.foreach { e =>
                if isOkToWrite(e, cfg) then
                  $elementFn(e.asInstanceOf[t], sb, cfg)
                  sb.append(',')
              }
              if sbLen == sb.length then sb.append(']')
              else sb.setCharAt(sb.length() - 1, ']')
            }

      case rt: JavaMapRef[?] =>
        if isMapKey then throw new JsonError("Maps cannot be map keys.")
        rt.elementRef.refType match
          case '[k] =>
            rt.elementRef2.refType match
              case '[v] =>
                val keyFn = writeJsonFn[k](rt.elementRef.asInstanceOf[RTypeRef[k]], true)
                val valueFn = writeJsonFn[v](rt.elementRef.asInstanceOf[RTypeRef[v]])
                '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
                  sb.append('{')
                  val sbLen = sb.length
                  a.asInstanceOf[java.util.Map[?, ?]].asScala.foreach { case (key, value) =>
                    if isOkToWrite(value, cfg) then
                      $keyFn(key.asInstanceOf[k], sb, cfg)
                      sb.append(':')
                      $valueFn(value.asInstanceOf[v], sb, cfg)
                      sb.append(',')
                  }
                  if sbLen == sb.length then sb.append('}')
                  else sb.setCharAt(sb.length() - 1, '}')
                }

      case rt: ObjectRef =>
        val name = Expr(rt.name)
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          sb.append('"')
          sb.append($name)
          sb.append('"')
        }

      case rt: Scala2Ref[?] =>
        val name = Expr(rt.name)
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          sb.append('"')
          sb.append($name)
          sb.append('"')
        }

      case rt: UnknownRef[?] =>
        '{ (a: T, sb: StringBuilder, cfg: JsonConfig) =>
          sb.append('"')
          sb.append("unknown")
          sb.append('"')
        }

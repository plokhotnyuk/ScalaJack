package co.blocke.scalajack
package parser2

case class JsonParserState(json: String) extends ParserState {

  type IN = String

  private var pos: Int = 0
  private val chars = json.toCharArray
  private val maxPos = chars.length

  private[parser2] def skipWhitespace() =
    while (pos < maxPos && chars(pos).isWhitespace)
      pos += 1

  @inline private[parser2] def skipInt() = {
    val mark = pos
    while (pos < maxPos && ((chars(pos) >= '0' && chars(pos) <= '9') || chars(pos) == '-' || chars(pos) == '+' || chars(pos) == 'e' || chars(pos) == 'E'))
      pos += 1
    json.substring(mark, pos)
  }

  @inline private[parser2] def skipTrue(): Boolean =
    if (pos + 3 < maxPos && json.substring(pos, pos + 4) == "true") {
      pos += 4
      true
    } else false

  @inline private[parser2] def skipFalse(): Boolean =
    if (pos + 4 < maxPos && json.substring(pos, pos + 5) == "false") {
      pos += 5
      true
    } else false

  @inline private[parser2] def char(): Char = chars(pos)
  @inline private[parser2] def advance(): Unit = pos += 1
}
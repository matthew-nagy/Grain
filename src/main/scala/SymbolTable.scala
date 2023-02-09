package Grain
import scala.collection.mutable.*

case class Symbol(
                   name: String,
                   token: Token,
                   dataType: Types.Type,
                   size: Int,
                   lineNumber: Int,
                   var defined: Boolean
                 )
object Symbol{
  def make(token: Token, dataType: Types.Type, defined: Boolean): Symbol =
    Symbol(token.lexeme, token, dataType, Types.getTypeSize(dataType), token.lineNumber, defined)
}

type Scope = Map[String, Symbol]

class SymbolTable{
  val globalScope: Scope = Map()
  val functions: Map[String, Scope] = Map()
  val types: Map[String, Types.Type] = Map(
    "word" -> Types.Word(),
    "bool" -> Types.Boolean()
  )
}

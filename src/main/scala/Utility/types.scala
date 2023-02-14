package Utility

import Grain.*

sealed trait Type
case class Empty() extends Type
case class Word() extends Type
case class SpriteSheet() extends Type
case class BooleanType() extends Type
case class StringLiteral() extends Type
case class Ptr(to: Type) extends Type
case class Array(of: Type, length: Int) extends Type
case class FunctionPtr(argumentTypes: List[Type], returnType: Type) extends Type
object Struct{
  case class Entry(symbol: Symbol, offset: Int)
  def generateEntries(symbols: List[Symbol]):List[Entry] = {
    var currentOffset = 0
    val makeEntryFromSymbol = (symbol: Symbol)=>{
      val priorOffset = currentOffset
      currentOffset = currentOffset + getTypeSize(symbol.dataType)
      Entry(symbol, priorOffset)
    }
    for symbol <- symbols yield makeEntryFromSymbol(symbol)
  }
}
case class Struct(entries: List[Struct.Entry]) extends Type{
  private var cachedSize: Option[Int] = None
  def size = cachedSize match
    case None =>
      val result = entries.last.offset + getTypeSize(entries.last.symbol.dataType)
      cachedSize = Some(result)
      result
    case Some(value) => value

  def typeof(name: String): Type = entries.filter(_.symbol.token.lexeme == name).head.symbol.dataType
}

def getTypeSize(dataType: Type):Int = {
  dataType match
    case Empty() => 0
    case Word() => 2
    case SpriteSheet() => 2
    case BooleanType() => 2
    case Ptr(_) => 2
    case Array(of, length) => getTypeSize(of) * length
    case FunctionPtr(_, _) => 2
    case s @ Struct(_) => s.size
}
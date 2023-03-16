package Utility

import Grain.*
import scala.collection.Set

type Bitdepth = 2 | 4 | 8
val bitdepthLiteralStrings: Set[String] = Set("2bpp", "4bpp", "8bpp")

sealed trait Type
sealed trait PtrType extends Type
sealed trait SpecialType extends Type


//If in the future multi sized tiles are supported nativly, here is where to put it
case class SpriteSheet(bitdepth: Bitdepth) extends SpecialType
case class Palette() extends SpecialType
case class BitdepthType() extends SpecialType


case class Empty() extends Type
case class Word() extends Type
case class BooleanType() extends Type
case class StringLiteral() extends Type
case class Ptr(to: Type) extends PtrType
case class Array(of: Type, length: Int) extends PtrType
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
  
  def contains(name: String): Boolean = entries.exists(_.symbol.token.lexeme == name)
}

def getTypeSize(dataType: Type):Int = {
  dataType match
    case Empty() => 0
    case Word() => 2
    case BooleanType() => 2
    case Ptr(_) => 2
    case Array(of, length) =>
      getTypeSize(of) * length
    case FunctionPtr(_, _) => 2
    case s @ Struct(_) => s.size
}

def stripPtrType(t: Type):Type = {
  t match
    case Ptr(to) => to
    case Array(of, _) => of
    case _ => throw new Exception(t.toString ++ " is not a pointer type")
}

def typeEquivilent(t1: Type, t2: Type): Boolean = {
  if(t1 == t2){
    true
  }
  else{
    if(t1.isInstanceOf[PtrType] && t2.isInstanceOf[PtrType]){
      typeEquivilent(stripPtrType(t1), stripPtrType(t2))
    }
    else{
      println(t1.toString ++ " != " ++ t2.toString)
      false
    }
  }
}
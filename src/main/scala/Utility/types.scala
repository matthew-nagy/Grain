package Utility

import Grain.*

import scala.annotation.tailrec
import scala.collection.Set
import scala.collection.mutable.ListBuffer

type Bitdepth = 2 | 4 | 8
val bitdepthLiteralStrings: Set[String] = Set("2bpp", "4bpp", "8bpp")


sealed trait Type{
  override def hashCode(): Int = toString.hashCode
}
sealed trait IndexableType(of: Type) extends Type
sealed trait SpecialType extends Type


//If in the future multi sized tiles are supported nativly, here is where to put it
case class Sprite(bitdepth: Bitdepth) extends SpecialType
case class GenericSprite() extends SpecialType
case class Palette() extends SpecialType
case class BitdepthType() extends SpecialType
case class DataBankIndex() extends SpecialType


case class Empty() extends Type
case class Word() extends Type
case class ROMWord() extends Type
case class BooleanType() extends Type
case class StringLiteral() extends Type
case class Ptr(of: Type) extends IndexableType(of){
  override def toString: String = "Ptr(" ++ typeToRecursionSafeString(of) ++ ")"
}
case class Array(of: Type, length: Int) extends IndexableType(of){
  override def toString: String =
    "Array<" ++ typeToRecursionSafeString(of) ++ ">[" ++ length.toString ++ "]"
}
case class FunctionPtr(argumentTypes: List[Type], returnType: Type, mmio: Boolean) extends Type{
  private def getArgsAsString: String =
    if(argumentTypes.isEmpty){
      ""
    }
    else if(argumentTypes.length == 1){
      typeToRecursionSafeString(argumentTypes.head)
    }
    else {
      argumentTypes.tail.foldLeft(typeToRecursionSafeString(argumentTypes.head))(_ ++ ", " ++ typeToRecursionSafeString(_))
    }
  override def toString: String =
    "FunctionPtr(" ++
      getArgsAsString ++
      "): " ++ typeToRecursionSafeString(returnType) ++ (if(mmio)""else " MMIO")
}
object Struct{
  case class Entry(symbol: Symbol, offset: Int)
  def generateEntries(symbols: List[Symbol]):ListBuffer[Entry] = {
    var currentOffset = 0
    val makeEntryFromSymbol = (symbol: Symbol)=>{
      val priorOffset = currentOffset
      currentOffset = currentOffset + getTypeSize(symbol.dataType)
      Entry(symbol, priorOffset)
    }
    ListBuffer[Entry]().addAll(for symbol <- symbols yield makeEntryFromSymbol(symbol))
  }
}
case class Struct(name: String, entries: ListBuffer[Struct.Entry], definedFunctions: ListBuffer[Symbol]) extends Type{
  private var cachedSize: Option[Int] = None

  //Can be accessed as `val sSize = myStruct.size`.
  def size: Int = cachedSize match
    case None =>
      val result =
        if(entries.nonEmpty)
          entries.last.offset + getTypeSize(entries.last.symbol.dataType)
        else
          0
      cachedSize = Some(result)
      result
    case Some(value) => value

  def getTypeOf(entryName: String): Type =
    entries.find(_.symbol.name == entryName).get.symbol.dataType

  def getOffsetOf(entryName: String): Int =
    entries.find(_.symbol.token.lexeme == entryName) match
      case Some(entry) => entry.offset
      case _ => throw Exception(entryName ++ " not found in class " ++ name)
  def typeof(entryName: String): Type =
    (entries.toList.map(_.symbol) ::: definedFunctions.toList).find(_.token.lexeme == entryName) match
      case Some(symbol) => symbol.dataType
      case None =>
        throw new Exception("Class " ++ name ++ " doesn't have a member variable called " ++ entryName)

  def contains(name: String): Boolean = entries.exists(_.symbol.token.lexeme == name)

  override def toString: String =
    name ++ ":\n\t" ++ entries.foldLeft("Variables:")(_ ++ "\n\t\t" ++ _.toString)
}

def getTypeSize(dataType: Type):Int = {
  dataType match
    case Empty() => 0
    case Word() => 2
    case ROMWord() => getTypeSize(Word())
    case BooleanType() => 2
    case Ptr(_) => 2
    case Array(of, length) =>
      getTypeSize(of) * length
    case FunctionPtr(_, _, _) => 2
    case s @ Struct(_, _, _) => s.size
    case DataBankIndex() => 2
    case Palette() => 256
    case Sprite(bpp) => 8 * 8 * bpp / 8
}

def stripPtrType(t: Type):Type = {
  t match
    case Ptr(to) => to
    case Array(of, _) => of
    case _ => throw new Exception(t.toString ++ " is not a pointer type")
}

def isSpriteType(quearyType: Type): Boolean =
  quearyType.isInstanceOf[GenericSprite] || quearyType.isInstanceOf[Sprite]

def typeEquivalent(t1: Type, t2: Type): Boolean = {
  val isT1Indexable = t1.isInstanceOf[IndexableType]
  val isT2Indexable = t2.isInstanceOf[IndexableType]
  //You can't assign a pointer to a non pointer, and vice versa
  if(isT1Indexable ^ isT2Indexable){
    false
  }
  else typeEquivalentPostIndexerCheck(t1, t2)
}
@tailrec
def typeEquivalentPostIndexerCheck(t1: Type, t2: Type): Boolean = {
  val t1IsIndexable = t1.isInstanceOf[IndexableType]
  val t2IsIndexable = t2.isInstanceOf[IndexableType]

  //Either look for an underlying pointer type
  (t1IsIndexable, t2IsIndexable) match
    case (true, true) => typeEquivalentPostIndexerCheck(stripPtrType(t1), stripPtrType(t2))
    case (false, true) => typeEquivalentPostIndexerCheck(t1, stripPtrType(t2))
    case (true, false) => typeEquivalentPostIndexerCheck(stripPtrType(t1), t2)
    case (false, false) => t1 == t2 || (isSpriteType(t1) && isSpriteType(t2))
}

def typeToRecursionSafeString(t: Type): String=
  t match
    case Struct(name, _, _) => name
    case _ => t.toString

//TODO if you want to test validity
def isCastValid(oldType: Type, newType: Type): Boolean = true
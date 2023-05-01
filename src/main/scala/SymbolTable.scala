package Grain
import Parser.ExpressionParser
import Utility.{Errors, ROMWord, Token, Type, Word, getTypeSize}

import scala.collection.mutable.*


sealed trait SymbolForm

case class Symbol(
                   name: String,
                   token: Token,
                   dataType: Type,
                   lineNumber: Int,
                   form: SymbolForm
                 ){
  def size: Int = Utility.getTypeSize(dataType)
}
object Symbol{
  def make(token: Token, dataType: Type, form: SymbolForm): Symbol =
    Symbol(token.lexeme, token, dataType, token.lineNumber, form)

  sealed trait StackStored(var stackOffset: Int = 0)
  //TODO chances are we need a form for member variables

  case class GlobalVariable(hiram: Boolean, var location: Int = 0) extends SymbolForm
  case class FunctionDefinition(isAllAssembly: Boolean) extends SymbolForm
  case class Argument() extends SymbolForm with StackStored(0)
  case class Variable() extends SymbolForm with StackStored(0)
  case class Data(values: List[String], var dataSize: Int, var dataBank: Int = 0) extends SymbolForm
}

class Scope(private val parentScope: Option[Scope], val symbolTable: SymbolTable, val mmio: Boolean){
  import scala.collection.mutable.ListBuffer
  private val symbolMap = Map.empty[String, Symbol]
  private var frameSize = 0
  private val children = Map.empty[Stmt.Statement, Scope]


  def printMap = symbolMap.map(println)

  def getReturnType: Type = parent.getReturnType

  def parent: Scope = parentScope match
    case Some(otherScope) => otherScope
    case None => throw new Exception("Can't exceed global scope")
  def getChild(statement: Stmt.Statement): Scope = children(statement)
  def hasChild(statement: Stmt.Statement): Boolean = children.contains(statement)
  def getChildOrThis(statement: Stmt.Statement): Scope =
    if hasChild(statement) then getChild(statement) else this

  def getSymbol(symbolName: String): Symbol =
    if(strictContains(symbolName)) symbolMap(symbolName) else parent.getSymbol(symbolName)

  def size: Int = frameSize
  def addToStack(form: Symbol.StackStored, symbolSize: Int) = {
    form.stackOffset = frameSize
    frameSize += symbolSize
  }
  def addGlobal(symbol: Symbol, form: Symbol.GlobalVariable, symbolSize: Int): Unit = throw new Exception("Can't add global to non global scope")

  def addSymbol(name: Token, symbol: Symbol, filename: String): Unit =
    symbolMap.contains(name.lexeme) match
      case false =>
        symbol.form match {
          case stored: Symbol.StackStored => addToStack(stored, symbol.size)
          case glob: Symbol.GlobalVariable => addGlobal(symbol, glob, symbol.size)
          case _ =>
        }
        symbolMap.addOne(name.lexeme, symbol)
      case true => throw Errors.SymbolRedefinition(filename, symbolMap(name.lexeme).token, name)

  def addSymbol(name: Token, varType: Utility.Type, form: SymbolForm, filename: String): Symbol = {
    val symbol = Symbol.make(name, varType, form)
    addSymbol(name, symbol, filename)
    symbol
  }
  def apply(index: String):Symbol =
    if(symbolMap.contains(index)){
      return symbolMap(index)
    }
    else {
      return parentScope match
        case None => throw new Exception("Cannot find symbol " ++ index)
        case Some(scope) => scope(index)
    }

  def filter(func: Symbol=>Boolean): List[Symbol] = symbolMap.map(
    (tup: (String, Symbol)) => {
      val (_, symbol) = tup
      symbol
    }
  ).filter(func(_)).toList

  def contains(index: String):Boolean =
    if(strictContains(index)) {
      true
    }
    else parentScope match
      case None => false
      case Some(scope) => scope.contains(index)

  def strictContains(index: String): Boolean = symbolMap.contains(index)
  def newChild(setAsMMIO: Boolean):Scope = Scope(Some(this), symbolTable, setAsMMIO || mmio)
  def linkStatementWithScope(parentStatement: Stmt.Statement, child: Scope): Stmt.Statement = {
    children.addOne(parentStatement, child)
    parentStatement
  }

  override def toString: String = {
    (for symbol <- symbolMap yield symbol.toString ++ "\n").toList.toString()
  }

  def getUncastTypeOf(expr: Expr.Expr): Type =
    expr match
      case Expr.Assign(target, _) =>
        target match
          case name: Utility.Token =>
            apply(name.lexeme).dataType
          case getter: Expr.Get =>
            getTypeOf(getter)
      case Expr.BooleanLiteral(_) => Utility.BooleanType()
      case Expr.BankLiteral(_) => Utility.DataBankIndex()
      case Expr.UnaryOp(_, e) => getTypeOf(e) //Could emit warning if trying to do a weird thing with types here
      case Expr.BinaryOp(op, left, _) =>
        op match
          case _ if Operation.Groups.ArithmeticTokens.contains(op) => Utility.Word()
          case _ if Operation.Groups.RelationalTokens.contains(op) => Utility.BooleanType()
          case _ if Operation.Groups.LogicalTokens.contains(op) => getTypeOf(left)
          case _ => throw new Exception("Not all binary operations are in a group")
      case Expr.NumericalLiteral(_) => Utility.Word()
      case Expr.StringLiteral(_) => Utility.StringLiteral()
      case Expr.Variable(name) => apply(name.lexeme).dataType
      case Expr.GetIndex(arrayExpr, _) =>
        arrayExpr match
          case Expr.Indirection(_) => getTypeOf(arrayExpr)
          case _ => Utility.stripPtrType(getTypeOf(arrayExpr))
      case Expr.SetIndex(_, to) => getTypeOf(to)
      case Expr.Indirection(e) =>
        val TST = getTypeOf(e)
        val Utility.Ptr(innerType) = getTypeOf(e)
        innerType
      case Expr.FunctionCall(funcExpr, _) =>
        getTypeOf(funcExpr) match
          case Utility.FunctionPtr(_, rt, _) => rt
          case _ => throw new Exception("It shouldn't be possible that a call expression doesn't call a function")
      case Expr.Get(left, name) =>
        getTypeOf(left) match
          case s@Utility.Struct(_, _, _) => s.typeof(name.lexeme)
          case t@_ => throw new Exception("It shouldn't be possible to get from a non-struct type")
      case Expr.GetAddress(e) =>
        Utility.Ptr(getTypeOf(e))
      case Expr.Grouping(internalExpr) => getTypeOf(internalExpr)
      case null => throw new Exception("Matching should be exhaustive")
  def getTypeOf(expr: Expr.Expr): Type =
    if(expr.castType.isDefined){
      expr.castType.get
    }
    else{
      getUncastTypeOf(expr)
    }
}

class FunctionScope(parentScope: Option[Scope], symbolTable: SymbolTable, mmio: Boolean) extends Scope(parentScope, symbolTable, mmio){
  private var returnType: Type = Utility.Empty()

  //You only know if it is MMIO after the definition has been parsed
  //Therefore when translating, this is here to store if function body should be mmio
  //and hence how to handle the stack
  var executesAsMMIO: Boolean = mmio

  override def getReturnType: Type = returnType
  def setReturnType(newReturnType: Type):Unit = returnType = newReturnType
}

def assemblyHexToInt(hex: String): Int ={
  val substr = hex.substring(1)
  substr.map(c => ExpressionParser.hexDigit(c)).foldLeft(0){
    (tot, next) =>
      println(tot.toString ++ "  " ++ next.toString)
      (tot << 4) + next
  }
}

class GlobalScope(symbolTable: SymbolTable) extends Scope(None, symbolTable, false){
  private var globalHeapPtr: Int = GlobalData.Config.globalsStart
  private var hiGlobalHeapPtr: Int = assemblyHexToInt(GlobalData.Config.wramTop)
  private var currentDataBank: Int = 0
  private var currentBankSize: Int = 0

  private var parsedFileSet: Set[String] = Set.empty[String]

  def newFunctionChild(isMMIO: Boolean): FunctionScope = FunctionScope(Some(this), symbolTable, isMMIO)

  private def stripFilename(filename: String): String = filename.split('/').last.split('\\').last
  def hasFileBeenParsed(filename: String): Boolean = parsedFileSet.contains(stripFilename(filename))
  def addParsedFile(filename: String): Unit = parsedFileSet = parsedFileSet.addOne(stripFilename(filename))
  

  def addData(name: Token, varType: Utility.Type, form: Symbol.Data, filename: String): Symbol = {
    val symbol = Symbol.make(name, varType, form)

    if(currentBankSize + form.dataSize >= GlobalData.snesData.bankSize){
      currentDataBank += 1
      currentBankSize = 0
    }

    form.dataBank = currentDataBank
    currentBankSize += form.dataSize

    addSymbol(name, symbol, filename)

    symbol
  }

  override def addToStack(form: Symbol.StackStored, symbolSize: Int) = throw new Exception("Can't add global variable to stack")
  override def addGlobal(symbol: Symbol, form: Symbol.GlobalVariable, symbolSize: Int): Unit = {
    if(!form.hiram) {
      form.location = globalHeapPtr
      globalHeapPtr += symbolSize
      val loLimit = assemblyHexToInt(GlobalData.Config.globalsLowLimit)
      if (globalHeapPtr > loLimit) {
        throw Errors.overflowedLowRam(symbol, loLimit, globalHeapPtr - symbolSize, symbolSize)
      }
    }
    else{
      hiGlobalHeapPtr -= symbolSize
      form.location = hiGlobalHeapPtr
      if(hiGlobalHeapPtr <= 0x1FFF){
        throw Errors.ranIntoStack(symbol)
      }
    }
  }
}

class SymbolTable{
  val globalScope = GlobalScope(this)
  val types: Map[String, Type] = Map(
    "word" -> Word(),
    "bool" -> Utility.BooleanType(),
    "rom_word" -> ROMWord(),
    "bit_depth" -> Utility.BitdepthType(),
    "data_bank" -> Utility.DataBankIndex(),
    "tile_data" -> Utility.GenericSprite(),
    "palette_data" -> Utility.Palette()
  )
}


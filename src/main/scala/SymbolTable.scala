package Grain
import Utility.{Errors, Token, Type, Word, getTypeSize}

import scala.collection.mutable.*


sealed trait SymbolForm

case class Symbol(
                   name: String,
                   token: Token,
                   dataType: Type,
                   size: Int,
                   lineNumber: Int,
                   form: SymbolForm
                 )
object Symbol{
  def make(token: Token, dataType: Type, form: SymbolForm): Symbol =
    Symbol(token.lexeme, token, dataType, getTypeSize(dataType), token.lineNumber, form)

  sealed trait StackStored(var stackOffset: Int = 0)

  case class GlobalVariable(var location: Int = 0) extends SymbolForm
  case class FunctionDefinition() extends SymbolForm
  case class Argument() extends SymbolForm with StackStored(0)
  case class Variable() extends SymbolForm with StackStored(0)
  case class Data(labelName: String) extends SymbolForm
}

class Scope(private val parentScope: Option[Scope], val symbolTable: SymbolTable){
  import scala.collection.mutable.ListBuffer
  private val symbolMap = Map.empty[String, Symbol]
  private var frameSize = 0
  private val children = Map.empty[Stmt.Statement, Scope]

  def getReturnType: Type = parent.getReturnType

  def parent: Scope = parentScope match
    case Some(otherScope) => otherScope
    case None => throw new Exception("Can't exceed global scope")
  def getChild(statement: Stmt.Statement): Scope = children(statement)
  def hasChild(statement: Stmt.Statement): Boolean = children.contains(statement)
  def getChildOrThis(statement: Stmt.Statement): Scope =
    if hasChild(statement) then getChild(statement) else this

  def size: Int = frameSize
  def addToStack(form: Symbol.StackStored, symbolSize: Int) = {
    form.stackOffset = frameSize
    frameSize += symbolSize
  }
  def addGlobal(form: Symbol.GlobalVariable, symbolSize: Int): Unit = throw new Exception("Can't add global to non global scope")

  def addSymbol(name: Token, symbol: Symbol): Unit =
    symbolMap.contains(name.lexeme) match
      case false =>
        symbol.form match {
          case stored: Symbol.StackStored => addToStack(stored, symbol.size)
          case glob: Symbol.GlobalVariable => addGlobal(glob, symbol.size)
          case _ =>
        }
        symbolMap.addOne(name.lexeme, symbol)
      case true => throw Errors.SymbolRedefinition(symbolMap(name.lexeme).token, name)

  def addSymbol(name: Token, varType: Utility.Type, form: SymbolForm): Symbol = {
    val symbol = Symbol.make(name, varType, form)
    addSymbol(name, symbol)
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
  def newChild():Scope = Scope(Some(this), symbolTable)
  def linkStatementWithScope(parentStatement: Stmt.Statement, child: Scope): Stmt.Statement = {
    children.addOne(parentStatement, child)
    parentStatement
  }

  override def toString: String = {
    (for symbol <- symbolMap yield symbol.toString ++ "\n").toList.toString()
  }

  private def getIndexTypeOf(arrayExpr: Expr.Expr): Type = {
    getTypeOf(arrayExpr) match
      case Utility.Ptr(to) => to
      case Utility.Array(of, _) => of
      case _ =>
        println(arrayExpr.toString)
        println(getTypeOf(arrayExpr).toString)
        throw new Exception("Cannot index given type '" ++ getTypeOf(arrayExpr).toString ++ "'")
  }

  def getTypeOf(expr: Expr.Expr): Type =
    expr match
      case Expr.Assign(name, _) => apply(name.lexeme).dataType
      case Expr.BooleanLiteral(_) => Utility.BooleanType()
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
      case Expr.GetIndex(arrayExpr, _) => getIndexTypeOf(arrayExpr)
      case Expr.SetIndex(_, to) => getTypeOf(to)
      case Expr.Indirection(e) =>
        val Utility.Ptr(innerType) = getTypeOf(e)
        innerType
      case Expr.FunctionCall(funcExpr, _) =>
        getTypeOf(funcExpr) match
          case Utility.FunctionPtr(_, rt) => rt
          case _ => throw new Exception("It shouldn't be possible that a call expression doesn't call a function")
      case Expr.Get(left, name) =>
        getTypeOf(left) match
          case s@Utility.Struct(_) => s.typeof(name.lexeme)
          case _ => throw new Exception("It shouldn't be possible to get from a non-struct type")
      case Expr.GetAddress(e) =>
        Utility.Ptr(getTypeOf(e))
      case Expr.Set(left, expr) => getTypeOf(left)
      case Expr.Grouping(internalExpr) => getTypeOf(internalExpr)
      case null => throw new Exception("Matching should be exhaustive")
}

class FunctionScope(parentScope: Option[Scope], symbolTable: SymbolTable) extends Scope(parentScope, symbolTable){
  private var returnType: Type = Utility.Empty()
  override def getReturnType: Type = returnType
  def setReturnType(newReturnType: Type):Unit = returnType = newReturnType
}

class GlobalScope(symbolTable: SymbolTable) extends Scope(None, symbolTable){
  private var globalHeapPtr: Int = 100
  def newFunctionChild(): FunctionScope = FunctionScope(Some(this), symbolTable)

  override def addToStack(form: Symbol.StackStored, symbolSize: Int) = throw new Exception("Can't add global variable to stack")
  override def addGlobal(form: Symbol.GlobalVariable, symbolSize: Int): Unit = {
    form.location = globalHeapPtr
    globalHeapPtr += 2
  }
}

class SymbolTable{
  val globalScope = GlobalScope(this)
  val types: Map[String, Type] = Map(
    "word" -> Word(),
    "bool" -> Utility.BooleanType()
  )
}


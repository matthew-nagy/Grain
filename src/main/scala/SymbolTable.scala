package Grain
import Utility.{Token, Type, Word, getTypeSize, Errors}

import scala.collection.mutable.*

enum DefinitionType:
  case NotDefined, Defined, Argument, FuncNotDefined, FuncDefined

case class Symbol(
                   name: String,
                   token: Token,
                   dataType: Type,
                   size: Int,
                   lineNumber: Int,
                   var definition: DefinitionType
                 )
object Symbol{
  def make(token: Token, dataType: Type, defined: DefinitionType): Symbol =
    Symbol(token.lexeme, token, dataType, getTypeSize(dataType), token.lineNumber, defined)
}

class Scope(private val parentScope: Option[Scope], val symbolTable: SymbolTable){
  import scala.collection.mutable.ListBuffer
  private val map = Map.empty[String, Int]
  private val symbols = ListBuffer.empty[(Symbol, Int)]
  private val children = Map.empty[Stmt.Statement, Scope]

  def getChild(statement: Stmt.Statement): Scope = children(statement)
  def hasChild(statement: Stmt.Statement): Boolean = children.contains(statement)
  def getChildOrThis(statement: Stmt.Statement): Scope =
    if hasChild(statement) then getChild(statement) else this

  def addSymbol(name: Token, symbol: Symbol): Unit =
    map.contains(name.lexeme) match
      case false =>
        val newOffset = getNewOffset
        map.addOne(name.lexeme, symbols.length)
        symbols.append((symbol, newOffset))
      case true => throw Errors.SymbolRedefinition(symbols(map(name.lexeme))(0).token, name)

  def addSymbol(name: Token, varType: Utility.Type, defined: DefinitionType): Unit = {
    val symbol = Symbol.make(name, varType, defined)
    addSymbol(name, symbol)
  }
  def apply(index: String):Symbol =
    if(map.contains(index)){
      symbols(map(index))(0)
    }
    else {
      parentScope match
        case None => throw new Exception("Cannot find symbol " ++ index)
        case Some(scope) => scope(index)
    }

  def getStackOffset(index: String): Int = {
    throw new Exception("The whole stack thing needs to be messed with rn")
    symbols(map(index))(1)
  }
  def contains(index: String):Boolean =
    if(strictContains(index)) {
      true
    }
    else parentScope match
      case None => false
      case Some(scope) => scope.contains(index)

  def strictContains(index: String): Boolean = map.contains(index)
  def newChild():Scope = Scope(Some(this), symbolTable)

  def linkStatementWithScope(parentStatement: Stmt.Statement, child: Scope): Stmt.Statement = {
    children.addOne(parentStatement, child)
    parentStatement
  }

  def getTypeOf(expr: Expr.Expr):Type =
    expr match
      case Expr.Assign(name, _) => apply(name.lexeme).dataType
      case Expr.BooleanLiteral(_) => Utility.BooleanType()
      case Expr.UnaryOp(_, e) => getTypeOf(e) //Could emit warning if trying to do a weird thing with types here
      case Expr.BinaryOp(op, left, _) =>
        op match
          case _ if Operation.Groups.ArithmeticTokens.contains(op) => Utility.Word()
          case _ if Operation.Groups.RelationalTokens.contains(op) => Utility.BooleanType()
          case _ if Operation.Groups.LogicalTokens.contains(op) => getTypeOf(left)
          case null => throw new Exception("Not all binary operations are in a group")
      case Expr.NumericalLiteral(_) => Utility.Word()
      case Expr.StringLiteral(_) => Utility.StringLiteral()
      case Expr.Variable(name) => apply(name.lexeme).dataType
      case Expr.GetIndex(arrayExpr, _) =>
        getTypeOf(arrayExpr) match
          case Utility.Ptr(to) => to
          case Utility.Array(of, _) => of
          case _ =>
            println(arrayExpr.toString)
            println(getTypeOf(arrayExpr).toString)
            throw new Exception("Cannot index given type '" ++ getTypeOf(arrayExpr).toString ++ "'")
      case Expr.Indirection(e) =>
        val Utility.Ptr(innerType) = getTypeOf(e)
        innerType
      case Expr.FunctionCall(funcExpr, _) =>
        getTypeOf(funcExpr) match
          case Utility.FunctionPtr(_, rt) => rt
          case _ => throw new Exception("It shouldn't be possible that a call expression doesn't call a function")
      case Expr.Get(left, name) =>
        getTypeOf(left) match
          case s @ Utility.Struct(_) => s.typeof(name.lexeme)
          case _ => throw new Exception("It shouldn't be possible to get from a non-struct type")
      case Expr.GetAddress(e) =>
        Utility.Ptr(getTypeOf(e))
      case Expr.Set(left, expr) => getTypeOf(left)
      case Expr.Grouping(internalExpr) => getTypeOf(internalExpr)
      case null => throw new Exception("Matching should be exhaustive")

  override def toString: String = {
    (for (symbol, _) <- symbols yield symbol.toString ++ "\n").toList.toString()
  }
  private def getNewOffset: Int = {
    if symbols.isEmpty then 0 else {
      val (lastSymbol, lastOffset) = symbols.last
      lastOffset + getTypeSize(lastSymbol.dataType)
    }
  }
}

class SymbolTable{
  val globalScope = Scope(None, this)
  val functions: Map[String, Scope] = Map()
  val types: Map[String, Type] = Map(
    "word" -> Word(),
    "bool" -> Utility.BooleanType()
  )
}

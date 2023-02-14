package Grain
import Utility.{Token, Type, Word, getTypeSize}

import scala.collection.mutable.*

case class Symbol(
                   name: String,
                   token: Token,
                   dataType: Type,
                   size: Int,
                   lineNumber: Int,
                   var defined: Boolean
                 )
object Symbol{
  def make(token: Token, dataType: Type, defined: Boolean): Symbol =
    Symbol(token.lexeme, token, dataType, getTypeSize(dataType), token.lineNumber, defined)
}

class Scope(private val parentScope: Option[Scope], val symbolTable: SymbolTable){
  private val map = Map.empty[String, Symbol]
  private val children = Map.empty[Stmt.Statement, Scope]

  def addSymbol(name: Token, varType: Utility.Type, defined: Boolean): Unit =
    map.contains(name.lexeme) match
      case false => map.addOne(name.lexeme, Symbol.make(name, varType, defined))
      case true => throw Errors.SymbolRedefinition(map(name.lexeme).token, name)
  def apply(index: String):Symbol =
    if(map.contains(index)){
      map(index)
    }
    else {
      parentScope match
        case None => throw new Exception("Cannot find symbol " ++ index)
        case Some(scope) => scope(index)
    }
  def contains(index: String):Boolean =
    if(map.contains(index)) {
      true
    }
    else parentScope match
      case None => false
      case Some(scope) => scope.contains(index)
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
      case Expr.FunctionCall(funcExpr, _) =>
        getTypeOf(funcExpr) match
          case Utility.FunctionPtr(_, rt) => rt
          case _ => throw new Exception("It shouldn't be possible that a call expression doesn't call a function")
      case Expr.Get(left, name) =>
        getTypeOf(left) match
          case s @ Utility.Struct(_) => s.typeof(name.lexeme)
          case _ => throw new Exception("It shouldn't be possible to get from a non-struct type")
      case Expr.Set(left, expr) => getTypeOf(left)
      case Expr.Grouping(internalExpr) => getTypeOf(internalExpr)
      case null => throw new Exception("Matching should be exhaustive")
}

class SymbolTable{
  val globalScope = Scope(None, this)
  val functions: Map[String, Scope] = Map()
  val types: Map[String, Type] = Map(
    "word" -> Word(),
    "bool" -> Utility.BooleanType()
  )
}

package Grain
package Stmt:

  import Utility.Token

  sealed trait Statement

  sealed trait TopLevel extends Statement

  case class Assembly(assembly: List[String]) extends Statement
  case class Block(statements: List[Statement]) extends Statement
  case class EmptyStatement() extends TopLevel
  case class Expression(expr: Expr.Expr) extends Statement
  case class For(
                  startStmt: Option[Statement], breakExpr: Option[Expr.Expr],
                  incrimentExpr: Option[Expr.Expr], body: Statement, lineNumber: Int
                ) extends Statement
  case class FunctionDecl(funcSymbol: Symbol, arguments: List[Symbol], body: Block) extends TopLevel

  case class Else(body: Statement) extends Statement
  case class If(condition: Expr.Expr, body: Statement, elseBranch: Option[Else], lineNumber: Int) extends Statement
  case class Return(value: Option[Expr.Expr]) extends Statement
  case class VariableDecl(assignment: Expr.Assign) extends TopLevel
  case class While(condition: Expr.Expr, body: Statement, lineNumber: Int) extends Statement

  case class Include(filename: Token) extends TopLevel
  case class Load(varName: Token, filename: Token) extends TopLevel

  def OptimiseStatement(stmt: Statement): Statement = throw new Exception("Haven't done yet lol")



package Grain
package Stmt:

  import Grain.Expr.StringLiteral
  import Grain.Expr.NumericalLiteral
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

  case class PaletteReference(referenceFilename: String, tileIndex: Int)
  case class Load(varName: Token, palleteName: Token, filename: Token, references: List[PaletteReference]) extends TopLevel

  def OptimiseStatement(stmt: Statement): Statement =
    stmt match
      case Assembly(_) => stmt
      case Block(statements) => Block(statements.map(OptimiseStatement))
      case Expression(expr) => Expression(Expr.OptimiseExpression(expr))
      case EmptyStatement() => EmptyStatement()
      case For(ss, be, ie, b, lineNumber) =>
        val optimisedBreakExpression = Expr.OptimiseExpression(be)
        if(optimisedBreakExpression.isDefined){
          if(optimisedBreakExpression.get == Expr.BooleanLiteral(false)){
            return EmptyStatement()
          }
        }
        For(OptimiseStatement(ss), optimisedBreakExpression, Expr.OptimiseExpression(ie), OptimiseStatement(b), lineNumber)
      case Else(body) => Else(OptimiseStatement(body))
      case If(cond, body, elseBranch, lineNumber) =>
        val optimisedCondition = Expr.OptimiseExpression(cond)
        optimisedCondition match
          case Expr.BooleanLiteral(true) => OptimiseStatement(body)
          case Expr.BooleanLiteral(false) =>
            elseBranch match
              case None => EmptyStatement()
              case Some(branch) => OptimiseStatement(branch)
          case _ => If(optimisedCondition, OptimiseStatement(body), OptimiseStatement(elseBranch).asInstanceOf[Option[Else]], lineNumber)
      case Return(value) => Return(Expr.OptimiseExpression(value))
      case VariableDecl(assignment) => VariableDecl(Expr.OptimiseExpression(assignment).asInstanceOf[Expr.Assign])
      case While(condition, body, lineNumber) =>
        val optimisedCondition = Expr.OptimiseExpression(condition)
        optimisedCondition match
          case Expr.BooleanLiteral(false) => EmptyStatement()
          case _ => While(optimisedCondition, OptimiseStatement(body), lineNumber)
      case _ => throw new Exception("Unhandled statement to be tree optimised parsed -> " ++ stmt.toString)


  def OptimiseStatement(stmt: Option[Statement]): Option[Statement] =
    stmt match
      case None => None
      case Some(s) => Some(OptimiseStatement(s))

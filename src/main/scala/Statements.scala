package Grain
package Stmt:

  import Grain.Expr.StringLiteral
  import Grain.Expr.NumericalLiteral
  import Utility.Token

  sealed trait Statement

  sealed trait TopLevel extends Statement

  case class Assembly(assembly: List[String]) extends Statement
  case class Block(statements: List[Statement], mmio: Boolean) extends Statement
  case class EmptyStatement() extends TopLevel
  case class Expression(expr: Expr.Expr) extends Statement
  case class For(
                  startStmt: Option[Statement], breakExpr: Option[Expr.Expr],
                  incrimentExpr: Option[Expr.Expr], body: Statement, lineNumber: Int, fileNumber: Int
                ) extends Statement
  case class FunctionDecl(funcSymbol: Symbol, arguments: List[Symbol], body: Block | Assembly, mmio: Boolean) extends TopLevel

  case class Else(body: Statement) extends Statement
  case class If(condition: Expr.Expr, body: Statement, elseBranch: Option[Else], lineNumber: Int, fileNumber: Int) extends Statement
  case class Return(value: Option[Expr.Expr]) extends Statement
  case class VariableDecl(assignment: Expr.Assign) extends TopLevel
  case class While(condition: Expr.Expr, body: Statement, lineNumber: Int, fileNumber: Int) extends Statement

  case class Include(filename: Token) extends TopLevel

  case class PaletteReference(referenceFilename: String, tileIndex: Int)
  case class LoadGraphics(varName: Token, palleteName: Token, filename: Token, references: List[PaletteReference]) extends TopLevel
  case class LoadData(varName: Token, filename: Token) extends TopLevel

  def OptimiseStatement(stmt: Statement): Statement =
    stmt match
      case Assembly(_) => stmt
      case Block(statements, mmio) => Block(statements.map(OptimiseStatement), mmio)
      case Expression(expr) => Expression(Expr.OptimiseExpression(expr))
      case EmptyStatement() => EmptyStatement()
      case For(ss, be, ie, b, lineNumber, fileNumber) =>
        val optimisedBreakExpression = Expr.OptimiseExpression(be)
        if(optimisedBreakExpression.isDefined){
          if(optimisedBreakExpression.get == Expr.BooleanLiteral(false)){
            return EmptyStatement()
          }
        }
        For(OptimiseStatement(ss), optimisedBreakExpression, Expr.OptimiseExpression(ie), OptimiseStatement(b), lineNumber, fileNumber)
      case Else(body) => Else(OptimiseStatement(body))
      case If(cond, body, elseBranch, lineNumber, fileNumber) =>
        val optimisedCondition = Expr.OptimiseExpression(cond)
        optimisedCondition match
          case Expr.BooleanLiteral(true) => OptimiseStatement(body)
          case Expr.BooleanLiteral(false) =>
            elseBranch match
              case None => EmptyStatement()
              case Some(branch) => OptimiseStatement(branch)
          case _ => If(optimisedCondition, OptimiseStatement(body), OptimiseStatement(elseBranch).asInstanceOf[Option[Else]], lineNumber, fileNumber)
      case Return(value) => Return(Expr.OptimiseExpression(value))
      case VariableDecl(assignment) => VariableDecl(Expr.OptimiseExpression(assignment).asInstanceOf[Expr.Assign])
      case While(condition, body, lineNumber, fileNumber) =>
        val optimisedCondition = Expr.OptimiseExpression(condition)
        optimisedCondition match
          case Expr.BooleanLiteral(false) => EmptyStatement()
          case _ => While(optimisedCondition, OptimiseStatement(body), lineNumber, fileNumber)
      case _ => throw new Exception("Unhandled statement to be tree optimised parsed -> " ++ stmt.toString)


  def OptimiseStatement(stmt: Option[Statement]): Option[Statement] =
    stmt match
      case None => None
      case Some(s) => Some(OptimiseStatement(s))

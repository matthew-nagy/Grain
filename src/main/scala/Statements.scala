package Grain
package Stmt:

  import Utility.Token

  sealed trait TopLevel
  sealed trait Statement

  case class Assembly(assembly: String) extends Statement
  case class Block(statements: List[Statement]) extends Statement
  case class EmptyStatement() extends Statement
  case class Expression(expr: Expr.Expr) extends Statement
  case class FunctionDecl(name: Token, arguments: List[Token], body: Option[Statement]) extends Statement with TopLevel
  case class If(condition: Expr.Expr, thenBranch: Statement, elseBranch: Option[Statement]) extends Statement
  case class Return(value: Option[Expr.Expr]) extends St
  atement
  case class VariableDecl(name: Token, initializer: Option[Expr.Expr]) extends Statement with TopLevel
  case class While(condition: Expr.Expr, body: Statement) extends Statement

  case class Include(filename: Token) extends TopLevel
  case class Load(varName: Token, filename: Token) extends TopLevel

  def OptimiseStatement(stmt: Statement): Statement =
    stmt match
      case Block(stataments) => Block(for s <- stataments yield OptimiseStatement(s))
      case Expression(expr) => Expression(Expr.OptimiseExpression(expr))
      case FunctionDecl(name, args, Some(body)) => FunctionDecl(name, args, Some(OptimiseStatement(body)))
      case fd @ FunctionDecl(_, _, None) => fd
      case If(condition, thenBranch, elseBranch) =>
        val optimisedExpr = Expr.OptimiseExpression(condition)
        optimisedExpr match
          //We know that we only need the statement, because the expression evaluated to numeric, therefore no side effects
          case Expr.NumericalLiteral(num) if num != 0 => OptimiseStatement(thenBranch)
          case Expr.NumericalLiteral(0) =>
            elseBranch match
              case Some(elseCode) => OptimiseStatement(elseCode)
              case None => EmptyStatement()
          case _ =>
            elseBranch match
              case Some(elseCode) => If(optimisedExpr, OptimiseStatement(thenBranch), Some(OptimiseStatement(elseCode)))
              case None => If(optimisedExpr, OptimiseStatement(thenBranch), None)

      case Return(Some(expression)) => Return(Some(Expr.OptimiseExpression(expression)))
      case VariableDecl(name, Some(initializer)) => VariableDecl(name, Some(Expr.OptimiseExpression(initializer)))
      case While(condition, body) => While(Expr.OptimiseExpression(condition), OptimiseStatement(body))
      case _ => stmt



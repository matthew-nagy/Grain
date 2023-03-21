package Grain

package Operation:

  import Grain.Operation.Binary.{GreaterEqual, LessEqual}

  enum Unary:
    case Minus, BooleanNegation, BitwiseNot



  enum Binary:
    case Add, Subtract, Multiply, Divide, Modulo,
         And, Or, Xor,
         Less, LessEqual, Greater, GreaterEqual, Equal, NotEqual,
         ShiftLeft, ShiftRight

  //Unfinished
  def applyOperation(op: Unary, value: Int): Int =
    op match
      case Unary.Minus => value * -1
      case Unary.BooleanNegation =>
        value match
          case 0 => 1
          case _ => 0
      case Unary.BitwiseNot => ~value
      case null => throw new RuntimeException  //Indirection and address getting should never get to the tree

  def applyOperation(op: Binary, left: Int, right: Int): Int =
    def asInt(result: Boolean): Int =
      if(result) 1
      else 0

    op match
      case Binary.Add => left + right
      case Binary.Subtract => left - right
      case Binary.Multiply => left * right
      case Binary.Divide => left / right
      case Binary.Modulo => left % right
      case Binary.And => left & right
      case Binary.Or => left | right
      case Binary.Xor => left ^ right
      case Binary.Less => asInt(left < right)
      case Binary.LessEqual => asInt(left <= right)
      case Binary.Greater => asInt(left > right)
      case Binary.GreaterEqual => asInt(left >= right)
      case Binary.Equal => asInt(left == right)
      case Binary.NotEqual => asInt(left != right)
      case Binary.ShiftLeft => left << right
      case Binary.ShiftRight => left >> right
      case null => throw new RuntimeException  //Just in case any illigal cases slipped into the tree

  object Groups{
    val ArithmeticTokens: Set[Binary] = Set(
      Binary.Add, Binary.Subtract, Binary.Multiply, Binary.Divide, Binary.Modulo,
      Binary.ShiftLeft, Binary.ShiftRight
    )
    val RelationalTokens: Set[Binary] = Set(
      Binary.Equal, Binary.NotEqual, Binary.Greater, Binary.GreaterEqual, Binary.Less, Binary.LessEqual
    )
    val LogicalTokens: Set[Binary] = Set(
      Binary.And, Binary.Or, Binary.Xor
    )

    val orderImportantOperations: Set[Operation.Binary] = Set(
      Operation.Binary.Subtract, Operation.Binary.Divide
    )
    
    val oppositeMap: Map[Binary, Binary] = Map(
      Binary.Less -> Binary.GreaterEqual, Binary.Greater -> Binary.LessEqual, Binary.Equal -> Binary.NotEqual,
      Binary.GreaterEqual -> Binary.Less, Binary.LessEqual -> Binary.Greater, Binary.NotEqual -> Binary.Equal
    )
  }


import Utility.{Token}

package Expr:

  import Utility.Word

  sealed trait Expr
  case class Assign(name: Token, arg: Expr) extends Expr
  case class BooleanLiteral(value: Boolean) extends Expr
  case class UnaryOp(op: Operation.Unary, arg: Expr) extends Expr

  case class BinaryOp(op: Operation.Binary, left: Expr, right: Expr) extends Expr
  case class NumericalLiteral(value: Int) extends Expr
  case class StringLiteral(value: String) extends Expr
  case class Indirection(expr: Expr) extends Expr
  case class Variable(name: Token) extends Expr
  case class FunctionCall(function: Expr, arguments: List[Expr]) extends Expr

  case class Get(left: Expr, name: Token) extends Expr
  case class GetAddress(expr: Expr) extends Expr

  case class GetIndex(of: Expr, by: Expr) extends Expr

  case class Set(left: Expr, right: Expr) extends Expr

  case class SetIndex(of: Expr, to: Expr) extends Expr
  case class Grouping(internalExpr: Expr) extends Expr

  def OptimiseExpression(expr: Expr): Expr =
    expr match
      case Assign(n, e) => Assign(n, OptimiseExpression(e))
      case Set(n, e) => Set(OptimiseExpression(n), OptimiseExpression(e))
      case u @ UnaryOp(_, _) => OptimiseUnaryExpression(u)
      case b @ BinaryOp(_, _, _) => OptimiseBinaryExpression(b)
      case FunctionCall(name, args) =>
        FunctionCall(name, for arg <- args yield OptimiseExpression(arg))
      case Indirection(GetAddress(e)) => OptimiseExpression(e)
      case Grouping(x) =>
        val optimised = OptimiseExpression(x)
        optimised match
          case nl@NumericalLiteral(_) => nl
          case sl@StringLiteral(_) => sl
          case bl@BooleanLiteral(_) => bl
          case v@Variable(_) => v
          case _ => Grouping(optimised)
      case _ => expr

  def OptimiseExpression(expr: Option[Expr]): Option[Expr] =
    expr match
      case None => None
      case Some(e) => Some(OptimiseExpression(e))

  //TODO recognise powers to 2 and not just 2 on its own for some of these
  def OptimiseBinaryExpression(expr: BinaryOp): Expr =
    val BinaryOp(op, left, right) = expr
    val treeOptimisedExpr = BinaryOp(op, OptimiseExpression(left), OptimiseExpression(right))
    /*There are some cases unconsidered
    0 - val is just negated val
    val - 0 and val + 0 and 0 + val are just val
    mult 1 and mult 0 is obs
    Powers of 2, rather than just 2 should cause a shift
    other shit*/
    import Operation.{Unary as UOp, Binary as BOp}
    treeOptimisedExpr match
      case BinaryOp(op, NumericalLiteral(val1), NumericalLiteral(val2)) =>
        NumericalLiteral(Operation.applyOperation(op, val1, val2))
      case BinaryOp(BOp.Add, x, y) if x == y => BinaryOp(BOp.ShiftLeft, x, NumericalLiteral(1))
      case BinaryOp(BOp.Subtract, x, y) if x == y => BinaryOp(BOp.ShiftRight, x, NumericalLiteral(1))
      case BinaryOp(BOp.Multiply, x, NumericalLiteral(2)) => BinaryOp(BOp.ShiftLeft, x, NumericalLiteral(2))
      case BinaryOp(BOp.Multiply, NumericalLiteral(2), x) => BinaryOp(BOp.ShiftLeft, x, NumericalLiteral(2))

      case BinaryOp(BOp.Divide, x, NumericalLiteral(2)) => BinaryOp(BOp.ShiftRight, x, NumericalLiteral(2))
      case _ => treeOptimisedExpr

  def OptimiseUnaryExpression(expr: UnaryOp): Expr =
    val UnaryOp(op, e) = expr
    val treeOptimisedExpr = UnaryOp(op, OptimiseExpression(e))

    import Operation.Unary as UOp
    treeOptimisedExpr match
      case UnaryOp(UOp.BooleanNegation, UnaryOp(UOp.BooleanNegation, e)) => e
      case UnaryOp(UOp.Minus, UnaryOp(UOp.Minus, e)) => e
      case UnaryOp(UOp.BitwiseNot, UnaryOp(UOp.BitwiseNot, e)) => e
      case UnaryOp(op, NumericalLiteral(num)) => NumericalLiteral(Operation.applyOperation(op, num))
      case _ => treeOptimisedExpr

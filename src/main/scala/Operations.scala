package Grain

package Operation:
  enum Unary:
    case Minus, Absolute, BooleanNegation, Xor, GetAddress, Indirection, Increment, Decrement

  enum Binary:
    case Add, Subtract, Multiply, Divide, Modulo,
         And, Or, Xor,
         Less, LessEqual, Greater, GreaterEqual, Equal, NotEqual,
         ShiftLeft, ShiftRight

  //Unfinished
  def applyOperation(op: Unary, value: Int): Int =
    op match
      case Unary.Minus => value * -1
      case Unary.Absolute => value.abs
      case Unary.BooleanNegation =>
        value match
          case 0 => 1
          case _ => 0
      case Unary.Xor => ~value
      case Unary.Increment => value + 1
      case Unary.Decrement => value - 1
      case _ => throw new RuntimeException  //Indirection and address getting should never get to the tree

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
      case _ => throw new RuntimeException  //Just in case any illigal cases slipped into the tree


package Expr:
  sealed trait Expr
  case class Assign(name: String, arg: Expr) extends Expr
  case class UnaryOp(op: Operation.Unary, arg: Expr) extends Expr
  case class BinaryOp(op: Operation.Binary, left: Expr, right: Expr) extends Expr
  case class NumericalLiteral(value: Int) extends Expr
  case class StringLiteral(value: String) extends Expr
  case class Variable(name: String) extends Expr
  case class FunctionCall(name: String, arguments: List[Expr]) extends Expr
  case class Grouping(internalExpr: Expr) extends Expr

/*

R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);

*/

  def OptimiseExpression(expr: Expr): Expr =
    expr match
      case Assign(n, e) => Assign(n, OptimiseExpression(e))
      case u @ UnaryOp(_, _) => OptimiseUnaryExpression(u)
      case b @ BinaryOp(_, _, _) => OptimiseBinaryExpression(b)
      case FunctionCall(name, args) =>
        FunctionCall(name, for arg <- args yield OptimiseExpression(arg))
      case Grouping(e) => Grouping(OptimiseExpression(e))
      case _ => expr

  def OptimiseBinaryExpression(expr: BinaryOp): Expr =
    val BinaryOp(op, left, right) = expr
    val treeOptimisedExpr = BinaryOp(op, OptimiseExpression(left), OptimiseExpression(right))

    /*
    There are some cases unconsidered
    0 - val is just negated val
    val - 0 and val + 0 and 0 + val are just val
    mult 1 and mult 0 is obs
    other shit
    */

    import Operation.{Unary as UOp, Binary as BOp}
    treeOptimisedExpr match
      case BinaryOp(op, NumericalLiteral(val1), NumericalLiteral(val2)) =>
        NumericalLiteral(Operation.applyOperation(op, val1, val2))
      case BinaryOp(BOp.Add, x, y) if x == y => BinaryOp(BOp.ShiftLeft, x, NumericalLiteral(2))

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
      case UnaryOp(UOp.Absolute, UnaryOp(UOp.Absolute, e)) => UnaryOp(UOp.Absolute, e)
      case UnaryOp(UOp.Absolute, UnaryOp(UOp.Minus, e)) => UnaryOp(UOp.Absolute, e)
      case UnaryOp(UOp.Xor, UnaryOp(UOp.Xor, e)) => e
      case UnaryOp(UOp.Indirection, UnaryOp(UOp.GetAddress, e)) => e
      case UnaryOp(op, NumericalLiteral(num)) => NumericalLiteral(Operation.applyOperation(op, num))
      case _ => treeOptimisedExpr


object Test{

  def main(args: Array[String]): Unit = {
    val expressions = List(
      Expr.Assign("Ham salad",
        Expr.BinaryOp(
          Operation.Binary.Multiply,
          Expr.NumericalLiteral(4),
          Expr.NumericalLiteral(2)
        ))
    )

    for e <- expressions do {
      println(e)
      println(Expr.OptimiseExpression(e))
    }
  }
}
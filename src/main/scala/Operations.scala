package Grain

package Operation:

  import Grain.Operation.Binary.{GreaterEqual, LessEqual}

  enum Unary:
    case Minus, BooleanNegation, BitwiseNot, Abs



  enum Binary:
    case Add, Subtract,
          Multiply, Multiply8Bit,
          Divide, Divide8Bit,
          Modulo, Modulo8Bit,
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
      case Binary.Multiply8Bit => left * (right & 0xFF)
      case Binary.Divide => left / right
      case Binary.Divide8Bit => left / (right & 0xFF)
      case Binary.Modulo => left % right
      case Binary.Modulo8Bit => left % (right & 0xFF)
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
      Binary.Add, Binary.Subtract, Binary.Multiply, Binary.Multiply8Bit,
      Binary.Divide, Binary.Divide8Bit,
      Binary.Modulo, Binary.Modulo8Bit,
      Binary.ShiftLeft, Binary.ShiftRight
    )
    val RelationalTokens: Set[Binary] = Set(
      Binary.Equal, Binary.NotEqual, Binary.Greater, Binary.GreaterEqual, Binary.Less, Binary.LessEqual
    )
    val LogicalTokens: Set[Binary] = Set(
      Binary.And, Binary.Or, Binary.Xor
    )

    val orderImportantOperations: Set[Operation.Binary] = Set(
      Operation.Binary.Subtract,
      Operation.Binary.Modulo, Operation.Binary.Modulo8Bit,
      Operation.Binary.Divide, Operation.Binary.Divide8Bit,
      Operation.Binary.Multiply, Operation.Binary.Multiply8Bit
    )
    
    val oppositeMap: Map[Binary, Binary] = Map(
      Binary.Less -> Binary.GreaterEqual, Binary.Greater -> Binary.LessEqual, Binary.Equal -> Binary.NotEqual,
      Binary.GreaterEqual -> Binary.Less, Binary.LessEqual -> Binary.Greater, Binary.NotEqual -> Binary.Equal
    )

    val commutative: Set[Binary] = Set(
      Binary.Add, Binary.And, Binary.Or, Binary.Xor
    )
  }


import Utility.{Token}

package Expr:

  import Utility.Word

  sealed trait Expr{
    private var alteredTypeInformation : Option[Utility.Type] = None
    def castType: Option[Utility.Type] = alteredTypeInformation
    def castToType(newType: Utility.Type): Unit = alteredTypeInformation = Some(newType)
  }
  case class Assign(name: Token | Get, arg: Expr) extends Expr
  case class BankLiteral(intermediateValue: Int) extends Expr
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

  case class SetIndex(of: Expr, to: Expr) extends Expr
  case class Grouping(internalExpr: Expr) extends Expr

  def OptimiseExpression(expr: Expr): Expr =
    expr match
      case Assign(n, e) if n.isInstanceOf[Token] => Assign(n, OptimiseExpression(e))
      case Assign(getter, e) if getter.isInstanceOf[Get] => Assign(OptimiseExpression(getter.asInstanceOf[Get]).asInstanceOf[Get], OptimiseExpression(e))
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
    other stuff*/
    //Everything to do with other versions of multiplication and division too
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

//  def ExecutePreprocessExpressions(expr: Expr, scope: Scope): Expr = {
//    def rcurs(rExpr: Expr) = ExecutePreprocessExpressions(rExpr, scope)
//    expr match
//      case Assign(t, arg) if t.isInstanceOf[Token] => Assign(t, rcurs(arg))
//      case Assign(Get(left, token), arg) => Assign(Get(rcurs(left), token), rcurs(arg))
//      case UnaryOp(op, arg) => UnaryOp(op, rcurs(arg))
//      case BinaryOp(op, left, right) => BinaryOp(op, rcurs(left), rcurs(right))
//      case Indirection(expr) => Indirection(rcurs(expr))
//      case FunctionCall(function, arguments) => FunctionCall(rcurs(function), arguments.map(rcurs))
//      case Get(left, name) => Get(rcurs(left), name)
//      case GetAddress(inner) => GetAddress(rcurs(inner))
//      case GetIndex(of, by) => GetIndex(rcurs(of), rcurs(by))
//      case SetIndex(of, to) => SetIndex(rcurs(of), rcurs(to))
//      case Grouping(internalExpr) => rcurs(internalExpr)
//  }
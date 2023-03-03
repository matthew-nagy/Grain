package TreeWalker

import Grain.Expr.*
import Grain.*
import Utility.{Token, TokenType}

object ExpressionTranslator {
  case class StackLocation(address: Address, toGetThere: IRBuffer)
  case class AccumulatorLocation(toGetThere: IRBuffer)

  private def translateFunctionCall(function: Expr.Expr, arguments: List[Expr.Expr], scope: TranslatorScope): AccumulatorLocation = {
    val buffer = IRBuffer().append(
      arguments.map(
        getFromAccumulator(_, scope).toGetThere
          .append(IR.PushRegister(AReg()))
          .toList
      ).foldLeft(IRBuffer().toList)(_ ::: _)
    )
    function match
      case Expr.Variable(funcToken) =>AccumulatorLocation(
        buffer.append(IR.JumpLongSaveReturn(Label("func_" ++ funcToken.lexeme ++ "_l" ++ funcToken.lineNumber.toString)))
      )
      case _ => throw new Exception("Cannot call type at this time")

    val stackSize: Int = arguments.map(e => Utility.getTypeSize(scope.inner.getTypeOf(e))).toList.foldLeft(0)(_+_)
    buffer.append(
      IR.Load(Immediate(stackSize), AReg()) :: IR.PushRegister(AReg()) :: IR.TransferToAccumulator(StackPointerReg()) ::
        IR.SetCarry() :: IR.SubtractCarry(StackRelative(1)) :: IR.TransferAccumulatorTo(StackPointerReg()) :: Nil
    )
    AccumulatorLocation(buffer)
  }
  def getFromAccumulator(expr: Expr.Expr, scope: TranslatorScope): AccumulatorLocation = {
    val result: AccumulatorLocation | StackLocation = expr match
      case Assign(varToken, arg) => getFromStack(expr, scope) //The same in both cases
      case BooleanLiteral(value) => AccumulatorLocation(
        loadImmediate(AReg(), if(value) 1 else 0)
      )
      case UnaryOp(op, arg) =>
        val toGetArg = getFromAccumulator(arg, scope)
        val afterArg = op match
          case Operation.Unary.BitwiseNot => getBitwiseNotA
          case Operation.Unary.Minus => getBitwiseNotA.append(IR.ClearCarry() :: IR.AddCarry(Immediate(1)) :: Nil)
          case Operation.Unary.BooleanNegation =>
            //Compare if 0. If it itsn't 0, set to 0 then go to the end. Otherwise
            //jump to setting it to 1, then fall through
            IRBuffer().append(
              IR.Compare(Immediate(0), AReg()) :: IR.BranchIfEqual(Label("+")) ::
                IR.Load(Immediate(0), AReg()) :: IR.BranchShort(Label("++")) ::
                IR.PutLabel(Label("+")) :: IR.Load(Immediate(1), AReg()) :: IR.PutLabel(Label("++")) ::
                Nil
            )
        AccumulatorLocation(toGetArg.toGetThere.append(afterArg))
      case BinaryOp(op, left, right) =>
        scope.rememberStackLocation()
        val leftToStack = getFromStack(left, scope)
        val rightToAccumulator = getFromAccumulator(right, scope)
        val buffer = leftToStack.toGetThere.append(rightToAccumulator.toGetThere)
        buffer.append(op match
          case Operation.Binary.Add => IR.ClearCarry() :: IR.AddCarry(leftToStack.address) :: Nil
          case Operation.Binary.Subtract => IR.SetCarry() :: IR.SubtractCarry(leftToStack.address) :: Nil
          case Operation.Binary.And => IR.AND(leftToStack.address) :: Nil
          case Operation.Binary.Or => IR.ORA(leftToStack.address) :: Nil
          case Operation.Binary.Xor => IR.EOR(leftToStack.address) :: Nil

          case _ => throw new Exception("Binary operation not supported yet")
        )
        buffer.append(scope.getFixStackDecay())
        AccumulatorLocation(buffer)
      case FunctionCall(function, arguments) => translateFunctionCall(function, arguments, scope)
      case NumericalLiteral(value) => AccumulatorLocation(loadImmediate(AReg(), value))
      case Indirection(expr) =>
        expr match
          case Variable(token) => AccumulatorLocation(IRBuffer().append(
            IR.Load(scope.getAddress(token.lexeme), XReg()) ::
              IR.Load(DirectIndexed(0, XReg()), AReg()) :: Nil
          ))
          case _ => throw new Exception("Cannot perform indirection on " ++ expr.toString)
      case Variable(token) => AccumulatorLocation(
        IRBuffer().append(IR.Load(scope.getAddress(token.lexeme), AReg()))
      )
      case GetAddress(expr) =>
        expr match
//          case Variable(token) => AccumulatorLocation(IRBuffer().append(
//            IR.TransferToAccumulator(StackPointerReg()) :: IR.ClearCarry() ::
//              IR.AddCarry(Immediate(scope.getStackOffset(token.lexeme))) :: Nil
//          ))
          case _ => throw new Exception("Cannot get address of " ++ expr.toString)
      case Grouping(internalExpr) => getFromAccumulator(internalExpr, scope)
      case _ =>
        AccumulatorLocation(new IRBuffer())
    result match
      case stackLocation: StackLocation => AccumulatorLocation(stackLocation.toGetThere)
      case accumulatorLocation: AccumulatorLocation => accumulatorLocation
  }

  //Can cause stack decay
  def getFromStack(expr: Expr.Expr, scope: TranslatorScope): StackLocation = {
    def stackFromAccumulator(expr: Expr.Expr, scope: TranslatorScope): StackLocation = {
      val intoAccumulator = getFromAccumulator(expr, scope)
      scope.push()
      StackLocation(StackRelative(0),
        intoAccumulator.toGetThere.append(IR.PushRegister(AReg()))
      )
    }
    val toGetToStack = new IRBuffer
    expr match
      case Assign(varToken, arg) =>
        val intoAccumulator = getFromAccumulator(arg, scope)
        toGetToStack.append(intoAccumulator.toGetThere)
        val targetAddress = scope.getAddress(varToken.lexeme)
        toGetToStack.append(IR.Store(targetAddress, AReg()))
        StackLocation(targetAddress, toGetToStack)

      case UnaryOp(_, _) => stackFromAccumulator(expr, scope)
      case BinaryOp(_, _, _) => stackFromAccumulator(expr, scope)
      case NumericalLiteral(_) => stackFromAccumulator(expr, scope)
      case Indirection(_) => stackFromAccumulator(expr, scope)//May be improvable later
      case Variable(name) => StackLocation(scope.getAddress(name.lexeme), IRBuffer())
      case GetAddress(_) => stackFromAccumulator(expr, scope)
      case Grouping(internalExpr) => getFromStack(internalExpr, scope)
      case _ => throw new Exception("Not handled in stack yet (" ++ expr.toString ++ ")")
  }

  private def loadImmediate(reg: TargetReg, value: Int): IRBuffer =
    IRBuffer().append(IR.Load(Immediate(value), reg))


  private def getBitwiseNotA: IRBuffer =
    IRBuffer().append(IR.EOR(Direct(0)))
}

object EXPTranslatorMain{
  def main(args: Array[String]): Unit = {
    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/ExpressionParserTest.txt"))
    val symbolTable = new SymbolTable

    symbolTable.globalScope.addSymbol(
      Token(TokenType.Identifier, "a", 0), Utility.Word(), Symbol.Variable()
    )
    symbolTable.globalScope.addSymbol(
      Token(TokenType.Identifier, "b", 0), Utility.Word(), Symbol.Variable()
    )

    while(tokenBuffer.peekType != TokenType.EndOfFile){
      val exp = Parser.ExpressionParser.parseOrThrow(symbolTable.globalScope, tokenBuffer)
      println(exp)
      println(ExpressionTranslator.getFromAccumulator(exp, TranslatorScope(symbolTable.globalScope)))
    }
  }
}

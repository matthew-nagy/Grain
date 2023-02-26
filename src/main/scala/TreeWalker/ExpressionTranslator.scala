package TreeWalker

import Grain.Expr.*
import Grain.*
import Utility.{Token, TokenType}

object ExpressionTranslator {
  case class StackLocation(address: Address, toGetThere: IRBuffer)
  case class AccumulatorLocation(toGetThere: IRBuffer)

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
                IR.PutLabel("+") :: IR.Load(Immediate(1), AReg()) :: IR.PutLabel("++") ::
                Nil
            )
        AccumulatorLocation(toGetArg.toGetThere.append(afterArg))
      case BinaryOp(op, left, right) =>
        scope.rememberStackLocation()
        val leftToStack = getFromStack(left, scope)
        val rightToAccumulator = getFromAccumulator(right, scope)
        val buffer = leftToStack.toGetThere.append(rightToAccumulator.toGetThere)
        op match
          case Operation.Binary.Add => buffer.append(
              IR.ClearCarry() :: IR.AddCarry(leftToStack.address) :: Nil
            )
          case Operation.Binary.Subtract => buffer.append(
              IR.SetCarry() :: IR.SubtractCarry(leftToStack.address) :: Nil
            )
          case _ => throw new Exception("Binary operation not supported yet")
        buffer.append(scope.getFixStackDecay())
        AccumulatorLocation(buffer)
      case NumericalLiteral(value) => AccumulatorLocation(loadImmediate(AReg(), value))
      case Variable(token) => AccumulatorLocation(
        IRBuffer().append(IR.Load(scope.getStackAddress(token.lexeme), AReg()))
      )
      case _ =>
        AccumulatorLocation(new IRBuffer())
    result match
      case stackLocation: StackLocation => AccumulatorLocation(stackLocation.toGetThere)
      case accumulatorLocation: AccumulatorLocation => accumulatorLocation
  }

  //Can cause stack decay
  def getFromStack(expr: Expr.Expr, scope: TranslatorScope): StackLocation = {
    val toGetToStack = new IRBuffer
    expr match
      case Assign(varToken, arg) =>
        val intoAccumulator = getFromAccumulator(arg, scope)
        toGetToStack.append(intoAccumulator.toGetThere)
        val targetAddress = scope.getStackAddress(varToken.lexeme)
        toGetToStack.append(IR.Store(targetAddress, AReg()))
        StackLocation(targetAddress, toGetToStack)
      case NumericalLiteral(value) =>
        scope.push()
        StackLocation(StackRelative(0),
          loadImmediate(AReg(), value).append(IR.PushRegister(AReg()))
        )
      case Variable(name) => StackLocation(scope.getStackAddress(name.lexeme), IRBuffer())
      case _ => throw new Exception("Not handled in stack yet")
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
      println(ExpressionTranslator.getFromAccumulator(exp, TranslatorScope(symbolTable.globalScope)))
    }
  }
}

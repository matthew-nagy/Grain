package TreeWalker

import Grain.Expr.*
import Grain.*
import Utility.{Token, TokenType}

object Indexing{

  private def maybeGetPowerOf2(value: Int): Option[Int] = {
    var result: Option[Int] = None
    var currentValue = value
    var currentPowerOf2 = 0
    while currentValue > 0 do{
      currentValue = currentValue >> 1
      currentPowerOf2 += 1
      if((currentValue & 1) > 0){
        if(result.isDefined){//Already found a bit, can't be a power of 2
          return None
        }
        else{
          result = Some(currentPowerOf2)
        }
      }
    }

    result
  }

  private def getInternalSizeOfArray(ar: Expr.Expr, scope: Scope): Int =
    Utility.getTypeSize(Utility.stripPtrType(scope.getTypeOf(ar)))

  private def correctAccumulatorIndexByType(indexing: Expr.Expr, scope: Scope): List[IR.Instruction] = {
    val size = getInternalSizeOfArray(indexing, scope)
    size match
      case 1 => List.empty[IR.Instruction]
      case x if maybeGetPowerOf2(x).isDefined =>
        (for i <- Range(0, maybeGetPowerOf2(x).get) yield IR.ShiftLeft(AReg())).toList
      case _ =>
        throw new Exception("Can't multiply yet. Size of " ++ size.toString ++ " isn't possible")//TODO
  }

  private def generateFetchIndexIR(index: Expr.Expr, list: Expr.Expr, scope: TranslatorScope): List[IR.Instruction] = {
    val indexIntoA = ExpressionTranslator.getFromAccumulator(index, scope).toGetThere.toList
    val indexCorrection = correctAccumulatorIndexByType(list, scope.inner)
    indexIntoA ::: indexCorrection
  }

  private def getPutFromAccumulatorToDesiredReg(into: TargetReg): List[IR.Instruction] = {
    into match
      case AReg() => List.empty[IR.Instruction]
      case XReg() => IR.TransferToX(AReg()) :: Nil
      case YReg() => IR.TransferToY(AReg()) :: Nil
  }
  private def getAddressOfVariableInto(name: Token, into: TargetReg, scope: TranslatorScope, addressMapper: Int => Int = num => num): List[IR.Instruction] = {
    val address = scope.getAddress(name.lexeme)
    address match
      case Direct(value) =>
        IR.Load(Immediate(addressMapper(value)), into)
          .addComment("Load in the global known value") :: Nil
      case StackRelative(offset) =>

        val getStackAddress = IR.TransferToAccumulator(StackPointerReg()).addComment("Get address of stack variable") ::
          IR.ClearCarry() :: IR.AddCarry(Immediate(addressMapper(offset))) :: getPutFromAccumulatorToDesiredReg(into)
        getStackAddress
  }
  private def getAddressOfIndexInto(of: Expr.Expr, by: Expr.Expr, into: TargetReg, scope: TranslatorScope): List[IR.Instruction] = {
    of match
      case Variable(name) if scope.getSymbol(name.lexeme).dataType.isInstanceOf[Utility.Array]=>
        scope.getAddress(name.lexeme) match
          case Direct(location)=>//Direct address instruction
            val fetchIndex = generateFetchIndexIR(by, of, scope)
            val addingLocation = IR.ClearCarry() :: IR.AddCarry(Immediate(location)).addComment("Add global address to index") :: Nil

            fetchIndex ::: addingLocation ::: getPutFromAccumulatorToDesiredReg(into)
          case _ =>
            val getAddressOfVariable = getAddressOfVariableInto(name, AReg(), scope)
            val pushAddress = IR.PushRegister(AReg()) :: Nil
            scope.push()
            val fetchIndex = generateFetchIndexIR(by, of, scope)
            val addVariableAddressAndPopAddress = IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(XReg()) :: Nil
            scope.pop()
            getAddressOfVariable ::: pushAddress ::: fetchIndex ::: addVariableAddressAndPopAddress ::: getPutFromAccumulatorToDesiredReg(into)
      case Variable(name) if scope.getSymbol(name.lexeme).dataType.isInstanceOf[Utility.Ptr] =>
        val indexToAccumulator = generateFetchIndexIR(by, of, scope)
        val addPointerValueToAccumulator = IR.ClearCarry() :: IR.AddCarry(scope.getAddress(name.lexeme)) :: Nil
        indexToAccumulator ::: addPointerValueToAccumulator ::: getPutFromAccumulatorToDesiredReg(into)
      case GetAddress(expr) =>
        expr match
          //Probably done for type reasons, just run again
          case expr: Variable => getAddressOfIndexInto(expr, by, into, scope)
          case _ => throw new Exception(expr.toString ++ " indexed doesn't make any sense")
      case GetIndex(innerArray, innerIndex) =>
        val getIndex = generateFetchIndexIR(by, of, scope)
        val pushIndexToStack = IR.PushRegister(AReg()) :: Nil
        scope.push()
        val getAddressOfTarget = getAddressOfIndexInto(innerArray, innerIndex, AReg(), scope)
        val addIndexToTarget = IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: Nil
        val popAddress = IR.PopDummyValue(XReg()) :: Nil
        scope.pop()

        getIndex :::pushIndexToStack ::: getAddressOfTarget ::: addIndexToTarget ::: popAddress ::: getPutFromAccumulatorToDesiredReg(into)
      case FunctionCall(function, arguments)=>
        throw new Exception("Can't do this yet") //TODO, thinking of just getting index then adding function result to it
      case _ => throw new Exception("Can't index expression " ++ of.toString)
  }
  def getAddressInto(of: Expr.Expr, into: TargetReg, scope: TranslatorScope): IRBuffer = {
    val list = of match
      case Variable(name) =>
        getAddressOfVariableInto(name, into, scope)
      case GetIndex(indexOf, indexedBy) =>
        getAddressOfIndexInto(indexOf, indexedBy, into, scope)
    IRBuffer().append(list)
  }

  //This could be changed to work with being indexed by a literal, or maybe a second version made
  //TODO add a getIndex case
  def getIndexValue(of: Expr.Expr, by: Expr.Expr, scope: TranslatorScope): IRBuffer = {
    val list: List[IR.Instruction] = of match
      //Indexing an array
      case Variable(name) if scope.getSymbol(name.lexeme).dataType.isInstanceOf[Utility.Array] =>
        val addressOfVariable = scope.getAddress(name.lexeme)
        addressOfVariable match
          case Direct(location) =>
            val indexToA = generateFetchIndexIR(by, of, scope)
            val moveIndexToXAndLoad = IR.TransferToX(AReg()) :: IR.Load(DirectIndexed(location, XReg()), AReg()) :: Nil
            indexToA ::: moveIndexToXAndLoad
          case _ =>
            val addressToX = getAddressOfIndexInto(of, by, XReg(), scope)
            val getIndexedValueToA = IR.Load(DirectIndexed(0, XReg()), AReg()) :: Nil
            addressToX ::: getIndexedValueToA
      //Indexing a pointer
      case Variable(name) if scope.getSymbol(name.lexeme).dataType.isInstanceOf[Utility.Ptr] =>
        val indexToA = generateFetchIndexIR(by, of, scope)
        val getValueIntoA = scope.getAddress(name.lexeme) match
          case Direct(location) =>
            //Add the value of the pointer to the index we already have, add to x, then do an indexed load
            IR.ClearCarry() :: IR.AddCarry(Direct(location)) :: IR.TransferToX(AReg()) :: IR.Load(DirectIndexed(0, XReg()), AReg()) :: Nil
          case StackRelative(offset) =>
            //We can put the index in y and try a stack relative indirect indexed by y
            IR.TransferToY(AReg()) :: IR.Load(StackRelativeIndirectIndexed(offset, YReg()), AReg()) :: Nil
        indexToA ::: getValueIntoA

      case GetAddress(subExpr) => getIndexValue(subExpr, by, scope).toList
      case GetIndex(_, _)=>
        val pushAddressToStack = getAddressInto(of, AReg(), scope).append(IR.PushRegister(AReg())).toList
        scope.push()
        val fetchIndex = generateFetchIndexIR(by, of, scope)
        val addIndicesAndPopAddress = IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(XReg()) :: Nil
        scope.pop()
        val transferToXThenIndex = IR.TransferToX(AReg()) :: IR.Load(DirectIndexed(0, XReg()), AReg()) :: Nil

        pushAddressToStack ::: fetchIndex ::: addIndicesAndPopAddress ::: transferToXThenIndex
      case FunctionCall(function, arguments) => //TODO
        throw new Exception("Can't index function call results yet")
      case _ => throw new Exception(of.toString ++ " indexed by " ++ by.toString ++ " doesn't make any sense")

    IRBuffer().append(list)
  }
}

object ExpressionTranslator {
  case class StackLocation(address: Address, toGetThere: IRBuffer)
  case class AccumulatorLocation(toGetThere: IRBuffer)

  private def pushArgument(arg: Expr.Expr, scope: TranslatorScope): List[IR.Instruction] = {
    //If you should do anything other than just get the value in A, here is where to do it
    val specialCaseResult: Option[List[IR.Instruction]] = arg match
      case Variable(name) =>
        val varSymbol = scope.getSymbol(name.lexeme)
        varSymbol.dataType match
          case _: Utility.Array =>
            val getAddressOfArray = Indexing.getAddressInto(arg, AReg(), scope).append(IR.PushRegister(AReg()))
            scope.push()
            Some(getAddressOfArray.toList)
          case _ =>None
      case _ => None

    specialCaseResult match
      case Some(result) => result
      case _ =>
        val pushList = getFromAccumulator(arg, scope).toGetThere
          .append(IR.PushRegister(AReg()))
          .toList
        scope.push()
        pushList
  }
  private def translateFunctionCall(function: Expr.Expr, arguments: List[Expr.Expr], scope: TranslatorScope): AccumulatorLocation = {
    scope.rememberStackLocation()
    val buffer = IRBuffer().append(
      arguments.map(
        arg => pushArgument(arg, scope)
      ).foldLeft(IRBuffer().toList)(_ ::: _)
    )
    function match
      case Expr.Variable(funcToken) =>
        val functionSymbol = scope.getSymbol(funcToken.lexeme)
        val functionDefinitionLine = functionSymbol.lineNumber.toString
        AccumulatorLocation(
          buffer.append(IR.JumpLongSaveReturn(Label("func_" ++ funcToken.lexeme ++ "_l" ++ functionDefinitionLine)))
        )
      case _ => throw new Exception("Cannot call type at this time")
    buffer.append(scope.getFixStackDecay())
    AccumulatorLocation(buffer)
  }

  private def getRegisterShifts(value: Expr.Expr, shiftBy: Expr.Expr, shiftInstruction: IR.Instruction, scope: TranslatorScope): IRBuffer = {
    println(value.toString ++ " shifting by " ++ shiftBy.toString)
    shiftBy match
      case Expr.NumericalLiteral(numericalValue) =>
        ExpressionTranslator.getFromAccumulator(value, scope).toGetThere.append((for i <- Range(0, numericalValue) yield shiftInstruction).toList)
      case _ =>
        val shifterToY = ExpressionTranslator.getFromAccumulator(shiftBy, scope).toGetThere.append(IR.PushRegister(AReg()))
        scope.push()
        val loadValueToA = ExpressionTranslator.getFromAccumulator(value, scope).toGetThere
        val buffer = shifterToY.append(loadValueToA).append(
          //Take advantage of the backwards loop optimisation
          IR.PopRegister(XReg()) :: IR.PutLabel(Label("-")) :: shiftInstruction :: IR.DecrementReg(XReg()) :: IR.BranchIfPlus(Label("-")) :: Nil
        )
        scope.pop()
        buffer
  }

  def getFromAccumulator(expr: Expr.Expr, scope: TranslatorScope): AccumulatorLocation = {
    val result: AccumulatorLocation | StackLocation = expr match
      case Assign(varToken, arg) =>
        val intoAccumulator = getFromAccumulator(arg, scope)
        val targetAddress = scope.getAddress(varToken.lexeme)
        AccumulatorLocation(intoAccumulator.toGetThere.append(IR.Store(targetAddress, AReg()).addComment("Storing the assignment")))
      case BooleanLiteral(value) => AccumulatorLocation(
        loadImmediate(AReg(), if(value) 1 else 0)
      )
      case UnaryOp(op, arg) =>
        val toGetArg = getFromAccumulator(arg, scope)
        val afterArg = op match
          case Operation.Unary.BitwiseNot => getBitwiseNotA
          case Operation.Unary.Minus => getBitwiseNotA.append(IR.IncrementReg(AReg()) :: Nil)
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
        val buffer = IRBuffer()
        op match
          case Operation.Binary.ShiftLeft =>
            buffer.append(getRegisterShifts(left, right, IR.ShiftLeft(AReg()), scope))
          case Operation.Binary.ShiftRight =>
            buffer.append(getRegisterShifts(left, right, IR.ShiftRight(AReg()), scope))
          case x if Operation.Groups.RelationalTokens.contains(x) =>
            scope.rememberStackLocation()
            val conditionCode = StatementTranslator.getConditionCode(expr, scope, Label("++"), StatementTranslator.BranchType.IfFalse)
            val setTrueThenToEnd = IR.Load(Immediate(1), AReg()).addComment(expr.toString ++ "is true") :: IR.BranchShort(Label("+++")) :: Nil
            val setFalseAndEnd = IR.PutLabel(Label("++")) :: IR.Load(Immediate(0), AReg()).addComment("Its false") :: IR.PutLabel(Label("+++")).addComment("End Binary check, clear stack beneath") :: Nil
            val fixStack = scope.getFixStackDecay().toList
            buffer.append(conditionCode ::: setTrueThenToEnd ::: setFalseAndEnd ::: fixStack)
          case _ =>
            val leftToStack = getFromStack(left, scope)
            val rightToAccumulator = getFromAccumulator(right, scope)
            buffer.append(leftToStack.toGetThere.append(rightToAccumulator.toGetThere))
            buffer.append(op match
              case Operation.Binary.Add => IR.ClearCarry() :: IR.AddCarry(leftToStack.address) :: Nil
              case Operation.Binary.Subtract => IR.SetCarry() :: IR.SubtractCarry(leftToStack.address) :: Nil
              case Operation.Binary.And => IR.AND(leftToStack.address) :: Nil
              case Operation.Binary.Or => IR.ORA(leftToStack.address) :: Nil
              case Operation.Binary.Xor => IR.EOR(leftToStack.address) :: Nil

              case _ => throw new Exception("Binary operation not supported yet '" ++ op.toString ++ "' in expression '" ++ expr.toString ++ "'")
            )

        buffer.append(scope.getFixStackDecay())
        AccumulatorLocation(buffer)
      case FunctionCall(function, arguments) => translateFunctionCall(function, arguments, scope)
      case NumericalLiteral(value) => AccumulatorLocation(loadImmediate(AReg(), value))
      case Indirection(expr) =>
        expr match
          case Variable(token) => AccumulatorLocation(IRBuffer().append(
            IR.Load(scope.getAddress(token.lexeme), AReg()) :: IR.TransferToX(AReg()) ::
              IR.Load(DirectIndexed(0, XReg()), AReg()) :: Nil
          ))
          case _ => throw new Exception("Cannot perform indirection on " ++ expr.toString)
      case SetIndex(of, to) =>
        val addressInA = Indexing.getAddressInto(of, AReg(), scope).toList
        val pushAddressToStack = IR.PushRegister(AReg()).addComment("Pushing address to set value to") :: Nil
        scope.push()
        val getWhatToSetInA = getFromAccumulator(to, scope).toGetThere.toList
        val getAddressInXAndSet = IR.PopRegister(XReg()) :: IR.Store(DirectIndexed(0, XReg()), AReg()) :: Nil
        scope.pop()
        AccumulatorLocation(
          IRBuffer().append(addressInA ::: pushAddressToStack ::: getWhatToSetInA ::: getAddressInXAndSet)
        )

      case Variable(token) => AccumulatorLocation(
        scope.getSymbol(token.lexeme).dataType match
          case _: Utility.Array => //Get the address
            IRBuffer().append(Indexing.getAddressInto(expr, AReg(), scope))
          case _ =>
            IRBuffer().append(IR.Load(scope.getAddress(token.lexeme), AReg()))
      )
      case GetAddress(expr) =>
        AccumulatorLocation(IRBuffer().append(Indexing.getAddressInto(expr, AReg(), scope)))
      case GetIndex(of, by) =>
        AccumulatorLocation(IRBuffer().append(Indexing.getIndexValue(of, by, scope)))
      case Grouping(internalExpr) => getFromAccumulator(internalExpr, scope)
      case _ =>
        throw new Exception("Cannot translate expression -> " ++ expr.toString)
    result match
      case stackLocation: StackLocation => AccumulatorLocation(stackLocation.toGetThere)
      case accumulatorLocation: AccumulatorLocation => accumulatorLocation
  }

  //Can cause stack decay
  def getFromStack(expr: Expr.Expr, scope: TranslatorScope): StackLocation = {
    def stackFromAccumulator(expr: Expr.Expr, scope: TranslatorScope): StackLocation = {
      val intoAccumulator = getFromAccumulator(expr, scope)
      scope.push()
      StackLocation(StackRelative(2),
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
      case Variable(name) =>
        scope.getSymbol(name.lexeme).dataType match
          case _: Utility.Array => //Get the address
            val toA = Indexing.getAddressInto(expr, AReg(), scope).toList
            val pushToStack = IR.PushRegister(AReg()) :: Nil
            scope.push()
            StackLocation(StackRelative(2), IRBuffer().append(toA ::: pushToStack))
          case _ =>
            StackLocation(scope.getAddress(name.lexeme), IRBuffer())
      case FunctionCall(_, _) => stackFromAccumulator(expr, scope)
      case GetAddress(_) => stackFromAccumulator(expr, scope)
      case GetIndex(_, _) => stackFromAccumulator(expr, scope)
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

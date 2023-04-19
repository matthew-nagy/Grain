package TreeWalker

import Grain.Expr.*
import Grain.*
import Utility.{Token, TokenType}

/*
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
  private def getAddressOfVariableInto(name: Token, into: TargetReg, scope: TranslatorScope): List[IR.Instruction] = {
    val address = scope.getAddress(name.lexeme)
    address match
      case Direct(value) =>
        IR.Load(Immediate(value), into)
          .addComment("Load in the global known value") :: Nil
      case StackRelative(offset) =>
        val getStackAddress = IR.TransferToAccumulator(StackPointerReg()).addComment("Get address of stack variable") ::
          IR.ClearCarry() :: IR.AddCarry(Immediate(offset)) :: getPutFromAccumulatorToDesiredReg(into)
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
}*/

object Getting{
  private def getInternalSizeOfArray(ar: Expr.Expr, scope: Scope): Int =
    Utility.getTypeSize(Utility.stripPtrType(scope.getTypeOf(ar)))

  private def maybeGetPowerOf2(value: Int): Option[Int] = {
    var result: Option[Int] = None
    var currentValue = value
    var currentPowerOf2 = 0
    while currentValue > 0 do {
      currentValue = currentValue >> 1
      currentPowerOf2 += 1
      if ((currentValue & 1) > 0) {
        if (result.isDefined) { //Already found a bit, can't be a power of 2
          return None
        }
        else {
          result = Some(currentPowerOf2)
        }
      }
    }

    result
  }

  private def correctAccumulatorIndexByType(indexing: Expr.Expr, scope: Scope): List[IR.Instruction] = {
    val size = getInternalSizeOfArray(indexing, scope)
    size match
      case 1 => List.empty[IR.Instruction]
      case x if maybeGetPowerOf2(x).isDefined =>
        IR.ShiftLeft(AReg(), maybeGetPowerOf2(x).get) :: Nil
      case _ =>
        throw new Exception("Can't multiply yet. Size of " ++ size.toString ++ " isn't possible") //TODO
  }

  private def useAddressAsIndirectIndexed(address: Address, offset: Int, toGetHere: IRBuffer): (Address, IRBuffer) =
    (DirectIndexed(offset, XReg()), toGetHere.append(IR.Load(address, AReg()) :: IR.TransferToX(AReg()) :: Nil))


  def offsetAddress(address: Offsetable, offset: Int): (Address, IRBuffer) =
    address match
      case Direct(value) => (Direct(value + offset), IRBuffer())
      case DirectIndexed(value, by) => (DirectIndexed(value + offset, by), IRBuffer())
      case DirectIndirectIndexed(value, by) => (DirectIndirectIndexed(value, by), IRBuffer()
        .append(IR.TransferToAccumulator(by) :: IR.ClearCarry() :: IR.AddCarry(Immediate(offset)) :: IR.TransferAccumulatorTo(by) :: Nil)
      )
      case StackRelative(value) => (StackRelative(value - offset), IRBuffer())
      case StackRelativeIndirectIndexed(value, by) => (StackRelativeIndirectIndexed(value, by), IRBuffer()
        .append(IR.TransferToAccumulator(by) :: IR.ClearCarry() :: IR.AddCarry(Immediate(offset)) :: IR.TransferToAccumulator(by) :: Nil)
      )

  def certainlyOffsetAddress(address: Address, offset: Int): (Address, IRBuffer) =
    address match
      case offsetable: Offsetable => offsetAddress(offsetable, offset)
      case simpleIndirectRemovable: SimpleIndirectRemovable =>
        val withoutIndirection = simpleRemoveIndirect(simpleIndirectRemovable)
        useAddressAsIndirectIndexed(withoutIndirection, offset, IRBuffer())

  def asIndirect(address: Address): Option[Address] =
    address match
      case Direct(location) => Some(DirectIndirect(location))
      case DirectIndexed(location, by) => Some(DirectIndexedIndirect(location, by))
      case _ => None
  def simpleRemoveIndirect(address: SimpleIndirectRemovable) : Address =
    address match
      case DirectIndirect(location) => Direct(location)
      case DirectIndexedIndirect(location, by) => DirectIndexed(location, by)

  def indexAddress(address: Indexable, by: Expr.Expr, scope: TranslatorScope, onceInAccumulator: List[IR.Instruction]): (Address, IRBuffer) =
    address match
      case Direct(value) => (DirectIndexed(value, XReg()), ExpressionTranslator.getFromAccumulator(by, scope).toGetThere
        .append(onceInAccumulator)
        .append(IR.TransferToX(AReg()))
      )
      case DirectIndirect(value) => (DirectIndirectIndexed(value, XReg()), ExpressionTranslator.getFromAccumulator(by, scope).toGetThere
        .append(IR.TransferToX(AReg()))
      )

  def getAddressIntoAcumulator(address: Address): List[IR.Instruction] = {
    address match
      case Direct(value) => IR.Load(Immediate(value), AReg()) :: Nil
      case DirectLabel(label) => IR.Load(Label(label), AReg()) :: Nil
      case DirectIndexed(offset, by) => IR.TransferToAccumulator(by) :: IR.ClearCarry() :: IR.AddCarry(Immediate(offset)) :: Nil
      case DirectIndirect(value) => IR.Load(Direct(value), AReg()) :: Nil
      case DirectIndexedIndirect(offset, by) => IR.Load(DirectIndexed(offset, by), AReg()) :: Nil
      case DirectIndirectIndexed(offset, by) => IR.PushRegister(by) :: IR.Load(Direct(offset), AReg()) ::
        IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(XReg()) :: Nil
      case StackRelative(offset) => IR.TransferToAccumulator(StackPointerReg()) :: IR.SetCarry() :: IR.SubtractCarry(Immediate(offset)) :: Nil
      case StackRelativeIndirectIndexed(stackOffset, by) => IR.PushRegister(by) :: IR.Load(StackRelative(stackOffset + 2), AReg()) :: IR.ClearCarry() ::
        IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(XReg()) :: Nil
  }


  def getIndexOfPtr(ptrExpr: Expr.Expr, index: Expr.Expr, scope: TranslatorScope): (Address, IRBuffer) = {
    val (addressOfPtr, codeToGetAddressOfPtr) = getAddressOf(ptrExpr, scope)
    val innerSizeOfArray = getInternalSizeOfArray(ptrExpr, scope.inner)
    def getIndexTo(reg: GeneralPurposeReg):IRBuffer =
      index match
        case NumericalLiteral(value) =>
          IRBuffer().append(IR.Load(Immediate(value * innerSizeOfArray), reg))
        case _ =>
          ExpressionTranslator.getFromAccumulator(index, scope).toGetThere
            .append(correctAccumulatorIndexByType(ptrExpr, scope.inner))
            .append(IR.TransferAccumulatorTo(reg))
    addressOfPtr match
      case Direct(location) =>
        val toAccumulatorToX = getIndexTo(XReg())
        (DirectIndirectIndexed(location, XReg()), codeToGetAddressOfPtr.append(toAccumulatorToX))
      case StackRelative(offset) =>
        val toAccumulatorToX = getIndexTo(YReg())
        (StackRelativeIndirectIndexed(offset, YReg()), codeToGetAddressOfPtr.append(toAccumulatorToX))
      case _ =>
        val ptrToAccumulator = IR.Load(addressOfPtr, AReg())
        index match
          case NumericalLiteral(value) =>
            (DirectIndexed(value * innerSizeOfArray, XReg()), codeToGetAddressOfPtr.append(ptrToAccumulator :: IR.TransferToX(AReg()) :: Nil))
          case _ =>
            val pushPtr = IR.PushRegister(AReg())
            scope.push()
            val indexToAccumulatorAndAdjusted = ExpressionTranslator.getFromAccumulator(index, scope).toGetThere.append(correctAccumulatorIndexByType(ptrExpr, scope.inner))
            val additionPopAndToX = IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(XReg()) :: IR.TransferToX(AReg()) :: Nil
            (DirectIndexed(0, XReg()), codeToGetAddressOfPtr
              .append(pushPtr)
              .append(indexToAccumulatorAndAdjusted)
              .append(additionPopAndToX)
            )
  }

  def getIndexOfArray(arrayExpr: Expr.Expr, index: Expr.Expr, scope: TranslatorScope):(Address, IRBuffer) = {
    val (addressOfArray, codeToGetAddressOfArray) = getAddressOf(arrayExpr, scope)
    val sizeOfArrayType = getInternalSizeOfArray(arrayExpr, scope.inner)
    index match
      case NumericalLiteral(value) =>
        addressOfArray match
          case offsettableAddressOfArray: Offsetable =>
            val (offsetArrayAddress, toOffsetArrayAddress) = offsetAddress(offsettableAddressOfArray, value * sizeOfArrayType)
            (offsetArrayAddress, codeToGetAddressOfArray.append(toOffsetArrayAddress))
          case _ =>
            (DirectIndexed(value * sizeOfArrayType, XReg()), codeToGetAddressOfArray
              .append(getAddressIntoAcumulator(addressOfArray))
              .append(IR.TransferToX(AReg()))
            )
      case _ =>
        addressOfArray match
          case indexibleAddressOfArray: Indexable =>
            val (indexedAddress, toIndexAddress) = indexAddress(indexibleAddressOfArray, index, scope, correctAccumulatorIndexByType(arrayExpr, scope.inner))
            (indexedAddress, codeToGetAddressOfArray.append(toIndexAddress))
          case _ =>
            val toStack = codeToGetAddressOfArray
              .append(getAddressIntoAcumulator(addressOfArray))
              .append(IR.PushRegister(AReg()))
            scope.push()
            val indexIntoAcc = ExpressionTranslator.getFromAccumulator(index, scope).toGetThere.append(
              correctAccumulatorIndexByType(arrayExpr, scope.inner)
            )
            val addThenToX: List[IR.Instruction] = IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(XReg()) :: IR.TransferToX(AReg()) :: Nil
            scope.pop()
            (DirectIndexed(0, XReg()), toStack.append(indexIntoAcc).append(addThenToX))
  }

  def getAddressOf(expr: Expr.Expr, scope: TranslatorScope): (Address, IRBuffer) = {
    expr match
      case Variable(varToken) => (scope.getAddress(varToken.lexeme), IRBuffer())
      case Get(innerExpr, memberToken) =>
        val (toGetThereAddress, toGetThereCode) = getAddressOf(innerExpr, scope)
        val offset = scope.inner.getTypeOf(innerExpr).asInstanceOf[Utility.Struct].getOffsetOf(memberToken.lexeme)
        toGetThereAddress match
          case offsetableAddress: Offsetable =>
            val (newAddress, toOffset) = offsetAddress(offsetableAddress, offset)
            (newAddress, toGetThereCode.append(toOffset))
          case indirection: SimpleIndirectRemovable =>
            val withoutIndirection = simpleRemoveIndirect(indirection)
            useAddressAsIndirectIndexed(withoutIndirection, 0, toGetThereCode)
      case Indirection(innerExpr) =>
        val (toGetThereAddress, toGetThereCode) = getAddressOf(innerExpr, scope)
        asIndirect(toGetThereAddress) match
          case Some(indirectAddress) => (indirectAddress, toGetThereCode)
          case _ =>  useAddressAsIndirectIndexed(toGetThereAddress, 0, toGetThereCode)

      //The big one
      case GetIndex(of, by) =>
        scope.inner.getTypeOf(of) match
          case _: Utility.Ptr => getIndexOfPtr(of, by, scope)
          case _: Utility.Array => getIndexOfArray(of, by, scope)
          case t@_ => throw new Exception("Cannot index type " ++ t.toString)
      case _ =>
        if(scope.inner.getTypeOf(expr).isInstanceOf[Utility.Ptr]){
          val intoAcc = ExpressionTranslator.getFromAccumulator(expr, scope)
          (DirectIndexed(0, XReg()), intoAcc.toGetThere.append(IR.TransferToX(AReg())))
        }
        else{
          throw Exception("Cannot get the address of a non pointer type")
        }

  }

  def asStruct(varName: Token, scope: TranslatorScope): Utility.Struct =
    scope.inner(varName.lexeme).dataType match
      case ptr: Utility.Ptr if ptr.to.isInstanceOf[Utility.Struct] => ptr.to.asInstanceOf[Utility.Struct]
      case ar: Utility.Array if ar.of.isInstanceOf[Utility.Struct] => ar.of.asInstanceOf[Utility.Struct]
      case st: Utility.Struct => st
      case _ => throw new Exception("Isn't a struct or struct ptr")
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
            val (addressOfArray, getAddressOfArray) = Getting.getAddressOf(arg, scope)
            scope.push()
            Some(getAddressOfArray.append(Getting.getAddressIntoAcumulator(addressOfArray)).append(IR.PushRegister(AReg())).toList)
          case _ =>None
          //TODO maybe a Get special case would be nice too
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
      //TODO differentiate between function and function ptr
      //Oh god the mistakes
      case Expr.Variable(funcToken) =>
        val functionSymbol = scope.getSymbol(funcToken.lexeme)
        val functionDefinitionLine = functionSymbol.lineNumber.toString
        val functionLabel = Label("func_" ++ funcToken.lexeme)
        scope.getTranslatorSymbolTable.usedFunctionLabels.addOne(functionLabel.name)
        AccumulatorLocation(
          buffer.append(IR.JumpLongSaveReturn(functionLabel))
        )
      case _ => throw new Exception("Cannot call type at this time")
    buffer.append(scope.getFixStackDecay())
    AccumulatorLocation(buffer)
  }

  private def getRegisterShifts(value: Expr.Expr, shiftBy: Expr.Expr, shiftInstruction: IR.Instruction, scope: TranslatorScope): IRBuffer = {
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

  //TODO This is dumb, give both expressions as an expression. The stack thing could be silly
  //TODO very useful to have that start thing finished for when you need to make sure you are in bank 0
  //TODO currently everything assumes an 8-bit right hand side
  //Fix that nonsense
  private def translateHardwareMaths(op: Operation.Binary, leftLocation: StackLocation, expr: Expr.Expr): List[IR.Instruction] = {
    //val start = IR.TransferToY(AReg()) :: IR.Load(leftLocation.address, AReg()) :: IR.TransferToX(AReg()) ::
    val startDivision: List[IR.Instruction] = IR.TransferToX(AReg()) :: IR.Load(leftLocation.address, AReg()) :: IR.SetReg8Bit(RegisterGroup.XY) ::
      IR.Store(Direct(GlobalData.Addresses.dividendLowByte), XReg()) :: IR.Store(Direct(GlobalData.Addresses.divisor), AReg()) :: Nil
    val waitForDivisionResult: List[IR.Instruction] = Range(0, 8).map(nothing => IR.NOP()).toList

    val putRightInMultiplicand = IR.SetReg8Bit(RegisterGroup.A) :: IR.Store(Direct(GlobalData.Addresses.signedMultiplyMultiplicand), AReg()) :: IR.ExchangeAccumulatorBytes() ::
      IR.Store(Direct(GlobalData.Addresses.signedMultiplyMultiplicand), AReg()) :: Nil

    val (offsetLocation, toOffsetLocation) = Getting.certainlyOffsetAddress(leftLocation.address, 1)

    op match
      case Operation.Binary.Multiply8Bit =>
        println(expr)
         putRightInMultiplicand ::: (IR.Load(leftLocation.address, AReg()) ::
          IR.Store(Direct(GlobalData.Addresses.signedMultiplyMultiplier), AReg()) :: IR.SetReg16Bit(RegisterGroup.A) ::
          IR.Load(Direct(GlobalData.Addresses.signedMultiplyLowByteResult), AReg()) :: Nil)

      case Operation.Binary.Multiply =>
        throw Exception("Not heccin done")

      case Operation.Binary.Divide8Bit =>
        val getResult = IR.Load(Direct(GlobalData.Addresses.divisionResultLowByte), AReg()) :: IR.SetReg16Bit(RegisterGroup.XY) :: Nil
        startDivision ::: waitForDivisionResult ::: getResult
      case Operation.Binary.Divide =>
        throw Exception("Not heccin done")
      case Operation.Binary.Modulo8Bit =>
        val getResult = IR.Load(Direct(GlobalData.Addresses.divisionRemainderLowByte), AReg()) :: IR.SetReg16Bit(RegisterGroup.XY) :: Nil
        startDivision ::: waitForDivisionResult ::: getResult
      case Operation.Binary.Modulo =>
        throw Exception("Not heccin done")
      case _ => throw new Exception("Binary operation not supported yet '" ++ op.toString ++ "' in expression '" ++ expr.toString ++ "'")
  }

  def getFromAccumulator(expr: Expr.Expr, scope: TranslatorScope): AccumulatorLocation = {
    val result: AccumulatorLocation | StackLocation = expr match
      case Assign(target, arg) =>
        target match
          case varToken: Utility.Token =>
            val intoAccumulator = getFromAccumulator(arg, scope)
            val targetAddress = scope.getAddress(varToken.lexeme)
            AccumulatorLocation(intoAccumulator.toGetThere.append(IR.Store(targetAddress, AReg()).addComment("Storing the assignment")))
          case getter: Expr.Get =>
            val (address, toGetAddress) = Getting.getAddressOf(getter, scope)
            if(toGetAddress.toList.isEmpty){
              val intoAccumulator = getFromAccumulator(arg, scope)
              AccumulatorLocation(intoAccumulator.toGetThere.append(IR.Store(address, AReg()).addComment("Assigning simple getter")))
            }
            else{
              val intoStack = getFromStack(arg, scope)
              val (addressWithStackCorrection, toGetAddressWithStackCorrection) = Getting.getAddressOf(getter, scope) //get again with the stack sorted
              intoStack.address match
                case StackRelative(2) =>
                  scope.pop()
                  AccumulatorLocation(
                    intoStack.toGetThere
                      .append(toGetAddressWithStackCorrection)
                      .append(IR.PopRegister(AReg()) :: IR.Store(addressWithStackCorrection, AReg()) :: Nil)
                  )
                case _ =>
                  AccumulatorLocation(
                    intoStack.toGetThere
                      .append(toGetAddressWithStackCorrection)
                      .append(IR.Load(intoStack.address, AReg()) :: IR.Store(addressWithStackCorrection, AReg()) :: Nil)
                  )
            }
      case BooleanLiteral(value) => AccumulatorLocation(
        loadImmediate(AReg(), if(value) 1 else 0)
      )
      case BankLiteral(intermediateValue) => AccumulatorLocation(
        IRBuffer().append(IR.Load(BankImmediate(intermediateValue), AReg()))
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
            right match
              case Expr.NumericalLiteral(value) =>
                buffer.append(IR.ShiftLeft(AReg(), value))
              case _ =>
                buffer.append(getRegisterShifts(left, right, IR.ShiftLeft(AReg(), 1), scope))
          case Operation.Binary.ShiftRight =>
            right match
              case Expr.NumericalLiteral(value) =>
                buffer.append(IR.ShiftRight(AReg(), value))
              case _ =>
                buffer.append(getRegisterShifts(left, right, IR.ShiftRight(AReg(), 1), scope))
          case x if Operation.Groups.RelationalTokens.contains(x) =>
            scope.rememberStackLocation()
            val conditionCode = StatementTranslator.getConditionCode(expr, scope, Label("++"), StatementTranslator.BranchType.IfFalse, 0)
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
              case _ => translateHardwareMaths(op, leftToStack, expr)
            )

        buffer.append(scope.getFixStackDecay())
        AccumulatorLocation(buffer)
      case FunctionCall(function, arguments) =>
        translateFunctionCall(function, arguments, scope)
      case NumericalLiteral(value) => AccumulatorLocation(loadImmediate(AReg(), value))
      case Indirection(expr) =>
        val (address, irToGetTheAddress) = Getting.getAddressOf(expr, scope)
        Getting.asIndirect(address) match
          case Some(indirectAddress) => AccumulatorLocation(irToGetTheAddress.append(IR.Load(indirectAddress, AReg())))
          case None =>
            AccumulatorLocation(IRBuffer().append(irToGetTheAddress).append(
                IR.Load(address, AReg()) :: IR.TransferToX(AReg()) :: IR.Load(DirectIndexed(0, XReg()), AReg()) :: Nil
              ))
      case SetIndex(of, to) =>
        val getWhatToSet = getFromAccumulator(to, scope).toGetThere
        getWhatToSet.toList match
          case IR.Load(x, AReg()) :: Nil if !(
            x.isInstanceOf[StackRelativeIndirectIndexed] || x.isInstanceOf[DirectIndexedIndirect] ||x.isInstanceOf[DirectIndexed] || x.isInstanceOf[DirectIndirectIndexed]
            ) =>
            val (addressOf, toGetAddressOf) = Getting.getAddressOf(of, scope)
            AccumulatorLocation(toGetAddressOf.append(IR.Load(x, AReg()) :: IR.Store(addressOf, AReg()) :: Nil))
          case _ =>
            val getWhatToSetToStack = getWhatToSet.append(IR.PushRegister(AReg()))
            scope.push()
            val (addressOf, toGetAddressOf) = Getting.getAddressOf(of, scope)
            scope.pop()
            val setting = IR.PopRegister(AReg()) :: IR.Store(addressOf, AReg()) :: Nil
            AccumulatorLocation(getWhatToSetToStack.append(toGetAddressOf).append(setting))


      case Variable(token) => AccumulatorLocation(
        scope.getSymbol(token.lexeme).dataType match
          case _: Utility.Array => //Get the address
            val (arrayAddress, getArrayAddress) = Getting.getAddressOf(expr, scope)
            getArrayAddress.append(Getting.getAddressIntoAcumulator(arrayAddress))
          case _ =>
            IRBuffer().append(IR.Load(scope.getAddress(token.lexeme), AReg()))
      )
      case GetAddress(expr) =>
        val (address, toGetAddress) = Getting.getAddressOf(expr, scope)
        AccumulatorLocation(toGetAddress.append(Getting.getAddressIntoAcumulator(address)))
      case GetIndex(of, by) =>
        val (address, toGetTheAddress) = Getting.getAddressOf(GetIndex(of, by), scope)
        AccumulatorLocation(toGetTheAddress.append(IR.Load(address, AReg())))
      case Grouping(internalExpr) => getFromAccumulator(internalExpr, scope)
      case Get(left, name) =>
        val (address, toGetThatAddress) = Getting.getAddressOf(Get(left, name), scope)
        AccumulatorLocation(toGetThatAddress.append(IR.Load(address, AReg())))
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
      case Assign(target, arg) =>
        target match
          case varToken: Utility.Token =>
            val intoAccumulator = getFromAccumulator(arg, scope)
            toGetToStack.append(intoAccumulator.toGetThere)
            val targetAddress = scope.getAddress(varToken.lexeme)
            toGetToStack.append(IR.Store(targetAddress, AReg()))
            StackLocation(targetAddress, toGetToStack)
          case getter: Expr.Get =>
            println("Do stack getter assign case")
            StackLocation(Direct(-1), IRBuffer().append(IR.NOP().addComment("Do the getter in assign stack!")))

      case UnaryOp(_, _) => stackFromAccumulator(expr, scope)
      case BinaryOp(_, _, _) => stackFromAccumulator(expr, scope)
      case NumericalLiteral(_) => stackFromAccumulator(expr, scope)
      case BooleanLiteral(_) => stackFromAccumulator(expr, scope)
      case BankLiteral(_) => stackFromAccumulator(expr, scope)
      case Indirection(_) => stackFromAccumulator(expr, scope)//May be improvable later
      case Variable(name) =>
        scope.getSymbol(name.lexeme).dataType match
          case _: Utility.Array => //Get the address
            val (address, toGetAddress) = Getting.getAddressOf(expr, scope)
            val toA = toGetAddress.append(Getting.getAddressIntoAcumulator(address))
            val pushToStack = IR.PushRegister(AReg()) :: Nil
            scope.push()
            StackLocation(StackRelative(2), IRBuffer().append(toA.append(pushToStack)))
          case _ =>
            StackLocation(scope.getAddress(name.lexeme), IRBuffer())
      case FunctionCall(_, _) => stackFromAccumulator(expr, scope)
      case GetAddress(_) => stackFromAccumulator(expr, scope)
      case GetIndex(_, _) => stackFromAccumulator(expr, scope)
      case SetIndex(_, _) => stackFromAccumulator(expr, scope)
      case Grouping(internalExpr) => getFromStack(internalExpr, scope)
      case Get(_, _) => stackFromAccumulator(expr, scope)
      case _ => throw new Exception("Not handled in stack yet (" ++ expr.toString ++ ")")
  }

  private def loadImmediate(reg: TargetReg, value: Int): IRBuffer =
    IRBuffer().append(IR.Load(Immediate(value), reg))


  private def getBitwiseNotA: IRBuffer =
    IRBuffer().append(IR.EOR(Direct(0)))
}

object EXPTranslatorMain{
  def main(args: Array[String]): Unit = {
    val filename = "src/main/ExpressionParserTest.txt"
    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText(filename), filename, 0)
    val symbolTable = new SymbolTable
    val translatorSymbolTable = new TranslatorSymbolTable

    symbolTable.globalScope.addSymbol(
      Token(TokenType.Identifier, "a", 0), Utility.Word(), Symbol.Variable(), filename
    )
    symbolTable.globalScope.addSymbol(
      Token(TokenType.Identifier, "b", 0), Utility.Word(), Symbol.Variable(), filename
    )

    while(tokenBuffer.peekType != TokenType.EndOfFile){
      val exp = Parser.ExpressionParser.parseOrThrow(symbolTable.globalScope, tokenBuffer)
      println(exp)
      println(ExpressionTranslator.getFromAccumulator(exp, TranslatorScope(symbolTable.globalScope, translatorSymbolTable)))
    }
  }
}

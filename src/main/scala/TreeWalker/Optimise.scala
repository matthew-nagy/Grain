package TreeWalker

import Grain.GlobalData
import TreeWalker.IR.{Instruction, PushRegister}

import scala.annotation.tailrec
import scala.collection.immutable.::
import scala.collection.mutable.ListBuffer

object Optimise {

  case class Result(optimisedFragment: List[IR.Instruction], remaining: List[IR.Instruction])

  var functionArity: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map.empty[String, Int]

  private var sweepDidWork: Boolean = false

  private def getAddressWithoutStackPush(something: Address): Address =
    something match
      case StackRelative(offset) => StackRelative(offset + 2)
      case StackRelativeIndirectIndexed(offset, reg) => StackRelativeIndirectIndexed(offset + 2, reg)
      case _ => something

  private def getImmediateOrAddressWithoutStackPush(something: ImmediateOrAddress): ImmediateOrAddress =
    something match
      case address: Address => getAddressWithoutStackPush(address)
      case _ => something

  //Actions:
  //  Finds dummy pulls followed by a dummy push, gets rid of them and vice virca->
  //    dummy pushes and pulls just change stack state, so this saves 8 cycles
  //  Finds dummy pulls followed by an AReg push, replaces to a stack relative store ->
  //    pulls and pushes take 4 cycles. Storing to stack takes 4. Therefore this saves 4 cycles each.
  //    Only A can be stored stack relative
  //  Finds a dummy push followed by a load and store, changes to load and push->
  //    Saves the 4 cycles of the dummy push
  //  Finds a function call being used as a new variable and gets rid of a dummy push beforehand ->
  //    Saves the 4 cycles of the dummy push by pushing A with the return value
  //  Finds pushing the first stack value as a single argument to a 1 arg function
  //    If you have already loaded/ stored the first stack value, you know that its already in the right place
  //  Finds pushing A, loading A, then pulling to X/Y
  //    You can just transfer to X/Y and not touch the stack. Saves 5 cycles.
  //  Finds pushing A, loading another register, then pulling A
  //    You don't need to mess with A there
  //  Finds pushing then immediately pulling a register
  //    Either don't do anything, or transfer
  //  Finds pushing something to the stack only to add it to a constant
  //    Changes from adc 1, s to just adding the constant, saving stack manipulation
  @tailrec
  def stackUsage(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.PopDummyValue(_) :: IR.PushDummyValue(_) :: remaining =>
        Result(Nil, remaining)
      case IR.PushDummyValue(_) :: IR.PopDummyValue(_) :: remaining =>
        Result(Nil, remaining)
      case IR.PopDummyValue(_) :: IR.PushRegister(AReg()) :: remaining =>
        Result(IR.Store(StackRelative(2), AReg()) :: Nil, remaining)
      case IR.PushDummyValue(_) :: IR.Load(loadOp, AReg()) :: IR.Store(StackRelative(2), AReg()) :: remaining =>
        Result(IR.Load(getImmediateOrAddressWithoutStackPush(loadOp), AReg()) :: IR.PushRegister(AReg()) :: Nil, remaining)
      case IR.PopDummyValue(_) :: IR.Load(opOrAd, reg) :: IR.PushRegister(reg2) :: remaining if reg == reg2 =>
        Result(IR.Load(getImmediateOrAddressWithoutStackPush(opOrAd), reg) :: IR.Store(StackRelative(2), reg2) :: Nil, remaining)
      case IR.PushDummyValue(_) :: IR.JumpLongSaveReturn(label) :: IR.Store(StackRelative(2), AReg()) :: remaining =>
        Result(IR.JumpLongSaveReturn(label) :: IR.PushRegister(AReg()) :: Nil, remaining)
      case IR.Load(StackRelative(2), reg1) :: PushRegister(reg2) :: IR.JumpLongSaveReturn(func) ::
        IR.PopDummyValue(_) :: remaining if reg1 == reg2 =>
       Result(IR.JumpLongSaveReturn(func) :: Nil, remaining)
      case IR.Store(StackRelative(2), reg1) :: PushRegister(reg2) :: IR.JumpLongSaveReturn(func) ::
        IR.PopDummyValue(_)  :: remaining if reg1 == reg2 =>
        Result(IR.Store(StackRelative(2), reg1) :: IR.JumpLongSaveReturn(func) :: Nil, remaining)
      case IR.PushRegister(AReg()) :: IR.Load(imOrAdd, AReg()) :: IR.PopRegister(targetReg) :: remaining if targetReg != AReg() =>
        Result(IR.TransferAccumulatorTo(targetReg) :: IR.Load(imOrAdd, AReg()) :: Nil, remaining)
      case IR.PushRegister(firstReg1) :: IR.Load(imOrAd, secondReg) :: IR.PopRegister(firstReg2) :: remaining if firstReg1 == firstReg2 && secondReg != firstReg1 =>
        Result(IR.Load(imOrAd, secondReg) :: Nil, remaining)
      case IR.PushRegister(reg1) :: IR.PopRegister(reg2) :: remaining if reg1 == reg2 => Result(Nil, remaining)
      case IR.PushRegister(reg1) :: IR.PopRegister(reg2) :: remaining =>
        reg1 match
          case AReg() => Result(IR.TransferAccumulatorTo(reg2) :: Nil, remaining)
          case XReg() => Result(IR.TransferXTo(reg2) :: Nil, remaining)
          case YReg() => Result(IR.TransferYTo(reg2) :: Nil, remaining)
          case _ => Result(instructions.head :: Nil, instructions.tail)
      case IR.PushRegister(AReg()) :: IR.Load(imOrAd, AReg()) :: IR.ClearCarry() :: IR.AddCarry(StackRelative(2)) :: IR.PopDummyValue(_) :: remaining =>
        Result(IR.ClearCarry() :: IR.AddCarry(imOrAd) :: Nil, remaining)
      case IR.PopDummyValue(_) :: IR.Load(imOrAd, AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) :: remaining =>
        Result(IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd), AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) :: Nil, remaining)
      case IR.PopDummyValue(_) :: IR.TransferAccumulatorTo(YReg()) :: IR.Load(imOrAd, AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) :: IR.TransferYTo(AReg()) :: remaining =>
        Result(IR.TransferAccumulatorTo(YReg()) :: IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd), AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) :: IR.TransferYTo(AReg()) ::  Nil, remaining)
      case _ =>
        Result(instructions.head :: Nil, instructions.tail)
    Optimise.stackUsage(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }

  //Actions:
  //  Finds incrementing a direct memory address
  //    load is 2, clc is 2, Add is 4, store is 4 cycles, so 12 cycles
  //    A single inc is 6, so can incriment once or twice with possible speedup and smaller code
  //  Find decrementing a direct memory address
  //    As with incrementing, but bc of how subtraction works, the immediate will be pushed to the stack
  //    Therefore this will save more
  @tailrec
  def directAddresses(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.Load(Immediate(x), AReg()) :: IR.ClearCarry() :: IR.AddCarry(Direct(address1)) ::
        IR.Store(Direct(address2), AReg()) :: remaining if x <= 2 && address1 == address2 && address1 < 0x1FFF =>
        Result((for i <- Range(0, x) yield IR.IncrementMemory(Direct(address1))).toList, remaining)

      case IR.Load(Immediate(x), reg1) :: IR.PushRegister(reg2) :: IR.Load(Direct(address1), AReg()) ::
        IR.SetCarry() :: IR.SubtractCarry(StackRelative(2)) :: IR.PopDummyValue(_) ::
        IR.Store(Direct(address2), AReg()) :: remaining if x <= 2 && address1 == address2 && reg1 == reg2 && address1 < 0x1FFF =>
        Result((for i <- Range(0, x) yield IR.DecrementMemory(Direct(address1))).toList, remaining)
      case _ =>
        Result(instructions.head :: Nil, instructions.tail)

    Optimise.directAddresses(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }

  //Actions:
  //  Finds places where you store then load the same thing to the same place
  //    Saves the load cycles
  //  Finds places where you store then load the same thing to different places
  //    In general a transfer would be faster, they are always 2 cycles, stack relative loading may be 4 or more
  //  Finds pointer indexing with x when it could be an indirection
  //    Saves a transfer and indexed load, if you can index it
  //  Finds loading something into A then transfering to X/Y, when that could be loaded directly into X/Y
  //    Changes to just load into the register
  //  Finds reloading the register with a value already in it (from push or transfer)
  //    Removes the second load
  @tailrec
  def registerUsage(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.Store(location1, reg1) :: IR.Load(location2, reg2) :: remaining if location1 == location2 =>
        if(reg1 == reg2) {
          Result(IR.Store(location1, reg1) :: Nil, remaining)
        }
        else{
          Result(IR.Store(location1, reg1) :: (
            reg1 match
              case AReg() => IR.TransferAccumulatorTo(reg2)
              case XReg() => IR.TransferXTo(reg2)
              case YReg() => IR.TransferYTo(reg2)
            ) :: Nil, remaining)
        }
      case IR.Load(address, AReg()) :: transfer :: IR.Load(DirectIndexed(directOffset, XReg()), AReg()) :: remaining=>
        val validTransfer = transfer match
          case IR.TransferToX(AReg()) => true
          case IR.TransferAccumulatorTo(XReg()) => true
          case _ => false
        if(validTransfer){
          address match
            case Direct(directAddress) => Result(IR.Load(DirectIndirect(directAddress + directOffset), AReg()) :: Nil, remaining)
            case StackRelative(stackAddress) => Result(IR.Load(Immediate(directOffset), YReg()) :: IR.Load(StackRelativeIndirectIndexed(stackAddress,YReg()), AReg()) :: Nil, remaining)
            case DirectIndexed(directAddress, indexReg) if directOffset == 0 => Result(IR.Load(DirectIndexedIndirect(directAddress, indexReg), AReg()) :: Nil, remaining)
            case _ => Result(instructions.head :: Nil, instructions.tail)
        }
        else{
          Result(instructions.head :: Nil, instructions.tail)
        }
      case IR.Load(imOrAd, AReg()) :: transfer :: remaining
        if imOrAd.isInstanceOf[Immediate] | imOrAd.isInstanceOf[Direct]
      =>
        transfer match
          case IR.TransferToX(AReg()) => Result(IR.Load(imOrAd, XReg()) :: Nil, remaining)
          case IR.TransferAccumulatorTo(XReg()) => Result(IR.Load(imOrAd, XReg()) :: Nil, remaining)
          case IR.TransferToY(AReg()) => Result(IR.Load(imOrAd, YReg()) :: Nil, remaining)
          case IR.TransferAccumulatorTo(YReg()) => Result(IR.Load(imOrAd, YReg()) :: Nil, remaining)
          case _ => Result(instructions.head :: Nil, instructions.tail)
      case IR.TransferToX(AReg()) :: IR.PushRegister(XReg()) :: remaining =>
        Result(IR.PushRegister(AReg()) :: Nil, remaining)
      case _ =>
        Result(instructions.head :: Nil, instructions.tail)
    Optimise.registerUsage(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }

  //  Finds places where you either add 1 or are adding to 1
  //    Replaces by incrementing the accumulator. Saves at minimum 2 cycles
  @tailrec
  def hardwareQuirks(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] ={
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.Load(Immediate(1), AReg()) :: IR.ClearCarry() :: IR.AddCarry(something) :: remaining =>
        Result(IR.Load(something, AReg()) :: IR.IncrementReg(AReg()) :: Nil, remaining)
      case IR.ClearCarry() :: IR.AddCarry(Immediate(1)) :: remaining =>
        Result(IR.IncrementReg(AReg()) :: Nil, remaining)
      case IR.ShiftLeft(AReg(), num) :: remaining if num >= 8=>
        Result(IR.ExchangeAccumulatorBytes() :: IR.AND(Immediate(0xFF00)) :: (if (num == 8) Nil else IR.ShiftLeft(AReg(), num - 8) :: Nil), remaining)
      case IR.ShiftRight(AReg(), num) :: remaining if num >= 8=>
        Result(IR.ExchangeAccumulatorBytes() :: IR.AND(Immediate(0x00FF)) :: (if (num == 8) Nil else IR.ShiftRight(AReg(), num - 8) :: Nil), remaining)
      case IR.Load(Immediate(0), reg) :: IR.Store(Direct(somewhere), reg2) :: remaining if reg == reg2 && somewhere < 0x1FFF =>
        Result(IR.SetZero(Direct(somewhere)) :: Nil, remaining)
      case IR.Load(Immediate(0), reg1) :: IR.Compare(someAddress, reg2) :: IR.BranchIfNotEqual(label) :: next :: remaining
        if reg1 == reg2 && someAddress.isInstanceOf[Address] && !next.isInstanceOf[IR.Branch] =>
        Result(IR.Load(someAddress, reg1) :: IR.BranchIfNotEqual(label) :: Nil, next :: remaining)
      case IR.Load(someAddress, reg1) :: IR.Compare(Immediate(0), reg2) :: IR.BranchIfNotEqual(label) :: next :: remaining
        if reg1 == reg2 && someAddress.isInstanceOf[Address] && !next.isInstanceOf[IR.Branch] =>
        Result(IR.Load(someAddress, reg1) :: IR.BranchIfNotEqual(label) :: Nil, next :: remaining)
      case IR.Load(Immediate(1), reg1) :: IR.Compare(Immediate(1), reg2) :: IR.BranchIfEqual(somewhere) :: remaining if reg1 == reg2 =>
        Result(IR.BranchShort(somewhere) :: Nil, remaining)
      case IR.Load(Immediate(1), reg1) :: IR.Compare(Immediate(1), reg2) :: IR.BranchIfNotEqual(_) :: remaining if reg1 == reg2 =>
        Result(Nil, remaining)
      case _ => Result(instructions.head :: Nil, instructions.tail)
    Optimise.hardwareQuirks(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }

  //Not finished due to bad IR planning. The most likely or easy cases are caught though
  def transfers(instructions: List[IR.Instruction]) : List[IR.Instruction] =
    instructions match
      case Nil => Nil
      case IR.TransferToX(otherReg) :: IR.TransferXTo(otherReg2) :: remaining if otherReg == otherReg2 =>
        Optimise.transfers(remaining)
      case IR.TransferAccumulatorTo(XReg()) :: IR.TransferXTo(AReg()) :: remaining =>
        Optimise.transfers(remaining)
      case IR.TransferAccumulatorTo(XReg()) :: IR.TransferToAccumulator(XReg()) :: remaining =>
        Optimise.transfers(remaining)
      case IR.TransferToX(AReg()) :: IR.TransferToAccumulator(XReg()) :: remaining =>
        Optimise.transfers(remaining)
      case IR.TransferToY(otherReg) :: IR.TransferYTo(otherReg2) :: remaining if otherReg == otherReg2 =>
        Optimise.transfers(remaining)
      case IR.TransferToAccumulator(otherReg) :: IR.TransferAccumulatorTo(otherReg2) :: remaining if otherReg == otherReg2 =>
        Optimise.transfers(remaining)
      case _ => instructions.head :: Optimise.transfers(instructions.tail)


  /*

    >>## The most important optimisation to have ##<<

    By checking how long the CRT beam moves during an update frame of snake, all other
    optimsations together gave a 1.16 optimisation over raw tree-walker output
    But adding this in, it reached 1.22.
    It trippled the speedup from the setup
    Bubbling instructions up past independant operations (especially pops and pushes)
    lets the other sweeps pick up on more optimisation oportunities by reordering the
    order of computation in the program

    Some more work could probably be done but a lot of them would benefit from having more IR classes
    This would make it just a bit more messy

    At some point, these should probably be split into three functions
    The dummy push, dummy pull, and other bubbles function
    tbh the push pop one *may* be able to be one function with some spicy arguments

  */
  @tailrec
  def bubbleUpInstructions(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.PopDummyValue(dummyReg) :: afterDummey =>
        afterDummey match
          case IR.SetZero(address) :: remaining =>
            sweepDidWork = true
            Result(IR.SetZero(address) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          case IR.Load(imOrAd, lReg) :: IR.Store(StackRelative(2), pReg) :: IR.JumpLongSaveReturn(arity1Func) :: remaining if lReg == pReg && dummyReg != lReg && functionArity(arity1Func.name) == 1 =>
            sweepDidWork = true
            Result(IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd), lReg) :: IR.Store(StackRelative(2), pReg) :: IR.JumpLongSaveReturn(arity1Func) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          //Auto converts the second argument; will bubble on next pass if arity 2
          case IR.Load(imOrAd1, lReg1) :: IR.Store(StackRelative(2), pReg1) :: IR.Load(imOrAd2, lReg2) :: IR.PushRegister(pReg2) :: IR.JumpLongSaveReturn(arity2Func) :: remaining if lReg1 == pReg1 && lReg2 == pReg2 && functionArity(arity2Func.name) == 2 =>
            sweepDidWork = true
            Result(IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd1), lReg1) :: IR.Store(StackRelative(4), pReg1) :: IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd2), lReg2) :: IR.Store(StackRelative(2), pReg2) :: IR.JumpLongSaveReturn(arity2Func) :: Nil, remaining)
          case IR.Load(imOrAd1, lReg1) :: IR.Store(StackRelative(4), pReg1) :: IR.Load(imOrAd2, lReg2) :: IR.Store(StackRelative(2), pReg2) :: IR.JumpLongSaveReturn(arity2Func) :: remaining if lReg1 == pReg1 && lReg2 == pReg2 && functionArity(arity2Func.name) == 2 =>
            sweepDidWork = true
            Result(IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd1), lReg1) :: IR.Store(StackRelative(4), pReg1) :: IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd2), lReg2) :: IR.Store(StackRelative(2), pReg2) :: IR.JumpLongSaveReturn(arity2Func) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          case IR.Load(imOrAd, lReg) :: IR.Store(ad, pReg) :: remaining if lReg == pReg && dummyReg != lReg =>
            sweepDidWork = true
            Result(IR.Load(getImmediateOrAddressWithoutStackPush(imOrAd), lReg) :: IR.Store(getAddressWithoutStackPush(ad), pReg) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          case IR.JumpLongSaveReturn(label) :: remaining if functionArity(label.name) == 0 =>
            sweepDidWork = true
            Result(IR.JumpLongSaveReturn(label) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          case IR.Store(ad, pReg) :: remaining if pReg != dummyReg =>
            sweepDidWork = true
            Result(IR.Store(getAddressWithAlteredStack(ad, 2), pReg) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          case arithmetic :: remaining if arithmetic.isInstanceOf[IR.SingleArgArithmetic] =>
            sweepDidWork = true
            Result(IR.bubbleArithmetic(arithmetic.asInstanceOf[IR.SingleArgArithmetic], 2) :: IR.PopDummyValue(dummyReg) :: Nil, remaining)
          case _ => Result(instructions.head :: Nil, instructions.tail)

      case IR.ReturnLong() :: next :: remaining if next != IR.Spacing() && !next.isInstanceOf[IR.PutLabel] =>
        Result(IR.ReturnLong() :: Nil, remaining)

      //Maybe this can be changed to decriments in the seconary sweep
      case IR.Load(Immediate(smaller), reg1) :: IR.Store(Direct(dA), reg1S) :: IR.Load(Immediate(larger), reg2) :: IR.Store(Direct(dB), reg2S) :: IR.Load(imOrAd, reg3) :: remaining
        if smaller < larger && reg1 == reg1S && reg2 == reg2S && reg1 == reg2 && reg2 == reg3 && dA != dB=>
        sweepDidWork = true
        Result(IR.Load(Immediate(larger), reg1) :: IR.Store(Direct(dB), reg1) :: IR.Load(Immediate(smaller), reg1) :: IR.Store(Direct(dA), reg1) :: Nil,IR.Load(imOrAd, reg3) :: remaining)

      case IR.PushDummyValue(dummyReg) :: afterDummy =>
        afterDummy match
          case IR.JumpLongSaveReturn(subroutine) :: remaining if functionArity(subroutine.name) == 0 =>
            sweepDidWork = true
            Result(IR.JumpLongSaveReturn(subroutine) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case IR.Load(imOrAd, ar1) :: IR.PushRegister(pr1) :: IR.JumpLongSaveReturn(arity1) :: remaining if ar1 == pr1 && functionArity(arity1.name) == 1 =>
            sweepDidWork = true
            Result(IR.Load(getAddressWithAlteredStack(imOrAd, -2), ar1) :: IR.PushRegister(pr1) :: IR.JumpLongSaveReturn(arity1) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case IR.Load(imOrAd, ar1) :: IR.PushRegister(pr1) :: IR.Load(imOrAd2, ar2) :: IR.PushRegister(pr2) :: IR.JumpLongSaveReturn(arity2) :: remaining if ar1 == pr1 && ar2 == pr2 && functionArity(arity2.name) == 2 =>
            sweepDidWork = true
            Result(IR.Load(getAddressWithAlteredStack(imOrAd, -2), ar1) :: IR.PushRegister(pr1) :: IR.Load(getAddressWithAlteredStack(imOrAd2, -2), ar2) :: IR.PushRegister(pr2) :: IR.JumpLongSaveReturn(arity2) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case IR.JumpLongSaveReturn(somewhere) :: remaining =>
            sweepDidWork = true
            Result(IR.JumpLongSaveReturn(somewhere) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case arithmetic :: remaining if arithmetic.isInstanceOf[IR.SingleArgArithmetic] =>
            sweepDidWork = true
            Result(IR.bubbleArithmetic(arithmetic.asInstanceOf[IR.SingleArgArithmetic], -2) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case IR.Store(address, reg) :: remaining if reg != dummyReg =>
            sweepDidWork = true
            address match
              case StackRelative(2) => Result(IR.PushRegister(reg) :: Nil, remaining)
              case _ => Result(IR.Store(getAddressWithAlteredStack(address, -2), reg) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case IR.Load(imOrAd, AReg()) :: IR.Store(address, AReg()) :: remaining =>
            val correctedImOrAd = getAddressWithAlteredStack(imOrAd, -2)
            address match
              case StackRelative(2) =>
                Result(IR.Load(correctedImOrAd, AReg()) :: IR.PushRegister(AReg()):: Nil, remaining)
              case _ =>
                Result(IR.Load(correctedImOrAd, AReg()) :: IR.Store(getAddressWithAlteredStack(address, -2), AReg()) :: IR.PushDummyValue(dummyReg) :: Nil, remaining)
          case _ => Result(instructions.head :: Nil, instructions.tail)
      case _ =>
        Result(instructions.head :: Nil, instructions.tail)

    Optimise.bubbleUpInstructions(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }


  /*
    A seconary pass after the main passes are done
    It is used for removing small things that other optimisation sweeps may have been able to use as something bigger
    For example, a convoluted return sequence using a stack frame that is already at the bottom of the stack
    Or doing the stack pointer at all, rather than just popping it
    Or returning after a long jump if it is the last thing in a function
  */
  @tailrec
  def removeUnneccesaryDetails(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.TransferAccumulatorTo(YReg()) :: IR.Load(StackRelative(2), AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) :: IR.TransferYTo(AReg()) :: remaining =>
        Result(IR.PopDummyValue(XReg()) :: Nil, remaining)
      case IR.Load(StackRelative(2), AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) :: remaining =>
        Result(IR.PopDummyValue(XReg()) :: Nil, remaining)
      case IR.JumpLongSaveReturn(subroutine) :: IR.PopDummyValue(dReg) :: IR.ReturnLong() :: remaining if functionArity(subroutine.name) == 0 =>
        Result(IR.PopDummyValue(dReg) :: IR.JumpLongWithoutReturn(subroutine).addComment("Return chain can be removed") :: Nil, remaining)
      case IR.JumpLongSaveReturn(somewhere) :: IR.ReturnLong() :: remaining =>
        Result(IR.JumpLongWithoutReturn(somewhere).addComment("Return  chain can be removed") :: Nil, remaining)
      //This can happen when returning a constant
      case IR.Load(something, YReg()) :: IR.PopDummyValue(XReg()) :: IR.TransferYTo(AReg()) :: remaining =>
        Result(IR.Load(something, AReg()) :: IR.PopDummyValue(XReg()) :: Nil, remaining)
      case _ =>
        Result(instructions.head :: Nil, instructions.tail)

    Optimise.removeUnneccesaryDetails(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }

  /*
    If you store a 15, then a 16, then a 17, etc
    Which tends to happen if moving consecutive tiles to a background
    It is better to incriment A than keep changing it
    So the smaller numbers are bubbled up
    And then this function does all the small scale tweaking
    To make it as efficient as possible
  */
  def getResultOfRegister1Off(firstPassage: List[IR.Instruction], startingValue: Int, currentReg: TargetReg, remaining: List[IR.Instruction]): Result = {
    var afterOperations = remaining
    var lookForRedundency = true
    val betterOrder = ListBuffer.empty[IR.Instruction].addAll(firstPassage)
    var currentNumber = startingValue
    while (lookForRedundency) do {
      afterOperations match
        case nonRegAlteringOperation :: IR.Load(Immediate(nextValue), reg) :: otherRemaining if nonRegAlteringOperation.isInstanceOf[IR.NonRegAltering] && reg == currentReg && (nextValue - currentNumber).abs <= 1.0 =>
          afterOperations = otherRemaining
          if(nextValue == currentNumber){
            betterOrder.addOne(nonRegAlteringOperation)
          }
          else if(nextValue == (currentNumber - 1)){
            betterOrder.addAll(nonRegAlteringOperation :: IR.DecrementReg(reg) :: Nil)
          }
          else{
            betterOrder.addAll(nonRegAlteringOperation :: IR.IncrementReg(reg) :: Nil)
          }
          currentNumber = nextValue
        case _ =>
          lookForRedundency = false
    }
    Result(betterOrder.toList, afterOperations)
  }

  /*
    If you are using the same value across several assignments, you will load that value each time
    Which is really not that efficient
    So you can go through and make sure
      `setBackrgoundsActive(true, false, false, false, false)`
    Only loads 0 once
  */
  @tailrec
  def removeRedundentRegisterLoads(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.Load(Immediate(value), reg) :: nonRegAlteringOperation :: IR.Load(Immediate(value2), reg2) :: remaining if reg == reg2 && (value - 1) == value2 && nonRegAlteringOperation.isInstanceOf[IR.NonRegAltering] =>
        getResultOfRegister1Off(IR.Load(Immediate(value), reg) :: nonRegAlteringOperation :: IR.DecrementReg(reg) :: Nil, value2, reg, remaining)
      case IR.Load(Immediate(value), reg) :: nonRegAlteringOperation :: IR.Load(Immediate(value2), reg2) :: remaining if reg == reg2 && (value + 1) == value2 && nonRegAlteringOperation.isInstanceOf[IR.NonRegAltering] =>
        getResultOfRegister1Off(IR.Load(Immediate(value), reg) :: nonRegAlteringOperation :: IR.IncrementReg(reg) :: Nil, value2, reg, remaining)
      case IR.Load(Immediate(value), reg) :: nonRegAlteringOperation :: IR.Load(Immediate(value2), reg2) :: remaining if reg == reg2 && value == value2 && nonRegAlteringOperation.isInstanceOf[IR.NonRegAltering] =>
        getResultOfRegister1Off(IR.Load(Immediate(value), reg) :: nonRegAlteringOperation :: Nil, value2, reg, remaining)
      case _ =>
        Result(instructions.head :: Nil, instructions.tail)

    Optimise.removeRedundentRegisterLoads(next.remaining, alreadyOptimised ::: next.optimisedFragment)
  }

  @tailrec
  def secondarySweeps(instructions: List[IR.Instruction]): List[IR.Instruction] = {
    val toReturn = removeRedundentRegisterLoads(removeUnneccesaryDetails(instructions))
    if(toReturn.length == instructions.length){
      return toReturn
    }
    secondarySweeps(toReturn)
  }

  def apply(instructions: List[IR.Instruction]): List[IR.Instruction] = {
    var result = instructions
    if (GlobalData.optimisationFlags.optimiseRegisterUsage) {
      result = Optimise.registerUsage(result)
    }
    if (GlobalData.optimisationFlags.optimiseStackUsage) {
      result = Optimise.stackUsage(result)
    }
    if (GlobalData.optimisationFlags.optimiseDirectAddresses) {
      result = Optimise.directAddresses(result)
    }
    if(GlobalData.optimisationFlags.optimiseTransfers){
      result = Optimise.transfers(result)
    }
    if(GlobalData.optimisationFlags.optimiseHardwareQuirks){
      result = Optimise.hardwareQuirks(result)
    }
    if (GlobalData.optimisationFlags.optimiseBubbleUp) {
      result = Optimise.bubbleUpInstructions(result)
    }

    println(result)
    println("-----")

    if(result.length == instructions.length && !sweepDidWork){
      secondarySweeps(result)
    }
    else{
      sweepDidWork = false
      apply(result)
    }
  }

  def main(args: Array[String]): Unit = {
    val in: List[IR.Instruction] =
      IR.PushDummyValue(XReg()) :: IR.PushDummyValue(XReg()) ::
      IR.Load(Immediate(0), AReg()) :: IR.Store(StackRelative(4), AReg()) ::
        IR.Load(Immediate(20), AReg()) :: IR.Store(StackRelative(2), AReg()) :: Nil

    functionArity.addOne("joke", 2)

    println(in)
    println("---")
    println(apply(in))
  }
}

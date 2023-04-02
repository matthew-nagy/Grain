package TreeWalker

import Grain.GlobalData
import TreeWalker.IR.{Instruction, PushRegister}

import scala.annotation.tailrec

object Optimise {

  case class Result(optimisedFragment: List[IR.Instruction], remaining: List[IR.Instruction])

  //Actions:
  //  Finds dummy pulls followed by a dummy push, gets rid of them->
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
  @tailrec
  def stackUsage(instructions: List[IR.Instruction], alreadyOptimised: List[IR.Instruction] = Nil): List[IR.Instruction] = {
    val next: Result = instructions match
      case Nil => return alreadyOptimised
      case IR.PopDummyValue(_) :: IR.PushDummyValue(_) :: remaining =>
        Result(Nil, remaining)
      case IR.PopDummyValue(_) :: IR.PushRegister(AReg()) :: remaining =>
        Result(IR.Store(StackRelative(2), AReg()) :: Nil, remaining)
      case IR.PushDummyValue(_) :: IR.Load(loadOp, AReg()) :: IR.Store(StackRelative(2), AReg()) :: remaining =>
        Result(IR.Load(loadOp, AReg()) :: IR.PushRegister(AReg()) :: Nil, remaining)
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
  def directAddresses(instructions: List[IR.Instruction]): List[IR.Instruction] = {
    instructions match
      case Nil => Nil
      case IR.Load(Immediate(x), AReg()) :: IR.ClearCarry() :: IR.AddCarry(Direct(address1)) ::
        IR.Store(Direct(address2), AReg()) :: remaining if x <= 2 && address1 == address2 =>
        (for i <- Range(0, x) yield IR.IncrementMemory(Direct(address1))).toList ::: Optimise.directAddresses(remaining)

      case IR.Load(Immediate(x), reg1) :: IR.PushRegister(reg2) :: IR.Load(Direct(address1), AReg()) ::
        IR.SetCarry() :: IR.SubtractCarry(StackRelative(2)) :: IR.PopDummyValue(_) ::
        IR.Store(Direct(address2), AReg()) :: remaining if x <= 2 && address1 == address2 && reg1 == reg2 =>
        (for i <- Range(0, x) yield IR.DecrementMemory(Direct(address1))).toList ::: Optimise.directAddresses(remaining)

      case _ =>
        instructions.head :: Optimise.directAddresses(instructions.tail)
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
  def registerUsage(instructions: List[IR.Instruction]): List[IR.Instruction] = {
    instructions match
      case Nil => Nil
      case IR.Store(location1, reg1) :: IR.Load(location2, reg2) :: remaining if location1 == location2 =>
        if(reg1 == reg2) {
          IR.Store(location1, reg1) :: Optimise.registerUsage(remaining)
        }
        else{
          IR.Store(location1, reg1) :: (
            reg1 match
              case AReg() => IR.TransferAccumulatorTo(reg2)
              case XReg() => IR.TransferXTo(reg2)
              case YReg() => IR.TransferYTo(reg2)
            ) :: Optimise.registerUsage(remaining)
        }
      case IR.Load(address, AReg()) :: transfer :: IR.Load(DirectIndexed(directOffset, XReg()), AReg()) :: remaining=>
        val validTransfer = transfer match
          case IR.TransferToX(AReg()) => true
          case IR.TransferAccumulatorTo(XReg()) => true
          case _ => false
        if(validTransfer){
          address match
            case Direct(directAddress) => IR.Load(DirectIndirect(directAddress + directOffset), AReg()) :: Optimise.registerUsage(remaining)
            case StackRelative(stackAddress) => IR.Load(Immediate(directOffset), YReg()) :: IR.Load(StackRelativeIndirectIndexed(stackAddress,YReg()), AReg()) :: Optimise.registerUsage(remaining)
            case DirectIndexed(directAddress, indexReg) if directOffset == 0 => IR.Load(DirectIndexedIndirect(directAddress, indexReg), AReg()) :: Optimise.registerUsage(remaining)
            case _ => instructions.head :: Optimise.registerUsage(instructions.tail)
        }
        else{
          instructions.head :: Optimise.registerUsage(instructions.tail)
        }
      case IR.Load(imOrAd, AReg()) :: transfer :: remaining
        if imOrAd.isInstanceOf[Immediate] | imOrAd.isInstanceOf[Direct]
      =>
        transfer match
          case IR.TransferToX(AReg()) => IR.Load(imOrAd, XReg()) :: Optimise.registerUsage(remaining)
          case IR.TransferAccumulatorTo(XReg()) => IR.Load(imOrAd, XReg()) :: Optimise.registerUsage(remaining)
          case IR.TransferToY(AReg()) => IR.Load(imOrAd, YReg()) :: Optimise.registerUsage(remaining)
          case IR.TransferAccumulatorTo(YReg()) => IR.Load(imOrAd, YReg()) :: Optimise.registerUsage(remaining)
          case _ => instructions.head :: Optimise.registerUsage(instructions.tail)

      case _ =>
        instructions.head :: Optimise.registerUsage(instructions.tail)
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


    if(result.length == instructions.length){
      result
    }
    else{
      apply(result)
    }
  }
}

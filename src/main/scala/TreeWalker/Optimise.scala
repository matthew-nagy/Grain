package TreeWalker

import Grain.GlobalData
import TreeWalker.IR.PushRegister

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
      case _ =>
        instructions.head :: Optimise.registerUsage(instructions.tail)
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
    result
  }
}

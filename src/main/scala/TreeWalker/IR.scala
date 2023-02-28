package TreeWalker

enum RegisterGroup:
  case XY, A, AXY

sealed trait TargetReg
sealed trait GeneralPurposeReg extends TargetReg
sealed trait AnyReg
case class XReg() extends GeneralPurposeReg with AnyReg
case class YReg() extends GeneralPurposeReg with AnyReg
case class AReg() extends TargetReg with AnyReg

case class DirectPageReg() extends AnyReg
case class StackPointerReg() extends AnyReg
case class DataPageReg() extends AnyReg


sealed trait Operand
sealed trait ImmediateOrAddress
sealed trait AccumulatorOrAddress
case class Immediate(value: Int) extends Operand with ImmediateOrAddress
case class Label(name: String) extends Operand
case class TargetAccumulator() extends Operand with AccumulatorOrAddress

sealed trait Address extends Operand with ImmediateOrAddress with AccumulatorOrAddress
case class Direct(address: Int) extends Address
case class DirectIndexed(address: Int, by: GeneralPurposeReg) extends Address
case class DirectIndirect(address: Int) extends Address
case class DirectIndirectIndexed(address: Int, by: GeneralPurposeReg) extends Address
case class StackRelative(offset: Int) extends Address
case class StackRelativeIndirectIndexed(offset: Int, by: GeneralPurposeReg) extends Address

package IR:
  sealed trait Instruction
  sealed trait Arithmetic extends Instruction
  sealed trait LoadStore extends Instruction
  sealed trait Transfer extends Instruction
  sealed trait Branch extends Instruction
  sealed trait JumpCall extends Instruction
  sealed trait Interrupts extends Instruction
  sealed trait ProcessorFlags extends Instruction
  sealed trait StackManipulation extends Instruction
  sealed trait Misc extends Instruction
  //Arithmetic
  case class AddCarry(op: ImmediateOrAddress) extends Arithmetic
  case class SubtractCarry(op: ImmediateOrAddress) extends Arithmetic
  case class AND(op: ImmediateOrAddress) extends Arithmetic
  case class EOR(op: ImmediateOrAddress) extends Arithmetic
  case class ORA(op: ImmediateOrAddress) extends Arithmetic

  case class ShiftLeft(op: AccumulatorOrAddress) extends Arithmetic
  case class ShiftRight(op: AccumulatorOrAddress) extends Arithmetic
  case class RotateLeft(op: AccumulatorOrAddress) extends Arithmetic
  case class RotateRight(op: AccumulatorOrAddress) extends Arithmetic

  case class BitTest(op: ImmediateOrAddress) extends Arithmetic

  case class DecrementReg(reg: TargetReg) extends Arithmetic
  case class DecrementMemory(address: Address) extends Arithmetic
  case class IncrementReg(reg: TargetReg) extends Arithmetic
  case class IncrementMemory(address: Address) extends Arithmetic
  case class NOP() extends Arithmetic
  case class ExchangeAccumulatorBytes() extends Arithmetic

  //Load Store
  case class Load(op: ImmediateOrAddress, reg: TargetReg) extends LoadStore
  case class Store(address: Address, reg: TargetReg) extends LoadStore
  case class SetZero(address: Address) extends LoadStore

  //Transfer
  case class TransferToAccumulator(reg: TargetReg | StackPointerReg | DirectPageReg) extends Transfer
  case class TransferAccumulatorTo(reg: TargetReg | StackPointerReg | DirectPageReg) extends Transfer
  case class TransferXTo(reg: TargetReg | StackPointerReg) extends Transfer
  case class TransferToX(reg: TargetReg | StackPointerReg) extends Transfer
  case class TransferYTo(reg: TargetReg) extends Transfer
  case class TransferToY(reg: TargetReg) extends Transfer

  //Branch
  case class Compare(op: ImmediateOrAddress, reg: TargetReg) extends Branch
  case class BranchIfNoCarry(label: Label) extends Branch
  case class BranchIfCarrySet(label: Label) extends Branch
  case class BranchIfNotEqual(label: Label) extends Branch
  case class BranchIfEqual(label: Label) extends Branch
  case class BranchIfPlus(label: Label) extends Branch
  case class BranchIfMinus(label: Label) extends Branch
  case class BranchIfNoOverflow(label: Label) extends Branch
  case class BranchIfOverflow(label: Label) extends Branch
  case class BranchShort(label: Label) extends Branch
  case class BranchLong(label: Label) extends Branch

  //Jump and Call
  case class JumpShortWithoutReturn(label: Label) extends JumpCall
  case class JumpLongWithoutReturn(label: Label) extends JumpCall
  case class JumpShortSaveReturn(label: Label) extends JumpCall
  case class JumpLongSaveReturn(label: Label) extends JumpCall
  case class ReturnShort() extends JumpCall
  case class ReturnLong() extends JumpCall

  //Interrupts
  case class Break(vector: Immediate) extends Interrupts
  case class Cop(vector: Immediate) extends Interrupts
  case class ReturnFromInterrupt() extends Interrupts
  case class StopClock() extends Interrupts
  case class WaitForInterrupt() extends Interrupts

  //Processor flag
  case class ClearCarry() extends ProcessorFlags
  case class SetCarry() extends ProcessorFlags
  case class CearOverflow() extends ProcessorFlags
  case class EnableInterrupts() extends ProcessorFlags
  case class DisableInterrupts() extends ProcessorFlags
  case class SetReg16Bit(group: RegisterGroup) extends ProcessorFlags //These are just SEP and REP but hidden
  case class SetReg8Bit(group: RegisterGroup) extends ProcessorFlags
  case class ResetStatusBit(value: Immediate) extends ProcessorFlags
  case class SetStatusBit(value: Immediate) extends ProcessorFlags
  case class ExchangeCarryFlagWithEmulation() extends ProcessorFlags

  //Stack Instructions
  case class PushRegister(reg: AnyReg) extends StackManipulation
  case class PopRegister(reg: AnyReg) extends StackManipulation
  case class PushProcessorStatus() extends StackManipulation
  case class PushEffectiveAddress(address: Address) extends StackManipulation
  case class PushEffectiveAddressIndirect(address: Address) extends StackManipulation
  case class PushEffectiveAddressRelative(address: Address) extends StackManipulation

  //Misc
  case class MovePositive(fromPage: Byte, toPage: Byte) extends Misc
  case class MoveNegative(fromPage: Byte, toPage: Byte) extends Misc
  case class PutLabel(labelName: Label) extends Misc
  case class UserAssembly(code: String) extends Misc
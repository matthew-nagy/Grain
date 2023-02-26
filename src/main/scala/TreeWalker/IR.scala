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
  //Arithmetic
  case class AddCarry(op: ImmediateOrAddress) extends Instruction
  case class SubtractCarry(op: ImmediateOrAddress) extends Instruction
  case class AND(op: ImmediateOrAddress) extends Instruction
  case class EOR(op: ImmediateOrAddress) extends Instruction
  case class ORA(op: ImmediateOrAddress) extends Instruction

  case class ShiftLeft(op: AccumulatorOrAddress) extends Instruction
  case class ShiftRight(op: AccumulatorOrAddress) extends Instruction
  case class RotateLeft(op: AccumulatorOrAddress) extends Instruction
  case class RotateRight(op: AccumulatorOrAddress) extends Instruction

  case class BitTest(op: ImmediateOrAddress) extends Instruction
  case class Compare(op: ImmediateOrAddress, reg: TargetReg) extends Instruction

  case class DecrementReg(reg: TargetReg) extends Instruction
  case class DecrementMemory(address: Address) extends Instruction
  case class IncrementReg(reg: TargetReg) extends Instruction
  case class IncrementMemory(address: Address) extends Instruction
  case class NOP() extends Instruction
  case class ExchangeAccumulatorBytes() extends Instruction

  //Load Store
  case class Load(op: ImmediateOrAddress, reg: TargetReg) extends Instruction
  case class Store(address: Address, reg: TargetReg) extends Instruction
  case class SetZero(address: Address) extends Instruction

  //Transfer
  case class TransferToAccumulator(reg: TargetReg | StackPointerReg | DirectPageReg) extends Instruction
  case class TransferAccumulatorTo(reg: TargetReg | StackPointerReg | DirectPageReg) extends Instruction
  case class TransferXTo(reg: TargetReg | StackPointerReg) extends Instruction
  case class TransferToX(reg: TargetReg | StackPointerReg) extends Instruction
  case class TransferYTo(reg: TargetReg) extends Instruction
  case class TransferToY(reg: TargetReg) extends Instruction

  //Branch
  case class BranchIfNoCarry(label: Label) extends Instruction
  case class BranchIfCarrySet(label: Label) extends Instruction
  case class BranchIfNotEqual(label: Label) extends Instruction
  case class BranchIfEqual(label: Label) extends Instruction
  case class BranchIfPlus(label: Label) extends Instruction
  case class BranchIfMinus(label: Label) extends Instruction
  case class BranchIfNoOverflow(label: Label) extends Instruction
  case class BranchIfOverflow(label: Label) extends Instruction
  case class BranchShort(label: Label) extends Instruction
  case class BranchLong(label: Label) extends Instruction

  //Jump and Call
  case class JumpShortWithoutReturn(label: Label) extends Instruction
  case class JumpLongWithoutReturn(label: Label) extends Instruction
  case class JumpShortSaveReturn(label: Label) extends Instruction
  case class JumpLongSaveReturn(label: Label) extends Instruction
  case class ReturnShort(label: Label) extends Instruction
  case class ReturnLong(label: Label) extends Instruction

  //Interrupts
  case class Break(vector: Immediate) extends Instruction
  case class Cop(vector: Immediate) extends Instruction
  case class ReturnFromInterrupt() extends Instruction
  case class StopClock() extends Instruction
  case class WaitForInterrupt() extends Instruction

  //Processor flag
  case class ClearCarry() extends Instruction
  case class SetCarry() extends Instruction
  case class CearOverflow() extends Instruction
  case class EnableInterrupts() extends Instruction
  case class DisableInterrupts() extends Instruction
  case class SetReg16Bit(group: RegisterGroup) extends Instruction //These are just SEP and REP but hidden
  case class SetReg8Bit(group: RegisterGroup) extends Instruction
  case class ResetStatusBit(value: Immediate) extends Instruction
  case class SetStatusBit(value: Immediate) extends Instruction
  case class ExchangeCarryFlagWithEmulation() extends Instruction

  //Stack Instructions
  case class PushRegister(reg: AnyReg) extends Instruction
  case class PopRegister(reg: AnyReg) extends Instruction
  case class PushProcessorStatus() extends Instruction
  case class PushEffectiveAddress(address: Address) extends Instruction
  case class PushEffectiveAddressIndirect(address: Address) extends Instruction
  case class PushEffectiveAddressRelative(address: Address) extends Instruction

  //Misc
  case class MovePositive(fromPage: Byte, toPage: Byte) extends Instruction
  case class MoveNegative(fromPage: Byte, toPage: Byte) extends Instruction
  case class PutLabel(labelName: String) extends Instruction
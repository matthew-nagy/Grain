package TreeWalker

enum RegisterGroup:
  case XY, A, AXY

sealed trait ImmediateOrAddress
sealed trait AccumulatorOrAddress

sealed trait AnyReg
sealed trait TargetOrStackReg extends AnyReg
sealed trait TargetReg extends TargetOrStackReg with AnyReg
sealed trait GeneralPurposeReg extends TargetReg with AnyReg
case class XReg() extends GeneralPurposeReg with TargetOrStackReg with TargetReg
case class YReg() extends GeneralPurposeReg with TargetOrStackReg with TargetReg
case class AReg() extends TargetOrStackReg with AccumulatorOrAddress with TargetReg

case class DirectPageReg() extends AnyReg
case class StackPointerReg() extends TargetOrStackReg


sealed trait Operand
case class Immediate(value: Int) extends Operand with ImmediateOrAddress
case class ImmediateLabel(label: Label) extends Operand with ImmediateOrAddress
case class Label(name: String) extends Operand
sealed trait Address extends Operand with ImmediateOrAddress with AccumulatorOrAddress
case class Direct(address: Int) extends Address
case class DirectIndexed(address: Int, by: GeneralPurposeReg) extends Address
case class DirectIndirect(address: Int) extends Address
case class DirectIndexedIndirect(address: Int, by: GeneralPurposeReg) extends Address
case class DirectIndirectIndexed(address: Int, by: GeneralPurposeReg) extends Address
case class StackRelative(offset: Int) extends Address
case class StackRelativeIndirectIndexed(offset: Int, by: GeneralPurposeReg) extends Address

def getAddressWithAlteredStack(value: Address, stackChange: Int): Address =
  value match
    case StackRelative(offset) => StackRelative(offset + stackChange)
    case StackRelativeIndirectIndexed(offset, by) => StackRelativeIndirectIndexed(offset + stackChange, by)
    case _ => value
def getAddressWithAlteredStack(value: ImmediateOrAddress, stackChange: Int): ImmediateOrAddress =
  value match
    case asAddress: Address => getAddressWithAlteredStack(asAddress, stackChange)
    case _ => value
def getAddressWithAlteredStack(value: AccumulatorOrAddress, stackChange: Int): AccumulatorOrAddress =
  value match
    case asAddress: Address => getAddressWithAlteredStack(asAddress, stackChange)
    case _ => value

package IR:
  sealed trait Instruction{
    private var comment: Option[String] = None
    def getComment: Option[String] = comment
    def addComment(c: String): Instruction = {
      comment = Some(c)
      this
    }
  }
  //When compiled, certain of these 'instructions' won't actually take any ROM size, such as comments or labels
  sealed trait ZeroSizeInstruction
  sealed trait LargeSizeInstruction(generalByteSize: Int)

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
  case class TransferToAccumulator(reg: AnyReg) extends Transfer
  case class TransferAccumulatorTo(reg: AnyReg) extends Transfer
  case class TransferXTo(reg: TargetOrStackReg) extends Transfer
  case class TransferToX(reg: TargetOrStackReg) extends Transfer
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
  case class ClearOverflow() extends ProcessorFlags
  case class EnableInterrupts() extends ProcessorFlags
  case class DisableInterrupts() extends ProcessorFlags
  case class SetReg16Bit(group: RegisterGroup) extends ProcessorFlags //These are just SEP and REP but hidden
  case class SetReg8Bit(group: RegisterGroup) extends ProcessorFlags
  case class ResetStatusBit(value: Immediate) extends ProcessorFlags
  case class SetStatusBit(value: Immediate) extends ProcessorFlags
  case class ExchangeCarryFlagWithEmulation() extends ProcessorFlags

  //Stack Instructions
  case class PushRegister(reg: TargetReg) extends StackManipulation
  case class PopRegister(reg: TargetReg) extends StackManipulation
  case class PushDirectPageRegister() extends StackManipulation
  case class PullDirectPageRegister() extends StackManipulation
  case class PushDataBankRegister() extends StackManipulation
  case class PullDataBankRegister() extends StackManipulation
  case class PushProcessorStatus() extends StackManipulation
  case class PullProcessorStatus() extends StackManipulation
  case class PushValue(value: Immediate) extends StackManipulation
  case class PushAddress(address: Address) extends StackManipulation

  case class PushDummyValue(reg: TargetReg) extends StackManipulation
  case class PopDummyValue(reg: TargetReg) extends StackManipulation

  //Misc
  case class MovePositive(fromPage: Byte, toPage: Byte) extends Misc
  case class MoveNegative(fromPage: Byte, toPage: Byte) extends Misc
  case class PutLabel(labelName: Label) extends Misc with ZeroSizeInstruction
  case class UserAssembly(code: List[String]) extends Misc
  case class Spacing() extends Misc with ZeroSizeInstruction
  case class Bank(bankName: Int) extends Misc with ZeroSizeInstruction
  case class UserData(dataName: String, data: List[String]) extends Misc


  def sizeOfIr(instruction: Instruction): Int =
    instruction match
      case UserAssembly(list) => list.length * 3
      case UserData(_, data) => data.length * 6 //Just because I guess thats how I did it with .dw
      case _: ZeroSizeInstruction => 0
      case _ => 3

  def sizeOfIr(buffer: List[Instruction]): Int = buffer.map(sizeOfIr).sum
  def sizeOfIr(buffer: IRBuffer): Int = sizeOfIr(buffer.toList)

/*

*/
  def getWithStackOffset(instruction: Instruction, stackOffset: Int): Instruction =
    instruction match
      case AddCarry(op) => AddCarry(getAddressWithAlteredStack(op, stackOffset))
      case SubtractCarry(op) => SubtractCarry(getAddressWithAlteredStack(op, stackOffset))
      case AND(op) => AND(getAddressWithAlteredStack(op, stackOffset))
      case EOR(op) => EOR(getAddressWithAlteredStack(op, stackOffset))
      case ORA(op) => ORA(getAddressWithAlteredStack(op, stackOffset))
      case ShiftLeft(op) => ShiftLeft(getAddressWithAlteredStack(op, stackOffset))
      case ShiftRight(op) => ShiftRight(getAddressWithAlteredStack(op, stackOffset))
      case RotateLeft(op) => RotateLeft(getAddressWithAlteredStack(op, stackOffset))
      case RotateRight(op) => RotateRight(getAddressWithAlteredStack(op, stackOffset))
      case BitTest(op) => BitTest(getAddressWithAlteredStack(op, stackOffset))
      case IncrementMemory(mem) => IncrementMemory(getAddressWithAlteredStack(mem, stackOffset))
      case DecrementMemory(mem) => DecrementMemory(getAddressWithAlteredStack(mem, stackOffset))
      case Load(op, reg) => Load(getAddressWithAlteredStack(op, stackOffset), reg)
      case Store(address, reg) => Store(getAddressWithAlteredStack(address, stackOffset), reg)
      case SetZero(address) => SetZero(getAddressWithAlteredStack(address, stackOffset))
      case Compare(address, reg) => Compare(getAddressWithAlteredStack(address, stackOffset), reg)
      case PushAddress(address) => PushAddress(getAddressWithAlteredStack(address, stackOffset))
      case _ => instruction
  def getWithStackOffset(instructions: List[Instruction], stackOffset: Int): List[Instruction] =
    instructions.map(instruction => getWithStackOffset(instruction, stackOffset))
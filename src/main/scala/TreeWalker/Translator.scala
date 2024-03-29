package TreeWalker

object Translator {
  import IR.*

  private def $(values: List[String]): String =
    values.tail.foldLeft(values.head)(_ ++ " " ++ _)

  private def getReg(reg: TargetOrStackReg | TargetReg): String =
    reg match
      case AReg() => "a"
      case XReg() => "x"
      case YReg() => "y"
      case StackPointerReg() => "s"
  private def getImmediate(immediate: Immediate): String =
    "#" ++ immediate.value.toString

  private def bankCorrect(location: Int, isHardware: Boolean): Int = {
    if(location > 0x1FFF && !isHardware){
//      throw new Exception("wut")
      location + 0x7E0000
    } else location
  }
  private def getAddress(address: Address, isHardware: Boolean = false): String =
    address match
      case Direct(address) => bankCorrect(address,isHardware).toString
      case DirectIndexed(address, by) => bankCorrect(address,isHardware).toString ++ ", " ++ getReg(by)
      case DirectIndirect(address) => "(" ++ bankCorrect(address,isHardware).toString ++ ")"
      case DirectIndexedIndirect(address, by) => "(" ++ bankCorrect(address,isHardware).toString ++ ", " ++ getReg(by) ++ ")"
      case DirectIndirectIndexed(address, by) => "(" ++ bankCorrect(address,isHardware).toString ++ "), " ++ getReg(by)
      case StackRelative(offset) => (offset - 1).toString ++ ", s"
      case StackRelativeIndirectIndexed(offset, by) => "(" ++ (offset - 1).toString ++ ", s), " ++ getReg(by)
      case null => throw new Exception("Cannot find this address type")

  private def getBankImmediate(dataImmediate: BankImmediate, dataBankStart: Int): String =
    if(dataImmediate.value == 0x7E)
      "#$7E"
    else
      "#" ++ (dataImmediate.value + dataBankStart).toString

  private def getImAd(imOrAd: ImmediateOrAddress, dataBankStart: Int, isHardware: Boolean): String =
    imOrAd match
      case immediate: Immediate => getImmediate(immediate)
      case address: Address => getAddress(address, isHardware)
      case dataImmediate: BankImmediate => getBankImmediate(dataImmediate, dataBankStart)
      case label: Label => "#" ++ label.name

  private def getAcAd(acOrAd: AccumulatorOrAddress): String =
    acOrAd match
      case _: AReg => "A"
      case address: Address => getAddress(address)

  private def smpl(name: String, op: ImmediateOrAddress, dataBankStart: Int, isHardware: Boolean): String = $(name :: getImAd(op, dataBankStart, isHardware) :: Nil)
  private def smpl(name: String, op: AccumulatorOrAddress): String = $(name :: getAcAd(op) :: Nil)
  private def smpl(name: String, address: Address, isHardware: Boolean): String = $(name :: getAddress(address, isHardware) :: Nil)
  private def smpl(name: String, immediate: Immediate): String = $(name :: getImmediate(immediate) :: Nil)
  private def smpl(name: String, label: Label): String = $(name :: label.name :: Nil)

  private def arithmetic(a: Arithmetic, dataBankStart: Int): String = {
    def incDec(changeType: "in" | "de", reg: TargetReg): String = reg match
      case AReg() => changeType ++ "c A"
      case XReg() => changeType ++ "x"
      case YReg() => changeType ++ "y"
    def unrollSequence(sequence: IndexedSeq[String]): String =
      if (sequence.length == 1) {
        sequence.head
      }
      else {
        sequence.tail.foldLeft(sequence.head)(_ ++ "\n" ++ _)
      }
    a match
      case AddCarry(op) => smpl("adc", op, dataBankStart, false)
      case SubtractCarry(op) => smpl("sbc", op, dataBankStart, false)
      case AND(op) => smpl("and", op, dataBankStart, false)
      case EOR(op) => smpl("eor", op, dataBankStart, false)
      case ORA(op) => smpl("ora", op, dataBankStart, false)
      case ShiftLeft(op, times) =>
        unrollSequence(for i <- Range(0, times) yield smpl("asl", op))
      case ShiftRight(op, times) =>
        unrollSequence(for i <- Range(0, times) yield smpl("lsr", op))
      case RotateLeft(op) => smpl("rol", op)
      case RotateRight(op) => smpl("ror", op)
      case BitTest(op) => smpl("bit", op, dataBankStart, false)
      case DecrementReg(reg) => incDec("de", reg)
      case IncrementReg(reg) => incDec("in", reg)
      case DecrementMemory(address) => $("dec" :: getAddress(address) :: Nil)
      case IncrementMemory(address) => $("inc" :: getAddress(address) :: Nil)
      case NOP() => "nop ; :)"
      case ExchangeAccumulatorBytes() => "xba"
  }
  private def loadStore(ls: LoadStore, dataBankStart: Int): String = {
    def ldOrSt(start: "ld" | "st", reg: TargetReg): String = start ++ getReg(reg)
    ls match
      case Load(op, reg) => smpl(ldOrSt("ld", reg), op, dataBankStart, ls.isHardware)
      case Store(address, reg) => smpl(ldOrSt("st", reg), address, ls.isHardware)
      case SetZero(address) => smpl("stz", address, ls.isHardware)
  }
  private def transfer(t: Transfer): String = {
    val transfers: Map[AnyReg, Map[AnyReg, String]] = Map(
      AReg() -> Map(XReg() -> "tax", YReg() -> "tay", DirectPageReg() -> "tcd", StackPointerReg() -> "tcs"),
      XReg() -> Map(AReg() -> "txa", YReg() -> "txy", StackPointerReg() -> "txs"),
      YReg() -> Map(AReg() -> "tya", XReg() -> "tyx"),
      StackPointerReg() -> Map(AReg() -> "tsc", XReg() -> "tsx"),
      DirectPageReg() -> Map(AReg() -> "tdc")
    )
    t match
      case TransferToAccumulator(reg) => transfers(reg)(AReg())
      case TransferAccumulatorTo(reg) => transfers(AReg())(reg)
      case TransferToX(reg) => transfers(reg)(XReg())
      case TransferXTo(reg) => transfers(XReg())(reg)
      case TransferToY(reg) => transfers(reg)(YReg())
      case TransferYTo(reg) => transfers(YReg())(reg)
  }
  private def branch(b: Branch, dataBankStart: Int): String = {
    b match
      case Compare(op, reg) =>
        val name = reg match
          case AReg() => "cmp"
          case XReg() => "cmx"
          case YReg() => "cmy"
        smpl(name, op, dataBankStart, false)
      case BranchIfNoCarry(label) => smpl("bcc", label)
      case BranchIfCarrySet(label) => smpl("bcs", label)
      case BranchIfNotEqual(label) => smpl("bne", label)
      case BranchIfEqual(label) => smpl("beq", label)
      case BranchIfPlus(label) => smpl("bpl", label)
      case BranchIfMinus(label) => smpl("bmi", label)
      case BranchIfNoOverflow(label) => smpl("bvc", label)
      case BranchIfOverflow(label) => smpl("bvs", label)
      case BranchShort(label) => smpl("bra", label)
      case BranchLong(label) => smpl("brl", label)
  }
  private def jumpCall(jc: JumpCall): String = {
    jc match
      case JumpShortWithoutReturn(label) => smpl("jmp", label)
      case JumpLongWithoutReturn(label) => smpl("jml", label)
      case JumpShortSaveReturn(label) => smpl("jsr", label)
      case JumpLongSaveReturn(label) => smpl("jsl", label)
      case ReturnShort() => "rts"
      case ReturnLong() => "rtl"
  }
  private def interrupts(i: Interrupts): String = {
    i match
      case Break(vector) => smpl("brk", vector)
      case Cop(vector) => smpl("cop", vector)
      case ReturnFromInterrupt() => "rti"
      case StopClock() => "stp"
      case WaitForInterrupt() => "wai"
  }
  private def processorFlags(pf: ProcessorFlags): String = {
    def groupToStr(group: RegisterGroup): String =
      group match
        case RegisterGroup.XY => "#$10"
        case RegisterGroup.A => "#$20"
        case RegisterGroup.AXY => "#$30"
    pf match
      case ClearCarry() => "clc"
      case SetCarry() => "sec"
      case ClearOverflow() => "clv"
      case EnableInterrupts() => "cli"
      case DisableInterrupts() => "sei"
      case SetReg16Bit(group) => "rep " ++ groupToStr(group)
      case SetReg8Bit(group) => "sep " ++ groupToStr(group)
      case ResetStatusBit(immediate) => smpl("rep", immediate)
      case SetStatusBit(immediate) => smpl("sep", immediate)
      case ExchangeCarryFlagWithEmulation() => "xce"
  }
  private def stackManipulation(sm: StackManipulation): String = {

    sm match
      case PushRegister(reg) => "ph" ++ getReg(reg)
      case PopRegister(reg) => "pl" ++ getReg(reg)
      case PushDirectPageRegister() => "phd"
      case PullDirectPageRegister() => "pld"
      case PushDataBankRegister() => "phb"
      case PullDataBankRegister() => "plb"
      case PushProcessorStatus() => "php"
      case PullProcessorStatus() => "plp"
      case PushValue(immediate) => smpl("pea", immediate)
      case PushAddress(address) => smpl("pei", address)
      case PushDummyValue(reg) => "ph" ++ getReg(reg) ++ "\t;Dummy push"
      case PopDummyValue(reg) => "pl" ++ getReg(reg) ++ "\t;Dummy pull"
  }
  private def misc(m: Misc): String = {
    m match
      case MovePositive(fromPage, toPage) => throw new Exception("Not implimented")
      case MoveNegative(fromPage, toPage) => throw new Exception("Not implimented")
      case PutLabel(labelName: Label) => labelName.name ++ ":"
      case UserAssembly(code) => code.foldLeft(";User Assembly")(_ ++ "\n" ++ _) ++ "\n;End Assembly"
      case Spacing() => "\n;---------------\n"
      case Bank(bankName) => ".bank " ++ bankName.toString
      case UserData(dataName, data) => dataName ++ data.foldLeft(":")(_ ++ "\n\t" ++ _)
  }

  private def translatedIR(instruction: IR.Instruction, dataBankStart: Int): String =
    instruction match
      case a: Arithmetic => arithmetic(a, dataBankStart)
      case ls: LoadStore => loadStore(ls, dataBankStart)
      case t: Transfer => transfer(t)
      case b: Branch => branch(b, dataBankStart)
      case jc: JumpCall => jumpCall(jc)
      case i: Interrupts => interrupts(i)
      case pc: ProcessorFlags => processorFlags(pc)
      case sm: StackManipulation => stackManipulation(sm)
      case m: Misc => misc(m)
      case null => throw new Exception("Unknown instruction type -> " ++ instruction.toString)

  private def translatedComment(instruction: IR.Instruction): String =
    instruction.getComment match
      case None => ""
      case Some(comment) => "\t;" ++ comment

  def apply(instruction: IR.Instruction, dataBankStart: Int): String =
    translatedIR(instruction, dataBankStart) ++ translatedComment(instruction)
}

package TreeWalker

object Translator {
  import IR.*

  private def $(values: List[String]): String =
    values.foldLeft("")(_ ++ " " ++ _)

  private def getReg(reg: TargetOrStackReg | TargetReg): String =
    reg match
      case AReg() => "a"
      case XReg() => "x"
      case YReg() => "y"
      case StackPointerReg() => "s"
  private def getImmediate(immediate: Immediate): String =
    "#" ++ immediate.value.toString
  private def getAddress(address: Address): String =
    address match
      case Direct(address) => address.toString
      case DirectIndexed(address, by) => address.toString ++ ", " ++ getReg(by)
      case DirectIndirect(address) => "(" ++ address.toString ++ ")"
      case DirectIndexedIndirect(address, by) => "(" ++ address.toString ++ ", " ++ getReg(by) ++ ")"
      case DirectIndirectIndexed(address, by) => "(" ++ address.toString ++ "), " ++ getReg(by)
      case StackRelative(offset) => (offset - 1).toString ++ ", s"
      case StackRelativeIndirectIndexed(offset, by) => "(" ++ (offset - 1).toString ++ ", s), " ++ getReg(by)
      case null => throw new Exception("Cannot find this address type")

  private def getImAd(imOrAd: ImmediateOrAddress): String =
    imOrAd match
      case immediate: Immediate => getImmediate(immediate)
      case address: Address => getAddress(address)

  private def getAcAd(acOrAd: AccumulatorOrAddress): String =
    acOrAd match
      case _: AReg => "A"
      case address: Address => getAddress(address)

  private def smpl(name: String, op: ImmediateOrAddress): String = $(name :: getImAd(op) :: Nil)
  private def smpl(name: String, op: AccumulatorOrAddress): String = $(name :: getAcAd(op) :: Nil)
  private def smpl(name: String, address: Address): String = $(name :: getAddress(address) :: Nil)
  private def smpl(name: String, immediate: Immediate): String = $(name :: getImmediate(immediate) :: Nil)
  private def smpl(name: String, label: Label): String = $(name :: label.name :: Nil)

  private def arithmetic(a: Arithmetic): String = {
    def incDec(changeType: "in" | "de", reg: TargetReg): String = reg match
      case AReg() => changeType ++ "c A"
      case XReg() => changeType ++ "x"
      case YReg() => changeType ++ "y"
    a match
      case AddCarry(op) => smpl("adc", op)
      case SubtractCarry(op) => smpl("sbc", op)
      case AND(op) => smpl("and", op)
      case EOR(op) => smpl("eor", op)
      case ORA(op) => smpl("ora", op)
      case ShiftLeft(op) => smpl("asl", op)
      case ShiftRight(op) => smpl("lsr", op)
      case RotateLeft(op) => smpl("rol", op)
      case RotateRight(op) => smpl("ror", op)
      case BitTest(op) => smpl("bit", op)
      case DecrementReg(reg) => incDec("de", reg)
      case IncrementReg(reg) => incDec("in", reg)
      case DecrementMemory(address) => $("dec" :: getAddress(address) :: Nil)
      case IncrementMemory(address) => $("inc" :: getAddress(address) :: Nil)
      case NOP() => "nop ; :)"
      case ExchangeAccumulatorBytes() => "xba"
  }
  private def loadStore(ls: LoadStore): String = {
    def ldOrSt(start: "ld" | "st", reg: TargetReg): String = start ++ getReg(reg)
    ls match
      case Load(op, reg) => smpl(ldOrSt("ld", reg), op)
      case Store(address, reg) => smpl(ldOrSt("st", reg), address)
      case SetZero(address) => smpl("stz", address)
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
  private def branch(b: Branch): String = {
    b match
      case Compare(op, reg) =>
        val name = reg match
          case AReg() => "cmp"
          case XReg() => "cmx"
          case YReg() => "cmy"
        smpl(name, op)
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
  }
  private def misc(m: Misc): String = {
    m match
      case MovePositive(fromPage, toPage) => throw new Exception("Not implimented")
      case MoveNegative(fromPage, toPage) => throw new Exception("Not implimented")
      case PutLabel(labelName: Label) => labelName.name ++ ":"
      case UserAssembly(code) => code
  }


  def apply(instruction: IR.Instruction): String =
    instruction match
      case a: Arithmetic => arithmetic(a)
      case ls: LoadStore => loadStore(ls)
      case t: Transfer => transfer(t)
      case b: Branch => branch(b)
      case jc: JumpCall => jumpCall(jc)
      case i: Interrupts => interrupts(i)
      case pc: ProcessorFlags => processorFlags(pc)
      case sm: StackManipulation => stackManipulation(sm)
      case m: Misc => misc(m)
      case null => throw new Exception("Unknown instruction type -> " ++ instruction.toString)
}

package TreeWalker

object Translator {
  import IR.*
  private def arithmetic(a: Arithmetic): String = ""
  private def loadStore(ls: LoadStore): String = ""
  private def transfer(t: Transfer): String = ""
  private def branch(b: Branch): String = ""
  private def jumpCall(jc: JumpCall): String = ""
  private def interrupts(i: Interrupts): String = ""
  private def processorFlags(pf: ProcessorFlags): String = ""
  private def stackManipulation(sm: StackManipulation): String = ""
  private def misc(m: Misc): String = ""


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

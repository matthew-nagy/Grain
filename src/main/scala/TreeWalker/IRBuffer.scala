package TreeWalker

import scala.collection.mutable.ListBuffer

class IRBuffer {
  private val irBuffer = new ListBuffer[IR.Instruction]

  def apply(index: Int): IR.Instruction = irBuffer(index)
  def append(instruction: IR.Instruction): IRBuffer =
    irBuffer.append(instruction)
    this
  def append(instructions: List[IR.Instruction]): IRBuffer =
    for instruction <- instructions do append(instruction)
    this
  def append(otherBuffer: IRBuffer): IRBuffer = append(otherBuffer.irBuffer.toList)
  
  def toList: List[IR.Instruction] = irBuffer.toList

  override def toString: String =
    (for i <- irBuffer.toList yield "\n\t" ++ i.toString).foldLeft("IRCode ->")(_ ++ _)
}

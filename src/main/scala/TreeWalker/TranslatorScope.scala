package TreeWalker

import scala.collection.mutable.Stack
import Grain.*
class TranslatorScope(private val innerScope: Scope) {
  private var pushesToTheStack = 0
  private val stackFrames = Stack.empty[Int]
  //In 2s because 16 bit. If bytes come later, byte versions will need to be added
  def push(): Unit = pushesToTheStack += 2
  def pop(): Unit = pushesToTheStack -= 2

  def size: Int = innerScope.size
  def inner: Scope = innerScope

  def rememberStackLocation(): Unit = stackFrames.push(pushesToTheStack)
  def getFixStackDecay(): IRBuffer = {
    val oldState = stackFrames.pop()
    val buffer = IRBuffer()
    //Don't do pointless things
    if(oldState == pushesToTheStack){
      return buffer
    }

    while(pushesToTheStack > oldState){
      pop()
      buffer.append(IR.PopRegister(XReg()))
    }

    pushesToTheStack = oldState
    buffer
  }

  def offsetToReturnAddress = pushesToTheStack + getOffsetToReturnAddress(0)
  private def getOffsetToReturnAddress(currentOffset: Int): Int =
    innerScope match
      case _: GlobalScope => throw new Exception("Went too far")
      case _: FunctionScope => currentOffset
      case s: Scope => getParent.getOffsetToReturnAddress(currentOffset + s.size)

  //TODO in future count stack size and maybe just ADC the stack pointer if too large
  def extendStack(): List[IR.Instruction] = {
    val stackExtentions = innerScope.size / 2
    (for i <- Range(0, stackExtentions)yield IR.PushRegister(XReg())).toList
  }

  def reduceStack(): List[IR.Instruction] = {
    val stackExtentions = innerScope.size / 2
    (for i <- Range(0, stackExtentions) yield IR.PopRegister(XReg())).toList
  }

  def getParent: TranslatorScope =
    TranslatorScope(innerScope.parent)

  def getChild(statement: Stmt.Statement): TranslatorScope =
    TranslatorScope(innerScope.getChildOrThis(statement))

  def getSymbol(symbolName: String): Symbol = innerScope(symbolName)

  def getAddress(varName: String): Address = {
    getSymbol(varName).form match
      case _: Symbol.Variable => getStackAddress(varName)
      case _: Symbol.Argument => StackRelative(getStackOffset(varName) + 3)
      case _: Symbol.FunctionDefinition => throw new Exception("Can't get the location of a function (I guess?)")
      case glob: Symbol.GlobalVariable => Direct(glob.location)
  }

  private def getStackAddress(varName: String): Address = StackRelative(getStackOffset(varName))

  private def getStackOffset(varName: String): Int = {
    var additionalOffset = pushesToTheStack
    var containingScope = innerScope
    while (!containingScope.strictContains(varName)) {
      additionalOffset += containingScope.size
      containingScope = containingScope.parent
    }

    val stackStoredForm = getAsStackStored(varName)
    val totalOffset = (containingScope.size - stackStoredForm.stackOffset) + additionalOffset
    totalOffset
  }

  private def getAsStackStored(varName: String): Symbol.StackStored ={
    val symbol = innerScope(varName)
    if(!symbol.form.isInstanceOf[Symbol.StackStored]){
      throw new Exception(varName ++ " is not stored on the stack")
    }
    symbol.form.asInstanceOf[Symbol.StackStored]
  }
}

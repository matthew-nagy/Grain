package TreeWalker

import scala.collection.mutable
import Grain.*

import scala.annotation.tailrec

class TranslatorSymbolTable{
  val usedFunctionLabels: mutable.Set[String] = mutable.Set.empty[String]
  val functionLabelPtrs: mutable.Set[String] = mutable.Set.empty[String]
}

class TranslatorScope(private val innerScope: Scope, private val translatorSymbolTable: TranslatorSymbolTable) {
  private var pushesToTheStack = 0
  private val stackFrames = mutable.Stack.empty[Int]
  //In 2s because 16 bit. If bytes come later, byte versions will need to be added
  def push(): Unit = pushesToTheStack += 2
  def pop(): Unit = pushesToTheStack -= 2

  def getTranslatorSymbolTable: TranslatorSymbolTable = translatorSymbolTable

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
      buffer.append(IR.PopDummyValue(XReg()))
    }

    pushesToTheStack = oldState
    buffer
  }

  def getStackDecaySize: Int =
    pushesToTheStack - stackFrames.top

  @tailrec
  private def getOffsetToReturnAddress(currentOffset: Int): Int =
    innerScope match
      case _: GlobalScope => throw new Exception("Went too far")
      case _: FunctionScope => currentOffset + pushesToTheStack
      case s: Scope => getParent.getOffsetToReturnAddress(currentOffset + s.size + pushesToTheStack)

  def extendStack(): List[IR.Instruction] = {
    val stackExtentions = innerScope.size / 2
    if(stackExtentions < 3) {
      (for i <- Range(0, stackExtentions) yield IR.PushDummyValue(XReg())).toList
    }
    else{
      IR.TransferToAccumulator(StackPointerReg()) :: IR.ClearCarry() ::
        IR.AddCarry(Immediate(innerScope.size)) :: IR.TransferAccumulatorTo(StackPointerReg()) :: Nil
    }
  }

  def reduceStack(): List[IR.Instruction] = {
    val stackExtentions = innerScope.size / 2
    if (stackExtentions < 3) {
      (for i <- Range(0, stackExtentions) yield IR.PopDummyValue(XReg())).toList
    }
    else {
      IR.TransferToAccumulator(StackPointerReg()) :: IR.SetCarry() ::
        IR.SubtractCarry(Immediate(innerScope.size)) :: IR.TransferAccumulatorTo(StackPointerReg()) :: Nil
    }
  }

  private def getParent: TranslatorScope =
    TranslatorScope(innerScope.parent, translatorSymbolTable)

  def getChild(statement: Stmt.Statement): TranslatorScope = {
    val child = TranslatorScope(innerScope.getChildOrThis(statement), translatorSymbolTable)
    child.pushesToTheStack = pushesToTheStack
    child
  }

  def getSymbol(symbolName: String): Symbol = innerScope(symbolName)

  def getAddress(varName: String): StackRelative | Direct | DirectLabel= {
    getSymbol(varName).form match
      case _: Symbol.Variable => getStackAddress(varName)
      case _: Symbol.Argument => StackRelative(getStackOffset(varName) + 5)
      case _: Symbol.FunctionDefinition => throw new Exception("Can't get the location of a function (I guess?)")
      case glob: Symbol.GlobalVariable => Direct(glob.location)
      case _: Symbol.Data => DirectLabel(varName)
  }

  def getStackFrameOffset: Int =
    pushesToTheStack + (if(inner.parent.isInstanceOf[GlobalScope])//This is the argument scope
      2 //Because stack relative loses 1 later
    else
      inner.size + getParent.getStackFrameOffset)


  private def getStackAddress(varName: String): StackRelative = StackRelative(getStackOffset(varName))

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

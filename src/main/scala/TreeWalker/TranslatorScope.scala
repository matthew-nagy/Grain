package TreeWalker

import scala.collection.mutable.Stack
import Grain.*
class TranslatorScope(private val innerScope: Scope) {
  private var pushesToTheStack = 0
  private val stackFrames = Stack.empty[Int]
  //In 2s because 16 bit. If bytes come later, byte versions will need to be added
  def push(): Unit = pushesToTheStack += 2
  def pop(): Unit = pushesToTheStack -= 2

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

  def getParent: TranslatorScope =
    TranslatorScope(innerScope.parent)
  def getChild(statement: Stmt.Statement): TranslatorScope =
    TranslatorScope(innerScope.getChild(statement))

  def getSymbol(symbolName: String): Symbol = innerScope(symbolName)

  //Currently cannot handle global variables. Maybe have another function that does that
  //and calls this.
  def getStackAddress(varName: String): Address = {
    var additionalOffset = pushesToTheStack
    var containingScope = innerScope
    while(!containingScope.strictContains(varName)){
      additionalOffset += containingScope.size
      containingScope = containingScope.parent
    }

    val stackStoredForm = getAsStackStored(varName)
    val totalOffset = (containingScope.size - stackStoredForm.stackOffset) + additionalOffset
    StackRelative(totalOffset)
  }

  private def getAsStackStored(varName: String): Symbol.StackStored ={
    val symbol = innerScope(varName)
    if(!symbol.form.isInstanceOf[Symbol.StackStored]){
      throw new Exception(varName ++ " is not stored on the stack")
    }
    symbol.form.asInstanceOf[Symbol.StackStored]
  }
}

package TreeWalker

import Grain.*
import Grain.Stmt.*
import TreeWalker.IR.Instruction

import scala.collection.mutable.ListBuffer

object GrainTranslator {
  sealed trait Result
  case class Nothing() extends Result
  case class FunctionCode(buffer: IRBuffer) extends Result
  case class GlobalsCode(buffer: IRBuffer) extends Result
  case class DataCode(labelName: String, data: List[String], var dataBank: Int) extends Result

  class IRAccumulator{
    private val globalVariableInitalisation = ListBuffer.empty[IRBuffer]
    private val functionCode = ListBuffer.empty[IRBuffer]
    private val ROMData = ListBuffer.empty[DataCode]

    def getSizeOfBufferCode(buffer: IRBuffer): Int =
      buffer.toList.map(ir =>
        ir match
          case _: IR.ZeroSizeInstruction => 0
          case _: IR.Instruction => GlobalData.snesData.generalInstructionSize
      ).sum

    private def append(result: Result): Unit =
      result match
        case Nothing() => return
        case FunctionCode(buffer) =>  functionCode.append(buffer)
        case GlobalsCode(buffer) => globalVariableInitalisation.append(buffer)
        case data: DataCode => ROMData.append(data)
        case null => throw new Exception("Not exhaustive on results")

    def ::(results: List[Result]): IRAccumulator = {
      for result <- results do append(result)
      this
    }

    def get: List[IR.Instruction] = {
      var currentBank = 0
      var currentBankSize = 0
      val codeByteSize = GlobalData.snesData.generalInstructionSize

      val unbankedBuffers: List[IRBuffer] = globalVariableInitalisation.toList :::
        (IRBuffer().append(IR.JumpLongWithoutReturn(Label("main_function")) :: IR.Spacing() :: Nil) :: Nil) :::
        functionCode.toList

      val bankedBuffers = (
        for buffer <- unbankedBuffers yield{
          val codeSize = getSizeOfBufferCode(buffer)
          val codeList = buffer.toList
          if(currentBankSize + codeSize >= GlobalData.snesData.bankSize){//Need a new bank
            currentBank += 1
            currentBankSize = codeSize
            (IR.Bank(currentBank) :: Nil) ::: codeList
          }
          else{
            currentBankSize += codeSize
            codeList
          }
        }
      ).foldLeft(List.empty[IR.Instruction])((l1, l2) => l1 ::: l2)

      currentBank += 1
      var lastDatabank = -1
      val dataBankCode = (for data <- ROMData yield{
        val dataIR: Instruction = IR.UserData(data.labelName, data.data)
        val targetBank = currentBank + data.dataBank
        if(lastDatabank != targetBank){
          lastDatabank = targetBank
          IR.Bank(targetBank) :: dataIR :: Nil
        }
        else{
          dataIR :: Nil
        }
      }).foldLeft(List.empty[Instruction])((l1, l2) => l1 ::: l2)

      (IR.UserAssembly(GlobalData.snesData.fileStart):: Nil) ::: bankedBuffers ::: dataBankCode
    }
  }

  private def translate(topLevel: TopLevel, scope: GlobalScope): List[Result] =
    topLevel match
      case EmptyStatement() => Nothing() :: Nil
      case VariableDecl(assignment) =>
        GlobalsCode(ExpressionTranslator.getFromAccumulator(assignment, TranslatorScope(scope)).toGetThere) :: Nil
      case FunctionDecl(funcSymbol, _, body) =>
        val functionScope = scope.getChild(topLevel).getChild(body)
        val tFuncScope = TranslatorScope(functionScope)
        val defaultFuncLabel = "func_" ++ funcSymbol.name ++ "_l" ++ funcSymbol.lineNumber.toString
        val functionStart = funcSymbol.name match
          case "main" => IR.PutLabel(Label("main_function")) :: Nil
          case "VBlank" => IR.PutLabel(Label("VBlank")) :: IR.PushRegister(AReg()) :: IR.PushRegister(XReg()) :: IR.PushRegister(YReg()) ::
            IR.PushProcessorStatus() :: Nil
          case _ => IR.PutLabel(Label(defaultFuncLabel)) :: Nil

        val saveStack = IR.TransferToX(StackPointerReg()) :: IR.PushRegister(XReg()).addComment("Record stack frame") :: Nil

        val prepareStack = tFuncScope.extendStack()

        val translatedBody =
          body.statements
            .map(StatementTranslator(_, tFuncScope))
            .foldLeft(IRBuffer())(_.append(_))

        val fixStack = IR.Load(StackRelative(tFuncScope.getStackFrameOffset), AReg()).addComment("Fix stack before return") ::
          IR.TransferAccumulatorTo(StackPointerReg()) :: Nil

        val funcEnd = funcSymbol.name match
          case "main" => IR.StopClock().addComment("At the end of main") :: Nil
          case "VBlank" => IR.PullProcessorStatus() :: IR.PopRegister(YReg()) :: IR.PopRegister(XReg()) :: IR.PopRegister(AReg()) :: IR.ReturnFromInterrupt() :: Nil
          case _ => IR.ReturnLong() :: Nil

        val instructionList = functionStart ::: saveStack ::: prepareStack ::: translatedBody.toList ::: fixStack ::: funcEnd

        FunctionCode(IRBuffer().append(instructionList).append(IR.Spacing())) :: Nil
      case Load(varName, palleteName, filename, references) =>
        val spriteSheetForm = scope.getSymbol(varName.lexeme).form.asInstanceOf[Symbol.Data]
        val paletteForm = scope.getSymbol(palleteName.lexeme).form.asInstanceOf[Symbol.Data]

        DataCode(varName.lexeme, spriteSheetForm.values, spriteSheetForm.dataBank) :: DataCode(palleteName.lexeme, paletteForm.values, paletteForm.dataBank) :: Nil

      case _ => throw new Exception("Not done yet -> " ++ topLevel.toString)

  def apply(statements: List[TopLevel], scope: GlobalScope): List[IR.Instruction] = {
    statements
      .map(translate(_, scope))
      .foldLeft(IRAccumulator())((buffer, ir) => ir :: buffer)
      .get
  }

  def main(args: Array[String]): Unit = {

    //val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/GrainTest.txt"))
    //val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/fragment.txt"))
    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/array2d.txt"))
    val symbolTable = new SymbolTable

    val topLevels = ListBuffer.empty[Stmt.TopLevel]
    var hadErrors = false

    while (tokenBuffer.peekType != Utility.TokenType.EndOfFile) {
      val result = Parser.TopLevelParser(symbolTable.globalScope, tokenBuffer)
      for s <- result.statements do {
        topLevels.append(s)
      }
      result.errors.map(println)
      hadErrors |= result.errors.nonEmpty
    }

    if(hadErrors){
      return
    }

    var generatedIr = apply(topLevels.toList, symbolTable.globalScope)
    generatedIr = Optimise(generatedIr)

    val assembly = generatedIr.map(Translator(_)).foldLeft("")(_ ++ "\n" ++ _)

    println(assembly)
//    generatedIr.map(i => println(i.toString))
    println("IR was length " ++ generatedIr.length.toString)
  }
}

package TreeWalker

import Grain.*
import Grain.Stmt.*
import TreeWalker.IR.Instruction

import scala.sys.process.Process
import sys.process.*
import java.io.FileWriter
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

object GrainTranslator {
  sealed trait Result
  case class Nothing() extends Result
  case class FunctionCode(buffer: IRBuffer, functionLabel: String) extends Result
  case class GlobalsCode(buffer: IRBuffer) extends Result
  case class DataCode(labelName: String, data: List[String], var dataBank: Int) extends Result

  private val VBlankEndLabel = Label("VBlank_End")

  case class Output(ir: List[IR.Instruction], firstDataBank: Int)

  //Load 6. The zero flag will be 1 if 0, checked by equal.
  //If you get past there, reset the flag
  private val VBlankReturnIfFrameIsUnfinished: List[Instruction] =
    IR.Load(Direct(6), AReg()) :: IR.BranchIfEqual(VBlankEndLabel) :: IR.SetZero(Direct(6)):: Nil

  class IRAccumulator(translatorSymbolTable: TranslatorSymbolTable){
    private val globalVariableInitalisation = ListBuffer.empty[IRBuffer]
    private val functionCode = ListBuffer.empty[IRBuffer]
    private val ROMData = ListBuffer.empty[DataCode]

    private def append(result: Result): Unit =
      result match
        case Nothing() => return
        case FunctionCode(buffer, label) =>
          if(translatorSymbolTable.usedFunctionLabels.contains(label)){
            functionCode.append(buffer)
          }
        case GlobalsCode(buffer) => globalVariableInitalisation.append(buffer)
        case data: DataCode => ROMData.append(data)
        case null => throw new Exception("Not exhaustive on results")

    def ::(results: List[Result]): IRAccumulator = {
      for result <- results do append(result)
      this
    }

    def get: Output = {
      var currentBank = 0
      var currentBankSize = 0
      val codeByteSize = GlobalData.snesData.generalInstructionSize

      val unbankedBuffers: List[IRBuffer] = globalVariableInitalisation.toList :::
        (IRBuffer().append(IR.JumpLongWithoutReturn(Label("main_function")) :: IR.Spacing() :: Nil) :: Nil) :::
        functionCode.toList

      val bankedBuffers = (
        for buffer <- unbankedBuffers yield{
          val codeSize = IR.sizeOfIr(buffer)
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
      val firstDataBank = currentBank
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

      Output((IR.UserAssembly(GlobalData.snesData.fileStart):: Nil) ::: bankedBuffers ::: dataBankCode, firstDataBank)
    }
  }

  private def translate(topLevel: TopLevel, scope: GlobalScope, translatorSymbolTable: TranslatorSymbolTable): List[Result] =
    topLevel match
      case EmptyStatement() => Nothing() :: Nil
      case VariableDecl(assignment) =>
        GlobalsCode(ExpressionTranslator.getFromAccumulator(assignment, TranslatorScope(scope, translatorSymbolTable)).toGetThere) :: Nil
      case FunctionDecl(funcSymbol, _, body, _) =>
        val defaultFuncLabel = (if(funcSymbol.name.contains('.'))"method_" else "func_") ++ funcSymbol.name

        body match
          case asmBody: Stmt.Assembly =>
            FunctionCode(IRBuffer().append(IR.PutLabel(Label(defaultFuncLabel)) :: StatementTranslator(asmBody, TranslatorScope(scope, translatorSymbolTable)).toList).append(IR.Spacing()), defaultFuncLabel):: Nil
          case blockBody: Stmt.Block =>
            val functionScope = scope.getChild(topLevel).getChild(body)
            val tFuncScope = TranslatorScope(functionScope, translatorSymbolTable)
            val (functionStart, chosenLabel) = funcSymbol.name match
              case "main" =>
                translatorSymbolTable.usedFunctionLabels.addOne("main_function")
                (IR.PutLabel(Label("main_function")) :: Nil, "main_function")
              case "VBlank" =>
                translatorSymbolTable.usedFunctionLabels.addOne("VBlank")
                (IR.PutLabel(Label("VBlank")) :: IR.PushRegister(AReg()) ::  IR.PushRegister(XReg()) :: IR.PushRegister(YReg()) ::
                IR.PushProcessorStatus() :: Nil ::: VBlankReturnIfFrameIsUnfinished ::: IR.SetReg8Bit(RegisterGroup.A) :: IR.Load(Immediate(0x80), AReg()) :: IR.Store(Direct(0x2100), AReg()) :: IR.SetReg16Bit(RegisterGroup.AXY) :: Nil, "VBlank")
              case _ => (IR.PutLabel(Label(defaultFuncLabel)) :: Nil, defaultFuncLabel)

            val saveStack =
              IR.TransferToX(StackPointerReg()) :: IR.PushRegister(XReg()).addComment("Record stack frame") :: Nil

            val prepareStack = tFuncScope.extendStack()

            val translatedBody =
              blockBody.statements
                .map(StatementTranslator(_, tFuncScope))
                .foldLeft(IRBuffer())(_.append(_))

            val fixStack = IR.Load(StackRelative(tFuncScope.getStackFrameOffset), AReg()).addComment("Fix stack before return") ::
              IR.TransferAccumulatorTo(StackPointerReg()) :: Nil

            val funcEnd = funcSymbol.name match
              case "main" => IR.StopClock().addComment("At the end of main") :: Nil
              case "VBlank" => IR.SetReg8Bit(RegisterGroup.A) :: IR.Load(Immediate(0x0F), AReg()) :: IR.Store(Direct(0x2100), AReg()) :: IR.PutLabel(VBlankEndLabel) :: IR.PullProcessorStatus() :: IR.PopRegister(YReg()) :: IR.PopRegister(XReg()) :: IR.PopRegister(AReg()) :: IR.ReturnFromInterrupt() :: Nil
              case _ => IR.ReturnLong() :: Nil

            val instructionList = functionStart ::: saveStack ::: prepareStack ::: translatedBody.toList ::: fixStack ::: funcEnd

            FunctionCode(IRBuffer().append(instructionList).append(IR.Spacing()), chosenLabel) :: Nil
      case LoadGraphics(varName, palleteName, filename, references) =>
        val spriteSheetForm = scope.getSymbol(varName.lexeme).form.asInstanceOf[Symbol.Data]
        val paletteForm = scope.getSymbol(palleteName.lexeme).form.asInstanceOf[Symbol.Data]

        DataCode(varName.lexeme, spriteSheetForm.values, spriteSheetForm.dataBank) :: DataCode(palleteName.lexeme, paletteForm.values, paletteForm.dataBank) :: Nil
      case LoadData(varName, filename) =>
        //Are you proud of me Alex
        val dataData = scope.getSymbol(varName.lexeme).form.asInstanceOf[Symbol.Data]

        DataCode(varName.lexeme, dataData.values, dataData.dataBank) :: Nil

      case _ => throw new Exception("Not done yet -> " ++ topLevel.toString)

  def apply(statements: List[TopLevel], scope: GlobalScope, translatorSymbolTable: TranslatorSymbolTable): Output = {
    statements
      .map(translate(_, scope, translatorSymbolTable))
      .foldLeft(IRAccumulator(translatorSymbolTable))((buffer, ir) => ir :: buffer)
      .get
  }

  def main(args: Array[String]): Unit = {
    println(GlobalData.Config)
    var filename = "src/main/"
    //filename += "array2d.txt"
    //filename += "fragment.txt"
    //filename += "snake.txt"
    filename += "Ackermann.grain"

    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText(filename), filename, 0)
    //val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/GrainLib/Random.grain"))
    val symbolTable = new SymbolTable
    val translatorSymbolTable = new TranslatorSymbolTable

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

    var Output(generatedIr, firstDataBank) = apply(topLevels.toList, symbolTable.globalScope, translatorSymbolTable)
    generatedIr = Optimise(generatedIr)

    val assembly = generatedIr.map(Translator(_, firstDataBank)).foldLeft("")(_ ++ "\n" ++ _)

    println(assembly)
//    generatedIr.map(i => println(i.toString))
    println("IR was length " ++ generatedIr.length.toString)

    val fileWriter = new FileWriter("snes/compiled.asm")
    fileWriter.write(assembly)
    fileWriter.close()

    {GlobalData.Config.assemblerPath ++ "wla-65816.exe -v -o snes/compiled.obj snes/compiled.asm" !}
    {GlobalData.Config.assemblerPath ++ "wlalink snes/compiled.link snes/compiled.smc" !}
    {GlobalData.Config.emulatorPath ++ " .\\snes\\compiled.smc" !}
  }
}

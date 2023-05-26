import Grain.{GlobalData, Scanner, Stmt, SymbolTable}
import TreeWalker.GrainTranslator.Output
import TreeWalker.{GrainTranslator, Optimise, Translator, TranslatorSymbolTable}

import java.io.FileWriter
import scala.collection.mutable.ListBuffer
import scala.sys.process.Parser

object Compiler {

  def main(args: Array[String]): Unit = {
    if(args.length != 1){
      println("Usage: Expects a single filename to compile")
      return
    }
    val filename = args(0)
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

    if (hadErrors) {
      return
    }

    var Output(generatedIr, firstDataBank) = apply(topLevels.toList, symbolTable.globalScope, translatorSymbolTable)
    generatedIr = Optimise(generatedIr)

    val assembly = generatedIr.map(Translator(_, firstDataBank)).foldLeft("")(_ ++ "\n" ++ _)

    val fileWriter = new FileWriter("snes/compiled.asm")
    fileWriter.write(assembly)
    fileWriter.close()

    {
      GlobalData.Config.assemblerPath ++ "wla-65816.exe -v -o snes/compiled.obj snes/compiled.asm" !
    }
    {
      GlobalData.Config.assemblerPath ++ "wlalink snes/compiled.link snes/compiled.smc" !
    }
    {
      GlobalData.Config.emulatorPath ++ " .\\snes\\compiled.smc" !
    }
  }
}

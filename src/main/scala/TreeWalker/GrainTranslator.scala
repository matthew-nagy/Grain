package TreeWalker

import Grain.*
import Grain.Stmt.*

import scala.collection.mutable.ListBuffer

object GrainTranslator {
  sealed trait Result
  case class Nothing() extends Result
  case class FunctionCode(buffer: IRBuffer) extends Result
  case class GlobalsCode(buffer: IRBuffer) extends Result

  class IRAccumulator{
    private val globalVariableInitalisation = ListBuffer.empty[IR.Instruction]
    private val functionCode = ListBuffer.empty[IR.Instruction]

    def ::(result: Result): IRAccumulator =
      result match
        case Nothing() => this
        case FunctionCode(buffer) =>
          for elem <- buffer.toList do functionCode.append(elem)
          this
        case GlobalsCode(buffer) =>
          for elem <- buffer.toList do globalVariableInitalisation.append(elem)
          this
        case null => throw new Exception("Not exhaustive on results")

    def get: List[IR.Instruction] =
      globalVariableInitalisation.toList ::: functionCode.toList
  }

  private def translate(topLevel: TopLevel, scope: GlobalScope): Result =
    topLevel match
      case EmptyStatement() => Nothing()
      case VariableDecl(assignment) =>
        GlobalsCode(ExpressionTranslator.getFromAccumulator(assignment, TranslatorScope(scope)).toGetThere)
      case FunctionDecl(funcSymbol, _, body) =>
        val functionScope = scope.getChild(topLevel).getChild(body)
        val tFuncScope = TranslatorScope(functionScope)
        val funcLabel = "func_" ++ funcSymbol.name ++ "_l" ++ funcSymbol.lineNumber.toString
        //Add an extra return in case you reach the bottom
        //Will cause errors if it is expecting a value, so
        //lets hope that doesn't happen
        val functionBody = IRBuffer()
          .append(IR.PutLabel(Label(funcLabel)))
          .append(tFuncScope.extendStack())
          .append(
            (body.statements ::: (Return(None) :: Nil))
            .map(StatementTranslator(_, TranslatorScope(functionScope)))
            .foldLeft(IRBuffer())(_.append(_))
            .append(tFuncScope.reduceStack())
            .append(IR.Spacing())
          )

        FunctionCode(functionBody)
      case _ => throw new Exception("Not done yet")

  def apply(statements: List[TopLevel], scope: GlobalScope): List[IR.Instruction] = {
    statements
      .map(translate(_, scope))
      .foldLeft(IRAccumulator())((buffer, ir) => ir :: buffer)
      .get
  }

  def main(args: Array[String]): Unit = {
    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/GrainTest.txt"))
    val symbolTable = new SymbolTable

    val topLevels = ListBuffer.empty[Stmt.TopLevel]

    while (tokenBuffer.peekType != Utility.TokenType.EndOfFile) {
      val result = Parser.TopLevelParser(symbolTable.globalScope, tokenBuffer)
      for s <- result.statements do {
        println(s)
        topLevels.append(s)
      }
      result.errors.map(println)
    }

    val generatedIr = apply(topLevels.toList, symbolTable.globalScope)

    println(generatedIr)

    val assembly = generatedIr.toList.map(Translator(_)).foldLeft("")(_ ++ "\n" ++ _)

    println(assembly)
  }
}

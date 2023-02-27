package TreeWalker

import Grain.*
import Grain.Stmt.*
import Utility.TokenType

import scala.collection.mutable.ListBuffer

object StatementTranslator {

  def translateStatement(stmt: Statement, scope: TranslatorScope): IRBuffer = {
    def toAccumulator(e: Expr.Expr, scope: TranslatorScope): List[IR.Instruction] =
      ExpressionTranslator.getFromAccumulator(e, scope).toGetThere.toList
    IRBuffer().append(
    stmt match
      case Assembly(assembly) => IR.UserAssembly(assembly) :: Nil
      case Block(statements) =>
        val blockScope = scope.getChild(stmt)
        println ("Need to add variables to the stack")
        statements.map(translateStatement(_, blockScope)).foldLeft(IRBuffer())(_.append(_)).toList
      case EmptyStatement() => Nil
      case Expression(expr) => toAccumulator(expr, scope)
      case For(startStmt, breakExpr, incrimentExpr, body, lineNumber) => throw new Exception("For is not yet supported")
      case If(condition, body, elseBranch, lineNumber) =>
        val elseStartLabel = Label("Else_l" ++ lineNumber.toString)
        val ifEndLabel = Label("If_End_l" ++ lineNumber.toString)
        val ifScope = scope.getChild(stmt)
        println("Need to add variables to the stack")
        val conditionCode = toAccumulator(condition, ifScope)
        val bodyCode = translateStatement(body, ifScope).toList
        val ifEnder = if(elseBranch.isDefined) IR.BranchShort(ifEndLabel) :: Nil else Nil
        val elseCode = if(elseBranch.isDefined) translateStatement(elseBranch.get, scope).toList else Nil

        conditionCode :::
        (IR.Compare(Immediate(1), AReg()) :: IR.BranchIfNotEqual(elseStartLabel) :: Nil) :::
        bodyCode ::: ifEnder :::
        (IR.PutLabel(elseStartLabel) :: Nil) ::: elseCode :::
        (IR.PutLabel(ifEndLabel) :: Nil)

      case Else(body) =>
        val elseScope = scope.getChild(stmt)
        translateStatement(body, elseScope).toList
      case Return(value) =>
        (value match
          case Some(value) => toAccumulator(value, scope)
          case None =>Nil
        ) ::: IR.ReturnLong() :: Nil
      case VariableDecl(varDecl) => toAccumulator(varDecl, scope)
      case While(condition, body, lineNumber) =>
        val whileScope = scope.getChild(stmt)
        println("Need to add variables to the stack")
        val startLabel = Label("While_l" ++ lineNumber.toString)
        val endLabel = Label("While_End_l" ++ lineNumber.toString)
        val conditionCode = toAccumulator(condition, whileScope)
        val bodyCode = translateStatement(body, whileScope)
        (IR.PutLabel(startLabel) :: Nil) :::
        conditionCode :::
        (IR.Compare(Immediate(0), AReg()) :: IR.BranchIfEqual(endLabel) :: Nil) :::
        bodyCode.toList :::
        (IR.BranchShort(startLabel) :: IR.PutLabel(endLabel) :: Nil)

      case _ => throw new Exception(stmt.toString ++ " cannot be translated yet")
    )
  }

  def main(args: Array[String]): Unit = {
    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/StatementParserTest.txt"))
    val symbolTable = new SymbolTable
    symbolTable.globalScope.setReturnType(Utility.Word())

    val statements = ListBuffer.empty[Stmt.Statement]

    while (tokenBuffer.peekType != TokenType.EndOfFile) {
      val stmt = Parser.StatementParser.parseOrThrow(symbolTable.globalScope, tokenBuffer)
      println(stmt)
      statements.append(stmt)
    }

    println(
      statements
      .map(translateStatement(_, TranslatorScope(symbolTable.globalScope)))
      .foldLeft(IRBuffer())(_.append(_))
    )
  }
}

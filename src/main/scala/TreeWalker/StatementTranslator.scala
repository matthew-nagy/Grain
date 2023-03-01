package TreeWalker

import Grain.*
import Grain.Stmt.*
import Utility.TokenType

import scala.collection.mutable.ListBuffer

object StatementTranslator {

  enum BranchType:
    case IfTrue, IfFalse

  private def toAccumulator(e: Expr.Expr, scope: TranslatorScope): List[IR.Instruction] =
    ExpressionTranslator.getFromAccumulator(e, scope).toGetThere.toList

  private def getConditionCode(expr: Expr.Expr, scope: TranslatorScope, toBranchTo: Label, branchType: BranchType): List[IR.Instruction] = {
    expr match
      case Expr.BinaryOp(op, left, right) if Operation.Groups.RelationalTokens.contains(op) =>
        val leftStack = ExpressionTranslator.getFromStack(left, scope)
        val rightToAcc = toAccumulator(right, scope)
        val trueOp = if(branchType == BranchType.IfTrue) op else Operation.Groups.oppositeMap(op)
        val cmpCode = IR.Compare(leftStack.address, AReg())
        val checkAndBranch = trueOp match
          case Operation.Binary.Equal => cmpCode :: IR.BranchIfEqual(toBranchTo) :: Nil
          case Operation.Binary.NotEqual => cmpCode :: IR.BranchIfNotEqual(toBranchTo) :: Nil
          case Operation.Binary.Less => cmpCode :: IR.BranchIfCarrySet(toBranchTo) :: Nil
          case Operation.Binary.LessEqual => cmpCode :: IR.BranchIfCarrySet(toBranchTo) :: IR.BranchIfEqual(toBranchTo) :: Nil
          case Operation.Binary.Greater =>
            cmpCode :: IR.BranchIfEqual(Label("+")) :: IR.BranchIfNoCarry(toBranchTo) ::
              IR.PutLabel(Label("+")) :: Nil
          case Operation.Binary.GreaterEqual => cmpCode :: IR.BranchIfNoCarry(toBranchTo) :: Nil
          case _ => throw new Exception("Relational operator " ++ trueOp.toString ++ " is not recognised")

        leftStack.toGetThere.toList ::: rightToAcc ::: checkAndBranch
      case _ =>
        val conditionCode = toAccumulator(expr, scope)
        val branchCondition =IR.Compare(Immediate(1), AReg()) ::
          (if(branchType == BranchType.IfTrue)
            IR.BranchIfEqual(toBranchTo)
          else
            IR.BranchIfNotEqual(toBranchTo)
          ) :: Nil
        conditionCode ::: branchCondition
  }


  def translateStatement(stmt: Statement, scope: TranslatorScope): IRBuffer = {
    IRBuffer().append(
    stmt match
      case Assembly(assembly) => IR.UserAssembly(assembly) :: Nil
      case Block(statements) =>
        val blockScope = scope.getChild(stmt)
        blockScope.extendStack() :::
        statements.map(translateStatement(_, blockScope)).foldLeft(IRBuffer())(_.append(_)).toList :::
        blockScope.reduceStack()
      case EmptyStatement() => Nil
      case Expression(expr) => toAccumulator(expr, scope)
      case For(startStmt, breakExpr, incrimentExpr, body, lineNumber) =>
        val forScope = scope.getChild(stmt)
        val startForLabel = Label("for_l" ++ lineNumber.toString)
        val endForLabel = Label("for_end_l" ++ lineNumber.toString)
        val startCode = if(startStmt.isDefined) translateStatement(startStmt.get, forScope).toList else Nil
        val breakCode = if(breakExpr.isDefined) getConditionCode(breakExpr.get, forScope, endForLabel, BranchType.IfFalse) else Nil
        val incCode = if(incrimentExpr.isDefined) ExpressionTranslator.getFromAccumulator(incrimentExpr.get, forScope).toGetThere.toList else Nil
        val forBodyCode = translateStatement(body, forScope).toList
          forScope.extendStack() ::: startCode ::: (IR.PutLabel(startForLabel) :: Nil) ::: breakCode :::
          forBodyCode ::: incCode ::: (IR.BranchShort(startForLabel) :: IR.PutLabel(endForLabel) :: Nil) :::
          forScope.reduceStack()
      case If(condition, body, elseBranch, lineNumber) =>
        val elseStartLabel = Label("Else_l" ++ lineNumber.toString)
        val ifEndLabel = Label("If_End_l" ++ lineNumber.toString)
        val ifScope = scope.getChild(stmt)
        val bodyCode = translateStatement(body, ifScope).toList
        val ifEnder = if(elseBranch.isDefined) IR.BranchShort(ifEndLabel) :: Nil else Nil
        val elseCode = if(elseBranch.isDefined) translateStatement(elseBranch.get, scope).toList else Nil

        ifScope.extendStack() :::
        getConditionCode(condition, scope, elseStartLabel, BranchType.IfFalse) :::
        bodyCode ::: ifEnder :::
        (IR.PutLabel(elseStartLabel) :: Nil) ::: elseCode :::
        (IR.PutLabel(ifEndLabel) :: Nil) ::: ifScope.reduceStack()

      case Else(body) =>
        val elseScope = scope.getChild(stmt)
        elseScope.extendStack() :::
        translateStatement(body, elseScope).toList :::
        elseScope.reduceStack()
      case Return(value) =>
        (value match
          case Some(value) => toAccumulator(value, scope)
          case None =>Nil
        ) ::: IR.ReturnLong() :: Nil
      case VariableDecl(varDecl) => toAccumulator(varDecl, scope)
      case While(condition, body, lineNumber) =>
        val whileScope = scope.getChild(stmt)
        val startLabel = Label("While_l" ++ lineNumber.toString)
        val endLabel = Label("While_End_l" ++ lineNumber.toString)
        val bodyCode = translateStatement(body, whileScope)
        whileScope.extendStack() :::
        (IR.PutLabel(startLabel) :: Nil) :::
        getConditionCode(condition, scope, endLabel, BranchType.IfFalse) :::
//      bodyCode.toList :::
        (IR.BranchShort(startLabel) :: IR.PutLabel(endLabel) :: Nil)  :::
        whileScope.reduceStack()

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

    val ir = statements
      .map(translateStatement(_, TranslatorScope(symbolTable.globalScope)))
      .foldLeft(IRBuffer())(_.append(_))

    println(ir)

    val assembly = ir.toList.map(Translator(_)).foldLeft("")(_ ++ "\n" ++ _)

    println(assembly)
  }
}

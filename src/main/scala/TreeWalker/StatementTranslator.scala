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

  //Can break the stack
  def getConditionCode(expr: Expr.Expr, scope: TranslatorScope, toBranchTo: Label, branchType: BranchType): List[IR.Instruction] = {
    val uncommentedConditionalCode = expr match
      case Expr.BinaryOp(op, left, right) if Operation.Groups.RelationalTokens.contains(op) =>
        val leftStack = ExpressionTranslator.getFromStack(left, scope)
        val rightToAcc = toAccumulator(right, scope)
        val trueOp = if(branchType == BranchType.IfTrue) op else Operation.Groups.oppositeMap(op)
        val compareAndReset = (IR.Compare(leftStack.address, AReg()) :: Nil)
        val branch = trueOp match
          case Operation.Binary.Equal => IR.BranchIfEqual(toBranchTo) :: Nil
          case Operation.Binary.NotEqual => IR.BranchIfNotEqual(toBranchTo) :: Nil
          case Operation.Binary.Greater => IR.BranchIfNoCarry(toBranchTo) :: Nil
          case Operation.Binary.GreaterEqual => IR.BranchIfNoCarry(toBranchTo) :: IR.BranchIfEqual(toBranchTo) :: Nil
          case Operation.Binary.Less =>
            IR.BranchIfEqual(Label("+")) :: IR.BranchIfCarrySet(toBranchTo) ::
              IR.PutLabel(Label("+")) :: Nil
          case Operation.Binary.LessEqual => IR.BranchIfCarrySet(toBranchTo) :: Nil
          case _ => throw new Exception("Relational operator " ++ trueOp.toString ++ " is not recognised")

        leftStack.toGetThere.toList ::: rightToAcc ::: compareAndReset ::: branch
      case _ =>
        val conditionCode = toAccumulator(expr, scope)
        val branchCondition =IR.Compare(Immediate(1), AReg()) ::
          (if(branchType == BranchType.IfTrue)
            IR.BranchIfEqual(toBranchTo)
          else
            IR.BranchIfNotEqual(toBranchTo)
          ) :: Nil
        conditionCode ::: branchCondition

    uncommentedConditionalCode.head.addComment("Condition: " ++ expr.toString) ::
      uncommentedConditionalCode.tail.init.toList :::
      (uncommentedConditionalCode.last.addComment("End of condition, either branched if " ++ branchType.toString ++ " or fallen through") :: Nil)
  }

  private def translateStatement(stmt: Statement, scope: TranslatorScope): IRBuffer = {
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
        var fixConditionStack = List.empty[IR.Instruction]
        var breakCode = List.empty[IR.Instruction]
        if(breakExpr.isDefined) {
          scope.rememberStackLocation()
          breakCode = getConditionCode(breakExpr.get, forScope, endForLabel, BranchType.IfFalse)
          fixConditionStack = scope.getFixStackDecay().toList
        }
        val incCode = if(incrimentExpr.isDefined) ExpressionTranslator.getFromAccumulator(incrimentExpr.get, forScope).toGetThere.toList else Nil
        val forBodyCode = translateStatement(body, forScope).toList
          forScope.extendStack() ::: startCode ::: (IR.PutLabel(startForLabel) :: Nil) ::: breakCode ::: fixConditionStack :::
          forBodyCode ::: incCode ::: (IR.BranchShort(startForLabel) :: IR.PutLabel(endForLabel) :: Nil) :::
          forScope.reduceStack() ::: fixConditionStack
      case If(condition, body, elseBranch, lineNumber) =>
        val elseStartLabel = Label("Else_l" ++ lineNumber.toString)
        val ifEndLabel = Label("If_End_l" ++ lineNumber.toString)
        val ifScope = scope.getChild(stmt)
        val ifEnder = if(elseBranch.isDefined) IR.BranchShort(ifEndLabel) :: Nil else Nil

        ifScope.rememberStackLocation()
        ifScope.extendStack() :::
        getConditionCode(condition, ifScope, elseStartLabel, BranchType.IfFalse) :::
        translateStatement(body, ifScope).toList :::
        ifEnder :::
        (IR.PutLabel(elseStartLabel) :: Nil) :::
        (if(elseBranch.isDefined) translateStatement(elseBranch.get, ifScope).toList else Nil) :::
        (IR.PutLabel(ifEndLabel) :: Nil) :::
        ifScope.reduceStack() :::
        ifScope.getFixStackDecay().toList


//        scope.rememberStackLocation()
//        ifScope.extendStack() :::
//        getConditionCode(condition, scope, elseStartLabel, BranchType.IfFalse) :::
//        bodyCode ::: ifEnder :::
//        (IR.PutLabel(elseStartLabel) :: Nil) ::: elseCode :::
//        (IR.PutLabel(ifEndLabel) :: Nil) ::: ifScope.reduceStack() ::: scope.getFixStackDecay().toList

      case Else(body) =>
        val elseScope = scope.getChild(stmt)
        elseScope.extendStack() :::
        translateStatement(body, elseScope).toList :::
        elseScope.reduceStack()
      case Return(value) =>
        val getStackPointerToX = IR.Load(StackRelative(scope.getStackFrameOffset), AReg()).addComment("Put where to return stack into X") ::
          IR.TransferToX(AReg()) :: Nil
        val commandsToLoadReturn = value match
          case None => Nil
          case Some(value) => toAccumulator(value, scope)
        val commandsToResetStackAndReturn = IR.TransferXTo(StackPointerReg()).addComment("Reset stack") ::  IR.ReturnLong() :: Nil

        getStackPointerToX ::: commandsToLoadReturn ::: commandsToResetStackAndReturn

      case VariableDecl(varDecl) => toAccumulator(varDecl, scope)
      case While(condition, body, lineNumber) =>
        val whileScope = scope.getChild(stmt)
        val startLabel = Label("While_l" ++ lineNumber.toString)
        val endLabel = Label("While_End_l" ++ lineNumber.toString)
        val bodyCode = translateStatement(body, whileScope)

        val whileStart = whileScope.extendStack() :::
          (IR.PutLabel(startLabel) :: Nil)
        scope.rememberStackLocation()
        val whileCondition = getConditionCode(condition, scope, endLabel, BranchType.IfFalse).toList
        val fixStack = scope.getFixStackDecay().toList

        whileStart ::: whileCondition ::: fixStack ::: bodyCode.toList :::
        (IR.BranchShort(startLabel) :: IR.PutLabel(endLabel) :: Nil)  :::
        whileScope.reduceStack() ::: fixStack

      case _ => throw new Exception(stmt.toString ++ " cannot be translated yet")
    )
  }

  def apply(stmt: Statement, scope:TranslatorScope): IRBuffer = translateStatement(stmt, scope.getChild(stmt))

  def main(args: Array[String]): Unit = {
    val tokenBuffer = Parser.TokenBuffer(Scanner.scanText("src/main/StatementParserTest.txt"))
    val symbolTable = new SymbolTable
    val funcScope = symbolTable.globalScope.newFunctionChild()
    funcScope.setReturnType(Utility.Word())

    val statements = ListBuffer.empty[Stmt.Statement]

    while (tokenBuffer.peekType != TokenType.EndOfFile) {
      val stmt = Parser.StatementParser.parseOrThrow(funcScope, tokenBuffer)
      println(stmt)
      statements.append(stmt)
    }

    val ir = statements
      .map(translateStatement(_, TranslatorScope(funcScope)))
      .foldLeft(IRBuffer())(_.append(_))

    println(ir)

    val assembly = ir.toList.map(Translator(_)).foldLeft("")(_ ++ "\n" ++ _)

    println(assembly)
  }
}

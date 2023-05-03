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
  def getConditionCode(expr: Expr.Expr, scope: TranslatorScope, toBranchTo: Label, branchType: BranchType, estimatedJumpLength: Int): List[IR.Instruction] = {
    if(estimatedJumpLength >= GlobalData.snesData.maxConditionalJumpLength){
      val interimLabel = Label("JMP_not_taken_to_"++toBranchTo.name)
      val primaryConditionCode = getConditionCode(expr, scope, interimLabel, if(branchType == BranchType.IfTrue)BranchType.IfFalse else BranchType.IfTrue, 0)
      primaryConditionCode ::: (IR.JumpShortWithoutReturn(toBranchTo) :: IR.PutLabel(interimLabel) :: Nil)
    }
    else {
      val uncommentedConditionalCode = expr match
        case _ if expr.isInstanceOf[Expr.Variable] || expr.isInstanceOf[Expr.Get] || expr.isInstanceOf[Expr.GetIndex] =>
          val toAcc = toAccumulator(expr, scope)
          val branch = if (branchType == BranchType.IfTrue) {
            IR.BranchIfNotEqual(toBranchTo)
          } else IR.BranchIfEqual(toBranchTo)
          toAcc ::: (branch :: Nil)
        //Check for some special cases too!
        case Expr.UnaryOp(Operation.Unary.BitwiseNot, innerExpr) if innerExpr.isInstanceOf[Expr.Variable] || innerExpr.isInstanceOf[Expr.Get] || innerExpr.isInstanceOf[Expr.GetIndex] =>
          val toAcc = toAccumulator(innerExpr, scope)
          val branch = if (branchType == BranchType.IfTrue) {
            IR.BranchIfEqual(toBranchTo)
          } else IR.BranchIfNotEqual(toBranchTo)
          toAcc ::: (branch :: Nil)
        //Check for some special cases too!
        case Expr.BinaryOp(Operation.Binary.Equal, lExpr, zeroOrFalse) if zeroOrFalse == Expr.NumericalLiteral(0) || zeroOrFalse == Expr.BooleanLiteral(false) =>
          val toAcc = toAccumulator(lExpr, scope)
          val branch = (if(branchType == BranchType.IfTrue){
            IR.BranchIfEqual(toBranchTo)
          } else IR.BranchIfNotEqual(toBranchTo)) :: Nil
          toAcc ::: branch
        case Expr.BinaryOp(Operation.Binary.Equal, zeroOrFalse, rExpr) if zeroOrFalse == Expr.NumericalLiteral(0) || zeroOrFalse == Expr.BooleanLiteral(false) =>
          val toAcc = toAccumulator(rExpr, scope)
          val branch = (if (branchType == BranchType.IfTrue) {
            IR.BranchIfEqual(toBranchTo)
          } else IR.BranchIfNotEqual(toBranchTo)) :: Nil
          toAcc ::: branch
        case Expr.BinaryOp(op, left, right) if Operation.Groups.RelationalTokens.contains(op) =>
          val leftStack = ExpressionTranslator.getFromStack(left, scope)
          val rightToAcc = toAccumulator(right, scope)
          val trueOp = if (branchType == BranchType.IfTrue) op else Operation.Groups.oppositeMap(op)
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
          val branchCondition = IR.Compare(Immediate(1), AReg()) ::
            (if (branchType == BranchType.IfTrue)
              IR.BranchIfEqual(toBranchTo)
            else
              IR.BranchIfNotEqual(toBranchTo)
              ) :: Nil
          conditionCode ::: branchCondition

      uncommentedConditionalCode.head.addComment("Condition: " ++ expr.toString) ::
        uncommentedConditionalCode.tail.init.toList :::
        (uncommentedConditionalCode.last.addComment("End of condition (" ++expr.toString ++ "), either branched if " ++ branchType.toString ++ " or fallen through") :: Nil)
    }
  }

  private def translateStatement(stmt: Statement, scope: TranslatorScope): IRBuffer = {
    IRBuffer().append(
    stmt match
      case Assembly(assembly) =>
        assembly.foreach(
          s => if(s.split(' ').contains("jsl")){
            scope.getTranslatorSymbolTable.usedFunctionLabels.addOne(s.split(' ')(1))
          }
        )
        IR.UserAssembly(assembly) :: Nil
      case Block(statements, _) =>
        //TODO switch into bank 0 if you need to
        val blockScope = scope.getChild(stmt)
        blockScope.extendStack() :::
        statements.map(translateStatement(_, blockScope)).foldLeft(IRBuffer())(_.append(_)).toList :::
        blockScope.reduceStack()
      case EmptyStatement() => Nil
      case Expression(expr) => toAccumulator(expr, scope)
      case For(startStmt, breakExpr, incrimentExpr, body, lineNumber, fileIndex) =>
        val forScope = scope.getChild(stmt)
        val startForLabel = Label("for_l" ++ lineNumber.toString ++ "_f" ++ fileIndex.toString)
        val endForLabel = Label("for_end_l" ++ lineNumber.toString ++ "_f" ++ fileIndex.toString)
        val startCode = if(startStmt.isDefined) translateStatement(startStmt.get, forScope).toList else Nil
        var fixConditionStack = List.empty[IR.Instruction]
        var breakCode = List.empty[IR.Instruction]
        var forBodyCode = translateStatement(body, forScope).toList
        if(breakExpr.isDefined) {
          scope.rememberStackLocation()
          breakCode = getConditionCode(breakExpr.get, forScope, endForLabel, BranchType.IfFalse, IR.sizeOfIr(forBodyCode))
          forBodyCode = translateStatement(body, forScope).toList
          fixConditionStack = scope.getFixStackDecay().toList
        }
        val incCode = if(incrimentExpr.isDefined) ExpressionTranslator.getFromAccumulator(incrimentExpr.get, forScope).toGetThere.toList else Nil

        val bodySize = IR.sizeOfIr(forBodyCode)

        forScope.extendStack() ::: startCode ::: (IR.PutLabel(startForLabel) :: Nil) ::: breakCode ::: fixConditionStack :::
        forBodyCode ::: incCode ::: ((if bodySize < GlobalData.snesData.maxConditionalJumpLength then IR.BranchShort(startForLabel) else IR.BranchLong(startForLabel)) :: IR.PutLabel(endForLabel) :: Nil) :::
        forScope.reduceStack() ::: fixConditionStack
      case If(condition, body, elseBranch, lineNumber, fileIndex) =>
        val elseStartLabel = Label("Else_l" ++ lineNumber.toString ++ "_f" ++ fileIndex.toString)
        val ifEndLabel = Label("If_End_l" ++ lineNumber.toString ++ "_f" ++ fileIndex.toString)
        val ifScope = scope.getChild(stmt)
        val elseBody: List[IR.Instruction] = (if(elseBranch.isDefined) translateStatement(elseBranch.get, ifScope).toList else Nil)
        val sizeUpBodyCode = translateStatement(body, ifScope).toList
        val ifEnder = if (elseBranch.isDefined)
            (if(IR.sizeOfIr(elseBody) > GlobalData.snesData.maxConditionalJumpLength)
              IR.BranchLong(ifEndLabel)
            else
              IR.BranchShort(ifEndLabel))
            :: Nil
          else Nil

        ifScope.rememberStackLocation()

        val extendIR = ifScope.extendStack()
        val conditionIR = getConditionCode(condition, ifScope, elseStartLabel, BranchType.IfFalse, IR.sizeOfIr(sizeUpBodyCode))
        val bodyIR = translateStatement(body, ifScope).toList //Re-translate the body code
        val enderAndElseStartIR = ifEnder ::: (IR.PutLabel(elseStartLabel) :: Nil)
        val elseBodyIR = (if(elseBranch.isDefined) translateStatement(elseBranch.get, ifScope).toList else Nil) //Re-translate the else code
        val endIfIR = (IR.PutLabel(ifEndLabel) :: Nil)
        val reduceStackIR = ifScope.reduceStack()
        val fixDecayIR = ifScope.getFixStackDecay().toList

        extendIR ::: conditionIR ::: bodyIR ::: enderAndElseStartIR ::: elseBodyIR ::: endIfIR ::: reduceStackIR ::: fixDecayIR

      case Else(body) =>
        val elseScope = scope.getChild(stmt)
        elseScope.extendStack() :::
        translateStatement(body, elseScope).toList :::
        elseScope.reduceStack()
      case Return(value) =>
        value match
          case None => IR.Load(StackRelative(scope.getStackFrameOffset), AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()).addComment("Reset the stack") :: IR.ReturnLong() :: Nil
          case Some(value) =>
            val valueToAccumulator = scope.inner.getTypeOf(value) match
              case Utility.Array(_, _) =>
                val (address, toGetAddress) = Getting.getAddressOf(value, scope)
                toGetAddress.append(Getting.getAddressIntoAcumulator(address)).toList
              case _ => toAccumulator(value, scope)
            valueToAccumulator :::
            (IR.TransferAccumulatorTo(YReg()) :: IR.Load(StackRelative(scope.getStackFrameOffset), AReg()) :: IR.TransferAccumulatorTo(StackPointerReg()) ::
              IR.TransferYTo(AReg()) :: IR.ReturnLong() :: Nil)

      case VariableDecl(varDecl) => toAccumulator(varDecl, scope)
      case While(condition, body, lineNumber, fileIndex) =>
        val whileScope = scope.getChild(stmt)
        val startLabel = Label("While_l" ++ lineNumber.toString ++ "_f" ++ fileIndex.toString)
        val endLabel = Label("While_End_l" ++ lineNumber.toString ++ "_f" ++ fileIndex.toString)
        val bodyCode = translateStatement(body, whileScope)

        val whileStart = whileScope.extendStack() :::
          (IR.PutLabel(startLabel) :: Nil)
        scope.rememberStackLocation()
        val bodySize = IR.sizeOfIr(bodyCode)
        val whileCondition = getConditionCode(condition, scope, endLabel, BranchType.IfFalse, bodySize)
        val fixStack = scope.getFixStackDecay().toList

        whileStart ::: whileCondition ::: fixStack ::: bodyCode.toList :::
        ((if bodySize < GlobalData.snesData.maxConditionalJumpLength then
          IR.BranchShort(startLabel) else IR.BranchLong(startLabel))
          :: IR.PutLabel(endLabel) :: Nil)  :::
        whileScope.reduceStack() ::: fixStack

      case _ => throw new Exception(stmt.toString ++ " cannot be translated yet")
    )
  }

  def apply(stmt: Statement, scope:TranslatorScope): IRBuffer = translateStatement(stmt, scope.getChild(stmt))

}

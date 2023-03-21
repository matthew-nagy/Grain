package Parser

import Grain.*
import Grain.Expr.*
import Utility.{Errors, Struct, SyntaxError, TokenType}

object ExpressionParser {

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr | SyntaxError =
    parseExpression(scope, tokenBuffer)

  def parseOrThrow(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr =
    parseExpression(scope, tokenBuffer) match
      case error: SyntaxError => throw error
      case expr: Expr.Expr => expr

  //Because of how the translator works, some ops need the branches the other way round
  private def swapArithmeticBranches(expr: Expr.Expr): Expr.Expr = {
    expr match
      case Expr.Assign(name, arg) => Expr.Assign(name, swapArithmeticBranches(arg))
      case Expr.UnaryOp(op, arg) => Expr.UnaryOp(op, swapArithmeticBranches(arg))
      case Expr.BinaryOp(op, left, right) =>
        if(Operation.Groups.orderImportantOperations.contains(op))
          Expr.BinaryOp(op, right, left)
        else expr
      case Expr.Indirection(expr) => Expr.Indirection(swapArithmeticBranches(expr))
      case Expr.FunctionCall(function, arguments) =>
        Expr.FunctionCall(swapArithmeticBranches(function), arguments.map(swapArithmeticBranches))
      case Expr.Get(left, name) => Expr.Get(swapArithmeticBranches(left), name)
      case Expr.GetAddress(expr) => Expr.GetAddress(swapArithmeticBranches(expr))
      case Expr.GetIndex(of, by) => Expr.GetIndex(swapArithmeticBranches(of), swapArithmeticBranches(by))
      case Expr.Set(left, right) => Expr.Set(swapArithmeticBranches(left), swapArithmeticBranches(right))
      case Expr.SetIndex(of, to) => Expr.SetIndex(swapArithmeticBranches(of), swapArithmeticBranches(to))
      case Expr.Grouping(internalExpr) => Expr.Grouping(swapArithmeticBranches(internalExpr))
      case _ => expr
  }


  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr | SyntaxError = {
    returnTypeOrError {
      val expr = parseAssignment(scope, tokenBuffer)
      swapArithmeticBranches(expr)
    }
  }

  private def parseAssignment(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val token = tokenBuffer.peek
    val expr = parseLogical(scope, tokenBuffer)

    if(tokenBuffer.peekType == TokenType.Equal) {
      tokenBuffer.advance()
      val right = parseAssignment(scope, tokenBuffer)

      var expression = expr
      var isGrouping = true
      while isGrouping do{
        expression match
          case Grouping(internalExpr) => expression = internalExpr
          case _ => isGrouping = false
      }
      return expression match
        case Expr.Get(_, _) => Expr.Set(expression, right)
        case Expr.Variable(name) => Expr.Assign(name, right)
        case Expr.GetIndex(_, _) => Expr.SetIndex(expression, right)
        case Expr.Indirection(inner) => Expr.SetIndex(GetIndex(inner, Expr.NumericalLiteral(0)), right)//This is horrible. I love it
        case _ =>
          println(expression)
          throw Errors.invalidLValue(token)
    }

    expr
  }
  private def parseLogical(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var expr = parseEquality(scope, tokenBuffer)

    while(
      tokenBuffer.peekType == TokenType.Or ||
        tokenBuffer.peekType == TokenType.And ||
        tokenBuffer.peekType == TokenType.Xor
    ){
      val tokenType = tokenBuffer.advance().tokenType
      val op = if(tokenType == TokenType.Or)
        Operation.Binary.Or else Operation.Binary.And
      val right = parseEquality(scope, tokenBuffer)
      expr = Expr.BinaryOp(op, expr, right)
    }
    expr
  }
  private def parseEquality(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseComparison(scope, tokenBuffer)

    if(tokenBuffer.peekType == TokenType.BangEqual ||
      tokenBuffer.peekType == TokenType.EqualEqual){
      val op = if(tokenBuffer.advance().tokenType == TokenType.BangEqual)
        Operation.Binary.NotEqual else Operation.Binary.Equal
      val right = parseComparison(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
    }

    expr
  }
  private def parseComparison(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseTerm(scope, tokenBuffer)

    val termDict = Map(
      TokenType.Greater -> Operation.Binary.Greater,
      TokenType.GreaterEqual -> Operation.Binary.GreaterEqual,
      TokenType.Less -> Operation.Binary.Less,
      TokenType.LessEqual -> Operation.Binary.LessEqual
    )

    if(termDict.contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val op = termDict(tokenType)
      val right = parseTerm(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
    }

    expr
  }
  private def parseTerm(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var expr = parseFactor(scope, tokenBuffer)

    val termDict = Map(
      TokenType.Minus -> Operation.Binary.Subtract,
      TokenType.Plus -> Operation.Binary.Add,
      TokenType.ShiftLeft -> Operation.Binary.ShiftLeft,
      TokenType.ShiftRight -> Operation.Binary.ShiftRight
    )

    while(termDict.contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val right = parseFactor(scope, tokenBuffer)
      expr = Expr.BinaryOp(termDict(tokenType), expr, right)
    }

    expr
  }
  private def parseFactor(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseUnary(scope, tokenBuffer)
    if((TokenType.Star :: TokenType.Slash :: TokenType.Percent :: Nil).contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val op = tokenType match
        case TokenType.Star => Operation.Binary.Multiply
        case TokenType.Slash => Operation.Binary.Divide
        case TokenType.Percent => Operation.Binary.Modulo
        case _ => throw new Exception("Shouldn't be able to get here")
      val right = parseUnary(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
    }
    expr
  }
  private def parseUnary(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    if((TokenType.Tilde :: TokenType.Bang :: TokenType.Asperand :: TokenType.Minus :: Nil).contains(tokenBuffer.peekType)){
      val token = tokenBuffer.advance()
      val op: Option[Operation.Unary]  = token.tokenType match {
        case TokenType.Tilde => Some(Operation.Unary.BitwiseNot)
        case TokenType.Bang => Some(Operation.Unary.BooleanNegation)
        case TokenType.Minus => Some(Operation.Unary.Minus)
        case TokenType.Asperand => None
        case _ => throw Errors.expectedUnary(token)
      }
      val right = parseUnary(scope, tokenBuffer)

      return op match
        case Some(operation) => Expr.UnaryOp(operation, right)
        case None => Expr.Indirection(right)
    }
    parseCall(scope, tokenBuffer)
  }
  private def finishCall(function: Expr.Expr, scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var arguments = List.empty[Expr.Expr]
    while(tokenBuffer.peekType != TokenType.RightParen){
      if arguments.nonEmpty then tokenBuffer.matchType(TokenType.Comma)
      arguments = ExpressionParser.parseOrThrow(scope, tokenBuffer) :: arguments
    }
    arguments = arguments.reverse
    tokenBuffer.matchType(TokenType.RightParen)
    Expr.FunctionCall(function, arguments)
  }
  private def parseCall(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var indirection = false
    if(tokenBuffer.peekType == TokenType.Asperand){
      indirection = true
      tokenBuffer.advance()
    }
    var expr = parsePrimary(scope, tokenBuffer)
    if(indirection){
      expr = Expr.Indirection(expr)
    }
    var expType = scope.getTypeOf(expr)

    while(tokenBuffer.peek.tokenType match{
      case TokenType.LeftParen =>
        if !expType.isInstanceOf[Utility.FunctionPtr] then throw Errors.CannotCallType(tokenBuffer.peek.lineNumber, expType)
        tokenBuffer.advance()
        expr = finishCall(expr, scope, tokenBuffer)
        true
      case TokenType.Dot =>
        tokenBuffer.advance()
        val name = tokenBuffer.matchType(TokenType.Identifier)
        expr = Expr.Get(expr, name)
        true
      case TokenType.Asperand =>
        tokenBuffer.advance()
        expr = Expr.GetAddress(expr)
        true
      case TokenType.LeftSquare =>
        tokenBuffer.advance()
        val indexBy = ExpressionParser.parseOrThrow(scope, tokenBuffer)
        tokenBuffer.matchType(TokenType.RightSquare)
        expr = Expr.GetIndex(expr, indexBy)
        true
      case _ => false
    }){
      expType = scope.getTypeOf(expr)
    }

    expr
  }

  private def hexDigit(digit: Char): Int = {
    val digitMap: Map[Char, Int] = Range(0, 10).map(v => v.toString()(0) -> v).toMap[Char, Int] ++
      Map[Char, Int]('A' -> 10, 'B' -> 11, 'C' -> 12, 'D' -> 13, 'E' -> 14, 'F' -> 15)
    digitMap(digit)
  }
  private def parseIntValue(int: String): Int = {
    if(int.length < 3 || int(0) != '0'){
      Integer.parseInt(int)
    }
    else{
      int(1) match
        case 'b' =>
          var value = 0
          for i <- Range(2, int.length) do{
            value <<= 1
            value |= (if(int(i) == '1') 1 else 0)
          }
          value
        case 'x' =>
          var value = 0
          for i <- Range(2, int.length) do {
            value <<= 4
            value |= hexDigit(int(i).toUpper)
          }
          value
        case _ => Integer.parseInt(int)
    }
  }

  private def parsePrimary(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val token = tokenBuffer.advance()
    token.tokenType match
      case TokenType.False => Expr.BooleanLiteral(false)
      case TokenType.True => Expr.BooleanLiteral(true)
      case TokenType.IntLiteral => Expr.NumericalLiteral(parseIntValue(token.lexeme))
      case TokenType.StringLiteral => Expr.StringLiteral(token.lexeme)
      case TokenType.LeftParen =>
        val nextExpression = parseExpression(scope, tokenBuffer)
        nextExpression match
          case nextExpression: SyntaxError => throw nextExpression
          case nextExpression: Expr.Expr =>
            val grouping = Expr.Grouping(nextExpression)
            tokenBuffer.matchType(TokenType.RightParen)
            grouping
      case TokenType.Identifier =>
        if(!scope.contains(token.lexeme)){
          throw Errors.SymbolNotFound(token)
        }
        Expr.Variable(token)
      case _ =>
        throw Errors.ExpectedExpression(token)
  }


  private def typeCheckUnary(unary: Operation.Unary, value: Utility.Type): Boolean =
    unary match
      case Operation.Unary.Minus => value == Utility.Word()
      case Operation.Unary.BooleanNegation => value == Utility.BooleanType()
      case Operation.Unary.BitwiseNot => value == Utility.BooleanType() || value == Utility.Word()
      case null => throw Exception("Invalid case")
  def typeCheck(expr: Expr.Expr, scope: Scope): Unit = {
    import Expr.*

    expr match
      case Assign(varToken, arg) =>
        typeCheck(arg, scope)
        if(!Utility.typeEquivilent(scope(varToken.lexeme).dataType, scope.getTypeOf(arg))){
          throw Errors.badlyTyped(
            varToken.lexeme ++ " has type " ++ scope(varToken.lexeme).dataType.toString ++ ", rValue has type " ++ scope.getTypeOf(arg).toString
          )
        }
      case BooleanLiteral(_) =>
      case UnaryOp(op, arg) =>
        typeCheck(arg, scope)
        typeCheckUnary(op, scope.getTypeOf(arg))
      case BinaryOp(op, left, right) =>
        typeCheck(left, scope)
        typeCheck(right, scope)
        op match
          case _ if Operation.Groups.LogicalTokens.contains(op) || Operation.Groups.RelationalTokens.contains(op) =>
            if(!Utility.typeEquivilent(scope.getTypeOf(left), scope.getTypeOf(right))){
              throw Errors.badlyTyped(
                "LValue " ++ left.toString ++ " with type " ++ scope.getTypeOf(left).toString ++
                  "cannot use " ++ op.toString ++ " with RValue of type " ++ scope.getTypeOf(right).toString ++ " (" ++ right.toString ++ ")"
              )
            }
          case _ if Operation.Groups.ArithmeticTokens.contains(op) =>
            if(!Utility.typeEquivilent(scope.getTypeOf(left), Utility.Word())){
              throw Errors.badlyTyped(
                op.toString ++ " requires arguments of type word. Left argument (" ++ left.toString ++ ") has type " ++ scope.getTypeOf(left).toString
              )
            }
            if(!Utility.typeEquivilent(scope.getTypeOf(right), Utility.Word())){
              throw Errors.badlyTyped(
                op.toString ++ " requires arguments of type word. Right argument (" ++ right.toString ++ ") has type " ++ scope.getTypeOf(right).toString
              )
            }
          case _ => throw Exception("Ungrouped binary operation")

      case NumericalLiteral(_) =>
      case StringLiteral(_) =>
      case Indirection(expr) =>
        typeCheck(expr, scope)
        if(!scope.getTypeOf(expr).isInstanceOf[Utility.Ptr]){
          Errors.badlyTyped(
            "Cannot use indirection on non pointer type. " ++ expr.toString ++ " is of type " ++ scope.getTypeOf(expr).toString
          )
        }
      case Variable(_) =>
      case FunctionCall(function, arguments) =>
        val functionType = scope.getTypeOf(function)
        functionType match
          case Utility.FunctionPtr(argTypes, _) =>
            argTypes.zip(arguments.map(scope.getTypeOf)).forall(
              (expectedT, givenT) => {
                if (!Utility.typeEquivilent(expectedT, givenT)) {
                  throw Errors.badlyTyped(
                    "Expected type " ++ expectedT.toString ++ " but got type " ++ givenT.toString ++ ". (" ++ expr.toString ++ ")"
                  )
                }
                true
              }
            ) && arguments.forall(arg => {
              typeCheck(arg, scope)
              true
            })
          case _ =>
      case Get(left, name) =>
        val leftType = scope.getTypeOf(left)
        leftType match {
          case leftType: Struct =>
            typeCheck(left, scope)
            if(!leftType.entries.contains(name.lexeme)){
              throw Errors.structDoesntHaveElement(name.lexeme, leftType.toString)
            }
          case _ =>
        }
      case GetAddress(of) =>
        //Only some types can have their address got
        typeCheck(of, scope)
        if(!(of.isInstanceOf[Variable] || of.isInstanceOf[Get] || of.isInstanceOf[GetIndex])){
          throw Errors.badlyTyped(
            of.toString ++ " is not an addressable value"
          )
        }
      case GetIndex(of, by) =>
        val ofType = scope.getTypeOf(of)
        typeCheck(of, scope)
        typeCheck(by, scope)
        if(!Utility.typeEquivilent(scope.getTypeOf(by), Utility.Word())){
          throw Errors.badlyTyped(by.toString ++ " has type " ++ scope.getTypeOf(by).toString ++ ", expected word")
        }
        if(!ofType.isInstanceOf[Utility.PtrType]){
          throw Errors.cannotIndexNonPoinerElements(of.toString, ofType)
        }
      case Set(left, right) =>
        typeCheck(left, scope)
        typeCheck(right, scope)
        if(!Utility.typeEquivilent(scope.getTypeOf(left), scope.getTypeOf(right))){
          throw Errors.badlyTyped(
            left.toString ++ " has type " ++ scope.getTypeOf(left).toString ++ ", rValue has type " ++ scope.getTypeOf(right).toString
          )
        }
      case SetIndex(left, right) =>
        typeCheck(left, scope)
        typeCheck(right, scope)
        if (!Utility.typeEquivilent(scope.getTypeOf(left), scope.getTypeOf(right))) {
          throw Errors.badlyTyped(
            left.toString ++ " has type " ++ scope.getTypeOf(left).toString ++ ", rValue has type " ++ scope.getTypeOf(right).toString
          )
        }
      case Grouping(internalExpr) => typeCheck(internalExpr, scope)
      case null => throw new Exception("What?")
  }
}

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

  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr | SyntaxError =
    returnTypeOrError {
      parseAssignment(scope, tokenBuffer)
    }

  private def parseAssignment(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val token = tokenBuffer.peek
    val expr = parseLogical(scope, tokenBuffer)

    //println(tokenBuffer.peekType.toString)

    if(tokenBuffer.peekType == TokenType.Equal) {
      tokenBuffer.advance()
      val right = parseAssignment(scope, tokenBuffer)

      return expr match
        case Expr.Get(_, _) => Expr.Set(expr, right)
        case Expr.Variable(name) => Expr.Assign(name, right)
        case Expr.GetIndex(_, _) => Expr.SetIndex(expr, right)
        case _ => throw Errors.invalidLValue(token)
    }

    expr
  }
  private def parseLogical(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var expr = parseEquality(scope, tokenBuffer)

    while(
      tokenBuffer.peekType == TokenType.Or ||
        tokenBuffer.peekType == TokenType.And
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
      val op = token.tokenType match {
        case TokenType.Tilde => Operation.Unary.BitwiseNot
        case TokenType.Bang => Operation.Unary.BooleanNegation
        case TokenType.Minus => Operation.Unary.Minus
        case _ => throw Errors.expectedUnary(token)
      }
      val right = parseUnary(scope, tokenBuffer)

      return Expr.UnaryOp(op, right)
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
  private def parsePrimary(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val token = tokenBuffer.advance()
    token.tokenType match
      case TokenType.False => Expr.BooleanLiteral(false)
      case TokenType.True => Expr.BooleanLiteral(true)
      case TokenType.IntLiteral => Expr.NumericalLiteral(Integer.parseInt(token.lexeme))
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
  def typeCheck(expr: Expr.Expr, scope: Scope): Boolean = {
    import Expr.*

    expr match
      case Assign(varToken, arg) =>
        typeCheck(arg, scope) && scope(varToken.lexeme).dataType == scope.getTypeOf(arg)
      case BooleanLiteral(_) => true
      case UnaryOp(op, arg) =>
        typeCheck(arg, scope) && typeCheckUnary(op, scope.getTypeOf(arg))
      case BinaryOp(op, left, right) =>
        typeCheck(left, scope) && typeCheck(right, scope) && (op match
          case _ if Operation.Groups.LogicalTokens.contains(op) || Operation.Groups.RelationalTokens.contains(op) =>
            scope.getTypeOf(left) == scope.getTypeOf(right)
          case _ if Operation.Groups.ArithmeticTokens.contains(op) =>
            scope.getTypeOf(left) == Utility.Word() && scope.getTypeOf(right) == Utility.Word()
          case _ => throw Exception("Ungrouped binary operation")
        )
      case NumericalLiteral(_) => true
      case StringLiteral(_) => true
      case Indirection(expr) =>
        typeCheck(expr, scope) && scope.getTypeOf(expr).isInstanceOf[Utility.Ptr]
      case Variable(_) => true
      case FunctionCall(function, arguments) =>
        val functionType = scope.getTypeOf(function)
        functionType match
          case Utility.FunctionPtr(argTypes, _) =>
            argTypes.zip(arguments.map(scope.getTypeOf)).forall(_ == _) && arguments.forall(typeCheck(_, scope))
          case _ => false
      case Get(left, name) =>
        val leftType = scope.getTypeOf(left)
        leftType match {
          case leftType: Struct =>
            leftType.entries.contains(name.lexeme) && typeCheck(left, scope)
          case _ => false
        }
      case GetAddress(of) =>
        //Only some types can have their address got
        (of.isInstanceOf[Variable] || of.isInstanceOf[Get] || of.isInstanceOf[GetIndex]) && typeCheck(of, scope)
      case GetIndex(of, by) =>
        val ofType = scope.getTypeOf(of)
        typeCheck(of, scope) && typeCheck(by, scope) && scope.getTypeOf(by) == Utility.Word() &&
          (ofType.isInstanceOf[Utility.Array] || ofType.isInstanceOf[Utility.Ptr])
      case Set(left, right) =>
        scope.getTypeOf(left) == scope.getTypeOf(right) && typeCheck(left, scope) && typeCheck(right, scope)
      case SetIndex(left, right) =>
        scope.getTypeOf(left) == scope.getTypeOf(right) && typeCheck(left, scope) && typeCheck(right, scope)
      case Grouping(internalExpr) => typeCheck(internalExpr, scope)
      case null => true
  }
}

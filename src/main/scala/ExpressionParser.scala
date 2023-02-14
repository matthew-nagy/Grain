package Grain

import Utility.{Errors, SyntaxError, TokenType}

object ExpressionParser {

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr | SyntaxError =
    parseExpression(scope, tokenBuffer)

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
        case _ => throw Errors.invalidLValue(token)
    }

    expr
  }
  private def parseLogical(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseEquality(scope, tokenBuffer)

    if(
      tokenBuffer.peekType == TokenType.Or ||
        tokenBuffer.peekType == TokenType.And
    ){
      val tokenType = tokenBuffer.advance().tokenType
      val op = if(tokenType == TokenType.Or)
        Operation.Binary.Or else Operation.Binary.And
      val right = parseEquality(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
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
    val expr = parseFactor(scope, tokenBuffer)

    val termDict = Map(
      TokenType.Minus -> Operation.Binary.Subtract,
      TokenType.Plus -> Operation.Binary.Add,
      TokenType.ShiftLeft -> Operation.Binary.ShiftLeft,
      TokenType.ShiftRight -> Operation.Binary.ShiftRight
    )

    if(termDict.contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val right = parseFactor(scope, tokenBuffer)
      return Expr.BinaryOp(termDict(tokenType), expr, right)
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
        case TokenType.Tilde => Operation.Unary.Xor
        case TokenType.Bang => Operation.Unary.BooleanNegation
        case TokenType.Asperand => Operation.Unary.Indirection
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
    if(tokenBuffer.peekType != TokenType.RightParen){
      throw new Exception("I didn't dooo this")
    }
    val paren = tokenBuffer.matchType(TokenType.RightParen)
    Expr.FunctionCall(function, arguments)
  }
  private def parseCall(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var expr = parsePrimary(scope, tokenBuffer)

    while(tokenBuffer.peek.tokenType match{
      case TokenType.LeftParen =>
        tokenBuffer.advance()
        expr = finishCall(expr, scope, tokenBuffer)
        true
      case TokenType.Dot =>
        tokenBuffer.advance()
        val name = tokenBuffer.matchType(TokenType.Identifier)
        expr = Expr.Get(expr, name)
        true
      case _ => false
    }){}

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

}

package Grain
import Grain.Scanner.scanText
import Utility.{Errors, SyntaxError, Token, TokenType, Type}

import scala.annotation.tailrec


class TokenBuffer(private val tokens: List[Token]){
  private var index = 0

  def peek: Token = tokens(index)

  def peekType: TokenType = tokens(index).tokenType
  def advance(): Token = {
    val toReturn = peek
    toReturn.tokenType match
      case TokenType.EndOfFile => peek
      case _ =>
        index += 1
        toReturn
  }

  def lookAhead(by: Int): Token = {
    val toCheck = index + by
    if(toCheck >= tokens.length){
      tokens.last
    }
    else{
      tokens(toCheck)
    }
  }

  def checkGroup(requestedTypes: List[TokenType]): Option[List[Token]] = {
    val tokens = for i <- Range(0, requestedTypes.length) yield lookAhead(i)
    requestedTypes.zip(tokens).forall((ttype, token) => ttype == token.tokenType) match
      case true =>
        index += requestedTypes.length
        Some(tokens.toList)
      case false => None
  }

  def matchType(requestedType: TokenType): Token = {
    if (peek.tokenType == requestedType) {
      advance()
    }
    else {
      throw Errors.expectedTokenError(peek, requestedType)
    }
  }

}


@tailrec
def parseTypeAddition(currentType: Type, tokenBuffer: TokenBuffer): Type = {
  tokenBuffer.peekType match
    case TokenType.Ptr =>
      tokenBuffer.advance()
      parseTypeAddition(Utility.Ptr(currentType), tokenBuffer)
    case TokenType.LeftSquare =>
      tokenBuffer.checkGroup(TokenType.LeftSquare :: TokenType.IntLiteral :: TokenType.RightSquare :: Nil) match
        case None => currentType
        case Some(tokens) => Utility.Array(currentType, tokens(1).lexeme.toInt)
    case _ =>
      println(tokenBuffer.peek)
      currentType

}

def parseType(symbolTable: SymbolTable, tokenBuffer: TokenBuffer): Type = {
  val startToken = tokenBuffer.advance()
  if(symbolTable.types.contains(startToken.lexeme)){
    val startType = symbolTable.types(startToken.lexeme)
    parseTypeAddition(startType, tokenBuffer)
  }
  else{
    throw Errors.expectedType(startToken)
  }
}

def returnTypeOrError[T](action: =>T): T | SyntaxError =
  try
    action
  catch
    case se: SyntaxError => se
    case e@_ => SyntaxError(-1, "Unknown error type; '" ++ e.toString ++ "'")


object StatementParser {

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement | SyntaxError =
    parseStatement(scope, tokenBuffer)
  private def parseStatement(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement | SyntaxError =
    returnTypeOrError {
      tokenBuffer.peekType match
        case TokenType.Assembly => parseAssembly(scope, tokenBuffer)
        case TokenType.LeftBrace => parseBlock(scope, tokenBuffer)
        case _ =>
          tokenBuffer.lookAhead(1).tokenType match
            case TokenType.Colon => parseVariableDecl(scope, tokenBuffer)
            case _ => parseExpression(scope, tokenBuffer)
    }

  private def parseAssembly(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    tokenBuffer.matchType(TokenType.Assembly)
    tokenBuffer.matchType(TokenType.LeftBrace)
    val literal = tokenBuffer.matchType(TokenType.StringLiteral)
    tokenBuffer.matchType(TokenType.RightBrace)
    Stmt.Assembly(literal.lexeme)
  }

  private def parseBlock(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    val startBrace = tokenBuffer.matchType(TokenType.LeftBrace)
    var inner = List.empty[Stmt.Statement]
    val newScope = scope.newChild()

    while (tokenBuffer.peekType match
      case TokenType.RightBrace =>
        tokenBuffer.advance()
        false
      case TokenType.EndOfFile => throw Errors.UnclosedCurlyBrackets(startBrace)
      case _ =>
        inner = parseStatement(newScope, tokenBuffer) :: inner
        true
    ) {}

    scope.linkStatementWithScope(Stmt.Block(inner.reverse), newScope)
  }

  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    val parsedExpr = ExpressionParser.parseExpression(scope, tokenBuffer)
    parsedExpr match
      case parsedExpr: Utility.SyntaxError => throw parsedExpr
      case parsedExpr: Expr.Expr => Stmt.Expression(parsedExpr)
  }


  private def parseVariableDecl(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    val name = tokenBuffer.advance()
    tokenBuffer.matchType(TokenType.Colon)
    val variableType = parseType(scope.symbolTable, tokenBuffer)

    val statement = tokenBuffer.peekType match
      case TokenType.Equal =>
        tokenBuffer.advance()
        val parsedInitialiser = ExpressionParser(scope, tokenBuffer)
        parsedInitialiser match
          case parsedInitialiser: SyntaxError => throw parsedInitialiser
          case parsedInitialiser: Expr.Expr => Stmt.VariableDecl(name, Some(parsedInitialiser))
      case _ =>
        Stmt.VariableDecl(name, None)

    scope.addSymbol(name, variableType, statement.initializer.isDefined)
  }

}

object ParseMain{
  def main(args: Array[String]): Unit = {
    val tokenBuffer = TokenBuffer(scanText("parserTest.txt"))
    val symbolTable = new SymbolTable
    val t = parseType(symbolTable, tokenBuffer)
    println(t)

    val e = ExpressionParser(new Scope(None, symbolTable), tokenBuffer)

    e match
      case e: Expr.Expr =>
        println(e)
        println(Expr.OptimiseExpression(e))
      case e: SyntaxError =>
        println(e)
  }
}

package Grain
import Grain.Scanner.scanText
import reflect.ClassTag
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
      currentType

}
def parseFunctionPtr(symbolTable: SymbolTable, tokenBuffer: TokenBuffer): Type = {
  import scala.collection.mutable.ListBuffer
  val args = ListBuffer.empty[Type]
  while(tokenBuffer.peekType != TokenType.RightParen){
    if(args.nonEmpty) tokenBuffer.matchType(TokenType.Comma)
    args.append(parseType(symbolTable, tokenBuffer))
  }
  tokenBuffer.matchType(TokenType.RightParen)

  val returnType = if(tokenBuffer.peekType != TokenType.Colon) Utility.Empty() else {
    tokenBuffer.advance()
    parseType(symbolTable, tokenBuffer)
  }

  Utility.FunctionPtr(args.toList, returnType)
}
def parseType(symbolTable: SymbolTable, tokenBuffer: TokenBuffer): Type = {
  val startToken = tokenBuffer.advance()
  val startType = if(symbolTable.types.contains(startToken.lexeme)) symbolTable.types(startToken.lexeme)
                  else if(startToken.tokenType == TokenType.LeftParen) parseFunctionPtr(symbolTable, tokenBuffer)
                  else throw Errors.expectedType(startToken)
  parseTypeAddition(startType, tokenBuffer)
}

def returnTypeOrError[T](action: =>T): T | SyntaxError =
  try
    action
  catch
    case se: SyntaxError => se
    case e@_ => throw e//SyntaxError(-1, "Unknown error type; '" ++ e.toString ++ "'")

def getTypeOrThrow[T: ClassTag](value: T | SyntaxError): T =
  value match
    case value: T => value
    case value: SyntaxError => throw value

object TopLevelParser{

  case class Result(statements: List[Stmt.TopLevel], errors: List[Utility.SyntaxError])

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Result = {
    import collection.mutable.ListBuffer
    val statements = ListBuffer.empty[Stmt.TopLevel]
    val errors = ListBuffer.empty[Utility.SyntaxError]

    while (tokenBuffer.peekType != TokenType.EndOfFile) {
      val ps = parseTopLevel(scope, tokenBuffer)
      ps match
        case ps: SyntaxError => errors.append(ps)
        case ps: Stmt.TopLevel => statements.append(ps)
    }

    Result(statements.toList.filter(!_.isInstanceOf[Stmt.EmptyStatement]), errors.toList)
  }

  def parseTopLevel(scope: Scope, tokenBuffer: TokenBuffer): Stmt.TopLevel | Utility.SyntaxError = {
    returnTypeOrError{
      tokenBuffer.peekType match
        case TokenType.Func => parseFunction(scope, tokenBuffer)
        case _ => StatementParser.parseVariableDecl(scope, tokenBuffer)
    }
  }

  private def parseFunction(scope: Scope, tokenBuffer: TokenBuffer): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.Func)
    val tokenName = tokenBuffer.matchType(TokenType.Identifier)
    tokenBuffer.matchType(TokenType.LeftParen)

    var argList = List.empty[Symbol]
    val funcScope = scope.newChild()
    var canHaveAnotherArgument = true

    while (tokenBuffer.peekType == TokenType.Identifier) {
      if (!canHaveAnotherArgument) {
        throw Errors.expectedTokenError(tokenBuffer.peek, TokenType.Comma)
      }
      val argName = tokenBuffer.matchType(TokenType.Identifier)
      tokenBuffer.matchType(TokenType.Colon)
      val tokenType = parseType(scope.symbolTable, tokenBuffer)

      val newSymbol = Symbol.make(argName, tokenType, DefinitionType.Argument)
      argList = newSymbol :: argList

      if (tokenBuffer.peekType == TokenType.Comma) {
        tokenBuffer.advance()
        canHaveAnotherArgument = true
      }
      else {
        canHaveAnotherArgument = false
      }
    }
    tokenBuffer.matchType(TokenType.RightParen)

    val signature = tokenBuffer.peekType match
      case TokenType.Colon =>
        tokenBuffer.advance()
        parseType(scope.symbolTable, tokenBuffer)
      case _ => Utility.Empty()

    val body = tokenBuffer.peekType match
      case TokenType.LeftBrace => Some(StatementParser.parseBlock(funcScope, tokenBuffer))
      case _ => None

    val definitionState = if (body.isDefined) DefinitionType.FuncDefined else DefinitionType.FuncNotDefined

    val funcSymbol = Symbol.make(tokenName, signature, definitionState)

    val funcType = Utility.FunctionPtr(argList.map(_.dataType), signature)
    scope.addSymbol(tokenName, funcType, definitionState)

    definitionState match
      case DefinitionType.FuncDefined =>
        val stmt = Stmt.FunctionDecl(funcSymbol, argList.reverse, body)
        scope.linkStatementWithScope(stmt, funcScope)
        stmt
      case DefinitionType.FuncNotDefined =>
        Stmt.EmptyStatement()
      case _ => throw new Exception("This definition type should not be possible")
  }
}

object ParseMain{
  def main(args: Array[String]): Unit = {
    val tokenBuffer = TokenBuffer(scanText("parserTest.txt"))
    val symbolTable = new SymbolTable

    val result = TopLevelParser(symbolTable.globalScope, tokenBuffer)

    result.errors.map(println)
    result.statements.map(println)

    println(symbolTable.globalScope)
  }
}

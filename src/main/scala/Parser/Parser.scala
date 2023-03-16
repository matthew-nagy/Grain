package Parser

import Grain.*
import Tool.ImageLoader
import Utility.{Errors, SyntaxError, Token, TokenType, Type}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag


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
    val next = advance()
    if (next.tokenType == requestedType) {
      next
    }
    else {
      throw Errors.expectedTokenError(peek, requestedType)
    }
  }

  override def toString: String =
    (for token<- tokens yield ("\n\t" ++ token.toString))
      .foldLeft("TokenBuffer->")(_ ++ _)

}


@tailrec
def parseTypeAddition(currentType: Type, tokenBuffer: TokenBuffer): Type = {
  tokenBuffer.peekType match
    case TokenType.Ptr =>
      tokenBuffer.advance()
      parseTypeAddition(Utility.Ptr(currentType), tokenBuffer)
    case TokenType.LeftSquare =>
      tokenBuffer.advance()
      val size = tokenBuffer.matchType(TokenType.IntLiteral)
      tokenBuffer.matchType(TokenType.RightSquare)
      parseTypeAddition(Utility.Array(currentType, size.lexeme.toInt), tokenBuffer)
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
    case se: SyntaxError =>
      println(se.toString)
      se
    case e@_ => throw e//SyntaxError(-1, "Unknown error type; '" ++ e.toString ++ "'")

object TopLevelParser{

  case class Result(statements: List[Stmt.TopLevel], errors: List[Utility.SyntaxError])

  def apply(scope: GlobalScope, tokenBuffer: TokenBuffer): Result = {
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

  def parseTopLevel(scope: GlobalScope, tokenBuffer: TokenBuffer): Stmt.TopLevel | Utility.SyntaxError = {
    returnTypeOrError{
      val top = tokenBuffer.peekType match
        case TokenType.Func => parseFunction(scope, tokenBuffer)
        case TokenType.Load => parseLoad(scope, tokenBuffer)
        case _ =>
          if tokenBuffer.lookAhead(1).tokenType == TokenType.Colon then
            StatementParser.parseVariableDecl(scope, tokenBuffer, true)
          else {
            println("Not allowed " ++ tokenBuffer.advance().toString ++ " at top level")
            Stmt.EmptyStatement()
          }
      top
    }
  }

  private def parseFunctionArguments(scope: FunctionScope, tokenBuffer: TokenBuffer): List[Symbol] = {
    tokenBuffer.matchType(TokenType.LeftParen)
    val arguments = ListBuffer.empty[Symbol]
    while(tokenBuffer.peekType != TokenType.RightParen) {
      if arguments.nonEmpty then
        tokenBuffer.matchType(TokenType.Comma)
      val argName = tokenBuffer.matchType(TokenType.Identifier)
      tokenBuffer.matchType(TokenType.Colon)
      val typeToken = tokenBuffer.peek
      val argType = parseType(scope.symbolTable, tokenBuffer)
      if(argType.isInstanceOf[Utility.Array]){
        throw Errors.CannotHaveArrayArgument(typeToken)
      }
      arguments.append(scope.addSymbol(argName, argType, Symbol.Argument()))
    }
    tokenBuffer.matchType(TokenType.RightParen)
    arguments.toList
  }
  private def parseFunction(scope: GlobalScope, tokenBuffer: TokenBuffer): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.Func)
    val functionScope = scope.newFunctionChild()

    val funcName = tokenBuffer.matchType(TokenType.Identifier)
    val arguments = parseFunctionArguments(functionScope, tokenBuffer)
    val returnType = tokenBuffer.peekType match
      case TokenType.Colon =>
        tokenBuffer.advance()
        parseType(scope.symbolTable, tokenBuffer)
      case _ => Utility.Empty()

    functionScope.setReturnType(returnType)

    val funcSymbol = scope.addSymbol(
      funcName,
      Utility.FunctionPtr(arguments.map(_.dataType), returnType),
      Symbol.FunctionDefinition())

    if(tokenBuffer.peekType != TokenType.LeftBrace){
      Stmt.EmptyStatement()
    }
    else{
      val funcStmt = Stmt.FunctionDecl(funcSymbol, arguments, StatementParser.parseBlock(functionScope, tokenBuffer))
      scope.linkStatementWithScope(funcStmt, functionScope)
      funcStmt
    }
  }

  private def parseReferencing(tokenBuffer: TokenBuffer): List[Stmt.PaletteReference] = {
    tokenBuffer.peekType match
      case TokenType.Referencing =>
        tokenBuffer.advance()
        val referenceImageFilename = tokenBuffer.matchType(TokenType.StringLiteral)
        tokenBuffer.matchType(TokenType.For)
        tokenBuffer.matchType(TokenType.Tile)
        val referenceIndex = tokenBuffer.matchType(TokenType.IntLiteral)

        Stmt.PaletteReference(referenceImageFilename.lexeme, referenceIndex.lexeme.toInt) :: parseReferencing(tokenBuffer)
      case _ => Nil
  }
  private def parseLoad(scope: GlobalScope, tokenBuffer: TokenBuffer): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.Load)
    val spriteToken = tokenBuffer.matchType(TokenType.Identifier)
    tokenBuffer.matchType(TokenType.Comma)
    val paletteToken = tokenBuffer.matchType(TokenType.Identifier)
    tokenBuffer.matchType(TokenType.From)
    val mainFilename = tokenBuffer.matchType(TokenType.StringLiteral)

    val references = parseReferencing(tokenBuffer)

    val loadedImage = ImageLoader(mainFilename.lexeme)

    scope.addSymbol(spriteToken, Utility.SpriteSheet(loadedImage.bpp), Symbol.Data(loadedImage.dataStrings, loadedImage.dataSize))
    scope.addSymbol(paletteToken, Utility.Palette(), Symbol.Data(loadedImage.paletteStrings, loadedImage.palleteSize))

    Stmt.Load(spriteToken, paletteToken, mainFilename, references)
  }
}

object ParseMain{
  def main(args: Array[String]): Unit = {
    val tokenBuffer = TokenBuffer(Scanner.scanText("parserTest.txt"))
    val symbolTable = new SymbolTable

    val result = TopLevelParser(symbolTable.globalScope, tokenBuffer)

    result.errors.map(println)
    result.statements.map(println)

    println(symbolTable.globalScope)
  }
}

package Parser

import Grain.*
import Tool.{BinaryLoader, ImageLoader}
import Utility.{Errors, SyntaxError, Token, TokenType, Type}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag


class TokenBuffer(private val tokens: List[Token], filename: String, fileIndex: Int){
  private var index = 0

  def getFileIndex: Int = fileIndex

  def getFilename: String = filename

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
      throw Errors.expectedTokenError(filename, peek, requestedType)
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

  val mmio = tokenBuffer.peekType match
    case TokenType.MMIO =>
      tokenBuffer.advance()
      true
    case _ => false

  Utility.FunctionPtr(args.toList, returnType, mmio)
}
def parseType(symbolTable: SymbolTable, tokenBuffer: TokenBuffer): Type = {
  val startToken = tokenBuffer.advance()
  val startType = if(symbolTable.types.contains(startToken.lexeme)) symbolTable.types(startToken.lexeme)
                  else if(startToken.tokenType == TokenType.LeftParen) parseFunctionPtr(symbolTable, tokenBuffer)
                  else throw Errors.expectedType(tokenBuffer.getFilename, startToken)
  parseTypeAddition(startType, tokenBuffer)
}

def returnTypeOrError[T](action: =>T): T | SyntaxError =
  try
    action
  catch
    case se: SyntaxError =>
      se
    case e@_ => throw e//SyntaxError(-1, "Unknown error type; '" ++ e.toString ++ "'")

object TopLevelParser{

  var fileIndex = 0

  case class Result(statements: List[Stmt.TopLevel], errors: List[Utility.SyntaxError])

  def apply(scope: GlobalScope, tokenBuffer: TokenBuffer): Result = {
    import collection.mutable.ListBuffer
    val statements = ListBuffer.empty[Stmt.TopLevel]
    val errors = ListBuffer.empty[Utility.SyntaxError]

    while (tokenBuffer.peekType != TokenType.EndOfFile) {
      val ps = parseTopLevel(scope, tokenBuffer)
      ps match
        case ps: SyntaxError => errors.append(ps)
        case ps: List[Stmt.TopLevel] => statements.appendAll(ps)
    }

    Result(statements.toList.filter(!_.isInstanceOf[Stmt.EmptyStatement]), errors.toList)
  }

  def parseTopLevel(scope: GlobalScope, tokenBuffer: TokenBuffer): List[Stmt.TopLevel] | Utility.SyntaxError = {
    returnTypeOrError{
      val top: List[Stmt.TopLevel] | Utility.SyntaxError = tokenBuffer.peekType match
        case TokenType.Func => parseFunction(scope, tokenBuffer) :: Nil
        case TokenType.Load => parseLoad(scope, tokenBuffer) :: Nil
        case TokenType.Include => parseInclude(scope, tokenBuffer)
        case TokenType.Class => parseClass(scope, tokenBuffer)
        case _ =>
          if tokenBuffer.lookAhead(1).tokenType == TokenType.Colon then
            StatementParser.parseVariableDecl(scope, tokenBuffer, true) :: Nil
          else {
            tokenBuffer.advance()
            Stmt.EmptyStatement() :: Nil
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
        throw Errors.CannotHaveArrayArgument(tokenBuffer.getFilename, typeToken)
      }
      arguments.append(scope.addSymbol(argName, argType, Symbol.Argument(), tokenBuffer.getFilename))
    }
    tokenBuffer.matchType(TokenType.RightParen)
    arguments.toList
  }

  private def parseFunctionOrEmpty(scope: GlobalScope, functionScope: FunctionScope, tokenBuffer: TokenBuffer,
                                   argumentParser: (FunctionScope, TokenBuffer)=> List[Symbol] = parseFunctionArguments,
                                   sendName: Token=>Unit = (s)=>{}): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.Func)

    val funcName = tokenBuffer.matchType(TokenType.Identifier)
    sendName(funcName)

    val arguments = argumentParser(functionScope, tokenBuffer)
    val returnType = tokenBuffer.peekType match
      case TokenType.Colon =>
        tokenBuffer.advance()
        parseType(scope.symbolTable, tokenBuffer)
      case _ => Utility.Empty()

    if(returnType.isInstanceOf[Utility.Array]){
      throw Errors.CannotHaveArrayReturnType(tokenBuffer.getFilename, funcName)
    }

    functionScope.setReturnType(returnType)

    val preAsmMMIO = tokenBuffer.peekType match
      case TokenType.MMIO =>
        tokenBuffer.advance()
        true
      case _ => false

    val funcSymbol = scope.addSymbol(
      funcName,
      Utility.FunctionPtr(arguments.map(_.dataType), returnType, preAsmMMIO),
      Symbol.FunctionDefinition(tokenBuffer.peekType == TokenType.Assembly),
      tokenBuffer.getFilename)

    functionScope.executesAsMMIO = preAsmMMIO

    tokenBuffer.peekType match
      case TokenType.Assembly =>
        val funcStmt = Stmt.FunctionDecl(funcSymbol, arguments, StatementParser.parseAssembly(functionScope, tokenBuffer), preAsmMMIO)
        funcStmt
      case TokenType.LeftBrace =>
        val funcStmt = Stmt.FunctionDecl(funcSymbol, arguments, StatementParser.parseBlock(functionScope, tokenBuffer, preAsmMMIO), preAsmMMIO)
        funcStmt
      case _ => Stmt.EmptyStatement()
  }
  private def parseFunction(scope: GlobalScope, tokenBuffer: TokenBuffer): Stmt.TopLevel = {
    val functionScope = scope.newFunctionChild(false)//Is set inside the function parser
    parseFunctionOrEmpty(scope, functionScope, tokenBuffer) match
      case funcStmt: Stmt.FunctionDecl =>
        scope.linkStatementWithScope(funcStmt, functionScope)
        funcStmt
      case _@x => x
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

  private def parseGraphisLoad(scope: GlobalScope, tokenBuffer: TokenBuffer, spriteToken: Token): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.Comma)
    val paletteToken = tokenBuffer.matchType(TokenType.Identifier)
    tokenBuffer.matchType(TokenType.From)
    val mainFilename = tokenBuffer.matchType(TokenType.StringLiteral)

    val references = parseReferencing(tokenBuffer)

    val loadedImage = ImageLoader(mainFilename.lexeme)

    scope.addSymbol(spriteToken, Utility.Array(Utility.Sprite(loadedImage.bpp), loadedImage.numberOfTiles), Symbol.Data(loadedImage.dataStrings, loadedImage.dataSize), tokenBuffer.getFilename)
    scope.addSymbol(paletteToken, Utility.Array(Utility.Palette(), loadedImage.numberOfPalettes), Symbol.Data(loadedImage.paletteStrings, loadedImage.palleteSize), tokenBuffer.getFilename)

    Stmt.LoadGraphics(spriteToken, paletteToken, mainFilename, references)
  }

  private def parseDataLoad(scope: GlobalScope, tokenBuffer: TokenBuffer, varToken: Token): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.From)
    val filename = tokenBuffer.matchType(TokenType.StringLiteral)

    val readBinary = BinaryLoader(filename.lexeme)

    scope.addSymbol(varToken, Utility.Array(Utility.ROMWord(), readBinary.wordLength), Symbol.Data(readBinary.binaryStrings, readBinary.wordLength), tokenBuffer.getFilename)
    Stmt.LoadData(varToken, filename)
  }
  private def parseLoad(scope: GlobalScope, tokenBuffer: TokenBuffer): Stmt.TopLevel = {
    tokenBuffer.matchType(TokenType.Load)
    val varToken = tokenBuffer.matchType(TokenType.Identifier)
    tokenBuffer.peekType match
      case TokenType.Comma => parseGraphisLoad(scope, tokenBuffer, varToken)
      case TokenType.From => parseDataLoad(scope, tokenBuffer, varToken)
      case _ => throw Errors.InvalidLoadType(tokenBuffer.getFilename, tokenBuffer.peek)
  }

  def parseInclude(scope: GlobalScope, tokenBuffer: TokenBuffer): List[Stmt.TopLevel] = {

    tokenBuffer.matchType(TokenType.Include)
    val newFilenameToken = tokenBuffer.matchType(TokenType.StringLiteral)

    fileIndex += 1
    val newTokenBuffer = TokenBuffer(Scanner.scanText(newFilenameToken.lexeme), newFilenameToken.lexeme, fileIndex)
    val result = apply(scope, newTokenBuffer)
    if(result.errors.nonEmpty){
      throw Errors.VariousErrors(newFilenameToken.lexeme, result.errors)
    }
    result.statements
  }

  private def translateFunctionDeclToMember(oldDecl: Stmt.FunctionDecl, className: Token): Stmt.FunctionDecl = {
    val newSymbol = Symbol.make(
      Token(
        oldDecl.funcSymbol.token.tokenType,
        "method_" ++ className.lexeme ++ "." ++ oldDecl.funcSymbol.token.lexeme,
        oldDecl.funcSymbol.token.lineNumber
      ), oldDecl.funcSymbol.dataType, oldDecl.funcSymbol.form)
    Stmt.FunctionDecl(newSymbol, oldDecl.arguments, oldDecl.body, oldDecl.mmio)
  }
  def parseClass(scope: GlobalScope, tokenBuffer: TokenBuffer): List[Stmt.TopLevel] = {
    tokenBuffer.matchType(TokenType.Class)
    val className = tokenBuffer.matchType(TokenType.Identifier)
    val typeSignature = Utility.Struct(className.lexeme, Utility.Struct.generateEntries(Nil), ListBuffer.empty[Symbol])
    scope.symbolTable.types.addOne(className.lexeme, typeSignature)
    tokenBuffer.matchType(TokenType.LeftBrace)

    def classArgParser(functionScope: FunctionScope, tokenBuffer: TokenBuffer): List[Symbol] = {
      val regularArguments = parseFunctionArguments(functionScope, tokenBuffer)
      val thisSymbol = Symbol("this", Token(TokenType.Identifier, "this", className.lineNumber), Utility.Ptr(typeSignature), className.lineNumber, Symbol.Argument())
      functionScope.addSymbol(thisSymbol.token, thisSymbol, tokenBuffer.getFilename)
      regularArguments ::: (thisSymbol :: Nil)
    }

    val parsedFunctions = ListBuffer.empty[Stmt.TopLevel]
    val parsedVariables = ListBuffer.empty[Symbol]

    while(tokenBuffer.peekType != TokenType.RightBrace){
      if(tokenBuffer.peekType == TokenType.Func){
        val functionScope = scope.newFunctionChild(false)
        var funcName = Token(TokenType.ErrorToken, "", -1)
        val newFunction = parseFunctionOrEmpty(scope, functionScope, tokenBuffer, classArgParser, funcName = _)
        newFunction match
          case func: Stmt.FunctionDecl =>
            val memberDecl = translateFunctionDeclToMember(func, className)
            scope.linkStatementWithScope(memberDecl, functionScope)
            typeSignature.definedFunctions.addOne(func.funcSymbol)
            parsedFunctions.addOne(memberDecl)
          case _ => throw Errors.ClassFunctionsMustHaveDefinitions(tokenBuffer.getFilename, funcName, className.lexeme)
      }
      else{
        val variableSymbol = StatementParser.parseVariableSymbol(scope, tokenBuffer, false)
        typeSignature.entries.addAll(Utility.Struct.generateEntries(variableSymbol :: Nil))
        parsedVariables.addOne(variableSymbol)
      }
    }
    tokenBuffer.matchType(TokenType.RightBrace)
    typeSignature.entries.clear()
    typeSignature.entries.addAll(Utility.Struct.generateEntries(parsedVariables.toList))

    parsedFunctions.toList
  }
}

object ParseMain{
  def main(args: Array[String]): Unit = {
    val tokenBuffer = TokenBuffer(Scanner.scanText("src/main/fragment.txt"), "src/main/fragment.txt", 0)
    val symbolTable = new SymbolTable

    val result = TopLevelParser(symbolTable.globalScope, tokenBuffer)

    result.errors.map(println)
    result.statements.map(println)

    println(symbolTable.globalScope)
  }
}

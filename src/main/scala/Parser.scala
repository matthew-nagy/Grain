package Grain

import scala.annotation.targetName

object Parser {

  private var currentDirectory: String = "./"
  def directory: String = currentDirectory
  def setDirectory(newDir: String): Unit = currentDirectory = newDir

  case class ParsedTree(
                         topLevelStatements: List[Stmt.TopLevel],
                         errorMessages: List[SyntaxError]
                       ){
    @targetName("parseTreeConcatenation")
    def ++(that: ParsedTree): ParsedTree = ParsedTree(
      this.topLevelStatements ++ that.topLevelStatements,
      this.errorMessages ++ that.errorMessages
    )
  }
  object ParsedTree{
    def Simple(statement: Stmt.TopLevel): ParsedTree = ParsedTree(statement :: Nil, Nil)
    def Simple(error: SyntaxError): ParsedTree = ParsedTree(Nil, error :: Nil)
  }

  private class ParseObject(tokens: List[Token]){
    private var tokenIndex = 0
    private var symbolTable: SymbolTable = SymbolTable()
    

    private def isAtEnd: Boolean =
      peek.tokenType == TokenType.EndOfFile;

    def peek: Token =
      tokens(tokenIndex);

    private def previous: Token =
      tokens(tokenIndex - 1);

    private def advance(): Token ={
      if(!isAtEnd) {
        tokenIndex += 1
      }
      previous
    }

    private def check(desiredType: TokenType): Boolean =
      if(isAtEnd){
        false
      }
      else{
        peek.tokenType == desiredType
      }

    private def matchToken(desiredType: TokenType): Boolean =
      if(check(desiredType)){
        advance()
        true
      }
      else{
        false
      }

    private def matchToken(desiredTypes: List[TokenType]): Boolean = {
      for tokenType <- desiredTypes do {
        if (check(tokenType)) {
          advance()
          return true
        }
      }
      false
    }

    private def withConsumed(desiredType: TokenType)(followUp: => ParsedTree): ParsedTree = {
      val token = advance()
      withConsumed(token, desiredType)(followUp)
    }

    private def withConsumed(foundToken: Token, desiredType: TokenType)(followUp: => ParsedTree): ParsedTree = {
      foundToken.tokenType match
        case `desiredType` => followUp
        case _ =>
          synchroniseToLine(foundToken.lineNumber)
          ParsedTree.Simple(Errors.expectedTokenError(foundToken, desiredType))
    }

    private def synchroniseToLine(currentLineNumber: Int): Unit =
      while(peek.lineNumber == currentLineNumber && peek.tokenType != TokenType.EndOfFile)
        advance()

    def parseInclude(): ParsedTree = {
      withConsumed(TokenType.Include){
        val filenameToken = advance()
        withConsumed(filenameToken, TokenType.StringLiteral){
          ParsedTree.Simple(Stmt.Include(filenameToken))
        }
      }
    }

    def parseLoad(): ParsedTree = {
      import java.io.File
      withConsumed(TokenType.Load){
        val variableName = advance()
        withConsumed(variableName, TokenType.Identifier){
          if(symbolTable.globalScope.contains(variableName.lexeme)){
            return ParsedTree.Simple(Errors.identifierRedeclarationError(variableName, symbolTable.globalScope(variableName.lexeme)))
          }
          withConsumed(TokenType.From){
            val filename = advance()
            withConsumed(filename, TokenType.StringLiteral){
              symbolTable.globalScope.addOne((
                variableName.lexeme,
                Symbol.make(variableName, Types.SpriteSheet(), true)
                ))
              ParsedTree.Simple(Stmt.Load(variableName, filename))
            }
          }
        }
      }
    }

    def parseVarDeclaration(currentScope: Scope): ParsedTree = {
      ParsedTree.Simple(SyntaxError(0, ""))
    }
    def parseTopLevel(): ParsedTree = {
      peek.tokenType match
        case TokenType.Include => parseInclude()
        case TokenType.Load => parseLoad()
        //case TokenType.Func => parseFunctionDeclaration()
        //case TokenType.Identifier => parseVarDeclaration()
        //case _ => Stmt.Include(tokens(tokenIndex))
        case _ =>
          val badToken = peek
          synchroniseToLine(peek.lineNumber)
          ParsedTree.Simple(Errors.expectedTopLevelDeclaration(badToken))
    }

  }

  def getAST(tokens: List[Token]): ParsedTree =
    var tree = ParsedTree(Nil, Nil)
    val parser = new ParseObject(tokens)
    while(parser.peek.tokenType != TokenType.EndOfFile)
      tree = tree ++ parser.parseTopLevel()
    tree


  def main(args: Array[String]): Unit = {
    val tokens = Scanner.scanText("./parserTest.txt")
    val ast = getAST(tokens)
    ast.topLevelStatements.map(println)
    ast.errorMessages.map(println)
  }
}

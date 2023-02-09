package Grain
case class SyntaxError(lineNumber: Int, message: String)
object Errors{
  def expectedTokenError(found: Token, expected: TokenType):SyntaxError =
    SyntaxError(
      found.lineNumber,
      "Unexpected token on line " ++ found.lineNumber.toString ++ ", expected '" ++ expected.toString ++ "' but found '" ++ found.tokenType.toString ++"'"
    )
  def identifierRedeclarationError(badToken: Token, oldSymbol: Symbol):SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Variable with name '" ++ badToken.lexeme ++ "' on line " ++ badToken.lineNumber.toString ++ " is already declared. Declaration was previously on line " ++ oldSymbol.lineNumber.toString
    )
  def expectedTopLevelDeclaration(badToken: Token): SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Expected top level declaration, found '" ++ badToken.lexeme ++ "'"
    )
}

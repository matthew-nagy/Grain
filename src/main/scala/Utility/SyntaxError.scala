package Utility

import Grain.Symbol

case class SyntaxError(lineNumber: Int, message: String) extends Exception(message){
  override def toString: String = "Syntax Error on line " ++ lineNumber.toString ++ ": " ++ message ++ " " ++ tag

  private var tag = ""
  def addTag(newTag: String):SyntaxError = {
    tag = newTag
    this
  }
}
object Errors{
  def expectedTokenError(found: Token, expected: TokenType):SyntaxError =
    SyntaxError(
      found.lineNumber,
      "Unexpected token on line " ++ found.lineNumber.toString ++ ", expected '" ++ expected.toString ++ "' but found '" ++ found.tokenType.toString ++"'"
    )
  def expectedType(badToken: Token): SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Expected a type, found '" ++ badToken.lexeme ++ "'"
    )
  def CannotCallType(lineNumber: Int, incorrectType: Type): SyntaxError =
    SyntaxError(
      lineNumber,
      "Cannot call type '" ++ incorrectType.toString ++ "'"
    )

  def CannotIndexType(lineNumber: Int, incorrectType: Type): SyntaxError =
    SyntaxError(
      lineNumber,
      "Cannot index type '" ++ incorrectType.toString ++ "'"
    )
  def ExpectedExpression(badToken: Token): SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Expected an expression, found '" ++ badToken.lexeme ++ "'"
    )

  def expectedUnary(badToken: Token): SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Expected a Unary Operator, found '" ++ badToken.lexeme ++ "'"
    )

  def invalidLValue(badToken: Token): SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Value starting with '" ++ badToken.lexeme ++ "' is not an assignable value"
    )
  def SymbolNotFound(badToken: Token):SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Symbol '" ++ badToken.lexeme ++ "' not found"
    )
  def CannotHaveArrayArgument(badToken: Token): SyntaxError =
    SyntaxError(
      badToken.lineNumber,
      "Cannot have a pure array type as an argument. Please use pointers"
    )

  def SymbolRedefinition(oldToken: Token, newToken: Token): SyntaxError =
    SyntaxError(
      newToken.lineNumber,
      "Redefinition of symbol '" ++ newToken.lexeme ++ "', last defined on line " ++ oldToken.lineNumber.toString
    )

  def DefinitionDoesntMatchType(oldType: Type, newType: Type, token: Token): SyntaxError =
    SyntaxError(
      token.lineNumber,
      "Redefinition of type of symbol '" ++ token.lexeme ++ "', was " ++ oldType.toString ++ " but is now " ++ newType.toString
    )

  def UnclosedCurlyBrackets(brackets: Token): SyntaxError =
    SyntaxError(
      brackets.lineNumber,
      "Brace is not closed before EOF"
    )

}

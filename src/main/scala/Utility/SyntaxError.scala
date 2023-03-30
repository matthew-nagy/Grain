package Utility

import Grain.Symbol

case class SyntaxError(filename: String, lineNumber: Int, message: String) extends Exception(message){
  override def toString: String = "In file " ++ filename ++ ". Syntax Error on line " ++ lineNumber.toString ++ ": " ++ message ++ " " ++ tag

  private var tag = ""
  def addTag(newTag: String):SyntaxError = {
    tag = newTag
    this
  }
}
object Errors{
  def expectedTokenError(filename: String, found: Token, expected: TokenType):SyntaxError =
    SyntaxError(
      filename,
      found.lineNumber,
      "Unexpected token on line " ++ found.lineNumber.toString ++ ", expected '" ++ expected.toString ++ "' but found '" ++ found.tokenType.toString ++"'"
    )
  def expectedType(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Expected a type, found '" ++ badToken.lexeme ++ "'"
    )
  def CannotCallType(filename: String, lineNumber: Int, incorrectType: Type): SyntaxError =
    SyntaxError(
      filename,
      lineNumber,
      "Cannot call type '" ++ incorrectType.toString ++ "'"
    )

  def CannotIndexType(filename: String, lineNumber: Int, incorrectType: Type): SyntaxError =
    SyntaxError(
      filename,
      lineNumber,
      "Cannot index type '" ++ incorrectType.toString ++ "'"
    )
  def ExpectedExpression(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Expected an expression, found '" ++ badToken.lexeme ++ "'"
    )

  def expectedUnary(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Expected a Unary Operator, found '" ++ badToken.lexeme ++ "'"
    )

  def invalidLValue(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Value starting with '" ++ badToken.lexeme ++ "' is not an assignable value"
    )
  def badlyTyped(filename: String, message: String): SyntaxError =
    SyntaxError(
      filename,
      -1,
      "Badly typed: " ++ message
    )
  def structDoesntHaveElement(filename: String, name: String, t: String): SyntaxError =
    SyntaxError(
      filename,
      -1,
      t.toString ++ " does not have member " ++ name
    )
  def cannotIndexNonPoinerElements(filename: String, expr: String, withType: Utility.Type): SyntaxError =
    SyntaxError(
      filename,
      -1,
      "Cannot index non pointer expression " ++ expr ++ " with type " ++ withType.toString
    )
  def SymbolNotFound(filename: String, badToken: Token):SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Symbol '" ++ badToken.lexeme ++ "' not found"
    )
  def CannotHaveArrayArgument(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Cannot have a pure array type as an argument. Please use pointers"
    )

  def SymbolRedefinition(filename: String, oldToken: Token, newToken: Token): SyntaxError =
    SyntaxError(
      filename,
      newToken.lineNumber,
      "Redefinition of symbol '" ++ newToken.lexeme ++ "', last defined on line " ++ oldToken.lineNumber.toString
    )

  def DefinitionDoesntMatchType(filename: String, oldType: Type, newType: Type, token: Token): SyntaxError =
    SyntaxError(
      filename,
      token.lineNumber,
      "Redefinition of type of symbol '" ++ token.lexeme ++ "', was " ++ oldType.toString ++ " but is now " ++ newType.toString
    )

  def UnclosedCurlyBrackets(filename: String, brackets: Token): SyntaxError =
    SyntaxError(
      filename,
      brackets.lineNumber,
      "Brace is not closed before EOF"
    )

  def VariousErrors(filename: String, errors: List[SyntaxError]): SyntaxError =
    SyntaxError(
      filename, 0,
      errors.foldLeft("Errors:")(_ ++ "\n\t- " ++ _.toString)
    )

  def ClassFunctionsMustHaveDefinitions(filename: String, badToken: Token, className: String): SyntaxError =
    SyntaxError(
      filename, badToken.lineNumber,
      "In class with name \"" ++ className ++ "\", function \"" ++ badToken.lexeme ++ "\" must have a definition"
    )
}

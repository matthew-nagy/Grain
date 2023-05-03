package Utility

import Grain.Symbol

case class SyntaxError(filename: String, lineNumber: Int, message: String) extends Exception(message){

  private var displayLine = true
  private var displayFile = true
  private var displayErrorType = true
  private var errorType = "Syntax"

  def hideLine: SyntaxError = {
    displayLine = false
    this
  }
  def hideFile: SyntaxError = {
    displayFile = false
    this
  }
  def hideErrorType: SyntaxError = {
    displayErrorType = false
    this
  }
  def semantic: SyntaxError = {
    errorType = "Semantic"
    this
  }
  def memory: SyntaxError ={
    errorType = "Memory"
    this
  }
  def typeError: SyntaxError = {
    errorType = "Type"
    this
  }

  override def toString: String = (if displayFile then "In file " ++ filename ++ ". " else "") ++ (if displayErrorType then errorType ++ " Error" else "") ++ (if displayLine then " on line " ++ lineNumber.toString ++ ":" else "") ++ " " ++ message ++ " " ++ tag

  private var tag = ""
  def addTag(newTag: String):SyntaxError = {
    tag = newTag
    this
  }
}
object Errors{
  def ranIntoStack(symbol: Symbol): SyntaxError =
    SyntaxError(
      "[NA]", symbol.token.lineNumber,
      "Variable '" ++ symbol.name ++ "' breaks into the stack! (too many variables in high ram. Either extend high ram or try optimising memory usage)"
    ).memory
  def overflowedLowRam(symbol: Symbol, lowLimit: Int, currentPlace: Int, size: Int): SyntaxError =
    SyntaxError(
      "[NA]", symbol.token.lineNumber,
      "Variable '" ++ symbol.name ++ "' overflows lowram globals limit defined in grain_config.json.(Either store some in high ram or optimise memory usage!) Globals were at " ++ currentPlace.toString ++ ", size was " ++ size.toString ++ ", limit is " ++ lowLimit.toString
    ).memory
  def CannotHaveHiramLocalVariable(filename: String, token: Token): SyntaxError =
    SyntaxError(
      filename, token.lineNumber,
      "Cannot specify local variable '" ++ token.lexeme ++ "' as hiram; only global variables can go in HighRAM"
    ).semantic
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
    ).typeError

  def CannotIndexType(filename: String, lineNumber: Int, incorrectType: Type): SyntaxError =
    SyntaxError(
      filename,
      lineNumber,
      "Cannot index type '" ++ incorrectType.toString ++ "'"
    ).typeError
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
    ).semantic
  def badlyTyped(filename: String, message: String): SyntaxError =
    SyntaxError(
      filename,
      -1,
      "Badly typed: " ++ message
    ).typeError.hideLine
  def structDoesntHaveElement(filename: String, name: String, t: String): SyntaxError =
    SyntaxError(
      filename,
      -1,
      t.toString ++ " does not have member " ++ name
    ).typeError.hideLine
  def cannotIndexNonPoinerElements(filename: String, expr: String, withType: Utility.Type): SyntaxError =
    SyntaxError(
      filename,
      -1,
      "Cannot index non pointer expression " ++ expr ++ " with type " ++ withType.toString
    ).typeError.hideLine
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
    ).semantic

  def CannotHaveArrayReturnType(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename,
      badToken.lineNumber,
      "Cannot have a pure array type as a return type. Please use pointers"
    ).semantic

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
      errors.foldLeft("Errors:")(_ ++ "\n\t- " ++ _.hideFile.toString)
    ).hideLine.hideErrorType

  def ClassFunctionsMustHaveDefinitions(filename: String, badToken: Token, className: String): SyntaxError =
    SyntaxError(
      filename, badToken.lineNumber,
      "In class with name \"" ++ className ++ "\", function \"" ++ badToken.lexeme ++ "\" must have a definition"
    )

  def CannotGetLengthOfNonArrayType(filename: String, badExpr: Grain.Expr.Expr, badType: Utility.Type, lineNumber: Int): SyntaxError =
    SyntaxError(
      filename, lineNumber,
      "Cannot get length of non array type. " ++ badExpr.toString ++ " has type " ++ badType.toString
    ).typeError

  def CannotGetBitDepthOfNonVariable(filename: String, badExpr: Grain.Expr.Expr, lineNumber: Int): SyntaxError =
    SyntaxError(
      filename, lineNumber,
      "Cannot get bit depth of non variable expression. Expression given was " ++ badExpr.toString
    ).typeError

  def CannotGetBitDepthOfNonSprite(filename: String, badExpr: Grain.Expr.Expr, badType: Utility.Type, lineNumber: Int): SyntaxError =
    SyntaxError(
      filename, lineNumber,
      "Cannot get bit depth of non sprite-typed expression. Expression " ++ badExpr.toString ++ " has type " ++ badType.toString
    ).typeError

  def CannotGetBankOfNonVariable(filename: String, badExpr: Grain.Expr.Expr, lineNumber: Int): SyntaxError =
    SyntaxError(
      filename, lineNumber,
      "Cannot get data bank of non variable expression. Expression given was " ++ badExpr.toString
    ).typeError

  def InvalidLoadType(filename: String, badToken: Token): SyntaxError =
    SyntaxError(
      filename, badToken.lineNumber,
      "Expected a comma-seperated palette identifier or 'from', was given " ++ badToken.lexeme
    )

  def UnrecognisedFileExtension(extension: String, filename: String, acceptedExtensions: List[String]): SyntaxError =
    SyntaxError(
      filename, -1,
      "Unrecognised extension for data '" ++ extension ++ "'. Accepted extensions are" ++ acceptedExtensions.foldLeft("")(_ ++ ", '" ++ _) ++ "'"
    ).hideLine
    
  def CannotCallNonMMIOFromMMIO(lineNumber: Int, nonMMIOFuncName: String, filename: String): SyntaxError =
    SyntaxError(
      filename, lineNumber,
      "Cannot call a non-MMIO function from inside an MMIO scope. Attempted to call non-MMIO function " ++ nonMMIOFuncName
    )

  def MemberDoesntExit(lineNumber: Int, tryingType: Utility.Struct, memberName: String, filename: String): SyntaxError =
    SyntaxError(
      filename, lineNumber,
      "'" ++ tryingType.name ++ "' has no member '" ++ memberName ++ "'."
    )
  def CannotUseGetOnType(lineNumber: Int, filename: String): SyntaxError=
    SyntaxError(
      filename, lineNumber,
      "Cannot use '.' operator on non struct type"
    )
}

package Utility

enum TokenType:
  //Single character tokens first
  case LeftParen, RightParen, LeftBrace, RightBrace, LeftSquare, RightSquare,
       Comma, Dot, Asperand, Colon, Semicolon,
       Minus, Plus, Star, Struct, Slash, Percent, Or, And, Equal, Xor, Bang, Tilde,
       Greater, Less,
  //Double character tokens
       BangEqual, EqualEqual, LessEqual, GreaterEqual, ShiftLeft, ShiftRight,
  //Literals
       Identifier, IntLiteral, StringLiteral,
  //Built-in Types
       Ptr, BitdepthLiteral, RamGateLiteral,
  //Keywords
       Abs, Assembly, Break, Case, Decrement, Do, Else, False, From, Func, For, If,
       Include, Increment, Load, Match, Null, Referencing, Return, Tile, True,
       Then, While,


       EndOfFile, ErrorToken


case class Token(
                tokenType: TokenType,
                lexeme: String,
                lineNumber: Int
                ){
  override def toString: String =
    "Token(" ++ tokenType.toString ++ ", '" ++ lexeme ++ "', " ++ lineNumber.toString ++ ")"
}

object Token{
  case class TokenOption(nextChar: Char, token: TokenType)
  case class DoubleTokenEntry(otherwise: TokenType, options: List[TokenOption])

  def isNumericChar(c: Char) = c >= '0' && c <= '9'
  def isHexDigit(c: Char) = isNumericChar(c) || (c>='A' && c<='F')
  def isBinaryDigit(c: Char) = c == '0' || c == '1'
  def isValidAlphabetChar(c: Char) =
    (c == '_') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
  def isAlphanumericChar(c: Char) = isNumericChar(c) || isValidAlphabetChar(c)

  //For now, only regular integers can be used. no hex, no binary
  def isValidInt(input: String):Boolean =
    ! (for c <- input yield isNumericChar(c)).contains(false)

  def isValidIndentifier(input: String): Boolean = {
    if(isNumericChar(input.head) || !isValidAlphabetChar(input.head)){
      false
    }
    else{
      !(for c <- input.tail yield isAlphanumericChar(c)).contains(false)
    }
  }

  import TokenType.*
  val singleCharTokens: Map[Char, TokenType] = Map(
    '(' -> LeftParen, ')' -> RightParen, '{' -> LeftBrace, '}' -> RightBrace, '[' -> LeftSquare, ']' -> RightSquare,
    ',' -> Comma, '.' -> Dot, '@' -> Asperand, ':' -> Colon, ';' -> Semicolon,
    '-' -> Minus, '+' -> Plus, '*' -> Star, '/' -> Slash, '%' -> Percent, '|' -> Or, '&' -> And, '^' -> Xor, '~' -> Tilde
  )

  val doubleCharTokens: Map[Char, DoubleTokenEntry] = Map(
    '!' -> DoubleTokenEntry(Bang, List(TokenOption('=', BangEqual))),
    '<' -> DoubleTokenEntry(Less, List(TokenOption('=', LessEqual), TokenOption('<', ShiftLeft))),
    '>' -> DoubleTokenEntry(Greater, List(TokenOption('=', GreaterEqual), TokenOption('<', ShiftRight))),
    '=' -> DoubleTokenEntry(Equal, List(TokenOption('=', EqualEqual))),
  )

  val keywordMap: Map[String, TokenType] = Map(
    "abs" -> Abs,
    "asm" -> Assembly,
    "break" -> Break,
    "case" -> Case,
    "decr" -> Decrement,
    "do" -> Do,
    "else" -> Else,
    "false" -> False,
    "from" -> From,
    "func" -> Func,
    "for" -> For,
    "if" -> If,
    "include" -> Include,
    "incr" -> Increment,
    "load" -> Load,
    "match" -> Match,
    "null" -> Null,
    "ptr" -> Ptr,
    "referencing" -> Referencing,
    "return" -> Return,
    "struct" -> Struct,
    "tile" -> Tile,
    "then" -> Then,
    "true" -> True,
    "while" -> While,

    "and" -> And,
    "or" -> Or,
    "xor" -> Xor,
    "not" -> Bang
  )

}
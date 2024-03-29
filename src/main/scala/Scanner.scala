package Grain

import Utility.{Token, TokenType}

import scala.io.Source
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object Scanner{

  case class LabeledLine(line:String, lineNumber:Int)

  //A class that can take many lines and abstract the access to a queue-like structure
  private class CharBank(val lines: List[LabeledLine]){
    private var bankIndex = 0
    private var charIndex = 0

    private var startLine = 0
    private var startIndex = 0

    def setStart():Unit = {
      startLine = bankIndex
      startIndex = charIndex
    }

    def getCurrentLine: Int = {
      if(bankIndex == lines.length){
        lines(lines.length - 1).lineNumber + 1
      } else{
        lines(bankIndex).lineNumber
      }
    }

    def goToNextLine():Unit = {
      bankIndex += 1
      charIndex = 0
    }

    def getSubstring:String = {
      val builder = new StringBuilder("")
      val endLine = bankIndex
      val endIndex = charIndex

      bankIndex = startLine
      charIndex = startIndex

      while(!(bankIndex == endLine && charIndex == endIndex)){
        builder += advance()
      }

      bankIndex = endLine
      charIndex = endIndex
      builder.toString.filterNot(_ == '\\')
    }

    def atEndOfFile:Boolean =
      bankIndex == lines.length

    def peek:Char =
      if(atEndOfFile) 0
      else lines(bankIndex).line(charIndex)

    def advance():Char = {
      val c = peek
      charIndex += 1
      if(charIndex == lines(bankIndex).line.length){
        bankIndex += 1
        charIndex = 0
      }
      c
    }
  }

  //Turns a string of characters into a string of tokens
  private class ScannerObject(val charBank: CharBank) {

    private def scanNumericLiteral(startLine: Int, c: Char): TokenType = {
      c match
        case '0' =>
          charBank.peek match
            case _ if charBank.getCurrentLine != startLine => TokenType.IntLiteral
            case 'b' =>
              charBank.advance()
              while (Token.isBinaryDigit(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
              TokenType.IntLiteral
              TokenType.IntLiteral //parse binary
            case 'x' =>
              charBank.advance()
              while (Token.isHexDigit(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
              TokenType.IntLiteral
              TokenType.IntLiteral //parse hex
            case _ => //Regular number
              while (Token.isNumericChar(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
              TokenType.IntLiteral
        case p @ _ if Utility.bitdepthLiteralStrings.exists(_.head == p) =>
          if(charBank.peek != 'b' || charBank.getCurrentLine != startLine){
            while (Token.isNumericChar(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
            TokenType.IntLiteral
          }
          else {
            val b = charBank.advance()
            val p1 = charBank.advance()
            val p2 = charBank.advance()
            if (b == 'b' && p1 == p2 && p1 == 'p') then TokenType.BitdepthLiteral else TokenType.ErrorToken
          }
        case _ =>
          while (Token.isNumericChar(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
          TokenType.IntLiteral
    }
    private def scanStringLiteral(startLine: Int): TokenType = {
      while(charBank.peek != '"'){
        if(charBank.peek == '\\')charBank.advance()//Skip the next one too lmao
        charBank.advance()
      }
      charBank.advance()

      TokenType.StringLiteral
    }
    private def scanIdentifier(startLine: Int): TokenType = {
      while(Token.isAlphanumericChar(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
      val tokenText = charBank.getSubstring

      tokenText match
        case x if Token.keywordMap.contains(x) => Token.keywordMap(x)
        case _ => TokenType.Identifier
    }

    @tailrec
    private def scanToken():TokenType = {
      charBank.setStart()
      val startLine = charBank.getCurrentLine
      val c = charBank.advance()
      c match
        case '/' if charBank.peek == '/' =>
          charBank.goToNextLine()
          scanToken()
        case x if Token.singleCharTokens.contains(x) => Token.singleCharTokens(x)
        case x if Token.doubleCharTokens.contains(x) =>
          val doubleTokenEntry = Token.doubleCharTokens(c)
          val nextChar = charBank.peek
          val tokenOptions = doubleTokenEntry.options.filter(_.nextChar == nextChar)
          tokenOptions match
            case head :: _ =>
              charBank.advance()
              head.token
            case Nil => doubleTokenEntry.otherwise
        case x if Token.isNumericChar(x) =>
          scanNumericLiteral(startLine, x)
        case '"' => scanStringLiteral(startLine)
        case x if Token.isValidAlphabetChar(x) => scanIdentifier(startLine)
        case ' ' => scanToken()
        case '\t' => scanToken()
        case '\r' => scanToken()
        case '\n' => scanToken()


        case _ => TokenType.ErrorToken
    }

    def scan():List[Token] = {
      var tokens = List.empty[Token]

      while(!charBank.atEndOfFile) {
        val tokenLineNumber = charBank.getCurrentLine + 1
        val tType = scanToken()
        val subStr = charBank.getSubstring
        val newToken = new Token(tType, if(tType == TokenType.StringLiteral) subStr.tail.init else subStr, tokenLineNumber)
        //println(newToken)
        tokens = tokens :+ newToken
      }

      tokens = tokens :+ new Token(TokenType.EndOfFile, "", charBank.getCurrentLine)
      tokens
    }
  }

  private def isCommentOrEmpty(line: String): Boolean = line match
      case "" => true
      case x if x.length < 2 => false
      case x if x.substring(0, 2) == "//" => true
      case _ => false


  def scanText(lines: List[String]): List[Token] = {
    val trimmedLines = lines.zipWithIndex.map {
      case (line, number) => LabeledLine(line.trim, number)
    }.filterNot{
      case LabeledLine(line, _) => isCommentOrEmpty(line)
    }
    // (for line <- lines.zipWithIndex yield case).filterNot(isCommentOrEmpty)
    val charBank = CharBank(trimmedLines)
    val scanner = ScannerObject(charBank)

    @tailrec
    def parseOutShorthands(list: List[Token], parsedList: List[Token]): List[Token] = {
      val (newlyFound, leftToParse) = list match
        case Nil => return parsedList
        case Token(TokenType.Identifier, varName, ln) :: Token(mathsToken, mt, _) :: Token(TokenType.Equal, _, _) :: remaining
          if Set(
            TokenType.Plus, TokenType.Minus, TokenType.Modulo8, TokenType.Percent, TokenType.ShiftLeft, TokenType.ShiftRight,
            TokenType.Star, TokenType.Multiply8, TokenType.Slash, TokenType.Divide8, TokenType.Xor, TokenType.And,
            TokenType.Or
          ).contains(mathsToken)
        =>
          (Token(TokenType.Identifier, varName, ln) :: Token(TokenType.Equal, "=", ln) :: Token(TokenType.Identifier, varName, ln) :: Token(mathsToken, mt, ln) :: Nil, remaining)
        case _ => (list.head :: Nil, list.tail)
      parseOutShorthands(leftToParse, parsedList ::: newlyFound)
    }

    parseOutShorthands(scanner.scan(), Nil)
  }

  def scanText(filename: String): List[Token] = scanText(Source.fromFile(filename).getLines.toList)

  def main(args: Array[String]): Unit = {
    val a = scanText("src/main/fragment.grain")
    for line <- a do {
      println(line)
    }
  }
}

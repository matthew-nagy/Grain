package Grain

import scala.io.Source
import scala.annotation.tailrec

object Scanner{

  //A class that can take many lines and abstract the access to a queue-like structure
  private class CharBank(val lines: List[String]){
    private var currentLine = 0
    private var charIndex = 0

    private var startLine = 0
    private var startIndex = 0

    def setStart():Unit = {
      startLine = currentLine
      startIndex = charIndex
    }

    def getCurrentLine: Int = currentLine

    def goToNextLine():Unit = {
      currentLine += 1
      charIndex = 0
    }

    def getSubstring:String = {
      val builder = new StringBuilder("")
      val endLine = currentLine
      val endIndex = charIndex

      currentLine = startLine
      charIndex = startIndex

      while(!(currentLine == endLine && charIndex == endIndex)){
        builder += advance()
      }

      currentLine = endLine
      charIndex = endIndex
      builder.toString
    }

    def atEndOfFile:Boolean =
      currentLine == lines.length

    def peek:Char =
      if(atEndOfFile) 0
      else lines(currentLine)(charIndex)

    def advance():Char = {
      val c = peek
      charIndex += 1
      if(charIndex == lines(currentLine).length){
        currentLine += 1
        charIndex = 0
      }
      c
    }
  }

  //Turns a string of characters into a string of tokens
  private class ScannerObject(val charBank: CharBank) {

    private def scanNumericLiteral(startLine: Int, c: Char): TokenType = {
      while (Token.isNumericChar(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
      TokenType.IntLiteral
    }
    private def scanStringLiteral(startLine: Int): TokenType = {
      while(charBank.peek != '"' && charBank.getCurrentLine == startLine) charBank.advance()
      charBank.advance()

      TokenType.StringLiteral
    }
    private def scanIdentifier(startLine: Int): TokenType = {
      while(Token.isAlphanumericChar(charBank.peek) && charBank.getCurrentLine == startLine) charBank.advance()
      val tokenText = charBank.getSubstring

      tokenText match
        case x if Token.keywordMap.contains(x) => Token.keywordMap(x)
        case x if Token.typeMap.contains(x) => Token.typeMap(x)
        case _ => TokenType.Identifier
    }

    @tailrec
    private def scanToken():TokenType = {
      charBank.setStart()
      val startLine = charBank.getCurrentLine
      val c = charBank.advance()
      c match
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
        case x if Token.isNumericChar(x) => scanNumericLiteral(startLine, x)
        case '"' => scanStringLiteral(startLine)
        case x if Token.isValidAlphabetChar(x) => scanIdentifier(startLine)
        case '/' if charBank.peek == '/' =>
          charBank.goToNextLine()
          scanToken()
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
        tokens = tokens :+ new Token(tType, charBank.getSubstring, tokenLineNumber)
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
    val trimmedLines = (for line <- lines yield line.trim).filterNot(isCommentOrEmpty)
    val charBank = CharBank(trimmedLines)
    val scanner = ScannerObject(charBank)

    scanner.scan()
  }

  def scanText(filename: String): List[Token] = scanText(Source.fromFile(filename).getLines.toList)

  def main(args: Array[String]): Unit = {
    val a = scanText("./testCode.txt")
    for line <- a do {
      println(line)
    }
  }
}

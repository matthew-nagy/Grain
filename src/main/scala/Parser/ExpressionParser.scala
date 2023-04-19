package Parser

import Grain.*
import Grain.Expr.*
import Utility.{Errors, Struct, SyntaxError, TokenType}

object ExpressionParser {

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr | SyntaxError =
    parseExpression(scope, tokenBuffer)

  def parseOrThrow(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr =
    parseExpression(scope, tokenBuffer) match
      case error: SyntaxError => throw error
      case expr: Expr.Expr => expr

  //Because of how the translator works, some ops need the branches the other way round
  private def swapArithmeticBranches(expr: Expr.Expr): Expr.Expr = {
    expr match
      case Expr.Assign(name, arg) => Expr.Assign(name, swapArithmeticBranches(arg))
      case Expr.UnaryOp(op, arg) => Expr.UnaryOp(op, swapArithmeticBranches(arg))
      case Expr.BinaryOp(op, left, right) =>
        if(Operation.Groups.orderImportantOperations.contains(op)) {
          println("Swapping")
          println("\t" ++ expr.toString)
          println("to")
          println("\t" ++ Expr.BinaryOp(op, right, left).toString)
          Expr.BinaryOp(op, right, left)
        } else expr
      case Expr.Indirection(expr) => Expr.Indirection(swapArithmeticBranches(expr))
      case Expr.FunctionCall(function, arguments) =>
        Expr.FunctionCall(swapArithmeticBranches(function), arguments.map(swapArithmeticBranches))
      case Expr.Get(left, name) => Expr.Get(swapArithmeticBranches(left), name)
      case Expr.GetAddress(expr) => Expr.GetAddress(swapArithmeticBranches(expr))
      case Expr.GetIndex(of, by) => Expr.GetIndex(swapArithmeticBranches(of), swapArithmeticBranches(by))
      case Expr.SetIndex(of, to) => Expr.SetIndex(swapArithmeticBranches(of), swapArithmeticBranches(to))
      case Expr.Grouping(internalExpr) => Expr.Grouping(swapArithmeticBranches(internalExpr))
      case _ => expr
  }


  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr | SyntaxError = {
    returnTypeOrError {
      val expr = parseAssignment(scope, tokenBuffer)
      swapArithmeticBranches(expr)
    }
  }

  private def parseAssignment(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val token = tokenBuffer.peek
    val expr = parseLogical(scope, tokenBuffer)

    if(tokenBuffer.peekType == TokenType.Equal) {
      tokenBuffer.advance()
      val right = parseAssignment(scope, tokenBuffer)

      var expression = expr
      var isGrouping = true
      while isGrouping do{
        expression match
          case Grouping(internalExpr) => expression = internalExpr
          case _ => isGrouping = false
      }
      return expression match
        case get: Expr.Get => Expr.Assign(get, right)
        case Expr.Variable(name) => Expr.Assign(name, right)
        case Expr.GetIndex(_, _) => Expr.SetIndex(expression, right)
        case Expr.Indirection(inner) => Expr.SetIndex(GetIndex(inner, Expr.NumericalLiteral(0)), right)//This is horrible. I love it
        case _ =>
          println(expression)
          throw Errors.invalidLValue(tokenBuffer.getFilename, token)
    }

    expr
  }

  private def parseLogical(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var expr = parseEquality(scope, tokenBuffer)

    while(
      tokenBuffer.peekType == TokenType.Or ||
        tokenBuffer.peekType == TokenType.And ||
        tokenBuffer.peekType == TokenType.Xor
    ){
      val tokenType = tokenBuffer.advance().tokenType
      val op = if(tokenType == TokenType.Or)
        Operation.Binary.Or else Operation.Binary.And
      val right = parseEquality(scope, tokenBuffer)
      expr = Expr.BinaryOp(op, expr, right)
    }
    expr
  }
  private def parseEquality(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseComparison(scope, tokenBuffer)

    if(tokenBuffer.peekType == TokenType.BangEqual ||
      tokenBuffer.peekType == TokenType.EqualEqual){
      val op = if(tokenBuffer.advance().tokenType == TokenType.BangEqual)
        Operation.Binary.NotEqual else Operation.Binary.Equal
      val right = parseComparison(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
    }

    expr
  }
  private def parseComparison(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseTerm(scope, tokenBuffer)

    val termDict = Map(
      TokenType.Greater -> Operation.Binary.Greater,
      TokenType.GreaterEqual -> Operation.Binary.GreaterEqual,
      TokenType.Less -> Operation.Binary.Less,
      TokenType.LessEqual -> Operation.Binary.LessEqual
    )

    if(termDict.contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val op = termDict(tokenType)
      val right = parseTerm(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
    }

    expr
  }
  private def parseTerm(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var expr = parseFactor(scope, tokenBuffer)

    val termDict = Map(
      TokenType.Minus -> Operation.Binary.Subtract,
      TokenType.Plus -> Operation.Binary.Add,
      TokenType.ShiftLeft -> Operation.Binary.ShiftLeft,
      TokenType.ShiftRight -> Operation.Binary.ShiftRight
    )

    while(termDict.contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val right = parseFactor(scope, tokenBuffer)
      expr = Expr.BinaryOp(termDict(tokenType), expr, right)
    }

    expr
  }
  private def parseFactor(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseUnary(scope, tokenBuffer)
    if((TokenType.Star :: TokenType.Multiply8 :: TokenType.Slash :: TokenType.Divide8 :: TokenType.Percent :: TokenType.Modulo8 :: Nil).contains(tokenBuffer.peekType)){
      val tokenType = tokenBuffer.advance().tokenType
      val op = tokenType match
        case TokenType.Star => Operation.Binary.Multiply
        case TokenType.Multiply8 => Operation.Binary.Multiply8Bit
        case TokenType.Slash => Operation.Binary.Divide
        case TokenType.Divide8 => Operation.Binary.Divide8Bit
        case TokenType.Percent => Operation.Binary.Modulo
        case TokenType.Modulo8 => Operation.Binary.Modulo8Bit
        case _ => throw new Exception("Shouldn't be able to get here")
      val right = parseUnary(scope, tokenBuffer)
      return Expr.BinaryOp(op, expr, right)
    }
    expr
  }
  private def parseUnary(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    if((TokenType.Tilde :: TokenType.Bang :: TokenType.Minus :: Nil).contains(tokenBuffer.peekType)){
      val token = tokenBuffer.advance()
      val op = token.tokenType match {
        case TokenType.Tilde => Operation.Unary.BitwiseNot
        case TokenType.Bang => Operation.Unary.BooleanNegation
        case TokenType.Minus => Operation.Unary.Minus
        case _ => throw Errors.expectedUnary(tokenBuffer.getFilename, token)
      }
      val right = parseUnary(scope, tokenBuffer)
      right match
        case NumericalLiteral(value) => Expr.NumericalLiteral(value * -1)
        case _ =>
          Expr.UnaryOp(op, right)
    }
    else {
      parseTypeAlteration(scope, tokenBuffer)
    }
  }

  private def parseTypeAlteration(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    val expr = parseCall(scope, tokenBuffer)

    if (tokenBuffer.peekType == TokenType.As) {
      tokenBuffer.advance()
      expr.castToType(parseType(scope.symbolTable, tokenBuffer))
    }

    expr
  }


  private def finishCall(function: Expr.Expr, scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var arguments = List.empty[Expr.Expr]
    while(tokenBuffer.peekType != TokenType.RightParen){
      if arguments.nonEmpty then tokenBuffer.matchType(TokenType.Comma)
      arguments = ExpressionParser.parseOrThrow(scope, tokenBuffer) :: arguments
    }
    arguments = arguments.reverse
    tokenBuffer.matchType(TokenType.RightParen)
    Expr.FunctionCall(function, arguments)
  }
  private def parseCall(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    var indirection = false
    if(tokenBuffer.peekType == TokenType.Asperand){
      indirection = true
      tokenBuffer.advance()
    }
    var callStartToken = tokenBuffer.peek
    var expr = parsePrimary(scope, tokenBuffer)
    if(indirection){
      expr = Expr.Indirection(expr)
    }
    var expType = scope.getTypeOf(expr)

    while(tokenBuffer.peek.tokenType match{
      case TokenType.LeftParen =>
        if(!expType.isInstanceOf[Utility.FunctionPtr]) {
          if(tokenBuffer.peek.lineNumber != callStartToken.lineNumber){
            false//It was on a different line so probably the start of the next statement/expression
          }
          else {
            throw Errors.CannotCallType(tokenBuffer.getFilename, tokenBuffer.peek.lineNumber, expType)
          }
        }
        else {
          callStartToken = tokenBuffer.advance()
          expr = finishCall(expr, scope, tokenBuffer)
          true
        }
      case TokenType.Dot =>
        callStartToken = tokenBuffer.advance()
        val name = tokenBuffer.matchType(TokenType.Identifier)
        expr = Expr.Get(expr, name)
        true
      case TokenType.Asperand =>
        if(tokenBuffer.peek.lineNumber != callStartToken.lineNumber){
          false
        }
        else {
          callStartToken = tokenBuffer.advance()
          expr = Expr.GetAddress(expr)
          true
        }
      case TokenType.LeftSquare =>
        callStartToken = tokenBuffer.advance()
        val indexBy = ExpressionParser.parseOrThrow(scope, tokenBuffer)
        tokenBuffer.matchType(TokenType.RightSquare)
        expr = Expr.GetIndex(expr, indexBy)
        true
      case _ => false
    }){
      expType = scope.getTypeOf(expr)
    }

    expr
  }

  private def hexDigit(digit: Char): Int = {
    val digitMap: Map[Char, Int] = Range(0, 10).map(v => v.toString()(0) -> v).toMap[Char, Int] ++
      Map[Char, Int]('A' -> 10, 'B' -> 11, 'C' -> 12, 'D' -> 13, 'E' -> 14, 'F' -> 15)
    digitMap(digit)
  }
  private def parseIntValue(int: String): Int = {
    if(int.length < 3 || int(0) != '0'){
      Integer.parseInt(int)
    }
    else{
      int(1) match
        case 'b' =>
          var value = 0
          for i <- Range(2, int.length) do{
            value <<= 1
            value |= (if(int(i) == '1') 1 else 0)
          }
          value
        case 'x' =>
          var value = 0
          for i <- Range(2, int.length) do {
            value <<= 4
            value |= hexDigit(int(i).toUpper)
          }
          value
        case _ => Integer.parseInt(int)
    }
  }

  private def parsePrimary(scope: Scope, tokenBuffer: TokenBuffer): Expr.Expr = {
    def getInnerExpr: Expr.Expr = {
      tokenBuffer.matchType(TokenType.LeftParen)
      val innerExpr = parseExpression(scope, tokenBuffer) match
        case newExpression: Expr.Expr => newExpression
        case error: SyntaxError => throw error
      tokenBuffer.matchType(TokenType.RightParen)
      innerExpr
    }
    val token = tokenBuffer.advance()
    token.tokenType match
      case TokenType.False => Expr.BooleanLiteral(false)
      case TokenType.True => Expr.BooleanLiteral(true)
      case TokenType.IntLiteral => Expr.NumericalLiteral(parseIntValue(token.lexeme))
      case TokenType.StringLiteral => Expr.StringLiteral(token.lexeme)
      case TokenType.LeftParen =>
        val nextExpression = parseExpression(scope, tokenBuffer)
        tokenBuffer.matchType(TokenType.RightParen)
        nextExpression match
          case nextExpression: SyntaxError => throw nextExpression
          case nextExpression: Expr.Expr => nextExpression
      case TokenType.Identifier =>
        if(!scope.contains(token.lexeme)){
          throw Errors.SymbolNotFound(tokenBuffer.getFilename, token)
        }
        Expr.Variable(token)
      case TokenType.Asperand =>
        Expr.Indirection(parsePrimary(scope, tokenBuffer))

      case TokenType.SizeOf =>
        if(scope.symbolTable.types.contains(tokenBuffer.lookAhead(1).lexeme)){
          tokenBuffer.matchType(TokenType.LeftParen)
          val result = Expr.NumericalLiteral(Utility.getTypeSize(parseType(scope.symbolTable, tokenBuffer)))
          tokenBuffer.matchType(TokenType.RightParen)
          result
        }
        else {
          val innerExpr = getInnerExpr
          Expr.NumericalLiteral(Utility.getTypeSize(scope.getTypeOf(innerExpr)))
        }
      case TokenType.LengthOf =>
        val innerExpr = getInnerExpr
        scope.getTypeOf(innerExpr) match
          case Utility.Array(_, length) => Expr.NumericalLiteral(length)
          case badType @ _ => throw Errors.CannotGetLengthOfNonArrayType(tokenBuffer.getFilename, innerExpr, badType, token.lineNumber)
      case TokenType.BitPerPixelOf =>
        val innerExpr = getInnerExpr
        innerExpr match
          case Variable(name) =>
            scope(name.lexeme).dataType match
              case Utility.Array(Utility.Sprite(bpp), _) => Expr.NumericalLiteral(bpp)
              case Utility.Sprite(bpp) => Expr.NumericalLiteral(bpp)
              case _ => throw Errors.CannotGetBitDepthOfNonSprite(tokenBuffer.getFilename, innerExpr, scope(name.lexeme).dataType, token.lineNumber)
          case _ =>
            throw Errors.CannotGetBitDepthOfNonVariable(tokenBuffer.getFilename, innerExpr, token.lineNumber)
      case TokenType.BankOf =>
        val innerExpr = getInnerExpr
        innerExpr match
          case Variable(name) =>
            val bankedSymbol = scope(name.lexeme)
            bankedSymbol.form match
              case Symbol.Data(_, _, dataBank) => Expr.BankLiteral(dataBank)
              case _ => throw Errors.CannotGetBankOfNonData(tokenBuffer.getFilename, innerExpr, bankedSymbol.dataType, token.lineNumber)
          case _ => throw Errors.CannotGetBankOfNonVariable(tokenBuffer.getFilename, innerExpr, token.lineNumber)
      case TokenType.AsColour =>
        tokenBuffer.matchType(TokenType.LeftParen)

        val r = tokenBuffer.matchType(TokenType.IntLiteral).lexeme.toInt
        tokenBuffer.matchType(TokenType.Comma)
        val g = tokenBuffer.matchType(TokenType.IntLiteral).lexeme.toInt
        tokenBuffer.matchType(TokenType.Comma)
        val b = tokenBuffer.matchType(TokenType.IntLiteral).lexeme.toInt
        tokenBuffer.matchType(TokenType.Comma)
        val a = tokenBuffer.matchType(TokenType.IntLiteral).lexeme.toInt

        tokenBuffer.matchType(TokenType.RightParen)

        Expr.NumericalLiteral(r + (g << 5) + (b << 10) + (a << 15))
      case _ =>
        throw Errors.ExpectedExpression(tokenBuffer.getFilename, token)
  }


  private def typeCheckUnary(unary: Operation.Unary, value: Utility.Type): Boolean =
    unary match
      case Operation.Unary.Minus => value == Utility.Word()
      case Operation.Unary.BooleanNegation => value == Utility.BooleanType()
      case Operation.Unary.BitwiseNot => value == Utility.BooleanType() || value == Utility.Word()
      case null => throw Exception("Invalid case")
  def typeCheck(filename: String, expr: Expr.Expr, scope: Scope): Unit = {
    import Expr.*

    if(expr.castType.isDefined){
      if !Utility.isCastValid(scope.getUncastTypeOf(expr), expr.castType.get) then throw new Exception("Type validity not yet supported")

    }

    expr match
      case Assign(target, arg) =>
        typeCheck(filename, arg, scope)
        target match
          case varToken: Utility.Token =>
            if(!Utility.typeEquivilent(scope(varToken.lexeme).dataType, scope.getTypeOf(arg))){
              throw Errors.badlyTyped(filename,
                varToken.lexeme ++ " has type " ++ scope(varToken.lexeme).dataType.toString ++ ", rValue has type " ++ scope.getTypeOf(arg).toString
              )
            }
          case Get(left, memberToken) =>
              typeCheck(filename, left, scope)
              if (!Utility.typeEquivilent(scope.getTypeOf(left).asInstanceOf[Utility.Struct].getTypeOf(memberToken.lexeme), scope.getTypeOf(arg))) {
                throw Errors.badlyTyped(filename,
                  memberToken.lexeme ++ " has type " ++ scope(memberToken.lexeme).dataType.toString ++ ", rValue has type " ++ scope.getTypeOf(arg).toString
                )
              }
      case BooleanLiteral(_) =>
      case BankLiteral(_) =>
      case UnaryOp(op, arg) =>
        typeCheck(filename, arg, scope)
        typeCheckUnary(op, scope.getTypeOf(arg))
      case BinaryOp(op, left, right) =>
        typeCheck(filename, left, scope)
        typeCheck(filename, right, scope)
        op match
          case _ if Operation.Groups.LogicalTokens.contains(op) || Operation.Groups.RelationalTokens.contains(op) =>
            if(!Utility.typeEquivilent(scope.getTypeOf(left), scope.getTypeOf(right))){
              throw Errors.badlyTyped(filename,
                "LValue " ++ left.toString ++ " with type " ++ scope.getTypeOf(left).toString ++
                  "cannot use " ++ op.toString ++ " with RValue of type " ++ scope.getTypeOf(right).toString ++ " (" ++ right.toString ++ ")"
              )
            }
          case _ if Operation.Groups.ArithmeticTokens.contains(op) =>
            if(!Utility.typeEquivilent(scope.getTypeOf(left), Utility.Word())){
              throw Errors.badlyTyped(
                filename,
                op.toString ++ " requires arguments of type word. Left argument (" ++ left.toString ++ ") has type " ++ scope.getTypeOf(left).toString
              )
            }
            if(!Utility.typeEquivilent(scope.getTypeOf(right), Utility.Word())){
              throw Errors.badlyTyped(
                filename,
                op.toString ++ " requires arguments of type word. Right argument (" ++ right.toString ++ ") has type " ++ scope.getTypeOf(right).toString
              )
            }
          case _ => throw Exception("Ungrouped binary operation")

      case NumericalLiteral(_) =>
      case StringLiteral(_) =>
      case Indirection(expr) =>
        typeCheck(filename, expr, scope)
        if(!scope.getTypeOf(expr).isInstanceOf[Utility.Ptr]){
          Errors.badlyTyped(
            filename,
            "Cannot use indirection on non pointer type. " ++ expr.toString ++ " is of type " ++ scope.getTypeOf(expr).toString
          )
        }
      case Variable(_) =>
      case FunctionCall(function, arguments) =>
        val functionType = scope.getTypeOf(function)
        functionType match
          case Utility.FunctionPtr(argTypes, _) =>
            argTypes.zip(arguments.map(scope.getTypeOf)).forall(
              (expectedT, givenT) => {
                if (!Utility.typeEquivilent(expectedT, givenT)) {
                  throw Errors.badlyTyped(
                    filename,
                    "Expected type " ++ expectedT.toString ++ " but got type " ++ givenT.toString ++ ". (" ++ expr.toString ++ ")"
                  )
                }
                true
              }
            ) && arguments.forall(arg => {
              typeCheck(filename, arg, scope)
              true
            })
          case _ =>
      case Get(left, name) =>
        val leftType = scope.getTypeOf(left)
        leftType match {
          case leftType: Struct =>
            typeCheck(filename, left, scope)
            if(!leftType.entries.exists(_.symbol.name == name.lexeme)){
              throw Errors.structDoesntHaveElement(filename, name.lexeme, leftType.toString)
            }
          case _ =>
        }
      case GetAddress(of) =>
        //Only some types can have their address got
        typeCheck(filename, of, scope)
        if(!(of.isInstanceOf[Variable] || of.isInstanceOf[Get] || of.isInstanceOf[GetIndex])){
          throw Errors.badlyTyped(
            filename,
            of.toString ++ " is not an addressable value"
          )
        }
      case GetIndex(of, by) =>
        val ofType = scope.getTypeOf(of)
        typeCheck(filename, of, scope)
        typeCheck(filename, by, scope)
        if(!Utility.typeEquivilent(scope.getTypeOf(by), Utility.Word())){
          throw Errors.badlyTyped(filename, by.toString ++ " has type " ++ scope.getTypeOf(by).toString ++ ", expected word")
        }
        if(!ofType.isInstanceOf[Utility.PtrType]){
          throw Errors.cannotIndexNonPoinerElements(filename, of.toString, ofType)
        }
      case SetIndex(left, right) =>
        typeCheck(filename, left, scope)
        typeCheck(filename, right, scope)
        if (!Utility.typeEquivilent(scope.getTypeOf(left), scope.getTypeOf(right))) {
          throw Errors.badlyTyped(
            filename,
            left.toString ++ " has type " ++ scope.getTypeOf(left).toString ++ ", rValue has type " ++ scope.getTypeOf(right).toString
          )
        }
      case Grouping(internalExpr) => typeCheck(filename, internalExpr, scope)
      case null => throw new Exception("What?")
  }
}

package Parser

import Grain.*
import Stmt.*
import Utility.{Errors, SyntaxError, TokenType}

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

object StatementParser {

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement | SyntaxError =
    parseStatement(scope, tokenBuffer)

  def parseOrThrow(scope:Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    parseStatement(scope, tokenBuffer) match
      case error: SyntaxError => throw error
      case stmt: Stmt.Statement => stmt
  }
  private def parseStatement(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement | SyntaxError =
    returnTypeOrError {
      val statement= tokenBuffer.peekType match
        case TokenType.Assembly => parseAssembly(scope, tokenBuffer)
        case TokenType.LeftBrace => parseBlock(scope, tokenBuffer)
        case TokenType.For => parseFor(scope, tokenBuffer)
        case TokenType.If => parseIf(scope, tokenBuffer)
        case TokenType.Return => parseReturn(scope, tokenBuffer)
        case TokenType.While => parseWhile(scope, tokenBuffer)
        case _ =>
          if tokenBuffer.lookAhead(1).tokenType == TokenType.Colon then
            parseVariableDecl(scope, tokenBuffer, false)
          else
            parseExpression(scope, tokenBuffer)
      if !typeCheck(statement, scope) then throw new Exception("Statement is badly typed: " ++ statement.toString)
      else statement
      statement
    }

  private def parseAssembly(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Assembly = {
    tokenBuffer.matchType(TokenType.Assembly)
    tokenBuffer.matchType(TokenType.LeftBrace)
    val content = tokenBuffer.matchType(TokenType.StringLiteral)
    tokenBuffer.matchType(TokenType.RightBrace)
    Stmt.Assembly(content.lexeme)
  }

  def parseBlock(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Block = {
    tokenBuffer.matchType(TokenType.LeftBrace)
    val stmtBuffer = ListBuffer.empty[Stmt.Statement]
    val blockScope = scope.newChild()
    while(tokenBuffer.peekType != TokenType.RightBrace){
      stmtBuffer.append(parseOrThrow(blockScope, tokenBuffer))
    }
    tokenBuffer.matchType(TokenType.RightBrace)

    val blockStmt = Stmt.Block(stmtBuffer.toList)
    scope.linkStatementWithScope(blockStmt, blockScope)
    blockStmt
  }

  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Expression = {
    Stmt.Expression(ExpressionParser.parseOrThrow(scope, tokenBuffer))
  }

  private def parseFor(scope: Scope, tokenBuffer: TokenBuffer): Stmt.For = {
    val forScope = scope.newChild()
    tokenBuffer.matchType(TokenType.For)

    val startStmt = if(tokenBuffer.peekType == TokenType.Semicolon) None
                    else Some(parseOrThrow(forScope, tokenBuffer))
    tokenBuffer.matchType(TokenType.Semicolon)
    val breakExpr = if (tokenBuffer.peekType == TokenType.Semicolon) None
                    else Some(ExpressionParser.parseOrThrow(forScope, tokenBuffer))
    tokenBuffer.matchType(TokenType.Semicolon)
    val incrimentExpr = if(tokenBuffer.peekType == TokenType.Do) None
                        else Some(ExpressionParser.parseOrThrow(forScope, tokenBuffer))

    tokenBuffer.matchType(TokenType.Do)
    val body = parseOrThrow(forScope, tokenBuffer)

    val forStmt = Stmt.For(startStmt, breakExpr, incrimentExpr, body)
    scope.linkStatementWithScope(forStmt, forScope)
    forStmt
  }

  def parseElse(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Else = {
    tokenBuffer.matchType(TokenType.Else)
    val elseScope = scope.newChild()
    val elseBody = parseOrThrow(elseScope, tokenBuffer)
    val elseStmt = Stmt.Else(elseBody)

    scope.linkStatementWithScope(elseStmt, elseScope)
    elseStmt
  }
  def parseIf(scope: Scope, tokenBuffer: TokenBuffer): Stmt.If = {
    val ifScope = scope.newChild()
    tokenBuffer.matchType(TokenType.If)
    val condition = ExpressionParser.parseOrThrow(ifScope, tokenBuffer)
    tokenBuffer.matchType(TokenType.Then)
    val ifBody = parseOrThrow(ifScope, tokenBuffer)

    val elseStmt = if(tokenBuffer.peekType != TokenType.Else) None
                   else Some(parseElse(scope, tokenBuffer))

    val ifStmt = Stmt.If(condition, ifBody, elseStmt)
    scope.linkStatementWithScope(ifStmt, ifScope)
    ifStmt
  }

  def parseReturn(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Return = {
    val returnLine = tokenBuffer.matchType(TokenType.Return).lineNumber
    val value =
      if(tokenBuffer.peek.lineNumber == returnLine)
        Some(ExpressionParser.parseOrThrow(scope, tokenBuffer))
      else None

    Stmt.Return(value)
  }

  def parseVariableDecl(scope: Scope, tokenBuffer: TokenBuffer, atTopLevel: Boolean): Stmt.TopLevel = {
    val varToken = tokenBuffer.matchType(TokenType.Identifier)
    tokenBuffer.matchType(TokenType.Colon)
    val varType = parseType(scope.symbolTable, tokenBuffer)
    val symbolForm = if(atTopLevel) Symbol.GlobalVariable() else Symbol.Variable()
    scope.addSymbol(varToken, varType, symbolForm)

    if(tokenBuffer.peekType != TokenType.Equal){
      Stmt.EmptyStatement()
    }
    else {
      tokenBuffer.advance()
      val init = ExpressionParser.parseOrThrow(scope, tokenBuffer)
      Stmt.VariableDecl(Expr.Assign(varToken, init))
    }
  }

  def parseWhile(scope: Scope, tokenBuffer: TokenBuffer): Stmt.While = {
    tokenBuffer.matchType(TokenType.While)
    val whileScope = scope.newChild()
    val condition = ExpressionParser.parseOrThrow(whileScope, tokenBuffer)
    tokenBuffer.matchType(TokenType.Do)
    val body = parseOrThrow(whileScope, tokenBuffer)

    val whileStmt = Stmt.While(condition, body)
    scope.linkStatementWithScope(whileStmt, whileScope)
    whileStmt
  }

  def typeCheck(statement: Stmt.Statement, scope: Scope): Boolean = {
    import Stmt.*
    statement match
      case Assembly(assembly) => true
      case Block(statements) =>
        statements.forall(typeCheck(_, scope.getChildOrThis(statement)))
      case EmptyStatement() => true
      case Expression(expr) => ExpressionParser.typeCheck(expr, scope)
      case For(startExpr, breakExpr, incrimentExpr, body) =>
        val forScope = scope.getChildOrThis(statement)
        val startOk = startExpr.isEmpty || startExpr.exists(typeCheck(_, forScope))
        val breakOk = breakExpr.isEmpty || breakExpr.exists(ExpressionParser.typeCheck(_, forScope))
        val incrimentOk = incrimentExpr.isEmpty || incrimentExpr.exists(ExpressionParser.typeCheck(_, forScope))
        startOk && breakOk && incrimentOk && typeCheck(body, forScope)
      case FunctionDecl(_, _, body) => typeCheck(body, scope.getChildOrThis(statement))
      case Else(_) => throw Exception("Else branch shouldn't be triggered; handle in the if")
      case If(condition, body, elseBranch) =>
        val ifOk = scope.getTypeOf(condition) == Utility.BooleanType() && ExpressionParser.typeCheck(condition, scope) &&
          typeCheck(body, scope.getChildOrThis(statement))
        elseBranch match
          case None => ifOk
          case Some(elseStmt) => ifOk && typeCheck(elseStmt.body, scope.getChildOrThis(elseStmt))
      case Return(_) => throw Exception("Need to edit symbol table " +
        "and scope to contain data on what type of function this is")
      case VariableDecl(Expr.Assign(name, initializer)) =>
        ExpressionParser.typeCheck(initializer, scope) && scope(name.lexeme).dataType == scope.getTypeOf(initializer)
      case While(condition, body) =>
        ExpressionParser.typeCheck(condition, scope) && scope.getTypeOf(condition) == Utility.BooleanType() && typeCheck(body, scope.getChildOrThis(statement))
      case a @ _ =>
        println(a.toString ++ " hasn't been handled yet")
        true
  }
}

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
      typeCheck(statement, scope)

      if GlobalData.optimisationFlags.staticOptimiseTree then OptimiseStatement(statement)
      else statement
    }

  def parseAssembly(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Assembly = {
    tokenBuffer.matchType(TokenType.Assembly)
    tokenBuffer.matchType(TokenType.LeftBrace)
    val assemblyContent = ListBuffer.empty[String]
    while(tokenBuffer.peekType == TokenType.StringLiteral){
      assemblyContent.append(tokenBuffer.advance().lexeme)
    }
    tokenBuffer.matchType(TokenType.RightBrace)
    Stmt.Assembly(assemblyContent.toList)
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

  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Expression =
    Stmt.Expression(ExpressionParser.parseOrThrow(scope, tokenBuffer))

  private def parseFor(scope: Scope, tokenBuffer: TokenBuffer): Stmt.For = {
    val forScope = scope.newChild()
    val forLine = tokenBuffer.matchType(TokenType.For).lineNumber

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

    val forStmt = Stmt.For(startStmt, breakExpr, incrimentExpr, body, forLine)
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
    val ifLine = tokenBuffer.matchType(TokenType.If).lineNumber
    val condition = ExpressionParser.parseOrThrow(ifScope, tokenBuffer)
    tokenBuffer.matchType(TokenType.Then)
    val ifBody = parseOrThrow(ifScope, tokenBuffer)

    val elseStmt = if(tokenBuffer.peekType != TokenType.Else) None
                   else Some(parseElse(ifScope, tokenBuffer))

    val ifStmt = Stmt.If(condition, ifBody, elseStmt, ifLine)
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
    val whileLine = tokenBuffer.matchType(TokenType.While).lineNumber
    val whileScope = scope.newChild()
    val condition = ExpressionParser.parseOrThrow(whileScope, tokenBuffer)
    tokenBuffer.matchType(TokenType.Do)
    val body = parseOrThrow(whileScope, tokenBuffer)

    val whileStmt = Stmt.While(condition, body, whileLine)
    scope.linkStatementWithScope(whileStmt, whileScope)
    whileStmt
  }

  def typeCheck(statement: Stmt.Statement, scope: Scope): Unit = {
    import Stmt.*
    statement match
      case Assembly(assembly) =>
      case Block(statements) =>
        statements.forall(stmt => {
          typeCheck(stmt, scope.getChildOrThis(statement))
          true
        })
      case EmptyStatement() =>
      case Expression(expr) => ExpressionParser.typeCheck(expr, scope)
      case For(startExpr, breakExpr, incrimentExpr, body, _) =>
        val forScope = scope.getChildOrThis(statement)
        startExpr.exists(value =>{
          typeCheck(value, forScope)
          true
        })
        breakExpr.exists(value => {
          ExpressionParser.typeCheck(value, forScope)
          true
        })
        incrimentExpr.exists(value => {
          ExpressionParser.typeCheck(value, forScope)
          true
        })
        typeCheck(body, forScope)
      case FunctionDecl(_, _, body) => typeCheck(body, scope.getChildOrThis(statement))
      case Else(_) => throw Exception("Else branch shouldn't be triggered; handle in the if")
      case If(condition, body, elseBranch, _) =>
        ExpressionParser.typeCheck(condition, scope)
        val ifScope = scope.getChildOrThis(statement)
        typeCheck(body, ifScope)
        if(!Utility.typeEquivilent(scope.getTypeOf(condition), Utility.BooleanType())){
          throw Errors.badlyTyped(
            "Expected boolean, " ++ condition.toString ++ " had type " ++ scope.getTypeOf(condition).toString
          )
        }
        elseBranch match
          case None =>
          case Some(elseStmt) => typeCheck(elseStmt.body, ifScope.getChildOrThis(elseStmt))
      case Return(Some(expr)) =>
        ExpressionParser.typeCheck(expr, scope)
        if(!Utility.typeEquivilent(scope.getTypeOf(expr), scope.getReturnType)){
          throw Errors.badlyTyped(expr.toString ++ " has type " ++ scope.getTypeOf(expr).toString ++ " but the stated return type is " ++ scope.getReturnType.toString)
        }
      case Return(None) =>
      case VariableDecl(Expr.Assign(name, initializer)) =>
        ExpressionParser.typeCheck(initializer, scope)
        if(!Utility.typeEquivilent(scope(name.lexeme).dataType, scope.getTypeOf(initializer))){
          throw Errors.badlyTyped(
            name.lexeme ++ " of type " ++ scope(name.lexeme).dataType.toString ++ " cannot be innitializeationabled with type " ++ scope.getTypeOf(initializer).toString
          )
        }
      case While(condition, body, _) =>
        ExpressionParser.typeCheck(condition, scope)
        if(!Utility.typeEquivilent(scope.getTypeOf(condition), Utility.BooleanType())){
          throw Errors.badlyTyped(
            "RRRRRRRRRRREEEEEEEEEEEEEEEEEEEEEEEEE"
          )
        }
        typeCheck(body, scope.getChildOrThis(statement))
      case a @ _ =>
        println(a.toString ++ " hasn't been handled yet")
        throw new Exception("AAAAAA")
  }
}

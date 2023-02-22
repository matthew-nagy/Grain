package Grain
import Utility.{Errors, SyntaxError, TokenType}

import scala.reflect.ClassTag

object StatementParser {

  def apply(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement | SyntaxError =
    parseStatement(scope, tokenBuffer)
  private def parseStatement(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement | SyntaxError =
    returnTypeOrError {
      tokenBuffer.peekType match
        case TokenType.Assembly => parseAssembly(scope, tokenBuffer)
        case TokenType.LeftBrace => parseBlock(scope, tokenBuffer)
        case TokenType.If => parseIfStmt(scope, tokenBuffer)
        case TokenType.For => parseFor(scope, tokenBuffer)
        case TokenType.Return => parseReturn(scope,tokenBuffer)
        case TokenType.While => parseWhile(scope, tokenBuffer)
        case _ =>
          tokenBuffer.lookAhead(1).tokenType match
            case TokenType.Colon => parseVariableDecl(scope, tokenBuffer)
            case _ => parseExpression(scope, tokenBuffer)
    }

  private def parseAssembly(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    tokenBuffer.matchType(TokenType.Assembly)
    tokenBuffer.matchType(TokenType.LeftBrace)
    val literal = tokenBuffer.matchType(TokenType.StringLiteral)
    tokenBuffer.matchType(TokenType.RightBrace)
    Stmt.Assembly(literal.lexeme)
  }

  def parseBlock(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Block = {
    val startBrace = tokenBuffer.matchType(TokenType.LeftBrace)
    var inner = List.empty[Stmt.Statement]
    val newScope = scope.newChild()

    while (tokenBuffer.peekType match
      case TokenType.RightBrace =>
        tokenBuffer.advance()
        false
      case TokenType.EndOfFile => throw Errors.UnclosedCurlyBrackets(startBrace)
      case _ =>
        val s: Stmt.Statement = getTypeOrThrow[Stmt.Statement](parseStatement(newScope, tokenBuffer))
          inner = s :: inner
        true
    ) {}

    val blockStmt = Stmt.Block(inner.reverse)
    scope.linkStatementWithScope(blockStmt, newScope)
    blockStmt
  }

  private def parseExpression(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    val parsedExpr = getTypeOrThrow[Expr.Expr](ExpressionParser(scope, tokenBuffer))
    Stmt.Expression(parsedExpr)
  }

  private def parseIfStmt(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    tokenBuffer.matchType(TokenType.If)
    val expr = getTypeOrThrow[Expr.Expr](ExpressionParser(scope, tokenBuffer))
    tokenBuffer.matchType(TokenType.Then)
    val ifScope = scope.newChild()
    val body = getTypeOrThrow[Stmt.Statement](parseStatement(ifScope, tokenBuffer))

    val elseBranch: Option[Stmt.Else] = tokenBuffer.peekType match
      case TokenType.Else =>
        tokenBuffer.advance()
        val elseScope = scope.newChild()
        val elseBody = getTypeOrThrow[Stmt.Statement](parseStatement(elseScope, tokenBuffer))
        val elseStmt = Stmt.Else(elseBody)
        scope.linkStatementWithScope(elseStmt, elseScope)
        Some(elseStmt)
      case _ => None

    val ifStmt = Stmt.If(expr, body, elseBranch)
    scope.linkStatementWithScope(ifStmt, ifScope)
    ifStmt
  }

  def parseVariableDecl(scope: Scope, tokenBuffer: TokenBuffer): Stmt.TopLevel = {
    val varName = tokenBuffer.advance()
    tokenBuffer.matchType(TokenType.Colon)
    val varType = parseType(scope.symbolTable, tokenBuffer)

    //If it isn't defined, just mess with the scope
    if(tokenBuffer.peekType != TokenType.Equal){
      if(scope.strictContains(varName.lexeme)){
        throw Errors.SymbolRedefinition(scope(varName.lexeme).token, varName)
      }
      else{
        scope.addSymbol(varName, varType, DefinitionType.NotDefined)
        return Stmt.EmptyStatement()
      }
    }
    tokenBuffer.matchType(TokenType.Equal)
    val initialiser = getTypeOrThrow[Expr.Expr](ExpressionParser(scope, tokenBuffer))
    //Update the symbol table
    if(scope.strictContains(varName.lexeme)){
      val lastDefinition = scope(varName.lexeme)
      if(lastDefinition.definition == DefinitionType.NotDefined){
        if lastDefinition.dataType != varType then throw Errors.DefinitionDoesntMatchType(lastDefinition.dataType, varType, varName)
        else lastDefinition.definition = DefinitionType.Defined
      }
      else throw Errors.SymbolRedefinition(lastDefinition.token, varName)
    }
    else{
      scope.addSymbol(varName, varType, DefinitionType.Defined)
    }

    Stmt.VariableDecl(varName, initialiser)
  }

  private def possibleParsePastType[T: ClassTag](tokenBuffer: TokenBuffer, breakType: TokenType)(action: => T|SyntaxError):Option[T] = {
    if(tokenBuffer.peekType == breakType){
      None
    }
    else{
      val parsedVal = action
      parsedVal match
        case parsedVal: SyntaxError => throw parsedVal
        case parsedVal: T => Some(parsedVal)
    }
  }

  private def parseFor(scope: Scope, tokenBuffer: TokenBuffer): Stmt.For = {
    tokenBuffer.matchType(TokenType.For)
    val forScope = scope.newChild()
    val initStmt = possibleParsePastType[Stmt.Statement](tokenBuffer, TokenType.Semicolon){
      parseStatement(forScope, tokenBuffer)
    }
    tokenBuffer.matchType(TokenType.Semicolon)

    val breakExpr = possibleParsePastType[Expr.Expr](tokenBuffer, TokenType.Semicolon){
      ExpressionParser(forScope, tokenBuffer)
    }
    tokenBuffer.matchType(TokenType.Semicolon)

    val incrimentExpr = possibleParsePastType[Expr.Expr](tokenBuffer, TokenType.LeftBrace){
      ExpressionParser(forScope, tokenBuffer)
    }

    tokenBuffer.matchType(TokenType.Do)

    val body = getTypeOrThrow[Stmt.Statement](parseStatement(forScope, tokenBuffer))
    val forStmt = Stmt.For(initStmt, breakExpr, incrimentExpr, body)
    scope.linkStatementWithScope(forStmt, forScope)
    forStmt
  }

  private def parseReturn(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    val returnToken = tokenBuffer.matchType(TokenType.Return)
    if(tokenBuffer.peek.lineNumber != returnToken.lineNumber){
      Stmt.Return(None)
    }
    else{
      Stmt.Return(Some(getTypeOrThrow[Expr.Expr](ExpressionParser(scope,tokenBuffer))))
    }
  }

  private def parseWhile(scope: Scope, tokenBuffer: TokenBuffer): Stmt.Statement = {
    tokenBuffer.matchType(TokenType.While)
    val expr = getTypeOrThrow[Expr.Expr](ExpressionParser(scope, tokenBuffer))

    tokenBuffer.matchType(TokenType.Do)

    val whileScope = scope.newChild()
    val body = getTypeOrThrow[Stmt.Statement](parseStatement(whileScope, tokenBuffer))
    val whileStmt = Stmt.While(expr, body)
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
      case For(startExpr, breakExpr, incrimentExpr, body) =>
        val forScope = scope.getChildOrThis(statement)
        val startOk = startExpr.isEmpty || startExpr.exists(typeCheck(_, forScope))
        val breakOk = breakExpr.isEmpty || breakExpr.exists(ExpressionParser.typeCheck(_, forScope))
        val incrimentOk = incrimentExpr.isEmpty || incrimentExpr.exists(ExpressionParser.typeCheck(_, forScope))
        startOk && breakOk && incrimentOk && typeCheck(body, forScope)
      case FunctionDecl(_, _, Some(body)) => typeCheck(body, scope.getChildOrThis(statement))
      case Else(body) => throw Exception("Else branch shouldn't be triggered; handle in the if")
      case If(condition, body, elseBranch) =>
        val ifOk = scope.getTypeOf(condition) == Utility.BooleanType() && ExpressionParser.typeCheck(condition, scope) &&
          typeCheck(body, scope.getChildOrThis(statement))
        elseBranch match
          case None => ifOk
          case Some(elseStmt) => ifOk && typeCheck(elseStmt.body, scope.getChildOrThis(elseStmt))
      case Return(_) => throw Exception("Need to edit symbol table and scope to contain data on what type of function this is")
      case VariableDecl(name, initializer) =>
        ExpressionParser.typeCheck(initializer, scope) && scope(name.lexeme).dataType == scope.getTypeOf(initializer)
      case While(condition, body) =>
        ExpressionParser.typeCheck(condition, scope) && scope.getTypeOf(condition) == Utility.BooleanType() && typeCheck(body, scope.getChildOrThis(statement))
  }
}

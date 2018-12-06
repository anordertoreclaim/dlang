//> Resolving and Binding resolver
package dlang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();


  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  @Override
  public Void visitBodyStmt(Stmt.Body stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitAssignmentStmt(Stmt.Assignment stmt) {
    resolve(stmt.left);
    resolve(stmt.right);
    return null;
  }

  //< visit-function-stmt
//> visit-if-stmt
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }
//< visit-if-stmt
//> visit-print-stmt
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-print-stmt
//> visit-return-stmt
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    for(Stmt.Var.VarDecl varDecl : stmt.varDecls) {
      declare(varDecl.name);
      if (varDecl.initializer != null) {
        resolve(varDecl.initializer);
      }
      define(varDecl.name);
    }
    return null;
  }
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitReferenceStmt(Stmt.Reference stmt) {
    resolve(stmt.reference);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }
  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitFunctionLiteralExpr(Expr.FunctionLiteral expr) {
    beginScope();
    for (Token param : expr.params) {
      declare(param);
      define(param);
    }
    resolve(expr.body);
    endScope();
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitRelationExpr(Expr.Relation expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitFactorExpr(Expr.Factor expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitTermExpr(Expr.Term expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.left);
    return null;
  }

  @Override
  public Void visitReferenceExpr(Expr.Reference expr) {
    resolve(expr.left);
    for (Expr argument : expr.exprList) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      DLang.error(expr.name,
          "Cannot read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

    @Override
    public Void visitArrayElementExpr(Expr.ArrayElement expr) {
      resolve(expr.index);
      return null;
    }

    @Override
    public Void visitReadExpr(Expr.Read expr) {
        return null;
    }

    private void resolve(Stmt stmt) {
    stmt.accept(this);
  }
  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }
  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      DLang.error(name,
          "Variable with this name already declared in this scope.");
    }

    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }
}

package dlang;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitBodyStmt(Body stmt);
    R visitAssignmentStmt(Assignment stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
    R visitReferenceStmt(Reference stmt);
  }

  static class Body extends Stmt {
    Body(List<Stmt> statements) {
      this.statements = statements;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBodyStmt(this);
    }

    final List<Stmt> statements;
  }

  static class Assignment extends Stmt {
    Assignment(Expr left, Expr right) {
      this.left = left;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignmentStmt(this);
    }

    final Expr left;
    final Expr right;
  }

  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value;
  }

  static class Var extends Stmt {
    static class VarDecl {
      final Token name;
      final Expr initializer;

      VarDecl(Token name, Expr initializer) {
        this.name = name;
        this.initializer = initializer;
      }
    }

    Var(List<VarDecl> varDecls) {
      this.varDecls = varDecls;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final List<VarDecl> varDecls;
}

  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }


  static class Reference extends Stmt {
    Reference(Expr reference) {
      this.reference = reference;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReferenceStmt(this);
    }

    final Expr reference;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

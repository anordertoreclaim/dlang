package dlang;

import java.util.List;

abstract class Expr {
    interface Visitor<R> {
        R visitLogicalExpr(Logical expr);

        R visitRelationExpr(Relation expr);

        R visitFactorExpr(Factor expr);

        R visitTermExpr(Term expr);

        R visitUnaryExpr(Unary expr);

        R visitReferenceExpr(Reference expr);

        R visitGroupingExpr(Grouping expr);

        R visitLiteralExpr(Literal expr);

        R visitFunctionLiteralExpr(FunctionLiteral expr);

        R visitVariableExpr(Variable expr);

        R visitArrayElementExpr(ArrayElement expr);

        R visitReadExpr(Read expr);
    }

    static class Logical extends Expr {
        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Relation extends Expr {
        Relation(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRelationExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Factor extends Expr {
        Factor(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFactorExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Term extends Expr {
        Term(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitTermExpr(this);
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Unary extends Expr {
        Unary(Expr left, Token operator, TypeIndicator type) {
            this.left = left;
            this.operator = operator;
            this.type = type;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        final Expr left;
        final Token operator;
        final TypeIndicator type;
    }

    static class Reference extends Expr {
        Reference(Expr left, Token operator, List<Expr> exprList, Token identifier) {
            this.left = left;
            this.operator = operator;
            this.exprList = exprList;
            this.identifier = identifier;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitReferenceExpr(this);
        }

        final Expr left;
        final Token operator;
        final List<Expr> exprList;
        final Token identifier;
    }

    static class Grouping extends Expr {
        Grouping(Expr expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        final Expr expression;
    }

    static class Literal extends Expr {
        Literal(Object value) {
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        final Object value;
    }

    static class FunctionLiteral extends Expr {
        FunctionLiteral(List<Token> params, List<Stmt> body) {
            this.params = params;
            this.body = body;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionLiteralExpr(this);
        }

        final List<Token> params;
        final List<Stmt> body;
    }

    static class Variable extends Expr {
        Variable(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }

        final Token name;
    }

    static class ArrayElement extends Expr {
        ArrayElement(Token name, Expr index) {
            this.name = name;
            this.index = index;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitArrayElementExpr(this);
        }

        final Token name;
        final Expr index;
    }

    static class Read extends Expr {
        Read(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitReadExpr(this);
        }

        final Token name;
        Object value = null;
    }

    abstract <R> R accept(Visitor<R> visitor);
}

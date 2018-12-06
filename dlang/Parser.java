package dlang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dlang.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }

        return statements;
    }

    private Stmt statement() {
        try {
            if (match(VAR)) return varDeclaration();
            if (match(FOR)) return forStatement();
            if (match(IF)) return ifStatement();
            if (match(PRINT)) return printStatement();
            if (match(RETURN)) return returnStatement();
            if (match(WHILE)) return whileStatement();
            if (match(LOOP)) return new Stmt.Body(body());
            return assignment();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt forStatement() {
        Stmt.Var initializer = (Stmt.Var) varDeclaration();
        if (initializer.varDecls.size() != 1) {
            throw error(peek(), "Wrong number of arguments in a loop.");
        }
        Stmt increment = assignment();

        Expr condition = expression();
        List<Stmt> loopBody = body();
        loopBody.add(increment);
        Stmt body = new Stmt.Body(loopBody);

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        body = new Stmt.Body(Arrays.asList(initializer, body));
        consume(END, "Expected 'end' in the end of for.");
        consume(SEMICOLON, "Expect ';' loop end.");
        return body;
    }

    private Stmt ifStatement() {
        Expr condition = expression();
        consume(THEN, "Expect 'then' after if condition.");
        Stmt thenBranch = new Stmt.Body(body());
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        consume(END, "Expect 'end' after if condition.");
        consume(SEMICOLON, "Expect ';' after if end.");
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        List<Stmt.Var.VarDecl> declarations = new ArrayList<>();
        do {
            Token name = consume(IDENTIFIER, "Expect variable name.");
            Expr initializer = null;
            if (match(ASSIGN)) {
                if (match(LEFT_BRACKET)) {
                  initializer = array();
                } else {
                    initializer = expression();
                }
            }
            declarations.add(new Stmt.Var.VarDecl(name, initializer));
        } while (match(COMMA));
        consume(SEMICOLON, "Expected ';' at the end of variable declaration.");
        return new Stmt.Var(declarations);
    }

    private Stmt whileStatement() {
        Expr condition = expression();
        Stmt body = new Stmt.Body(body());
        consume(END, "Expected 'end' in the end of while");
        consume(SEMICOLON, "Expect ';' after while end.");
        return new Stmt.While(condition, body);
    }

    private List<Stmt> body() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(END) && !check(ELSE) && !isAtEnd()) {
            if (match(VAR)) {
                do {
                    statements.add(varDeclaration());
                } while (match(COMMA));
            } else statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt assignment() {
        Expr expr = reference(null);
        if (expr == null) {
            throw error(peek(),"Invalid assignment target.");
        }

        if (match(ASSIGN)) {
            Expr value;
            if (match(LEFT_BRACKET)) {
                value = array();
            } else {
                value = expression();
            }

            consume(SEMICOLON, "Expected ';' after assignment.");
            return new Stmt.Assignment(expr, value);
        } else {
            consume(SEMICOLON, "Expected ';' after reference");
            return new Stmt.Reference(expr);
        }
    }

    private Expr expression() {
        Expr expr = relation();

        while (match(OR, XOR, AND)) {
            Token operator = previous();
            Expr right = relation();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr relation() {
        Expr expr = factor();

        if (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Relation(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = term();

        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Factor(expr, operator, right);
        }

        return expr;
    }

    private Expr array() {
        List<Expr> values = new ArrayList<>();
        do {
            values.add(expression());
        } while (match(COMMA));
        consume(RIGHT_BRACKET, "Expected ']' at the end of array.");
        return new Expr.Literal(values);
    }

    private Expr term() {
        Expr expr = unary();

        while (match(STAR, SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Term(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        Expr expr = reference(null);
        if (expr != null) {
            if (match(IS)) {
                Token operator = previous();
                TypeIndicator typeCheck = typeIndicator();
                expr = new Expr.Unary(expr, operator, typeCheck);
            } else {
                expr = new Expr.Unary(expr, null, null);
            }
        } else {
            if (match(PLUS, MINUS, NOT)) {
                Token operator = previous();
                Expr right = primary();
                expr = new Expr.Unary(right, operator, null);
            } else {
                Expr right = primary();
                expr = new Expr.Unary(right, null, null);
            }
        }

        return expr;
    }

    private TypeIndicator typeIndicator() {
        if (match(TokenType.INT)) {
            return TypeIndicator.INT;
        }
        if (match(TokenType.REAL)) {
            return TypeIndicator.REAL;
        }
        if (match(TokenType.STRING)) {
            return TypeIndicator.STRING;
        }
        if (match(TokenType.BOOL)) {
            return TypeIndicator.BOOL;
        }
        if (match(TokenType.EMPTY)) {
            return TypeIndicator.EMPTY;
        }
        if (match(TokenType.LEFT_BRACKET)) {
            consume(TokenType.RIGHT_BRACKET, "Expected ']'.");
            return TypeIndicator.ARRAY;
        }
        if (match(TokenType.LEFT_BRACE)) {
            consume(TokenType.RIGHT_BRACE, "Expected '}'.");
            return TypeIndicator.TUPLE;
        }
        if (match(TokenType.FUNC)) {
            return TypeIndicator.FUNC;
        }
        throw error(peek(), "Unknown TypeIndicator.");
    }

    private Expr reference(Expr.Reference expr) {
        Token identifier = null;
        if (expr == null) {
            if (match(IDENTIFIER)) {
                identifier = previous();
            } else {
                return null;
            }
        }
        if (match(LEFT_BRACKET, LEFT_PAREN, DOT)) {
            Token operator = previous();
            if (operator.type == DOT) {
//                if (match(IDENTIFIER)) {
//                    Token id = previous();
//                    if (expr == null) {
//                        expr = new Expr.Reference(new Expr.Variable(identifier), operator, null, id);
//                    } else {
//                        expr = new Expr.Reference(expr, operator, null, id);
//                    }
//                } else {
//                    throw error(peek(), "Error in reference.");
//                }
                return null;
            } else if (operator.type == LEFT_PAREN) {
                if (match(RIGHT_PAREN)) {
                    ArrayList<Expr> exprList = new ArrayList<>();
                    if (expr == null) {
                        return new Expr.Reference(new Expr.Variable(identifier), operator, exprList, null);
                    } else {
                        return new Expr.Reference(expr, operator, exprList, null);
                    }
                }
                Expr right = expression();
                ArrayList<Expr> exprList = new ArrayList<>();
                exprList.add(right);
                while (match(COMMA)) {
                    exprList.add(expression());
                }
                consume(TokenType.RIGHT_PAREN, "Expected ')'.");
                if (expr == null) {
                    expr = new Expr.Reference(new Expr.Variable(identifier), operator, exprList, null);
                } else {
                    expr = new Expr.Reference(expr, operator, exprList, null);
                }
            } else {
                Expr index = expression();
                consume(TokenType.RIGHT_BRACKET, "Expected '].'");
                return new Expr.ArrayElement(new Expr.Variable(identifier).name, index);
            }
            return reference(expr);
        } else {
            if (expr == null) {
                return new Expr.Variable(identifier);
            } else {
                return expr;
            }
        }
    }

    private Expr primary() {
        if (match(STRING_LITERAL, NUMBER)) {
            return new Expr.Literal(previous().literal);
        } else if (match(TRUE)) {
            return new Expr.Literal(true);
        } else if (match(FALSE)) {
            return new Expr.Literal(false);
        } else if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        } else if (match(FUNC)) {
            return functionLiteral();
        } else if (match(READ_INT, READ_REAL, READ_STRING)) {
            return readExpression();
        } else if (match(LEFT_PAREN)) {
            Expr grouping = new Expr.Grouping(expression());
            consume(RIGHT_PAREN, "Expect ')' at the end of the string.");
            return grouping;
        } else if (match(LEFT_BRACKET)) {
            return array();
        }
        throw error(peek(), "Error in primary.");
    }

    private Expr readExpression() {
        return new Expr.Read(previous());
    }

    private Expr functionLiteral() {
        ArrayList<Token> params = new ArrayList<>();
        if (match(LEFT_PAREN)) {
            if (!check(RIGHT_PAREN)) {
                do {
                    params.add(consume(IDENTIFIER, "Expected parameter name"));
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expect ')' after parameters.");
            if (params.size() == 0) {
                throw error(peek(), "Ti loh, gde parametry");
            }
        }
        if (match(IS)) {
            Token op = previous();
            List<Stmt> body = body();
            if (op.type.equals(IS)) {
                consume(END, "Expected 'end'.");
            }
            return new Expr.FunctionLiteral(params, body);
        }
        if (match(LAMBDA)) {
            List<Stmt> body = new ArrayList<>(1);
            body.add(statement());
            return new Expr.FunctionLiteral(params, body);
        }
        throw error(peek(), "Dolbil Sillitti v tuza");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        DLang.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}

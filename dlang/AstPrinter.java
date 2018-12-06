package dlang;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    String print(Stmt stmt) {
        return stmt.accept(this);
    }

    @Override
    public String visitBodyStmt(Stmt.Body stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(body ");

        for (Stmt statement : stmt.statements) {
            builder.append(statement.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitAssignmentStmt(Stmt.Assignment stmt) {
        return "(assignment " +
                stmt.left.accept(this) +
                " := " +
                stmt.right.accept(this);
    }

    @Override
    public String visitFunctionLiteralExpr(Expr.FunctionLiteral stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(func (");

        for (Token param : stmt.params) {
            if (param != stmt.params.get(0)) builder.append(" ");
            builder.append(param.lexeme);
        }

        builder.append(") ");

        for (Stmt body : stmt.body) {
            builder.append(body.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        if (stmt.elseBranch == null) {
            return parenthesize2("if", stmt.condition, stmt.thenBranch);
        }

        return parenthesize2("if-else", stmt.condition, stmt.thenBranch,
                stmt.elseBranch);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value == null) return "(return)";
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < stmt.varDecls.size(); i++) {
            if (stmt.varDecls.get(i).initializer == null) {
                output.append(parenthesize2("var", stmt.varDecls.get(i).name));
                if (i != stmt.varDecls.size() - 1) {
                    output.append(",");
                }
                continue;
            }

            output.append(parenthesize2("var", stmt.varDecls.get(i).name, ":=", stmt.varDecls.get(i).initializer));
            if (i != stmt.varDecls.size() - 1) {
                output.append(",");
            }
        }
        return output.toString();
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return parenthesize2("while", stmt.condition, stmt.body);
    }

    @Override
    public String visitReferenceStmt(Stmt.Reference stmt) {
        return stmt.reference.accept(this);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "empty";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitRelationExpr(Expr.Relation expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitFactorExpr(Expr.Factor expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitTermExpr(Expr.Term expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        if (expr.operator == null) {
            return expr.left.accept(this);
        }
        if (expr.operator.type == TokenType.PLUS ||
                expr.operator.type == TokenType.MINUS ||
                expr.operator.type == TokenType.NOT) {
            return expr.operator.lexeme + " " + expr.left.accept(this);
        } else {
            return expr.left.accept(this) + " is " + expr.type.toString();
        }
    }

    @Override
    public String visitReferenceExpr(Expr.Reference expr) {
        String output = "";
        if (expr.left != null) {
            output = expr.left.accept(this) + output;
        }
        if (expr.operator != null) {
            output += expr.operator.lexeme;
            if (expr.operator.type != TokenType.DOT) {
                for (Expr e : expr.exprList) {
                    output += e.accept(this) + " ";
                }
                output += expr.operator.type == TokenType.RIGHT_BRACKET ? "]" : ")";
            } else {
                output += expr.identifier.lexeme;
            }
        }
        return "reference: " + output;
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitArrayElementExpr(Expr.ArrayElement expr) {
        return "(arrayElement " + expr.name + "[" + expr.index + "])";
    }

    @Override
    public String visitReadExpr(Expr.Read expr) {
        return "(read " + expr.name;
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    private String parenthesize2(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);

        for (Object part : parts) {
            builder.append(" ");

            if (part instanceof Expr) {
                builder.append(((Expr) part).accept(this));
            } else if (part instanceof Stmt) {
                builder.append(((Stmt) part).accept(this));
            } else if (part instanceof Token) {
                builder.append(((Token) part).lexeme);
            } else {
                builder.append(part);
            }
        }
        builder.append(")");

        return builder.toString();
    }
}

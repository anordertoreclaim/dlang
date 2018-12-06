package dlang;


import java.util.*;

import static dlang.TokenType.LEFT_BRACKET;
import static dlang.TokenType.LEFT_PAREN;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();

    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            DLang.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        if (expr instanceof Expr.Read && ((Expr.Read) expr).value != null) return ((Expr.Read) expr).value;
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBody(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBodyStmt(Stmt.Body stmt) {
        executeBody(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitAssignmentStmt(Stmt.Assignment stmt) {
        Object value = evaluate(stmt.right);

        Integer distance = locals.get(stmt.left);
        if (distance != null) {
            if (stmt.left instanceof Expr.Variable) {
                environment.assignAt(distance, ((Expr.Variable) stmt.left).name, value);
            } else if (stmt.left instanceof Expr.ArrayElement) {
                Double index = (Double) evaluate(((Expr.ArrayElement) stmt.left).index);
                if (index.intValue() == index) {
                    List<Object> array = (List) environment.get(((Expr.ArrayElement) stmt.left).name);
                    array.set(index.intValue() - 1, value);
                    environment.assignAt(distance, ((Expr.ArrayElement) stmt.left).name, array);
                } else {
                    throw new RuntimeError(new Token(LEFT_BRACKET, "left bracket", "left bracket", 0), "Index of type double");
                }
            }
        } else {
            if (stmt.left instanceof Expr.Variable) {
                globals.assign(((Expr.Variable) stmt.left).name, value);
            } else if (stmt.left instanceof Expr.ArrayElement) {
                Double index = (Double) evaluate(((Expr.ArrayElement) stmt.left).index);
                if (index.intValue() == index) {
                    List<Object> array = (List) globals.get(((Expr.ArrayElement) stmt.left).name);
                    array.set(index.intValue() - 1, value);
                    globals.assign(((Expr.ArrayElement) stmt.left).name, array);
                } else {
                    throw new RuntimeError(new Token(LEFT_BRACKET, "left bracket", "left bracket", 0), "Index of type double");
                }
            }
        }

        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }


    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }


    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }


    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        for (Stmt.Var.VarDecl varDecl : stmt.varDecls) {
            if (varDecl.initializer != null) {
                value = evaluate(varDecl.initializer);
            }
            environment.define(varDecl.name.lexeme, value);
        }

        return null;
    }


    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitReferenceStmt(Stmt.Reference stmt) {
        evaluate(stmt.reference);
        return null;
    }


    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }


    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof List) {
            List<Object> right = new ArrayList<>();
            List<Expr> values = (List) expr.value;
            values.forEach(i -> right.add(evaluate(i)));
            return right;
        } else {
            return expr.value;
        }
    }

    @Override
    public Object visitFunctionLiteralExpr(Expr.FunctionLiteral expr) {
        return expr;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case OR:
                return isTruthy(left) || isTruthy(right);
            case AND:
                return isTruthy(left) && isTruthy(right);
            case XOR:
                return isTruthy(left) ^ isTruthy(right);
            default:
                return null;
        }
    }

    @Override
    public Object visitRelationExpr(Expr.Relation expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case NOT_EQUAL:
                return !isEqual(left, right);
            case EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
        }
        // Unreachable.
        return null;
    }

    @Override
    public Object visitFactorExpr(Expr.Factor expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                if (left instanceof List && right instanceof List) {
                    ArrayList<Object> result = new ArrayList<>();

                    result.addAll((List) left);
                    result.addAll((List) right);

                    return result;
                }

                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
        }
        //Unreachable
        return null;
    }

    @Override
    public Object visitTermExpr(Expr.Term expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
        }
        //Unreachable
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object left = evaluate(expr.left);
        if (expr.operator != null) {
            if (expr.operator.type == TokenType.IS) {
                switch (expr.type) {
                    case INT:
                        return left instanceof Integer;
                    case BOOL:
                        return left instanceof Boolean;
                    case FUNC:
                        try {
                            Expr.FunctionLiteral func = (Expr.FunctionLiteral) left;
                            return true;
                        } catch (ClassCastException e) {
                            return false;
                        }
                    case REAL:
                        return left instanceof Double;
                    //TODO
                    case ARRAY:
                        return false;
                    case TUPLE:
                        return false;
                    case STRING:
                        return left instanceof String;
                    default:
                        return isEqual(left, null);
                }
            }
            switch (expr.operator.type) {
                case PLUS:
                case MINUS:
                    if (left instanceof Double) {
                        return -(Double) left;
                    } else {
                        throw new RuntimeError(expr.operator, "Incorrect operand for MINUS.");
                    }
                case NOT:
                    if (left instanceof Boolean) {
                        return !(Boolean) left;
                    } else {
                        throw new RuntimeError(expr.operator, "Incorrect operand for NOT.");
                    }
                default:
                    return left;
            }
        } else {
            return left;
        }
    }

    @Override
    public Object visitReferenceExpr(Expr.Reference expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == LEFT_PAREN) {
            try {
                if (left == null) {
                    throw new RuntimeError(expr.operator, "Undefined function.");
                }
                Expr.FunctionLiteral func = (Expr.FunctionLiteral) left;
                ;
                List<Token> funcParams = func.params;
                List<Expr> params = expr.exprList;
                List<Stmt> body = func.body;
                if (params.size() != funcParams.size()) {
                    throw new RuntimeError(expr.operator, "Incorrect number of parameters.");
                }
                List<Stmt> inits = new ArrayList<>();
                for (int i = 0; i < params.size(); i++) {
                    List<Stmt.Var.VarDecl> paramDeclaration = new ArrayList<>(1);
                    if (params.get(i) instanceof Expr.Literal) {
                        paramDeclaration.add(new Stmt.Var.VarDecl(funcParams.get(i), params.get(i)));
                    } else {
                        paramDeclaration.add(new Stmt.Var.VarDecl(funcParams.get(i), new Expr.Literal(evaluate(params.get(i)))));
                    }
                    Stmt.Var param = new Stmt.Var(paramDeclaration);
                    inits.add(param);
                }
                inits.addAll(body);
                try {
                    executeBody(inits, new Environment(environment));
                } catch (Return value) {
                    return value.value;
                }
                return null;
            } catch (ClassCastException e) {
                throw new RuntimeError(expr.operator, "Object not callable.");
            }

        } else {
            //TODO
            return null;
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    @Override
    public Object visitArrayElementExpr(Expr.ArrayElement expr) {
        return lookUpArrayElement(expr.name, expr, expr.index);
    }

    @Override
    public Object visitReadExpr(Expr.Read expr) {
        java.util.Scanner in = new java.util.Scanner(System.in).useLocale(Locale.US);

        Object value = null;

        try {
            switch (expr.name.type) {
                case READ_INT:
                    value = in.nextDouble();
                    break;
                case READ_REAL:
                    value = in.nextDouble();
                    break;
                case READ_STRING:
                    value = in.next();
                    break;
            }
        } catch (Exception ex) {
            throw new RuntimeError(expr.name, expr.name.lexeme + " input mismatch exception");
        }

        expr.value = value;

        return value;
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private Object lookUpArrayElement(Token name, Expr expr, Expr index) {
        Object indexEval = evaluate(index);
        if (!(indexEval instanceof Double)) {
            throw new RuntimeError(name, "Operand must be a number.");
        }

        Integer distance = locals.get(expr);

        if (distance != null) return environment.getAt(distance, name.lexeme);
        else return globals.get(name, ((Double) indexEval).intValue() - 1);
    }


    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }


    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }


    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}

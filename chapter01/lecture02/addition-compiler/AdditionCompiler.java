import java.util.*;

/**
 * 加法编译器：将加法算术表达式转换为栈式计算机指令
 * 语法如下：
 *  expr -> number
 *          | expr + number
 *          ;
 */
public class AdditionCompiler {
    // Token 类型
    private enum TokenType {
        EOF,
        PLUS,
        NUM
    }

    private static class Token {
        private final TokenType type;
        private final String lexeme;
        private final Object literal; // 字面量

        public Token(TokenType type, String lexeme, Object literal) {
            this.type = type;
            this.lexeme = lexeme;
            this.literal = literal;
        }

        @Override
        public String toString() {
            return "[Token - " + type + "] " + lexeme;
        }
    }

    private static abstract class Expr {
        interface Visitor<R> {
            R visitBinaryExpr(Binary expr);
            R visitNumExpr(Num expr);
        }

        abstract <R> R accept(Visitor<R> visitor);

        private static class Binary extends Expr {
            private final Expr left;
            private final Token operator;
            private final Expr right;

            public Binary(Expr left, Token operator, Expr right) {
                this.left = left;
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitBinaryExpr(this);
            }
        }

        private static class Num extends Expr {
            private final Object literal;

            public Num(Object literal) {
                this.literal = literal;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitNumExpr(this);
            }
        }
    }

    // 词法分析器
    private static class Scanner {
        private final String source;
        private int idx;

        public Scanner(String source) {
            this.source = source;
            this.idx = 0;
        }

        public List<Token> scanTokens() {
            List<Token> tokens = new ArrayList<>();
            do {
                tokens.add(scanToken());
            } while (tokens.get(tokens.size() - 1).type != TokenType.EOF);
            return tokens;
        }

        private Token scanToken() {
            while (true) {
                if (idx == source.length()) {
                    return new Token(TokenType.EOF, null, null);
                }
                char c = source.charAt(idx++);
                if (c == '+') {
                    return new Token(TokenType.PLUS, null, null);
                }
                if (c >= '0' && c <= '9') {
                    StringBuilder sb = new StringBuilder();
                    sb.append(c);
                    while (idx < source.length() && (c = source.charAt(idx++)) >= '0' && c <= '9') {
                        sb.append(c);
                    }
                    return new Token(TokenType.NUM, sb.toString(), Integer.parseInt(sb.toString()));
                }
            }
        }
    }

    private static class Parser {

        private final List<Token> tokens;
        private int current;

        /**
         * 查看当前 Token（跟数据结构栈中的 peek 语义类似，但这里 tokens 不是数据结构栈）
         */
        private Token peek() {
            return tokens.get(current);
        }

        /**
         * 检查是否是 EOF Token
         */
        private boolean isAtEnd() {
            return peek().type == TokenType.EOF;
        }

        /**
         * 检查当前 Token 的类型是否为 type
         */
        private boolean check(TokenType type) {
            if (isAtEnd()) return false;
            return peek().type == type;
        }

        private Token previous() {
            return tokens.get(current - 1);
        }

        /**
         * 前进一个 Token，并返回当前（前进前的）Token
         */
        private Token advance() {
            if (!isAtEnd()) current++;
            return previous();
        }

        /**
         * 检查当前 Token 的类型是否为 types 中的一种，若是则前进一个 Token，并返回 true；否则，返回 false
         */
        private boolean match(TokenType... types) {
            for (TokenType type : types) {
                if (check(type)) {
                    advance();
                    return true;
                }
            }
            return false;
        }

        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.current = 0;
        }

        public Expr parse() {
            return expr();
        }

        public Expr expr() {
            Expr expr = num();
            while (match(TokenType.PLUS)) {
                Token operator = previous();
                Expr right = num();
                expr = new Expr.Binary(expr, operator, right);
            }
            if (peek().type != TokenType.EOF) {
                System.err.println("[Parser Error] 当前期待一个 EOF 类型的 Token，实际 Token 类型为 " + peek().type);
                System.exit(1);
            }
            return expr;
        }

        private Expr num() {
            if (match(TokenType.NUM)) {
                return new Expr.Num(previous().literal);
            }
            System.err.println("[Parser Error] 当前期待一个 NUMBER 类型的 Token，实际 Token 类型为 " + peek().type);
            System.exit(1);
            return null;
        }
    }

    private static class Interpreter implements Expr.Visitor<Integer> {

        public Integer evaluate(Expr expr) {
            return expr.accept(this);
        }

        @Override
        public Integer visitBinaryExpr(Expr.Binary expr) {
            int left = evaluate(expr.left);
            int right = evaluate(expr.right);
            if (expr.operator.type == TokenType.PLUS) {
                return left + right;
            }
            System.err.println("[Interpreter Error] 非法操作符 " + expr.operator.type);
            System.exit(1);
            return null;
        }

        @Override
        public Integer visitNumExpr(Expr.Num expr) {
            return (Integer) (expr.literal);
        }
    }

    private static class Compiler implements Expr.Visitor<Void> {

        public Void evaluate(Expr expr) {
            expr.accept(this);
            return null;
        }

        @Override
        public Void visitBinaryExpr(Expr.Binary expr) {
            evaluate(expr.left);
            evaluate(expr.right);
            if (expr.operator.type == TokenType.PLUS) {
                System.out.println("add");
                return null;
            } 
            System.err.println("[Interpreter Error] 非法操作符 " + expr.operator.type);
            System.exit(1);
            return null;
        }

        @Override
        public Void visitNumExpr(Expr.Num expr) {
            int val = (Integer) (expr.literal);
            System.out.println("push " + val);
            return null;
        }
    }

    public static void main(String[] args) {
        String str = "11 + 22 + 33 - 55";
        Scanner scanner = new Scanner(str);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expr = parser.parse();
        // Interpreter interpreter = new Interpreter();
        Compiler compiler = new Compiler();
        compiler.evaluate(expr);
    }
}


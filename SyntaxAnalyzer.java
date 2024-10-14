import java.io.*;
import java.util.*;

public class SyntaxAnalyzer {

    private static Map<String, Object> variables = new HashMap<>();
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto",
            "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
    ));

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            StringBuilder codeBlock = new StringBuilder();
            System.out.println("\nWelcome to Syntax Analyzer! Input your code below: ");
            System.out.println("(Type 'done' on a new line to finish and analyze, or 'quit' to exit the program):");


            while (true) {
                String input = scanner.nextLine();
                if (input.trim().equalsIgnoreCase("done")) {
                    System.out.println("\nAnalyzing the code block...\n");
                    analyzeBlock(codeBlock.toString());

                    System.out.println("\n------------- End of analysis -------------\n");
                    break;
                }
                if (input.trim().equalsIgnoreCase("quit")) {
                    System.out.println("Exiting the program...");
                    scanner.close();
                    return;
                }
                codeBlock.append(input).append("\n");
            }
        }
    }

    private static void analyzeBlock(String codeBlock) {
        String[] lines = codeBlock.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            analyzeLine(lines[i].trim(), i + 1);
        }
    }

    private static void analyzeLine(String input, int lineNumber) {
        if (input.isEmpty()) {
            return;  // Ignore empty lines
        }

        try {
            StringReader reader = new StringReader(input);
            StreamTokenizer tokenizer = new StreamTokenizer(reader);
            tokenizer.ordinaryChar('.');
            tokenizer.ordinaryChar('-');
            tokenizer.ordinaryChar('/');
            tokenizer.wordChars('_', '_');
            tokenizer.wordChars('$', '$');
            tokenizer.quoteChar('"');
            tokenizer.quoteChar('\'');

            int token = tokenizer.nextToken();
            if (token == StreamTokenizer.TT_WORD) {
                String firstWord = tokenizer.sval;
                if (isPrimitiveType(firstWord) || firstWord.equals("String")) {
                    handleVariableDeclaration(tokenizer, firstWord, lineNumber);
                } else if (firstWord.equalsIgnoreCase("System")) {
                    handlePrintStatement(tokenizer, lineNumber);
                } else if (firstWord.equals("Scanner")) {
                    handleScannerDeclaration(tokenizer, lineNumber);
                } else if (variables.containsKey(firstWord)) {
                    handleVariableUsage(tokenizer, firstWord, lineNumber);
                } else {
                    System.out.println("Line " + lineNumber + ": Invalid syntax or undeclared variable: " + firstWord);
                }
            } else {
                System.out.println("Line " + lineNumber + ": Invalid syntax: Line does not start with a valid token");
            }
        } catch (IOException e) {
            System.out.println("Line " + lineNumber + ": Error: " + e.getMessage());
        }
    }

    private static boolean isPrimitiveType(String word) {
        return word.equals("byte") || word.equals("short") || word.equals("int") || word.equals("long") ||
                word.equals("float") || word.equals("double") || word.equals("char") || word.equals("boolean");
    }

    private static boolean isValidVariableName(String varName) {
        if (RESERVED_WORDS.contains(varName)) {
            return false;
        }
        if (!Character.isLetter(varName.charAt(0)) && varName.charAt(0) != '$' && varName.charAt(0) != '_') {
            return false;
        }
        for (int i = 1; i < varName.length(); i++) {
            char ch = varName.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '$' && ch != '_') {
                return false;
            }
        }
        return true;
    }

    private static void handleVariableDeclaration(StreamTokenizer tokenizer, String varType, int lineNumber) throws IOException {
        int token = tokenizer.nextToken();
        if (token == StreamTokenizer.TT_WORD) {
            String varName = tokenizer.sval;
            if (!isValidVariableName(varName)) {
                System.out.println("Line " + lineNumber + ": Invalid variable name: " + varName);
                return;
            }

            token = tokenizer.nextToken();
            if (token == '=') {
                tokenizer.nextToken();
                Object value = parseValue(tokenizer, varType);
                if (value != null) {
                    variables.put(varName, value);
                    System.out.println("Line " + lineNumber + ": Syntax is Valid, " + varType + " " + varName + " = " + value);
                } else {
                    System.out.println("Line " + lineNumber + ": Invalid value for " + varType + " " + varName);
                }
            } else {
                System.out.println("Line " + lineNumber + ": Syntax error: Expected '=' after variable name.");
            }
        } else {
            System.out.println("Line " + lineNumber + ": Syntax error: Expected variable name.");
        }
    }

    private static Object parseValue(StreamTokenizer tokenizer, String varType) throws IOException {
        switch (varType) {
            case "byte":
            case "short":
            case "int":
            case "long":
                if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                    return (int) tokenizer.nval;
                }
                break;
            case "float":
            case "double":
                if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                    return tokenizer.nval;
                }
                break;
            case "boolean":
                if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
                    if (tokenizer.sval.equals("true") || tokenizer.sval.equals("false")) {
                        return Boolean.parseBoolean(tokenizer.sval);
                    }
                }
                break;
            case "char":
                if (tokenizer.ttype == '\'') {
                    return tokenizer.sval.charAt(0);
                }
                break;
            case "String":
                if (tokenizer.ttype == '"') {
                    return tokenizer.sval;
                }
                break;
        }
        return null;
    }

    private static void handlePrintStatement(StreamTokenizer tokenizer, int lineNumber) throws IOException {
        int token = tokenizer.nextToken();
        if (token != '.') {
            System.out.println("Line " + lineNumber + ": Invalid syntax: Expected '.' after 'System'");
            return;
        }

        token = tokenizer.nextToken();
        if (token != StreamTokenizer.TT_WORD || (!tokenizer.sval.equals("out") && !tokenizer.sval.equals("err"))) {
            System.out.println("Line " + lineNumber + ": Invalid syntax: Expected 'out' or 'err' after 'System.'");
            return;
        }

        String outputStream = tokenizer.sval;

        token = tokenizer.nextToken();
        if (token != '.') {
            System.out.println("Line " + lineNumber + ": Invalid syntax: Expected '.' after 'System." + outputStream + "'");
            return;
        }

        token = tokenizer.nextToken();
        if (token != StreamTokenizer.TT_WORD ||
                (!tokenizer.sval.equals("print") && !tokenizer.sval.equals("println") && !tokenizer.sval.equals("printf"))) {
            System.out.println("Line " + lineNumber + ": Invalid print statement: " + tokenizer.sval);
            return;
        }

        String printType = tokenizer.sval;

        token = tokenizer.nextToken();
        if (token != '(') {
            System.out.println("Line " + lineNumber + ": Invalid syntax: Expected '(' after '" + printType + "'");
            return;
        }

        StringBuilder expression = new StringBuilder();
        boolean insideString = false;
        int parenCount = 1;

        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
                case '"':
                    insideString = !insideString;
                    expression.append('"').append(tokenizer.sval).append('"');
                    break;
                case '\'':
                    expression.append('\'').append(tokenizer.sval).append('\'');
                    break;
                case StreamTokenizer.TT_NUMBER:
                    expression.append(tokenizer.nval);
                    break;
                case StreamTokenizer.TT_WORD:
                    if (tokenizer.sval.equals("true") || tokenizer.sval.equals("false")) {
                        expression.append(tokenizer.sval);
                    } else if (variables.containsKey(tokenizer.sval)) {
                        expression.append(variables.get(tokenizer.sval));
                    } else {
                        expression.append(tokenizer.sval);
                    }
                    break;
                case '(':
                    parenCount++;
                    expression.append('(');
                    break;
                case ')':
                    parenCount--;
                    if (parenCount == 0) {
                        String result = evaluateExpression(expression.toString());
                        System.out.println("Line " + lineNumber + ": Syntax is Valid, System." + outputStream + "." + printType + " statement, Output: " + result);
                        return;
                    }
                    expression.append(')');
                    break;
                default:
                    expression.append((char) token);
                    break;
            }
        }

        System.out.println("Line " + lineNumber + ": Invalid print statement: Unmatched parentheses");
    }

    private static String evaluateExpression(String expression) {
        try {
            // Try to evaluate the expression as a mathematical operation first
            double result = evaluateMathExpression(expression);
            // Check if the result is a whole number or not
            if (result == (int) result) {
                return String.valueOf((int) result);
            } else {
                return String.valueOf(result);
            }
        } catch (Exception e) {
            // If not a math expression, fall back to string concatenation handling
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                StringBuilder result = new StringBuilder();
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("\"") && part.endsWith("\"")) {
                        result.append(part.substring(1, part.length() - 1));
                    } else if (variables.containsKey(part)) {
                        result.append(variables.get(part));
                    } else {
                        result.append(part);
                    }
                }
                return result.toString();
            }
        }
        return expression;
    }


    //lowkey I think we can remove this method na but I dont want to sira
    private static String evaluateSimpleExpression(String expression) {
        // Handle boolean values
        if (expression.equals("true") || expression.equals("false")) {
            return expression;
        }

        // Handle character literals
        if (expression.startsWith("'") && expression.endsWith("'") && expression.length() == 3) {
            return expression.substring(1, 2);
        }

        // Handle variables
        if (variables.containsKey(expression)) {
            return String.valueOf(variables.get(expression));
        }

        // If it's not a boolean, character, or variable, try to evaluate as a math expression
        try {
            double result = evaluateMathExpression(expression);
            if (result == (int) result) {
                return String.valueOf((int) result);  // Return as integer if it's a whole number
            } else {
                return String.valueOf(result);  // Return as double with full precision
            }
        } catch (Exception e) {
            // If it's not a valid math expression, return as is (likely a string)
            return expression.replaceAll("\"", ""); // Remove quotes from string literals
        }
    }

    private static double evaluateMathExpression(String expression) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double y = parseFactor();
                        if (y == 0) throw new ArithmeticException("Division by zero");
                        x /= y;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }

    private static void handleScannerDeclaration(StreamTokenizer tokenizer, int lineNumber) throws IOException {
        tokenizer.nextToken(); // variable name
        String varName = tokenizer.sval;
        if (!isValidVariableName(varName)) {
            System.out.println("Line " + lineNumber + ": Invalid Scanner variable name: " + varName);
            return;
        }
        tokenizer.nextToken(); // =
        tokenizer.nextToken(); // new
        tokenizer.nextToken(); // Scanner
        tokenizer.nextToken(); // (
        tokenizer.nextToken(); // System
        tokenizer.nextToken(); // .
        tokenizer.nextToken(); // in
        tokenizer.nextToken(); // )
        tokenizer.nextToken(); // ;

        if (tokenizer.ttype == ';') {
            System.out.println("Line " + lineNumber + ": Syntax is Valid, Scanner declared - " + varName);
        } else {
            System.out.println("Line " + lineNumber + ": Invalid Scanner declaration");
        }
    }

    private static void handleVariableUsage(StreamTokenizer tokenizer, String varName, int lineNumber) throws IOException {
        int nextToken = tokenizer.nextToken();
        if (nextToken == '=') {
            tokenizer.nextToken();
            Object value = parseValue(tokenizer, getType(variables.get(varName)));
            if (value != null) {
                variables.put(varName, value);
                System.out.println("Line " + lineNumber + ": Syntax is Valid, " + varName + " = " + value);
            } else {
                System.out.println("Line " + lineNumber + ": Invalid value assignment for " + varName);
            }
        } else {
            System.out.println("Line " + lineNumber + ": Syntax is Valid, Variable used: " + varName + " = " + variables.get(varName));
        }
    }

    private static String getType(Object value) {
        if (value instanceof Byte) return "byte";
        if (value instanceof Short) return "short";
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        if (value instanceof Float) return "float";
        if (value instanceof Double) return "double";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Character) return "char";
        if (value instanceof String) return "String";
        return "unknown";
    }
}
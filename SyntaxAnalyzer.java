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
        if (input.isEmpty() || input.startsWith("//")) {
            System.out.println(input);
            return;  // Ignore empty lines
        }

        // Check for unclosed parentheses
        int openParentheses = 0;
        for (char ch : input.toCharArray()) {
            if (ch == '(') {
                openParentheses++;
            } else if (ch == ')') {
                openParentheses--;
            }
        }

        // Report if there are unclosed parentheses
        if (openParentheses > 0) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Unclosed parenthesis.");
            return; // Exit after reporting the error
        }

        if (input.contains("Scanner")) {
            handleScannerDeclaration(input, lineNumber);
            return;
        }

        if (!input.endsWith(";") || input.endsWith(";;")) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Missing semicolon or there's something after the semicolon");
            return;
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
                } else if (variables.containsKey(firstWord)) {
                    handleVariableUsage(tokenizer, firstWord, lineNumber);
                } else {
                    System.out.println("Line " + lineNumber + ": Invalid Syntax: undeclared variable: " + firstWord);
                }
            } else {
                System.out.println("Line " + lineNumber + ": Invalid Syntax: Line does not start with a valid token");
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
                System.out.println("Line " + lineNumber + ": Invalid Syntax: invalid variable name: " + varName);
                return;
            }

            token = tokenizer.nextToken();
            if (token == '=') {
                tokenizer.nextToken();
                Object value = parseValue(tokenizer, varType);
                if (value == null) {
                    System.out.println("Line " + lineNumber + ": Invalid Syntax, mismatched data type for " + varType + " " + varName);
                    return;
                }

                // Check for semicolon after the value
                token = tokenizer.nextToken();
                if (token == ';') {
                    // Check for any unexpected tokens after the semicolon
                    token = tokenizer.nextToken();
                    if (token != StreamTokenizer.TT_EOL && token != StreamTokenizer.TT_EOF) {
                        System.out.println("Line " + lineNumber + ": Invalid Syntax: Unexpected token after ';'.");
                        return;
                    }
                    // Now safe to declare syntax valid after confirming no extra tokens
                    variables.put(varName, value);
                    System.out.println("Line " + lineNumber + ": Syntax is Valid, " + varType + " " + varName + " = " + value);
                } else {
                    System.out.println("Line " + lineNumber + ": Invalid Syntax : Expected ';' after value.");
                }

            } else {
                System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected '=' after variable name.");
            }
        } else {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected variable name.");
        }
    }


    private static Object parseValue(StreamTokenizer tokenizer, String varType) throws IOException {
        switch (varType) {
            case "byte":
            case "short":
            case "int":
                if (tokenizer.ttype == StreamTokenizer.TT_NUMBER && tokenizer.nval == (int) tokenizer.nval) {
                    return (int) tokenizer.nval;
                } else {
                    System.out.println("Invalid value for int or related types.");
                    return null;
                }
            case "long":
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
                if (tokenizer.ttype == '\'' && tokenizer.sval.length() == 1) {
                    return tokenizer.sval.charAt(0);
                } else {
                    System.out.println("Invalid char value.");
                    return null;
                }
            case "String":
                if (tokenizer.ttype == '"') {
                    return tokenizer.sval;
                } else {
                    System.out.println("Invalid String value: Missing double quotes.");
                    return null;
                }
        }
        return null;
    }

    private static void handlePrintStatement(StreamTokenizer tokenizer, int lineNumber) throws IOException {
        int token = tokenizer.nextToken();
        if (token != '.') {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected '.' after 'System'");
            return;
        }

        token = tokenizer.nextToken();
        if (token != StreamTokenizer.TT_WORD || (!tokenizer.sval.equals("out") && !tokenizer.sval.equals("err"))) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected 'out' or 'err' after 'System.'");
            return;
        }

        String outputStream = tokenizer.sval;

        token = tokenizer.nextToken();
        if (token != '.') {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected '.' after 'System." + outputStream + "'");
            return;
        }

        token = tokenizer.nextToken();
        if (token != StreamTokenizer.TT_WORD ||
                (!tokenizer.sval.equals("print") && !tokenizer.sval.equals("println") && !tokenizer.sval.equals("printf"))) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: print statement: " + tokenizer.sval);
            return;
        }

        String printType = tokenizer.sval;

        token = tokenizer.nextToken();
        if (token != '(') {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected '(' after '" + printType + "'");
            return;
        }

        StringBuilder expression = new StringBuilder();
        boolean insideString = false;
        int parenCount = 1;

        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
                case '"':
                    insideString = !insideString;
                    if (!insideString) {
                        expression.append('"');
                    } else {
                        expression.append('"').append(tokenizer.sval).append('"');
                    }
                    break;
                case '\'':
                    // Check for valid character literal (single character within single quotes)
                    if (tokenizer.sval.length() == 1) {
                        expression.append('\'').append(tokenizer.sval).append('\'');
                    } else {
                        System.out.println("Line " + lineNumber + ": Invalid char literal: Char literals can only be one character.");
                        return;
                    }
                    break;
                case StreamTokenizer.TT_NUMBER:
                    expression.append(tokenizer.nval);
                    break;
                case StreamTokenizer.TT_WORD:
                    if (tokenizer.sval.equals("true") || tokenizer.sval.equals("false") || tokenizer.sval.equals("null")) {
                        expression.append(tokenizer.sval);
                    } else if (variables.containsKey(tokenizer.sval)) {
                        expression.append(variables.get(tokenizer.sval));
                    } else {
                        System.out.println("Line " + lineNumber + ": Invalid Syntax: variable or undeclared identifier: " + tokenizer.sval);
                        return;
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
                case ';':
                    // Check for unexpected tokens after the semicolon
                    token = tokenizer.nextToken();
                    if (token != StreamTokenizer.TT_EOF) {
                        System.out.println("Line " + lineNumber + ": Invalid Syntax: Unexpected token after ';'.");
                        return;
                    }
                    // If no unexpected tokens, break out of the loop
                    return;

                default:
                    expression.append((char) token);
                    break;
            }
        }

        if (parenCount != 0) {
            System.out.println("Line " + lineNumber + ": Invalid print statement: Unmatched parentheses");
        }
    }


    private static String evaluateExpression(String expression) {
        // Handle string concatenation
        if (expression.contains("+") && expression.contains("\"")) {
            String[] parts = expression.split("\\+");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    result.append(part.substring(1, part.length() - 1));
                } else {
                    result.append(evaluateExpression(part));
                }
            }
            return result.toString();
        }

        // Handle boolean values
        if (expression.equals("true") || expression.equals("false")) {
            return expression;
        }

        // Handle character literals
        if (expression.startsWith("'") && expression.endsWith("'") && expression.length() == 3) {
            return expression.substring(1, 2);
        }

        // If it's not a boolean or character, try to evaluate as a math expression
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

    private static void handleScannerDeclaration(String line, int lineNumber) {
        // Check for valid Scanner declaration
        if (line.trim().equals("Scanner scan = new Scanner(System.in);")) {
            System.out.println("Line " + lineNumber + ": Syntax is Valid");
        }
        // Check for invalid syntax - missing "in"
        else if (!(line.contains("(System.in)"))) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: needs (System.in)");
        }
        // Check for invalid scanner declaration - variable name issue
        else if (line.startsWith("Scan ") || line.startsWith("scan ")) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Scanner not properly declared");
        }
        // Check for extra characters after the semicolon
        else if (!(line.endsWith(";"))) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: there's something after the semicolon");
        }
        // Check for missing "new" in scanner declaration
        else if (line.contains("Scanner") && !line.contains("new Scanner")) {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: need to declare new scanner object");
        }
        // Handle any other cases if needed
        else {
            System.out.println("Line " + lineNumber + ": Invalid Syntax: Unrecognized Scanner declaration");
        }
    }


    private static void handleVariableUsage(StreamTokenizer tokenizer, String varName, int lineNumber) throws IOException {
        int nextToken = tokenizer.nextToken();
        if (nextToken == '=') {
            tokenizer.nextToken();
            Object value = parseValue(tokenizer, getType(variables.get(varName)));
            if (value == null) {
                System.out.println("Line " + lineNumber + ": Invalid value assignment for " + varName);
                return;
            }

            // Check for semicolon after the value
            nextToken = tokenizer.nextToken();
            if (nextToken == ';') {
                // Check for any unexpected tokens after the semicolon
                nextToken = tokenizer.nextToken();
                if (nextToken != StreamTokenizer.TT_EOL && nextToken != StreamTokenizer.TT_EOF) {
                    System.out.println("Line " + lineNumber + ": Invalid Syntax: Unexpected token after ';'.");
                    return;
                }
                // Now safe to declare syntax valid after confirming no extra tokens
                variables.put(varName, value);
                System.out.println("Line " + lineNumber + ": Syntax is Valid, " + varName + " = " + value);
            } else {
                System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected ';' after value.");
            }

        } else {
            System.out.println("Line " + lineNumber + ": Syntax is Valid, Variable used: " + varName + " = " + variables.get(varName));

            // Check for semicolon after the usage
            nextToken = tokenizer.nextToken();
            if (nextToken == ';') {
                // Check for any unexpected tokens after the semicolon
                nextToken = tokenizer.nextToken();
                if (nextToken != StreamTokenizer.TT_EOL && nextToken != StreamTokenizer.TT_EOF) {
                    System.out.println("Line " + lineNumber + ": Invalid Syntax: Unexpected token after ';'.");
                }
            } else {
                System.out.println("Line " + lineNumber + ": Invalid Syntax: Expected ';' after variable usage.");
            }
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
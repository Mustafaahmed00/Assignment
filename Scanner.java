package jminusminus;

import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import static jminusminus.TokenKind.*;

/**
 * A lexical analyzer for j--, that has no backtracking mechanism.
 */
class Scanner {
    /**
     * End of file character.
     */
    public final static char EOFCH = CharReader.EOFCH;

    // Keywords in j--.
    private final Hashtable<String, TokenKind> reserved;

    // Source characters.
    private final CharReader input;

    // Next unscanned character.
    private char ch;

    // Whether a scanner error has been found.
    private boolean isInError;

    // Source file name.
    private final String fileName;

    // Line number of current token.
    private int line;

    /**
     * Constructs a Scanner from a file name.
     *
     * @param fileName name of the source file.
     * @throws FileNotFoundException when the named file cannot be found.
     */
    public Scanner(String fileName) throws FileNotFoundException {
        this.input = new CharReader(fileName);
        this.fileName = fileName;
        isInError = false;

        // Initialize keywords in j--
        reserved = new Hashtable<>();
        reserved.put(ABSTRACT.image(), ABSTRACT);
        reserved.put(BOOLEAN.image(), BOOLEAN);
        reserved.put(CHAR.image(), CHAR);
        reserved.put(CLASS.image(), CLASS);
        reserved.put(ELSE.image(), ELSE);
        reserved.put(EXTENDS.image(), EXTENDS);
        reserved.put(FALSE.image(), FALSE);
        reserved.put(IF.image(), IF);
        reserved.put(IMPORT.image(), IMPORT);
        reserved.put(INSTANCEOF.image(), INSTANCEOF);
        reserved.put(INT.image(), INT);
        reserved.put(NEW.image(), NEW);
        reserved.put(NULL.image(), NULL);
        reserved.put(PACKAGE.image(), PACKAGE);
        reserved.put(PRIVATE.image(), PRIVATE);
        reserved.put(PROTECTED.image(), PROTECTED);
        reserved.put(PUBLIC.image(), PUBLIC);
        reserved.put(RETURN.image(), RETURN);
        reserved.put(STATIC.image(), STATIC);
        reserved.put(SUPER.image(), SUPER);
        reserved.put(THIS.image(), THIS);
        reserved.put(TRUE.image(), TRUE);
        reserved.put(VOID.image(), VOID);
        reserved.put(WHILE.image(), WHILE);

        // Added for Problem 2: New Reserved Words
        reserved.put(BREAK.image(), BREAK);
        reserved.put(CASE.image(), CASE);
        reserved.put(CONTINUE.image(), CONTINUE);
        reserved.put(DEFAULT.image(), DEFAULT);
        reserved.put(DO.image(), DO);
        reserved.put(DOUBLE.image(), DOUBLE);
        reserved.put(FOR.image(), FOR);
        reserved.put(LONG.image(), LONG);
        reserved.put(SWITCH.image(), SWITCH);

        // Prime the pump.
        nextCh();
    }

    /**
     * Scans and returns the next token from input.
     *
     * @return the next scanned token.
     */
    public TokenInfo getNextToken() {
        StringBuilder buffer;
        boolean moreWhiteSpace = true;
        while (moreWhiteSpace) {
            // Skip any whitespace characters
            while (isWhitespace(ch)) {
                nextCh();
            }

            if (ch == '/') {
                nextCh();
                if (ch == '/') {
                    // CharReader maps all new lines to '\n'.
                    while (ch != '\n' && ch != EOFCH) {
                        nextCh();
                    }
                } else if (ch == '*') { //multi-line
                    while (ch != EOFCH) {
                        nextCh();
                        if (ch == '*') {
                            nextCh();
                            if (ch == '/') {
                                nextCh();
                                break;
                            }
                        }
                    }
                } else if (ch == '=') {
                    // Handle '/=' operator
                    nextCh();
                    return new TokenInfo(DIV_ASSIGN, line);
                } else {
                    // '/' is not part of a comment; it's the DIV operator
                    return new TokenInfo(DIV, line);
                }
            } else {
                moreWhiteSpace = false;
            }
        }

        // Proceed to tokenize the next character
        switch (ch) {
            case EOFCH:
                return new TokenInfo(EOF, line);
            case ',':
                nextCh();
                return new TokenInfo(COMMA, line);
            case '[':
                nextCh();
                return new TokenInfo(LBRACK, line);
            case '{':
                nextCh();
                return new TokenInfo(LCURLY, line);
            case '(':
                nextCh();
                return new TokenInfo(LPAREN, line);
            case ']':
                nextCh();
                return new TokenInfo(RBRACK, line);
            case '}':
                nextCh();
                return new TokenInfo(RCURLY, line);
            case ')':
                nextCh();
                return new TokenInfo(RPAREN, line);
            case ';':
                nextCh();
                return new TokenInfo(SEMI, line);

            case '?':
                nextCh();
                return new TokenInfo(QUESTION, line);
            case ':':
                nextCh();
                return new TokenInfo(COLON, line);
            case '-':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(MINUS_ASSIGN, line);
                } else if (ch == '-') {
                    nextCh();
                    return new TokenInfo(DEC, line);
                } else {
                    return new TokenInfo(MINUS, line);
                }

            case '+':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(PLUS_ASSIGN, line);
                } else if (ch == '+') {
                    nextCh();
                    return new TokenInfo(INC, line);
                } else {
                    return new TokenInfo(PLUS, line);
                }
            case '*':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(STAR_ASSIGN, line);
                } else {
                    return new TokenInfo(STAR, line);
                }

            case '.':
                nextCh(); // Consume '.'
                if (isDigit(ch)) {
                    buffer = new StringBuilder();
                    buffer.append('.');
                    // Scan fractional digits
                    while (isDigit(ch)) {
                        buffer.append(ch);
                        nextCh();
                    }

                    // Check for exponent
                    if (ch == 'e' || ch == 'E') {
                        buffer.append(ch);
                        nextCh();
                        // Optional '+' or '-' after 'e'/'E'
                        if (ch == '+' || ch == '-') {
                            buffer.append(ch);
                            nextCh();
                        }
                        // Must have at least one digit after 'e'/'E'
                        if (isDigit(ch)) {
                            while (isDigit(ch)) {
                                buffer.append(ch);
                                nextCh();
                            }
                        } else {
                            reportScannerError("Invalid exponent format");
                        }
                    }

                    // Check for suffix
                    if (ch == 'D' || ch == 'd') {
                        buffer.append(ch);
                        nextCh();
                        return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                    }

                    // Determine token kind
                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                } else {
                    // It's just a DOT token
                    return new TokenInfo(DOT, line);
                }

            case '%':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(MOD_ASSIGN, line);
                } else {
                    return new TokenInfo(MOD, line);
                }
            case '!':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(NOT_EQUAL, line);
                } else {
                    return new TokenInfo(LNOT, line);
                }
            case '>':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(GREATER_EQUAL, line);
                } else {
                    return new TokenInfo(GT, line);
                }
            case '<':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(LE, line);
                } else {
                    return new TokenInfo(LT, line);
                }
            case '|':
                nextCh();
                if (ch == '|') {
                    nextCh();
                    return new TokenInfo(OR_OR, line);
                } else {
                    reportScannerError("operator | is not supported in j--");
                    return getNextToken();
                }
            case '&':
                nextCh();
                if (ch == '&') {
                    nextCh();
                    return new TokenInfo(LAND, line);
                } else {
                    return new TokenInfo(AMPERSAND, line);
                }
            case '~':
                nextCh();
                return new TokenInfo(TILDE, line);

            case '=':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(EQUAL, line);
                } else {
                    return new TokenInfo(ASSIGN, line);
                }
            case '\'':
                buffer = new StringBuilder();
                buffer.append('\'');
                nextCh();
                if (ch == '\\') {
                    nextCh();
                    buffer.append(escape());
                } else {
                    buffer.append(ch);
                    nextCh();
                }
                if (ch == '\'') {
                    buffer.append('\'');
                    nextCh();
                    return new TokenInfo(CHAR_LITERAL, buffer.toString(), line);
                } else {
                    reportScannerError(ch + " found by scanner where closing ' was expected");
                    while (ch != '\'' && ch != '\n' && ch != EOFCH) {
                        nextCh();
                    }
                    if (ch == '\'') {
                        buffer.append('\'');
                        nextCh();
                    }
                    return new TokenInfo(CHAR_LITERAL, buffer.toString(), line);
                }
            case '"':
                buffer = new StringBuilder();
                buffer.append("\"");
                nextCh();
                while (ch != '"' && ch != '\n' && ch != EOFCH) {
                    if (ch == '\\') {
                        nextCh();
                        buffer.append(escape());
                    } else {
                        buffer.append(ch);
                        nextCh();
                    }
                }
                if (ch == '\n') {
                    reportScannerError("unexpected end of line found in string");
                } else if (ch == EOFCH) {
                    reportScannerError("unexpected end of file found in string");
                } else {
                    // Scan the closing "
                    nextCh();
                    buffer.append("\"");
                }
                return new TokenInfo(STRING_LITERAL, buffer.toString(), line);

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                buffer = new StringBuilder();
                boolean isDouble = false;
                boolean isLong = false;
                boolean hasExponent = false;

                // Scan integer part
                while (isDigit(ch)) {
                    buffer.append(ch);
                    nextCh();
                }

                // Check for decimal point
                if (ch == '.') {
                    isDouble = true;
                    buffer.append(ch);
                    nextCh();
                    // Scan fractional part
                    while (isDigit(ch)) {
                        buffer.append(ch);
                        nextCh();
                    }
                }

                // Check for exponent
                if (ch == 'e' || ch == 'E') {
                    isDouble = true;
                    hasExponent = true;
                    buffer.append(ch);
                    nextCh();
                    // Optional '+' or '-' after 'e'/'E'
                    if (ch == '+' || ch == '-') {
                        buffer.append(ch);
                        nextCh();
                    }
                    // Must have at least one digit after 'e'/'E'
                    if (isDigit(ch)) {
                        while (isDigit(ch)) {
                            buffer.append(ch);
                            nextCh();
                        }
                    } else {
                        reportScannerError("Invalid exponent format");
                    }
                }

                // Check for suffix
                if (ch == 'L' || ch == 'l') {
                    isLong = true;
                    buffer.append(ch);
                    nextCh();
                    return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                } else if (ch == 'D' || ch == 'd') {
                    isDouble = true;
                    buffer.append(ch);
                    nextCh();
                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                }

                // Determine the token kind based on flags
                if (isDouble) {
                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                } else {
                    return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                }

            default:
                if (isIdentifierStart(ch)) {
                    buffer = new StringBuilder();
                    while (isIdentifierPart(ch)) {
                        buffer.append(ch);
                        nextCh();
                    }
                    String identifier = buffer.toString();
                    if (reserved.containsKey(identifier)) {
                        // Return the reserved word token
                        return new TokenInfo(reserved.get(identifier), line);
                    } else {
                        // Return an identifier token
                        return new TokenInfo(IDENTIFIER, identifier, line);
                    }
                } else {
                    reportScannerError("Unidentified input token: '%c'", ch);
                    nextCh();
                    return getNextToken(); // Attempt to recover by getting the next token
                }
        }
    }

    /**
     * @return true if an error has occurred, and false otherwise.
     */
    public boolean errorHasOccurred() {
        return isInError;
    }

    /**
     * @return the name of the source file.
     */
    public String fileName() {
        return fileName;
    }

    // Scans and returns an escaped character.
    private String escape() {
        switch (ch) {
            case 'b':
                nextCh();
                return "\\b";
            case 't':
                nextCh();
                return "\\t";
            case 'n':
                nextCh();
                return "\\n";
            case 'f':
                nextCh();
                return "\\f";
            case 'r':
                nextCh();
                return "\\r";
            case '"':
                nextCh();
                return "\\\"";
            case '\'':
                nextCh();
                return "\\'";
            case '\\':
                nextCh();
                return "\\\\";
            default:
                reportScannerError("Badly formed escape: \\" + ch);
                nextCh();
                return "";
        }
    }

    // Advances ch to the next character from input, and updates the line number.
    private void nextCh() {
        try {
            ch = input.nextChar();
            line = input.line();
        } catch (Exception e) {
            reportScannerError("unable to read characters from input");
            ch = EOFCH; // To prevent infinite loops
        }
    }

    // Reports a lexical error and records the fact that an error has occurred.
    private void reportScannerError(String message, Object... args) {
        isInError = true;
        System.err.printf("%s:%d: error: ", fileName, line);
        System.err.printf(message, args);
        System.err.println();
    }

    // Returns true if the specified character is a digit (0-9), and false otherwise.
    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    // Returns true if the specified character is whitespace, and false otherwise.
    private boolean isWhitespace(char c) {
        return (c == ' ' || c == '\t' || c == '\n' || c == '\f');
    }

    // Returns true if the specified character can start an identifier name, and false otherwise.
    private boolean isIdentifierStart(char c) {
        return Character.isJavaIdentifierStart(c);
    }

    // Returns true if the specified character can be part of an identifier name, and false otherwise.
    private boolean isIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}

/**
 * A buffered character reader, which abstracts out differences between platforms,
 * mapping all new lines to '\n', and also keeps track of line numbers.
 */
class CharReader {
    /**
     * Representation of the end of file as a character.
     */
    public final static char EOFCH = (char) -1;

    // The underlying reader records line numbers.
    private final LineNumberReader lineNumberReader;

    // Name of the file that is being read.
    private final String fileName;

    /**
     * Constructs a CharReader from a file name.
     *
     * @param fileName the name of the input file.
     * @throws FileNotFoundException if the file is not found.
     */
    public CharReader(String fileName) throws FileNotFoundException {
        lineNumberReader = new LineNumberReader(new FileReader(fileName));
        this.fileName = fileName;
    }

    /**
     * Scans and returns the next character.
     *
     * @return the character scanned.
     * @throws IOException if an I/O error occurs.
     */
    public char nextChar() throws IOException {
        int next = lineNumberReader.read();
        if (next == -1) {
            return EOFCH;
        } else {
            return (char) next;
        }
    }

    /**
     * Returns the current line number in the source file.
     *
     * @return the current line number in the source file.
     */
    public int line() {
        return lineNumberReader.getLineNumber() + 1; // LineNumberReader counts lines from 0
    }

    /**
     * Returns the file name.
     *
     * @return the file name.
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Closes the file.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        lineNumberReader.close();
    }
}

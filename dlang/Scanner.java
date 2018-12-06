package dlang;

import java.util.*;

import static dlang.TokenType.*; // [static-import]

class Scanner {
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("if",     IF);
    keywords.put("empty",    EMPTY);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("true",   TRUE);
    keywords.put("var",    VAR);
    keywords.put("while",  WHILE);
    keywords.put("func",    FUNC);
    keywords.put("not",    NOT);
    keywords.put("xor",     XOR);
    keywords.put("int",   INT);
    keywords.put("real",    REAL);
    keywords.put("bool",    BOOL);
    keywords.put("string",  STRING);
    keywords.put("loop",    LOOP);
    keywords.put("end",     END);
    keywords.put("then",    THEN);
    keywords.put("is",      IS);
    keywords.put("in",      IN);
    keywords.put("readInt", READ_INT);
    keywords.put("readReal",READ_REAL);
    keywords.put("readString",READ_STRING);
  }
  private final String source;
  private final List<Token> tokens = new ArrayList<>();

  // scan states
  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source){

    this.source = source;
  }
  // scan process
  List<Token> scanTokens() {

    while (!isAtEnd()) {

      // We are at the beginning of the next lexeme?
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    System.out.println((tokens));
    return tokens;
  }

  // token scan
  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case '[': addToken(LEFT_BRACKET); break;
      case ']': addToken(RIGHT_BRACKET); break;
      case ',': addToken(COMMA); break;
      case '.':
        if (match('.')) {
          Token identifier = getLastTokenByTokenType(IDENTIFIER);

          // if it just double dot, we pass it, but further it will be handled
          if (identifier == null) {
            addToken(DOUBLE_DOT);
            break;
          }

          // rewrite cycle format
          String lexeme =  identifier.lexeme;
          addToken(SEMICOLON, ";", null);
          addToken(IDENTIFIER, lexeme, null);
          addToken(ASSIGN, ":=", null);
          addToken(IDENTIFIER, lexeme, null);
          addToken(PLUS, "+", null);
          addToken(NUMBER, "1", 1.0);
          addToken(SEMICOLON, ";", null);
          addToken(IDENTIFIER, lexeme, null);
          addToken(LESS, "<", null);
        }
        else addToken(DOT);
        break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;

      case '=': addToken(match('>') ? LAMBDA : EQUAL); break;
      case ':': if(match('=')) addToken(ASSIGN); break;
      case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

      case '/':
        if (match('/')) {
          // skip line
          while (peek() != '\n' && !isAtEnd()) advance();
        }
        else {
          addToken(match('=') ? NOT_EQUAL : SLASH);
        }
        break;
      // skip whitespaces
      case ' ':
      case '\r':
      case '\t':
        break;
      case '\n':
        line++;
        break;

      // string
      case '"': string(); break;
      default:
        // number
        if (isDigit(c)) {
          number();
        }
        // name
        else if (isAlphabet(c)) {
          identifier();
        }
        else {
          DLang.error(line, "wrong character");
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphabetOrNumeric(peek())) advance();

    // check reserved word
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    // for cycle format
    if (type == IN) {
      addToken(ASSIGN, ":=", null);
    }
    else {
      if (type == null) type = IDENTIFIER;
      addToken(type);
    }
  }

  private void number() {
    while (isDigit(peek())) advance();

    // Look for drobnaya !!
    if (peek() == '.' && isDigit(peekNext())) {

      advance();

      while (isDigit(peek())) advance();
    }

    addToken(NUMBER,
            Double.parseDouble(source.substring(start, current)));
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    // incomplete string
    if (isAtEnd()) {
      DLang.error(line, "Incomplete string");
      return;
    }

    // eat closing "
    advance();

    // delete ""
    String value = source.substring(start + 1, current - 1);
    addToken(STRING_LITERAL, value);
  }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAlphabet(char c) {
    return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphabetOrNumeric(char c) {
    return isAlphabet(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  // move pointer to next and return current
  private char advance() {
    current++;
    return source.charAt(current - 1);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  private void addToken(TokenType type, String lexeme, Object literal) {
    tokens.add(new Token(type, lexeme, literal, line));
  }

  private Token getLastTokenByTokenType(TokenType type) {
    List<Token> reversed = new ArrayList<>(tokens);
    Collections.reverse(reversed);
    for (Token token : reversed) {
      if (token.type == type) return token;
    }
    return null;
  }
}

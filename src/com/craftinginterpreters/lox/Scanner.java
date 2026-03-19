package com.craftinginterpreters.lox;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*; //So that i won't have to write TokenType.smth everytime

public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>(); //List of Token objects
  private int start = 0; //first element of the substring (lexeme)
  private int current = 0; //last element of the substring (lexeme)
  private int line = 1; //which line in the lexeme is

  //Constructor
  Scanner(String source)
  {
    this.source = source;
  }

//----------------------------HELPER FUNCTIONS FOR CHARACTERS--------------------------------------------
  //Checks if the current pointer is at the end
  private boolean isAtEnd()
  {
    return current >= source.length();
  }

  //If the character is poitive digit
  private boolean isDigit(char c)
  {
      return c >='0' && c <= '9';
  }

  //If the character is alphabetic
  private boolean isAlpha(char c)
  {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  //If the character is alphanumberic
  private boolean isAlphaNumberic(char c)
  {
    return isAlpha(c) || isDigit(c);
  }
//=======================================================================================================



//------------------------HELPER FUNCTIONS FOR ITERATING-------------------------------------------------

  //It returns where the current pointer is, which starts from index 0 (first element)
  private char peek()
  {
    if (isAtEnd()) return '\0'; //Strings end up with /0
    return source.charAt(current);
  }

  //Similar to peek, but it returns the the same value with incrementing the current afterwards
  private char advance()
  {
    return source.charAt(current++); //post increment
  }

  //It returns the next element unlike peek, but not moving the current
  private char peekNext()
  {
    if(current + 1 >= source.length()) return '\0';
    return source.charAt(current+1); //Don't confuse this with post increment
  }

  //Match is checking if the character is matched with expected value or not, BUT, only after that, it moves the current
  //It is used for defining tokens such as ==, <= and so on
  private boolean match(char expected)
  {
    if(isAtEnd()) return false;
    if(source.charAt(current) != expected) return false;
    current++;
    return true;
  }

//=========================================================================================================


//----------------------------CORE FUNCTIONS---------------------------------------------------------------

  //Method overloading for addToken()

  //Some tokens don't have literal values such as = , ! and so on
  private void addToken(TokenType type)
  {
    addToken(type,null);
  }


  //Adding token to the Array list
  private void addToken(TokenType type, Object literal)
  {
    String text = source.substring(start,current);

    tokens.add(new Token(type,text,literal,line));
    //Token = type(STRING,NUMBER), lexeme("hello", "123"), literal("hello", 123), line
  }

  // If it starts with double quotes, this method is being executed
  // "hello", in something like this, current should go past the ending quote, otherwise, the scanner would loop the second
  // string with the closing quote. Therefore, current-1 is used in substring after using advance so that it will point past the string
  private void string()
  {
    while (peek() != '"' && !isAtEnd()) //Starts loop until it sees the second double quote
    {
      if(peek() == '\n') line++;
      advance();
    }
    if(isAtEnd())
    {
      Lox.error(line,"Unterminated string.");
      return;
    }

    //Current is past the closing quote
    advance();

    String value = source.substring(start+1,current-1); //In substring, ending is EXCLUDED
    addToken(STRING, value);

  }

  //This one is quite straightforward. in 1234, it loops. If the current character is digit, moves to the next one.
  private void number()
  {
      while(isDigit(peek())) advance();
      if(peek() == '.' && isDigit(peekNext())){
        advance();
        while(isDigit(peek())) advance();
      }

      addToken(NUMBER, Double.parseDouble(source.substring(start,current)));
  }


  // Being Executed from the Lox.java, therefore it is not private
  List<Token> scanTokens()
  {
    while(!isAtEnd())
    {
      start = current;
      scanToken();
    }

    //Adds end of line token after adding all of the tokens
    tokens.add(new Token(EOF, "",null,line));
    return tokens;
  }

  //Identifier is not string or number, it could be variable name reserved words such as "AND, OR, ELSE" and such
  private void identifier()
  {
    while(isAlphaNumberic(peek())) advance();
    String text = source.substring(start,current);
    TokenType type = keywords.get(text);
    if(type == null) type = IDENTIFIER;
    addToken(type);
  }

  //Can't be changed whatsovever, therefore it is final
  private static final Map<String, TokenType> keywords;

  //It is easier to create the hashmap of the reserved words
  static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("class",  CLASS);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("fun",    FUN);
    keywords.put("if",     IF);
    keywords.put("nil",    NIL);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",  SUPER);
    keywords.put("this",   THIS);
    keywords.put("true",   TRUE);
    keywords.put("var",    VAR);
    keywords.put("while",  WHILE);
  }

//=========================================================================================================


//--------------------------------SCANNER------------------------------------------------------------------

  private void scanToken()
  {
    char c = advance();
    switch(c){
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : EQUAL);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : EQUAL);
        break;
      case '/':
        if(match('/'))
        {
          while(peek() != '\n' && !isAtEnd()) advance();
        }
        else{
          addToken(SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        break;
      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
      default:
        if(isDigit(c))
        {
          number();
        }
        else if(isAlpha(c))
        {
          identifier();
        }
        else
        {
          Lox.error(line,"Unexpected character.");
        }
        break;

    }
  }
//=========================================================================================================
}

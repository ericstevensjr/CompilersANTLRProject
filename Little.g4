/**
 * Define a lexer called Little
 */
lexer grammar Little;

ID : [a-zA-Z][a-zA-Z0-9]* ;             

INTLITERAL : [0-9]+ ; 

FLOATLITERAL: [0-9]*.[0-9]+ ;

STRINGLITERAL: [a-zA-Z|!|@|#|'|%|^|&|*|_|`|~|?|]+ ;

COMMENT: [--][a-z]+ ;

KEYWORDS: [PROGRAM|BEGIN|END|FUNCTION|READ|WRITE|IF|ELSE|ENDIF|WHILE|ENDWHILE|CONTINUE|BREAK|RETURN|INT|VOID|STRING|FLOAT] ;

OPERATORS: [+|:=|-|*|/|=|!=|>|<|>=|<=|(|)|;|,|] ; 
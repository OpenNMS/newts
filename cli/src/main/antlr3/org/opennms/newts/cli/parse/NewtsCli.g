
grammar NewtsCli;

options {
    language = Java;
}

@header {
    package org.opennms.newts.cli.parse;
}

@lexer::header {
    package org.opennms.newts.cli.parse;
}

@lexer::members {
    public void reportError(RecognitionException e) {
        StringBuilder message = new StringBuilder("Syntax error at position " + e.charPositionInLine + ": ");

        if (e instanceof NoViableAltException) {
            int index = e.charPositionInLine;
            String error = this.input.substring(index, index);
            String statement = this.input.substring(0, this.input.size() - 1);

            message.append("unexpected \"" + error + "\" for `" + statement + "`.");
        }
        else {
            message.append(this.getErrorMessage(e, this.getTokenNames()));
        }

        throw new ReportableError(message.toString());
    }
}

@parser::members {
    public void reportError(RecognitionException e) {
        String error = "Syntax error at position " + e.charPositionInLine + ": " + this.getErrorMessage(e, this.getTokenNames());
        throw new ReportableError(error);
    }
}


// Root
r returns [Statement ret]
    : st=statement (';')? EOF { $ret = st; }
    ;

statement returns [Statement ret]
    : st0=getSamples    { $ret = st0; }
    | st1=exitStatement { $ret = st1; }
    ;

getMeasurements returns [ MeasurementsGet ret]
    : K_GET K_MEASUREMENTS where=whereClause { return new MeasurementsGet(where); }
    ;

getSamples returns [SamplesGet ret]
    : K_GET K_SAMPLES where=whereClause { return new SamplesGet(where); }
    ;

whereClause returns [WhereClause where]
    @init {
        $where = new WhereClause();
    }
    : K_WHERE rel=relation { $where.and(rel); } (K_AND reln=relation { $where.and(reln); })*
    ;

relation returns [Relation ret]
    : r0=resource     { $ret = r0; }
    | r1=rangeStart   { $ret = r1; }
    | r2=rangeEnd     { $ret = r2; }
    ;

resource returns [Resource ret]
    : 'resource' EQUALS id=STRING_LITERAL
      {
          return new Resource(id.getText()); 
      }
    ;

rangeEnd returns [RangeEnd ret]
    : 'timestamp' '<' time=STRING_LITERAL
      {
          return new RangeEnd(time.getText());
      }
    ; 

rangeStart returns [RangeStart ret]
    : 'timestamp' '>' time=STRING_LITERAL
      {
          return new RangeStart(time.getText()); 
      }
    ;

exitStatement returns [Statement ret]
    : (K_QUIT | K_EXIT)
      {
          $ret = new Statement() {
              @Override
              public Type getType() {
                  return Statement.Type.EXIT;
              }
          };
      }
    ;

// Keywords
K_AND : A N D;
K_EXIT : E X I T;
K_FROM : F R O M;
K_GET : G E T;
K_MEASUREMENTS : M E A S U R E M E N T S;
K_SAMPLES : S A M P L E S;
K_WHERE : W H E R E;
K_QUIT : Q U I T;

fragment A: ('a'|'A');
fragment B: ('b'|'B');
fragment C: ('c'|'C');
fragment D: ('d'|'D');
fragment E: ('e'|'E');
fragment F: ('f'|'F');
fragment G: ('g'|'G');
fragment H: ('h'|'H');
fragment I: ('i'|'I');
fragment J: ('j'|'J');
fragment K: ('k'|'K');
fragment L: ('l'|'L');
fragment M: ('m'|'M');
fragment N: ('n'|'N');
fragment O: ('o'|'O');
fragment P: ('p'|'P');
fragment Q: ('q'|'Q');
fragment R: ('r'|'R');
fragment S: ('s'|'S');
fragment T: ('t'|'T');
fragment U: ('u'|'U');
fragment V: ('v'|'V');
fragment W: ('w'|'W');
fragment X: ('x'|'X');
fragment Y: ('y'|'Y');
fragment Z: ('z'|'Z');

STRING_LITERAL
    @init{
        StringBuilder b = new StringBuilder();
    }
    @after{
        setText(b.toString());
    }
    : '\'' (c=~('\'') { b.appendCodePoint(c);} | '\'' '\'' { b.appendCodePoint('\''); })* '\''
    ;

fragment DIGIT
    : '0' .. '9'
    ;

fragment LETTER
    : ('A'..'Z' | 'a'..'z')
    ;

EQUALS
    : '='
    ;

GT
    : '>'
    ;

LT
    : '<'
    ;

IDENT
    : LETTER (LETTER | DIGIT | '_')*
    ;

WS
    : (' ' | '\t' | '\n' | '\r')+ { $channel = HIDDEN; }
    ;

COMMENT
    : ('--' | '//') .* ('\n'|'\r') { $channel = HIDDEN; }
    ;

MULTILINE_COMMENT
    : '/*' .* '*/' { $channel = HIDDEN; }
    ;


package qbql.parser;

public enum Token {
    COMMENT,LINE_COMMENT,QUOTED_STRING,DQUOTED_STRING,
    WS,DIGITS,OPERATION,IDENTIFIER,AUXILIARY, PRE
    ,CDATA  //XML Crappy Data
}

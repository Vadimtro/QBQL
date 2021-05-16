package qbql.parser;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import qbql.util.Util;

public class LexerToken {
    public String content;
    public int begin;
    public int end;
    public Token type;

    public LexerToken( CharSequence text, int from, int to, Token t ) {
        content = text.toString();
        begin = from;
        end = to;
        type = t;
    }

    public void print() {
        System.out.println(toString()); 
    }
    public String toString() {
        return "["+begin+","+end+") "+content+"   <"+type+">"; 
    }
    public static void print( List<LexerToken> src ) {
        int j = 0;
        for( LexerToken t: src ) {
            System.out.print(j+"    "); 
            t.print(); 
            j++;
        }
    }
    public static void print( List<LexerToken> src, int from, int to ) {		
        System.out.println(toString(src, from, to)); 
    }
    public static String toString( List<LexerToken> src, int from, int to ) {
        StringBuilder ret = new StringBuilder();
        for( int i = from; i < to; i++ ) {
            ret.append(" "+src.get(i).content);
        }
        return ret.toString(); 
    }
}

package qbql.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.parser.Earley.Tuple;
import qbql.util.Util;

public class SyntaxError extends AssertionError {
	public int line;
	public int offset;
	public int end;
	public String code;
	public String marker;
	public String[] suggestions;
	
	/**
     * @param input -- sql text
     * @return null if valid input, otherwise
     * 		   syntax error message structured as SyntaxError 
     */
    public static SyntaxError checkSyntax( String input, String[] grammarSymbols, List<LexerToken> src, Earley earley, Matrix matrix) {
        Cell top = matrix.get(Util.pair(0, src.size()));
        
        if( top != null ) {
            for( String s : grammarSymbols ) {
                for( int i = 0; i < top.size(); i++ ) {
                    Tuple tuple = earley.rules[top.getRule(i)];
                    String candidate = earley.allSymbols[tuple.head];
                    if( candidate.equals(s) )
                        return null;
                }
            }
        }
        
        int y = src.size();
        for( ; 0 < y; y-- )
            if( matrix.get(Util.pair(y,y)) != null )
                break;
        SortedMap<Integer,Cell> range = matrix.subMap(Util.pair(y, y), true, matrix.lastKey(), true);
        for( int key : range.keySet() ) {
            int tmpY = Util.Y(key);
            if( y < tmpY )
                y = tmpY;
        }
        
		int end = 0;
		if( 0 < y )
			end = src.get(y-1).end;
        
        int line = 0;
        int beginLinePos = 0;
        int endLinePos = input.length(); 
        for( int i = 0; i < endLinePos; i++ ) {
			if( input.charAt(i)=='\n' ) {
				if ( i < end ) {
					line++;
					beginLinePos = i;
				} else {
					endLinePos = i;
					break;
				}
			}
		}
                
        String code = input.substring(beginLinePos,endLinePos);
        final int offset = end - beginLinePos;
        
        Set<String> suggestions = new TreeSet<String>();
        for( int x = 0; x <= y; x++ ) {
            Cell cell = matrix.get(Util.pair(x,y));
            if( cell != null ) {
            	for( int i = 0; i < cell.size(); i++ ) {
            		int rule = cell.getRule(i);
            		int pos = cell.getPosition(i);
            		if( pos < earley.rules[rule].rhs.length ) {
            			String e = earley.allSymbols[earley.rules[rule].rhs[pos]];
            			if( !e.startsWith("xml") ) {
            				suggestions.add(e);
            			}
            		}
            	}
            }			
		}
        
        /*StringBuilder expected = new StringBuilder("Expected: ");
        int cnt = -1;
        for( String s : grammarSymbols ) {
            if( 0 < ++cnt )
                expected.append(" or ");
            expected.append(s);
        }
        StringBuilder parsed = new StringBuilder("Parsed: ");
        if( top != null ) {
            for( int i = 0; i < top.size() && i < 20 ; i++ ) {
                Tuple tuple = earley.rules[top.getRule(i)];
                if( i == 0 )
                    parsed.append(earley.allSymbols[tuple.head]);
                if( tuple.rhs.length <= pos )
                    continue;
                suggestions.add(earley.allSymbols[tuple.rhs[pos]]);
            }           
        }*/

        String token = "";
        if( y < src.size() )
            token = src.get(y).content.toUpperCase();
        return new SyntaxError(line,offset,end,code,Util.identln(offset, "^^^"),order(suggestions, "'"+token+"'"));
    }

    private static String[] order( Set<String> suggestions, String token ) {
    	String candidate = null;
    	for( String s : suggestions )
    		if( isTypo(s,token) ) {
    			candidate = s;
    		}
    	
    	String[] ret = new String[suggestions.size()];
    	int i = 0;
    	for( String s : suggestions )
    		ret[i++] = s;
    	
    	if( candidate != null ) {
        	String swap = ret[0];
    		ret[0] = candidate;
    		for( int j = 1; j < ret.length; j++ ) 
    			if( ret[j].equals(ret[0]) )
    				ret[j] = swap;				
    	}
    	
		return ret;
	}
    
    
	private static boolean isTypo( String s, String candidate ) {
		if( s.length()+1 == candidate.length() 
		 ||	s.length()   == candidate.length() +1	
		 ||	s.length()   == candidate.length() 	
		) {
			int matched = 0;
			int brokenAt = -1;
			for( int i = 0; i < s.length() && i < candidate.length(); i++ ) {
				if( s.charAt(i) == candidate.charAt(i) )
					matched++;
				else {
					brokenAt = i;
					break;
				}
			}
			if( brokenAt+1 < s.length() && brokenAt+1 < candidate.length() 
			 && s.charAt(brokenAt+1) == candidate.charAt(brokenAt+1) ) 
				for( int i = brokenAt+1; i < s.length() && i < candidate.length(); i++ ) {
					if( s.charAt(i) == candidate.charAt(i) )
						matched++;
					else 
						break;
				}
			else if( brokenAt+1 < s.length() 
			         && s.charAt(brokenAt+1) == candidate.charAt(brokenAt) ) 
				for( int i = brokenAt; i+1 < s.length() && i < candidate.length(); i++ ) {
					if( s.charAt(i+1) == candidate.charAt(i) )
						matched++;
					else 
						break;
				}
			else if( brokenAt+1 < candidate.length() 
					 && s.charAt(brokenAt) == candidate.charAt(brokenAt+1) ) 
				for( int i = brokenAt; i < s.length() && i+1 < candidate.length(); i++ ) {
					if( s.charAt(i) == candidate.charAt(i+1) )
						matched++;
					else 
						break;
				}
			if( matched == s.length()-1 || matched == candidate.length()-1 )
				return true;
		}
		return false;
	}
	
	
	private SyntaxError( int line, int offset, int end, String code, String marker, String[] suggestions ) {
		this.line = line;
		this.offset = offset;
		this.end = end;
		this.code = code;
		this.marker = marker;
		this.suggestions = suggestions;
	}

    @Override
    public String toString() {
        return getMessage();
    }
    
    public String getMessage() {
        // Pad out marker to be same size as code so center aligned text
        // lines up correctly
    	StringBuilder allSuggestions = new StringBuilder();
		for( String s : suggestions )
			allSuggestions.append(s+',');
        return "Error at line "+line+", col "+offset+"\nExpected:"+allSuggestions.toString()+"\n"+code+"\n"+marker;
    }
    
    public static void main( String[] args ) throws Exception {
        String input = //"blah blah blah";
            Util.readFile(SyntaxError.class,"test.sql"); //$NON-NLS-1$ 
        SyntaxError ret = null;//checkSyntax(input);
        if( ret != null ) {
        	/*System.out.println("Syntax Error");
        	System.out.println("at line#"+ret.line);
        	System.out.println(ret.code);
        	System.out.println(ret.marker);*/
        	//System.out.println("Expected:  ");
    		//for( String s : ret.suggestions )
    			//System.out.print(s+',');
        	System.out.println(ret.getMessage());
        }
    }
}

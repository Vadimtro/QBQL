package qbql.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import qbql.parser.Earley;
import qbql.parser.Lex;
import qbql.parser.Parser.EarleyCell;
import qbql.parser.Parser.Tuple;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.Token;
import qbql.parser.RecognizedRule;
import qbql.util.Util;

/**
 * SQL syntax analysis with user friendly error messages 
 * see: SyntaxError.main() for example usage
 * @author Dim
 */
public class SyntaxError extends AssertionError {
    public int line;
	public int offset;
	public int end;
	public String code;
	public String marker = "^^^";
    public String detailedMessageKey = "SyntaxError_DetailedMessage"; //$NON-NLS-1$
	
	
    private Map<RecognizedRule,Integer> weightedRules;
	
    private Earley earley;
    
    public Matrix matrix;
    
	static String TITLE = "Syntax Error"; //$NON-NLS-1$
    
    
    public String[] getSuggestions() {
        List<String> ret = new LinkedList<String>();
        for( Long s : topNsuggestions() ) {
            String candidate = earley.allSymbols[Util.lX(s)];
            if( ret.contains(candidate) )
                continue;
            if( 0 < candidate.indexOf('[') )
                continue;
            ret.add(candidate);
        }
        return ret.toArray(new String[0]);
    }

    
	/**
     * @param input -- sql text
     * @param grammarSymbols -- expected grammar symbols, e.g. "expr"
     * @return null if valid input, otherwise
     * 		   syntax error message structured as SyntaxError 
     */
    public static SyntaxError checkSyntax( String input, String[] grammarSymbols, List<LexerToken> src, Earley earley, Matrix matrix) {
        return checkSyntax(input, grammarSymbols, src, earley, matrix, "^^^", "SyntaxError_DetailedMessage");
    }
    public static SyntaxError checkSyntax( String input, String[] grammarSymbols, List<LexerToken> src, Earley earley, Matrix matrix, String marker, String format) {
    	EarleyCell top = matrix.get(0, src.size());
        
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
        
        // not clear what to do if unexpected content in the top cell
        
        int maxY = matrix.lastY();
        
		int end = 0;
		if( 0 < maxY )
			end = src.get(maxY-1).end;
        
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
        
        LexerToken token = null;
        if( maxY < src.size() )
            token = src.get(maxY);
        else if( maxY == src.size() )
            token = src.get(maxY-1);
        
        Map<RecognizedRule,Integer> rules = new TreeMap<RecognizedRule,Integer>();
        pendingRules(earley, matrix, maxY, null, rules);
        if( token.end == input.length() ) // prefix
            pendingRules(earley, matrix, maxY-1, token, rules);        
       
        for( RecognizedRule rp : rules.keySet() ) {
            long s = parentChildSymbols(rp, earley);
            String ss = earley.allSymbols[Util.lX(s)];
            if( isTypo(ss,token) ) {  
                rules = new TreeMap<RecognizedRule,Integer>();
                rules.put(rp, 1);
                return new SyntaxError(line,offset,token.begin,code,Util.identln(offset, marker),rules,earley,matrix,format);
            }
        }
        
        return new SyntaxError(line,offset,end,code,Util.identln(offset, marker),rules,earley,matrix,format);
    }
    
    private static void pendingRules( Earley earley, Matrix matrix, int y, LexerToken token, Map<RecognizedRule,Integer> rules ) {
        if( y < 0 )
            return;
        for( int x = 0; x <= y; x++ ) {
            EarleyCell cell = matrix.get(x,y);
            if( cell != null ) {
            	for( int i = 0; i < cell.size(); i++ ) {
            		int rule = cell.getRule(i);
            		int pos = cell.getPosition(i);
            		Tuple t = earley.rules[rule];
            		if( pos < t.rhs.length ) {
            			String symbol = earley.allSymbols[t.rhs[pos]];
            			if( !symbol.startsWith("xml") ) {
            				String[] rhs = new String[t.rhs.length];
            				for( int j = 0; j < rhs.length; j++ ) 
								rhs[j] = earley.allSymbols[t.rhs[j]];							
                            if( token == null )
                                rules.put(new RecognizedRule(earley, earley.allSymbols[t.head],rhs,pos,x,y,1),1);
                            else if(   token.type == Token.IDENTIFIER 
                                 && earley.isTerminal(earley.rules[rule].rhs[pos]) 
                                 && symbol.substring(1).toUpperCase().startsWith(token.content.toUpperCase()) 
                                 && symbol.length()!=token.content.length()+2 
                            )
                                rules.put(new RecognizedRule(earley, earley.allSymbols[t.head],rhs,pos,x,y,10),10);

            			}
            		}
            	}
            }			
		}
    }

    
	private static boolean isTypo( String symbol, LexerToken token ) {
	    if( token == null || token.begin+1 == token.end || token.begin+2 == token.end )
	        return false;
	    String candidate = "'"+token.content.toUpperCase()+"'";
		if( symbol.length()+1 == candidate.length() 
		 ||	symbol.length()   == candidate.length() +1	
		 ||	symbol.length()   == candidate.length() 	
		) {
			int matched = 0;
			int brokenAt = -1;
			for( int i = 0; i < symbol.length() && i < candidate.length(); i++ ) {
				if( symbol.charAt(i) == candidate.charAt(i) )
					matched++;
				else {
					brokenAt = i;
					break;
				}
			}
			if( brokenAt+1 < symbol.length() && brokenAt+1 < candidate.length() 
			 && symbol.charAt(brokenAt+1) == candidate.charAt(brokenAt+1) ) 
				for( int i = brokenAt+1; i < symbol.length() && i < candidate.length(); i++ ) {
					if( symbol.charAt(i) == candidate.charAt(i) )
						matched++;
					else 
						break;
				}
			else if( brokenAt+1 < symbol.length() 
			         && symbol.charAt(brokenAt+1) == candidate.charAt(brokenAt) ) 
				for( int i = brokenAt; i+1 < symbol.length() && i < candidate.length(); i++ ) {
					if( symbol.charAt(i+1) == candidate.charAt(i) )
						matched++;
					else 
						break;
				}
			else if( brokenAt+1 < candidate.length() 
					 && symbol.charAt(brokenAt) == candidate.charAt(brokenAt+1) ) 
				for( int i = brokenAt; i < symbol.length() && i+1 < candidate.length(); i++ ) {
					if( symbol.charAt(i) == candidate.charAt(i+1) )
						matched++;
					else 
						break;
				}
			if( matched == symbol.length()-1 || matched == candidate.length()-1 )
				return true;
		}
		return false;
	}
	
	private SyntaxError( int line, int offset, int end, String code, String marker, Map<RecognizedRule,Integer> rules, Earley earley, Matrix matrix, String format ) {
		this.line = line;
		this.offset = offset;
		this.end = end;
		this.code = code;
		this.marker = marker;
		this.weightedRules = rules;
        this.earley = earley;
        this.detailedMessageKey = format;
        this.matrix = matrix;
	}

    @Override
    public String toString() {
        return getDetailedMessage();
    }
    
    public String getDetailedMessage() {
        // Pad out marker to be same size as code so center aligned text
        // lines up correctly
        String pointer = Util.padln(marker, code.length());
    	StringBuilder allSuggestions = new StringBuilder();
        List<String> ret = new LinkedList<String>();
		for( long s : topNsuggestions() ) {
	        String candidate = earley.allSymbols[Util.lX(s)];
	        if( ret.contains(candidate) )
	            continue;
            if( 0 < candidate.indexOf('[') )
                continue;
	        ret.add(candidate);
			allSuggestions.append(candidate+',');
		}
        String suggestions = allSuggestions.toString();
        if( 60 < suggestions.length() )
        	suggestions = suggestions.substring(0,50);
        return "Syntax Error at line "+(line+1)+", column "+offset+"\n\n"+suggestions+"\n"+code+"\n\nExpected: "+pointer;
    }

    @Override
    public String getMessage() {
    	return getDetailedMessage();
    }

    public String getTitle() {
        return TITLE;
    }
    
    public List<Long> topNsuggestions() {
        Map<Long,Integer> topN = new TreeMap<Long,Integer>();  // symbol -> frequency
        final int N = 10;
        for( RecognizedRule rp : weightedRules.keySet() ) {
            long minVar = -1;
            int minVal = Integer.MAX_VALUE;
            for( long s : topN.keySet() ) {
                int tmp = topN.get(s);
                if( tmp < minVal ) {
                    minVar = s;
                    minVal = tmp;
                }
            }
            long suggestedVar = parentChildSymbols(rp, earley);
            Integer suggestedVal = 0;
            if( topN.size() == N ) {
                if( suggestedVal != null && minVal < suggestedVal ) {
                    topN.remove(minVar);
                    topN.put(suggestedVar,suggestedVal);
                }
            } else
                topN.put(suggestedVar,suggestedVal);
        }
        
        // http://stackoverflow.com/questions/13015699/representing-binary-relation-in-java
        List<Entry<Long, Integer>> myList = new ArrayList<Entry<Long, Integer>>();
        for (Entry<Long, Integer> e : topN.entrySet())
              myList.add(e);

        Collections.sort( myList, new Comparator<Entry<Long, Integer>>(){
            public int compare( Entry a, Entry b ){
                // compare b to a to get reverse order
                return ((Integer) b.getValue()).compareTo((Integer)a.getValue());
            }
        } );

        //myList = myList.sublist(0, 3);
        
        List<Long> ret = new LinkedList<Long>();
        int i = 0;
        for( Entry<Long, Integer> e : myList ) {
            if( N < i )
                break;
            ret.add(e.getKey());
            i++;
        }
        return ret;
    }
    
    /**
     * Convert rules into RecognizedRule and order them by cumulative frequency. Cut top N
     * @param N -- limit
     * @return
     */
    public List<RecognizedRule> topNrules( final int N, boolean excludeAux ) {
        Map<RecognizedRule,Integer> topN = new TreeMap<RecognizedRule,Integer>();  // rule -> cumulative frequency
        for( RecognizedRule rp : weightedRules.keySet() ) {
            long suggestedVar = parentChildSymbols(rp, earley);
            Integer suggestedVal = 0;
            suggestedVal *= weightedRules.get(rp);
            
            if( excludeAux && 0 < rp.head.indexOf('[') )
                continue;
            RecognizedRule candidate = rp;

            Integer frequency = topN.get(candidate);
            if( frequency == null )
                frequency = 0;
            candidate.weight = frequency+suggestedVal;
            topN.put(candidate,frequency+suggestedVal);            
        }
        
        // http://stackoverflow.com/questions/13015699/representing-binary-relation-in-java
        List<Entry<RecognizedRule, Integer>> myList = new ArrayList<Entry<RecognizedRule, Integer>>();
        for (Entry<RecognizedRule, Integer> e : topN.entrySet())
              myList.add(e);

        Collections.sort( myList, new Comparator<Entry<RecognizedRule, Integer>>(){
            public int compare( Entry a, Entry b ){
                // compare b to a to get reverse order
                return ((Integer) b.getValue()).compareTo((Integer)a.getValue());
            }
        } );

        //myList = myList.sublist(0, 3);
        
        List<RecognizedRule> ret = new LinkedList<RecognizedRule>();
        int i = 0;
        for( Entry<RecognizedRule, Integer> e : myList ) {
            if( N < i )
                break;
            ret.add(e.getKey());
            i++;
        }
        return ret;
    }
    
    private static long parentChildSymbols( RecognizedRule rp, Earley earley ) {
        long entry = Util.lPair(earley.symbolIndexes.get(rp.rhs[rp.pos]),earley.symbolIndexes.get(rp.head));
        return entry;
    }
    

}

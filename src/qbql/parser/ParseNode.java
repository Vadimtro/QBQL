package qbql.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Icon;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.Parser;
import qbql.util.Array;
import qbql.util.Pair;
import qbql.util.Util;

public class ParseNode implements Comparable {
    public int from;
    public int to;
    
    /**
     * Main method of investigating node payload: Check if the node contains "symbol" numeric encoding
     */
    public boolean contains( int symbol ) {
        return symbols[Array.indexOf(symbols, symbol)] == symbol;
    }
    /**
     * Main method of investigating node payload: Check if the node contains symbol
     * @param symbol
     * @return
     */
    public boolean contains( String symbol ) {
        if( parser == null )
            return false;
        Integer code = parser.symbolIndexes.get(symbol);
        if( code == null )
            throw new AssertionError("No such symbol `"+symbol+"` in the grammar");
        return contains(code);
    }

    /**
     * Walk this tree branch depth-first and return the list of all node's descendants
     * @return
     */
    public List<ParseNode> descendants() {
        List<ParseNode> ret = new ArrayList<ParseNode>();
        ret.add(this);
        for( ParseNode n : children() )
            ret.addAll(n.descendants());
        return ret;
    }

    public ParseNode parent;

    /**
     * Navigate to node's parent
     * It is fast, because reference to parent is kept during node's creation
     * @return
     */
    public ParseNode parent() {
        if( parent == null )
            return null;
        if( !parent.isAuxiliary() )
            return parent;
        // For auxiliary root node would proceed down
        ParseNode ret = parent.parent();
        if( ret == null )
            return parent; // not ret which is null
        return ret;
    }
    

    /**
     * Get node's child which interval covers [head, tail)
     * Deprecated as it is tempting to mistakenly pick up this method over the locate()
     * @param head
     * @param tail
     * @return
     */
    @Deprecated
    public ParseNode childAt( int head, int tail ) {
        if( topLevel != null ) {
            for( ParseNode child : children() ) {
                if( child.from <= head && tail <= child.to ) 
                    return child;
            }
            return null;
        } 
        if( lft != null && lft.from <= head && tail <= lft.to ) 
            return lft;
        if( rgt != null && rgt.from <= head && tail <= rgt.to ) 
            return rgt;
        return null;        
    }

    
    /**
     * Node's ordering (e.g. used in collections of nodes created via tree traversal)
     */
    public int compareTo( Object obj ) {
        ParseNode src = (ParseNode)obj;
        if( from != src.from )
            return from - src.from;
        else
            return to - src.to;
    }
    @Override
    public String toString() { return toString(0); }


    /**
     * Walk this tree branch depth-first and return the list of all node's descendants
     * each coupled with its parent (calculating parent for a node is not efficient otherwise)
     * @return 
     */
    public ArrayList<Pair<ParseNode,ParseNode>> descendants( ParseNode parent ) {
    	ArrayList<Pair<ParseNode,ParseNode>> ret = new ArrayList<Pair<ParseNode,ParseNode>>();
        ret.add(new Pair<ParseNode,ParseNode>(this,parent));
        for( ParseNode n : children() )
            ret.addAll(n.descendants(this));
        return ret;
    }
    
    /**
     * Walk the tree without consuming memory
     */
    public ParseNode next() {
        if( lft != null ) {
            if( lft.isAuxiliary() )
                return lft.next();
            return lft;
        }
        ParseNode prt = parent;
        while( prt != null ) {
        	ParseNode nextSibling = prt.rgt;
            if( nextSibling == null && prt.topLevel != null ) {
                for( ParseNode p : prt.topLevel ) {
                	if( to <= p.from ) {
                        if( p.isAuxiliary() )
                            return p.next();
                        return p;
                	}
                }
            }
            if( nextSibling != null ) {
                if( to == nextSibling.from ) {
                    if( nextSibling.isAuxiliary() )
                        return nextSibling.next();
                    return nextSibling;
                }
            }
            prt = prt.parent;
        }
        if( topLevel != null ) {
            for( ParseNode child : topLevel ) {
                 return child;
            }
        } 
        return null;
    }

    
    /**
     * Ancestor chain from the leaf at position pos to the root "this" 
     * @param pos
     * @return
     */
    public List<ParseNode> ancestors( int pos ) {
        return intermediates(pos,pos+1);
    }

    /**
     * All the descendants of "this" containing the given interval [head,tail).
     * That is, all the nodes on the ancestor chain from [head,tail) to "this".
     * In other words, these are all the nodes between ParseNode[head,tail) and "this".
     */ 	
    public  List<ParseNode> intermediates( int head, int tail ) {
        List<ParseNode> ret = new ArrayList<ParseNode>();
        if( from <= head && tail <= to ) 
            ret.add(this);
        for( ParseNode n : children() )
            if( n.from <= head && tail <= n.to ) 
                ret.addAll(n.intermediates(head, tail));
        return ret;
    }

    @Deprecated
    public ParseNode ancestor( int head, int tail, int content ) {
        /*ParseNode parent = parent(head, tail);
        if( parent == this || parent == null ) // that is root
            return null;
        else if( parent.contains(content) )
            return parent;
        else
            return ancestor(parent.from, parent.to, content);*/
        throw new AssertionError("Deprecated");
    }
    /**
     * The closest ancestor with the "content"
     */     
    public ParseNode ancestor( int content ) {
        if( contains(content) )
            return this;
        if( parent() == null )
            return null;
        return parent().ancestor(content);
    }
    
    /**
     * The closest descendant of this (root) with the "content" covering [head,tail)
     */     
    public ParseNode descendant( int head, int tail, int content ) {
        if( contains(content) )
            return this;
        for( ParseNode child : children() )
            if( child.from <= head && tail <= child.to )
                if( child.contains(content) )
                    return child;
                else
                    return child.descendant(head, tail, content);
        return null;
    }
    
    /**
     * locate the node [head,tail)
     */     
    public ParseNode locate( int head, int tail ) {
        if( from == head && tail == to )
            return this;
        for( ParseNode child : children() ) {
            if( child.from <= head && tail <= child.to )
                return child.locate(head, tail);
        }
        return null;
    }


    /**
     * Parent of the ParseNode[head,tail), not "this" (which assumed to be the root)
     * @param head
     * @param tail
     * @return
     *  Too slow!
     */ 
    @Deprecated
    public ParseNode parent( int head, int tail ) {
    	ParseNode lastNotAux = null;
    	ParseNode current = this;
    	do {
    		if( !current.isAuxiliary() )
    			lastNotAux = current;
    		current = current.childAt(head,tail);
    		if( current == null ) 
    			return null;
    	} while( current.from != head || current.to != tail );
    	return lastNotAux;
    }
    
    
    /**
     * Print tree branch at a given offset
     * @param depth -- tree branch is positioned at
     * (depth is calculated by relative position of ParseNode to the root)
     */
    void print( int depth ) {
        System.out.println(toString(depth)); // (authorized)
    }
    /**
     * @return the leaf node, normally [pos,pos+1)
     */
    public ParseNode leafAtPos( int pos ) {
        if( children().size() == 0 && pos == from )
            return this;
        for( ParseNode child : children() ) {
            if( child.from <= pos && pos < child.to ) 
                return child.leafAtPos(pos);
        }
        return null;
    }
    private void calculateDepth( Map<Long,Integer> depthMap, int depth ) {
        depthMap.put(Util.lPair(from,to), depth);
        for( ParseNode child : children() ) {
            child.calculateDepth(depthMap, depth+1);
        }
    }
    Map<Long, Integer> calculateDepth() {
        Map<Long,Integer> depthMap = new TreeMap<Long,Integer>();
        calculateDepth(depthMap, 0);
        return depthMap;
    }
    
    /**
     * The most common method to output tree to the console
     * Normally invoked on the root
     */
    public void printTree() {
        Map<Long,Integer> depthMap = calculateDepth();

        int i = 0;
        for( ParseNode n : descendants() ) {
            //if( i++>500 ) {
        	    //System.out.println("...");
                //return;}
            int depth = depthMap.get(Util.lPair(n.from, n.to));
            n.print(depth);
        }
    }
    /**
     * For CYK method grammar is in Chomski Normal Form (CNF)
     * with binary productions. While the print() method
     * ignores CNF intermediatory symbols, this mehod outputs the full tree 
     * @param depth
     */
    public void printBinaryTree( int depth ) {
        print(depth);
        if( lft!=null )
            lft.printBinaryTree(depth+1);
        if( rgt!=null )
            rgt.printBinaryTree(depth+1);
        if( topLevel!=null )
            for( ParseNode n : topLevel )
                n.printBinaryTree(depth+1);
    }
    /**
     * Output to a string rather than console
     * Dismiss all auxiliary nodes (useful for parse tree regression testing)
     */
    public String tree() {
        StringBuilder ret = new StringBuilder();
        
        Map<Long,Integer> depthMap = calculateDepth();

        int i = 0;
        for( ParseNode n : descendants() ) {
            int depth = depthMap.get(Util.lPair(n.from, n.to));
            ret.append(n.toString(depth,"["));
            ret.append("\n");
        }
        return ret.toString();
    }

    /**
     * @param src -- scanner output
     * @return -- scanner content corresponding to the parse node
     */
    public String content( List<LexerToken> src ) {
    	return content(src, null);
    }
    public String content( List<LexerToken> src, Boolean addWsDividers ) {
        StringBuilder sb = new StringBuilder();
        try { 
            int lastEnd = -1;  // accomodate both "a.b" and "a b" 
        	for( int i = from; i < to; i++ ) {
        	    LexerToken t = src.get(i);
        	    if( addWsDividers == null && from < i && lastEnd < t.begin  
        	     || addWsDividers != null && addWsDividers		
        	    ) {
                    sb.append(' ');
        	    }
        		sb.append(t.content);
        		lastEnd = t.end;
        	}
        } catch( IndexOutOfBoundsException e ) {        	
        	System.err.println("src out of sync with parse tree?");
        	e.printStackTrace(); // investigating nasty hang
        }
        return sb.toString();
    }
    

    ParseNode lft = null;
    ParseNode rgt = null;

    int[] symbols = new int[0];
    public int[] content() {
    	return symbols;
    }
    
    /**
     * Return the content as strings
     * 
     * @return array of Strings
     */
    public String[] contentAsStrings() {
    		String[] ret = new String[symbols.length];
    		for (int i = 0; i < ret.length; ++i) {
    			ret[i] = parser.allSymbols[symbols[i]];
    		}
    		return ret;
    }

    /**
     * Return one symbol
     * 
     * @return The i<em>th</em> content
     */
    public String content(int i) {
    		if (symbols[i] < 0) {
    			return "[symbols["+i+"] = "+symbols[i]+"]"; 
    		}
    		return parser.allSymbols[symbols[i]];
    }

    public void addContent( int symbol ) {
    	symbols = Array.insert(symbols, symbol);
    }
    public void addContent( String symbol ) {
        //if( symbol.charAt(0) == '"' )
            //symbol = symbol.substring(1,symbol.length()-1);
        Integer s = parser.symbolIndexes.get(symbol);
        if( s == null )
            System.err.println(symbol+" not found");
        addContent(s);
    }
    public void deleteContent( int symbol ) {
    	symbols = Array.delete(symbols, symbol);
    }
    

    // If fail to parse complete text, then accumulate all children here
    // if topLevel != null then lft and rgt == null, and content is empty. 
    public Set<ParseNode> topLevel = null;
    
    public void addTopLevel( ParseNode child ) {
        if( topLevel == null )
            topLevel = new TreeSet<ParseNode>();
        topLevel.add(child);
    }
    
    public ParseNode coveredByOnTopLevel( int pos  ) {
    	if( topLevel == null )
    		return null;
    	for( ParseNode node: topLevel ) {
    		if( node.from <= pos && pos < node.to )
    			return node;
    	}
    	return null;
    }


    Parser parser;
    /**
     * Create node at [begin,end) with content {symbol} 
     * Nodes are generated by parser internally, so it is not part of parser API
     * @param dummy -- not used since deprecation of CYK parsing method
     */
    public ParseNode( int begin, int end, int symbol, int dummy, Parser p ) {
        this(begin,end,symbol,p);
    }
    public ParseNode( int begin, int end, int symbol, Parser p ) {
        from = begin;
        to = end;
        addContent(symbol);
        parser = p;
    }

    // skip unimportant node content
    // typical marker characters are [ , ) , and " 
    public static String ignoreMarkers = null;
    
    protected String toString( int depth ) {
        if( ignoreMarkers != null )
            return toString(depth,ignoreMarkers);
        return toString(depth,"");
    }
    protected String toString( int depth, String auxMarkers ) {      
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < depth ;i++)
            sb.append("  ");  //$NON-NLS-1$
        sb.append(interval()+" ");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for( Integer i : content() ) {
            if( i==-1 )
                continue;
        	String symbol = parser.allSymbols[i];
        	//symbol = symbol.endsWith(")")?("\""+symbol+"\""):symbol;
        	//symbol = symbol.endsWith(")")?"":symbol;
        	//symbol = symbol.endsWith("\"")?"":symbol;
        	boolean skip = false;
        	for( char c : auxMarkers.toCharArray() )
        	    if( 0 <= symbol.indexOf(c) ) {
        	        skip = true;
        	        break;
        	    }
        	if( !skip )
        	    sb.append("  "+ symbol); //$NON-NLS-1$
        }
        return sb.toString();
    }
    
    /**
     * The node's interval is a unique node identifier in the parse tree
     * @return
     */
    public String interval() {
		return "["+from+","+to+")";
	}
    /**
     * Another excellent node identifier
     * @return
     */
    public long id() {
        return Util.lPair(from,to);
    }

    /**
     * Was somewhat frivolous definition which parse tree nodes are less important than the others 
     * 
     * For Earley parser auxiliary node contains empty set of symbols 
     */
    public boolean isAuxiliary() {
        // careful changing this method: parse tree traversal depends on it
    	//if( contains(-1) )
    		//return true;
        if( symbols[0] == -1 )
            return true;
    	
    	// shortcut
    	if( /*parser.auxSymbols.length == 0*/ true )
    		return false;
    	
    	if( from+1 == to )
    		return false;
    	
        boolean isAux = false;        
        //boolean containsConcat = false;
        //boolean containsRawBnf = false;
        //boolean containsBlock = false;
        for( Integer symbol : content() ) {        	
        	for( int i = 0; i < 100000/*parser.auxSymbols.length*/; i++ ) {
				if( i == symbol ) {
					isAux = true;
					break;
				}
			}
		    //if(  "concat".equals(symb) ) //$NON-NLS-1$ 
				//containsConcat = true;
			//if(  "rawbnf".equals(symb) ) //$NON-NLS-1$ 
				//containsRawBnf = true;		               		            
			//if(  "block".equals(symb) ) //$NON-NLS-1$ 
				//containsBlock = true;		               		            
        }
        //if( containsConcat && !containsRawBnf && !containsBlock )
        	//return true;
        return isAux;   
    }


    /**
     *  Navigates through children (skipping all the auxiliary nodes)
     */
    public Set<ParseNode> children() {
        Set<ParseNode> ret = new TreeSet<ParseNode>();
        if( topLevel != null ) {
            for( ParseNode child: topLevel ) {
                if( child.isAuxiliary() )
                    ret.addAll(child.children());
                else
                    ret.add(child);
            }
            return ret;
        }
        if( lft == null )
            return ret;
        if( lft.isAuxiliary() ) {
            ret.addAll( lft.children() );
        } else
            ret.add(lft);
        if( rgt == null )
            return ret;
        if( rgt.isAuxiliary() ) {
            ret.addAll( rgt.children() );
        } else
            ret.add(rgt);
        return ret;
    }

    /**
     * Used in a very narrow context of parsing only code fragment within bigger source
     * @param offset
     */
    public void moveInterval( int offset ) {
        from += offset;
        to += offset;
        if( topLevel != null )
            for( ParseNode p :  topLevel ) 
                p.moveInterval(offset);
        else {
            if( lft != null )
                lft.moveInterval(offset);
            if( rgt != null )
                rgt.moveInterval(offset);
        }
    }

    
    public int treeDepth() {
        int ret = 0;
        for(  ParseNode child : children() ) {
            int tmp = child.treeDepth();
            if( ret < tmp )
                ret = tmp;
        }
        return ret+1;
    }
    
    // abstract method
    public Icon getIcon() {
    	return null;
    }
    
    /**
     * Tree node encoding resilient to tree modifications, e.g .2.5  (the fifth child of the second child of the root node)
     * @return
     */
    public String path() {
    	ParseNode parent = parent();
		if( parent == null )
    		return "";
		int i = -1;
		for( ParseNode child: parent.children() ) {
			i++;
			if( child == this )
				return parent.path()+"."+i;		
		}
		throw new AssertionError("impossible case");
    }
    /**
     * Descend according to the path,  e.g .2.5  to the second child of the root node, then to fifth child...
     * @param root
     * @param path
     * @return
     */
    public ParseNode locate( ParseNode root, String path ) {
    	if( path.length() == 0 )
    		return root;
    	int sibling = Integer.parseInt(path.substring(1,2));
        String truncated = path.substring(2);
		int i = -1;
		for( ParseNode child: parent.children() ) {
			i++;
			if( i == sibling )
				return locate(child,truncated);
		}
 		throw new AssertionError("impossible case");
    }

}


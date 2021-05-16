package qbql.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.util.Util;

public class ParseNode implements Comparable {
    public int from;
    public int to;

    public int compareTo( Object obj ) {
        ParseNode src = (ParseNode)obj;
        if( from != src.from )
            return from - src.from;
        else
            return to - src.to;
    }
    public String toString() { return toString(0); }

    public List<ParseNode> descendants() {
        List<ParseNode> ret = new ArrayList<ParseNode>();
        ret.add(this);
        for( ParseNode n : children() )
            ret.addAll(n.descendants());
        return ret;
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

    /**
     * The closest ancestor with the "content"
     */     
    public ParseNode ancestor( int head, int tail, int content ) {
        ParseNode parent = parent(head, tail);
        if( parent == this ) // that is root
            return null;
        else if( parent.contains(content) )
            return parent;
        else
            return ancestor(parent.from, parent.to, content);
    }
    
    /**
     * The closest descendant of this (root) with the "content" covering [head,tail)
     */     
    public ParseNode descendant( int head, int tail, int content ) {
        for( ParseNode child : children() )
            if( child.from <= head && tail <= child.to )
                if( child.contains(content) )
                    return child;
                else
                    return child.descendant(head, tail, content);
        return null;
    }

    public ParseNode locate( int head, int tail ) {
        if( from == head && tail == to ) 
            return this;
        for( ParseNode n : children() )
            if( n.from <= head && tail <= n.to ) 
                return n.locate(head, tail);
        return null;
    }
    
    /**
     * Parent of the ParseNode[head,tail), not "this" (which assumed to be the root)
     * @param head
     * @param tail
     * @return
     */
    public ParseNode parent( int head, int tail ) {
        for( ParseNode descendant : intermediates(head, tail) )
            for( ParseNode child : descendant.children() )
                if( child.from == head && child.to == tail )
                    return descendant;
        return null;
    }

    void print( int depth ) {
        System.out.println(toString(depth)); // (authorized)
    }
    public ParseNode leafAtPos( int pos ) {
        if( children().size() == 0 && pos == from )
            return this;
        for( ParseNode child : children() ) {
            if( child.from <= pos && pos < child.to ) 
                return child.leafAtPos(pos);
        }
        return null;
    }
    private void calculateDepth( Map<Integer,Integer> depthMap, int depth ) {
        depthMap.put(Util.pair(from,to), depth);
        for( ParseNode child : children() ) {
            child.calculateDepth(depthMap, depth+1);
        }
    }
    Map<Integer,Integer> calculateDepth() {
        Map<Integer,Integer> depthMap = new TreeMap<Integer,Integer>();
        calculateDepth(depthMap, 0);
        return depthMap;
    }
    public void printTree() {
        Map<Integer,Integer> depthMap = calculateDepth();

        int i = 0;
        for( ParseNode n : descendants() ) {
            //if( i++>500 ) {
        	    //System.out.println("...");
                //return;}
            int depth = depthMap.get(Util.pair(n.from, n.to));
            n.print(depth);
        }
    }
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
     * @param src -- scanner output
     * @return -- scanner content corresponding to the parse node
     */
    public String content( List<LexerToken> src ) {
        StringBuilder sb = new StringBuilder();
        for( int i = from; i < to; i++ )
            sb.append(src.get(i).content);
        return sb.toString();
    }

    //////////////////// Implementation (Former subclass) ///////////////////
    ParseNode lft = null;
    ParseNode rgt = null;

    private Set<Integer> content = new HashSet<Integer>();
    public Set<Integer> content() {
    	return content;
    }
    public void addContent( int symbol ) {
    	content.add(symbol);
    }
    /**
     * Check if the node contains "symbol"
     * If "reducedContent" is not null then refer to this map
     */
    public boolean contains( int symbol ) {
        return content().contains(symbol);
    }

    // If fail to parse complete text, then accumulate all children here
    // if topLevel != null then lft and rgt == null, and content is empty. 
    public Set<ParseNode> topLevel = null;
    
    public void addTopLevel( ParseNode child ) {
        if( topLevel == null )
            topLevel = new TreeSet<ParseNode>();
        topLevel.add(child);
    }

    private Parser cyk;
    public ParseNode( int begin, int end, int sIn, int sOut, Parser c ) {
        from = begin;
        to = end;
        content.add(sIn);
        content.add(sOut);
        cyk = c;
    }
    
    ParseNode parent;
    public ParseNode parent() {
        if( parent == null )
            return null;
        if( !parent.isAuxiliary() )
            return parent;
        return parent.parent();
    }


    protected String toString( int depth ) {      
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < depth ;i++)
            sb.append("  ");  //$NON-NLS-1$
        sb.append(interval()+" ");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for( Integer i : content() ) {
            if( i==-1 )
                continue;
        	String symbol = cyk.allSymbols[i];
        	//symbol = symbol.endsWith(")")?("\""+symbol+"\""):symbol;
        	//symbol = symbol.endsWith(")")?"":symbol;
        	sb.append("  "+ symbol); //$NON-NLS-1$
        }
        return sb.toString();
    }
    
	public String interval() {
		return "["+from+","+to+")";
	}

    // careful changing this method: parse tree traversal depends on it
    public boolean isAuxiliary() {
    	if( contains(-1) )
    		return true;
    	
    	if( from+1 == to )
    		return false;
    	
        boolean noneAux = true;
        boolean containsConcat = false;
        boolean containsRawBnf = false;
        boolean containsBlock = false;
        for( Integer symbol : content() ) {
            String symb = cyk.allSymbols[symbol];
			if( symb.indexOf("+") < 0  //$NON-NLS-1$
             || "'+'".equals(symb)  //$NON-NLS-1$
            ) {
                noneAux = false;
                //break;
            }
			if(  "concat".equals(symb) ) //$NON-NLS-1$ 
				containsConcat = true;
			if(  "rawbnf".equals(symb) ) //$NON-NLS-1$ 
				containsRawBnf = true;		               		            
			if(  "block".equals(symb) ) //$NON-NLS-1$ 
				containsBlock = true;		               		            
        }
        //if( containsConcat && !containsRawBnf && !containsBlock )
        	//return true;
        return noneAux;   
    }


    // navigates through children skipping all the auxiliary nodes
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


    public static void main(String[] args) throws Exception {
        //System.out.println(CYK.allSymbols[1660]);
        //System.out.println(CYK.symbolIndexes.get("'SELECT'+select_list+table_expression"));
    }
}


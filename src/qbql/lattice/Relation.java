package qbql.lattice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.RuleTuple;
import qbql.parser.Token;
import qbql.util.Permutations;
import qbql.util.Util;

public class Relation extends Predicate {

    Set<Tuple> content = new HashSet<Tuple>(); 
    //private Set<Tuple> content = new TreeSet<Tuple>(); switch to for big relations

    public Relation( String[] columns ) {
        super(columns);		
    }

    public void renameInPlace( String from, String to ) {
        if( from.equals(to) )
            return;
        int colFrom = header.get(from);
        Integer colTo = header.get(to);
        if( colTo == null ) {
            colNames[colFrom] = to;
            header.remove(from);
            header.put(to, colFrom);            
        } else {
            HashMap<String,Integer> newHeader = new HashMap<String,Integer>();
            String[] newColNames = new String[colNames.length-1];
            Set<Tuple> newContent = new HashSet<Tuple>();
            for( int i = 0; i < colNames.length; i++ ) 
                if( i < colFrom ) {
                    newColNames[i] = colNames[i];
                    newHeader.put(colNames[i], i);
                } else if( colFrom < i ) {
                    newColNames[i-1] = colNames[i];
                    newHeader.put(colNames[i], i-1);
                }

            for( Tuple t : content ) {
                if( !t.data[colFrom].equals(t.data[colTo]) )
                    continue;
                Object[] newT = new String[newColNames.length];
                for( int i = 0; i < colNames.length; i++ ) 
                    if( i < colFrom ) {
                        newT[i] = t.data[i];
                    } else if( colFrom < i ) {
                        newT[i-1] = t.data[i];
                    }
                newContent.add(new Tuple(newT));
            }

            header = newHeader;
            colNames = newColNames;
            content = newContent;
        }
    }

    public void addTuple( Map<String,Object> content ) {
        Object[] newTuple = new Object[colNames.length];
        Set<String> columns = content.keySet();
        for( String colName : columns )
            newTuple[header.get(colName)] = content.get(colName);
        addTuple(newTuple);
    }
    public void addTuple( Object[] newTuple ) {
        this.content.add(new Tuple(newTuple));

        if( this.content.size() > 100 && this.content instanceof HashSet ) {
            Set<Tuple> newContent = new TreeSet<Tuple>();
            for( Tuple t : this.content )
                newContent.add(t);
            this.content = newContent;
        }
    }

    public Set<Tuple> getContent() {
        return content;		
    }

    private TreeSet<Tuple> getOrderedContent() {
        if( content instanceof TreeSet )
            return (TreeSet<Tuple>)content;
        TreeSet<Tuple> ret = new TreeSet<Tuple>();
        for( Tuple tuple : content ) 
            ret.add(tuple);
        return ret;		
    }


    public String toString( int ident ) {
        if( colNames.length == 0 ) {
            if( content.size() == 0 )
                return "R00";
            else
                return "R01";
        }
        StringBuffer ret = new StringBuffer("");
        //ret.append(header.keySet()+"\n");
        // no commas
        ret.append("[");
        for( int i = 0; i < colNames.length; i++ )
            ret.append((i>0?"  ":"")+colNames[i]);
        ret.append("]\n");
        for( Tuple tuple : getOrderedContent() ) {
            boolean firstTuple = true;
            for( int i = 0; i < tuple.data.length; i++ ) {
                String value = tuple.data[i].toString();
                if( tuple.data[i] instanceof Relation ) // nested relation
                    value = "("+value.replace('\n', ' ')+")";
                ret.append((firstTuple?Util.identln(ident," "):"  ")+value);
                firstTuple = false;
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    public static Relation join( Relation x, Relation y ) {
        Set<String> header = new TreeSet<String>();
        header.addAll(x.header.keySet());
        header.addAll(y.header.keySet());		
        Relation ret = new Relation(header.toArray(new String[0]));
        for( Tuple tupleX: x.content )
            for( Tuple tupleY: y.content ) {				
                Object[] retTuple = new Object[header.size()];
                for( String attr : ret.colNames ) {
                    Integer xAttr = x.header.get(attr);
                    Integer yAttr = y.header.get(attr);
                    if( xAttr == null )
                        retTuple[ret.header.get(attr)] = tupleY.data[y.header.get(attr)];
                    else if( yAttr == null )
                        retTuple[ret.header.get(attr)] = tupleX.data[x.header.get(attr)];
                    else {
                        if( !tupleY.data[y.header.get(attr)].equals(tupleX.data[x.header.get(attr)]) ) {
                            retTuple = null;
                            break;
                        } else
                            retTuple[ret.header.get(attr)] = tupleX.data[x.header.get(attr)];
                    }
                }
                if( retTuple != null )
                    ret.content.add(new Tuple(retTuple));
            }
        return ret;
    }

    public static Relation directProduct( Relation x, Relation y ) {
        Set<String> header = new TreeSet<String>();
        header.addAll(x.header.keySet());
        header.retainAll(y.header.keySet());		
        Relation ret = new Relation(header.toArray(new String[0]));
        for( Tuple tupleX: x.content )
            for( Tuple tupleY: y.content ) {				
                Object[] retTuple = new Object[header.size()];
                for( String attr : ret.colNames ) {
                    Integer xAttr = x.header.get(attr);
                    Integer yAttr = y.header.get(attr);
                    String xVal = tupleX.data[x.header.get(attr)].toString();
                    if( xVal.charAt(0)=='"' )  // make it associative
                    	xVal = xVal.substring(1,xVal.length()-1);
                    String yVal = tupleY.data[y.header.get(attr)].toString();
                    if( yVal.charAt(0)=='"' )  // make it associative
                    	yVal = yVal.substring(1,yVal.length()-1);
                    if( xVal.equals(yVal) )
                    	retTuple[ret.header.get(attr)] = tupleX.data[x.header.get(attr)];  // object, not string comparison!
                    else if( 0 < xVal.compareTo(yVal) )
                    	retTuple[ret.header.get(attr)] = "\""+yVal+","+xVal+"\"";
                    else 
                    	retTuple[ret.header.get(attr)] = "\""+xVal+","+yVal+"\"";
                }
                if( retTuple != null )
                    ret.content.add(new Tuple(retTuple));
            }
        return ret;
    }
    
    public static Relation union( Relation x, Relation y ) {
        Set<String> header = new TreeSet<String>();
        header.addAll(x.header.keySet());
        header.retainAll(y.header.keySet());		
        Relation ret = new Relation(header.toArray(new String[0]));
        for( Tuple tupleX: x.content ){
            Object[] retTuple = new Object[header.size()];
            for( String attr : ret.colNames ) {
                retTuple[ret.header.get(attr)] = tupleX.data[x.header.get(attr)];
            }
            ret.content.add(new Tuple(retTuple));
        }
        for( Tuple tupleY: y.content ){
            Object[] retTuple = new Object[header.size()];
            for( String attr : ret.colNames ) {
                retTuple[ret.header.get(attr)] = tupleY.data[y.header.get(attr)];
            }
            ret.content.add(new Tuple(retTuple));
        }
        return ret;
    }


    public boolean equals( Object o ) {
        if( !super.equals(o) )
            return false;
        Relation src = (Relation) o;
        if( content.size() != src.content.size() )
            return false;
        LinkedList<Tuple> list = new LinkedList<Tuple>();
        for( Tuple t : content )
            list.add(t);
        while( list.size() > 0 &&  matchAndDelete(list, src) )
            ;
        return list.size()==0;
    }
    private boolean matchAndDelete( LinkedList<Tuple> list, Relation y ) {
        Tuple tx = list.getFirst();
        for( Tuple ty : y.content ) {
            if( tx.equals(ty,this,y) ) {
                list.removeFirst();
                return true;
            }
        }
        return false;
    }

    // rename is in-place operation.
    // therefore auxiliary methods to restore relation header after each rename
    /*private static String[] cloneColumnNames( Relation src ) {
        String[] origColNames = new String[src.colNames.length];
        for( int i = 0; i < origColNames.length; i++ )
            origColNames[i] = src.colNames[i];
        return origColNames;
    }
    private static Map<String, Integer> cloneHeader( Relation src ) {
        Map<String,Integer> origHdr = new HashMap<String,Integer>();
        for( String key : src.header.keySet() )
            origHdr.put(key, src.header.get(key));
        return origHdr;
    }*/

    public int hashCode() {
        StringBuffer ret = new StringBuffer();
        ret.append(colNames.length);
        ret.append(content.size());
        if( colNames.length > 0 && content.size() > 0 ) {
            for( Tuple t : content ) {
                ret.append(t.data[0]);
                break;
            }
        }
        return ret.toString().hashCode();
    }

    private static boolean map( Relation x, Relation y, int colX, int colY ) {
        boolean matchedAllRows = true;
        for ( Tuple tx: x.content ) { 
            boolean matchedX = false;
            for ( Tuple ty: y.content ) {
                if( !tx.data[colX].equals(ty.data[colY]) )
                    continue;
                matchedX = true;
                break;
            }
            if( !matchedX ) {
                matchedAllRows = false;
                break;
            }
        }
        if( matchedAllRows )
            return true;
        return false;
    }
    static boolean match( Relation x, Relation y, int colX, int colY ) {
        return map(x,y, colX,colY) && map(y,x, colY,colX);
    }

    static Relation renameCols( Relation x, Relation y, int[] indexes ) {
        Relation ret = y.clone(); 
        for( int i = 0; i < ret.colNames.length; i++) 
            if( indexes[i] < 0 ) 
                ret.renameInPlace(y.colNames[i], y.colNames[i]+"1");
        Map<String,String> rename = new HashMap<String,String>();
        for( int i = 0; i < ret.colNames.length; i++) 
            if( 0 <= indexes[i] ) 
                rename.put(y.colNames[i], x.colNames[indexes[i]]);     
        for( String from : rename.keySet() ) 
            ret.renameInPlace(from, rename.get(from));
        return ret;
    }

    protected Relation clone() {
        return join(this,Database.R01);
    }

    public static Predicate count( Predicate lft, Predicate rgt ) {
        if( !(lft instanceof Relation) || !(rgt instanceof Relation) )
            throw new AssertionError("!(lft instanceof Relation) || !(rgt instanceof Relation)");
        Relation x = (Relation)lft;
        Relation y = (Relation)rgt;
        Relation xvy = union(x, y);		
        Set<String> header = new TreeSet<String>();
        header.addAll(y.header.keySet());
        Relation ret = new Relation(header.toArray(new String[0]));
        for( Tuple tupleXvY: xvy.content ){
            Object[] retTuple = new Object[header.size()];
            int cnt = 0;
            for( Tuple tupleX : x.content ) {
                boolean matched = true;
                for( String attr : xvy.colNames )
                    if( !tupleX.data[x.header.get(attr)].equals(tupleXvY.data[xvy.header.get(attr)]) )
                        matched = false;
                if( matched )
                    cnt++;
            }

            for( String attr : ret.colNames ) {
                Object cell = x.header.get(attr);
                if( cell != null )
                    retTuple[ret.header.get(attr)] = tupleXvY.data[xvy.header.get(attr)];
                else
                    retTuple[ret.header.get(attr)] = cnt;
            }
            ret.content.add(new Tuple(retTuple));
        }
        return ret;
    }

    public Predicate CPclosure() {
        if( this.equals(Database.R00) )
            return Database.R00;
        
        Map<String, Relation> domains = new HashMap<String, Relation>();
        for( String col : colNames )
            domains.put(col, new Relation(new String[]{col}));

        for( Tuple t : getContent() )
            for( int i = 0; i < t.data.length; i++ ) {
                domains.get(colNames[i]).addTuple(new Object[]{t.data[i]});
            }

        Map<String, Object[]> doms = new TreeMap<String, Object[]>();
        for( String r : domains.keySet() ) {
            Set<Tuple> tuples = domains.get(r).getContent();
            Object[] content = new Object[tuples.size()];
            int i = 0;
            for( Tuple t : tuples )
                content[i++] = t.data[0];
            doms.put(r, content);
        }
        
        Relation ret = new Relation(doms.keySet().toArray(new String[0]));
        
        try {
            Map<String, Integer> indexes = new HashMap<String, Integer>();
            for (String key: doms.keySet())
                indexes.put(key, 0);
            do {
                Object[] t = new Object[ ret.colNames.length ];
                for (String key: doms.keySet())
                    t[ ret.header.get(key) ] = doms.get(key)[ indexes.get(key) ];

                ret.addTuple(t);

            } while (Database.next(indexes, doms));
        } catch( Exception e ) { // for empty domains
        }        
        
        return ret;
    }

}

package qbql.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.index.NamedTuple;
import qbql.lattice.Relation;

public abstract class Util {

    public static String readFile( String file ) throws IOException {
        return readFile(new FileInputStream(file));
    }

    public static String readFile( Class c, String file ) throws IOException {
        URL u = c.getResource( file );
        String fname = u.getFile();        
        fname = fname.substring(3);   // truncate /C:/    
		return new String(Files.readAllBytes(Paths.get(fname)));
    }

    public static String readFile(InputStream is) throws IOException {
        byte[] bytes = new byte[4096];
        int bytesRead = 0;
        BufferedInputStream bin = null;
        StringBuilder ret = new StringBuilder();
        try {
            bin = new BufferedInputStream(is);
            bytesRead = bin.read(bytes, 0, bytes.length);
            while (bytesRead != -1) {
                ret.append(new String(bytes).substring(0, bytesRead));
                bytesRead = bin.read(bytes, 0, bytes.length);
            }
        } finally {
            if (bin != null)
                bin.close();
        }
        return ret.toString();
    }

    public static String toNull( String src ) {
        return "".equals(src)? null : src;
    }

    public static String identln( int level, String txt ) {
        StringBuilder b = new StringBuilder();
        for(int i = 0; i< level;i++)
            b.append(" "); 
        b.append(txt); 
        return b.toString();
    }
    public static String padln( String txt, int newsize ) {
        StringBuilder b = new StringBuilder(txt);
        for(int i = txt.length(); i < newsize; i++) {
            b.append(" "); 
        }
        return b.toString();
    }
    
    public static String removeComments( String txt ) {
        int beginMlComment = txt.indexOf("/*");
        int beginSlComment = txt.indexOf("--");
		if( beginMlComment < 0 && beginSlComment < 0 )
        	return txt;
        int endMlComment = txt.indexOf("*/", beginMlComment);
        int endSlComment = txt.indexOf("\n", beginSlComment);
		if( endMlComment < 0 && endSlComment < 0)
        	throw new AssertionError("Comment not ending?");
		int beginComment = beginMlComment < beginSlComment ? beginSlComment : beginMlComment;
		int endComment = beginMlComment < beginSlComment ? endSlComment+1 : endMlComment+2;
        return removeComments(txt.substring(0, beginComment)+txt.substring(endComment));
    }
    
    
    public static String[] union( String[] x, String[] y ) {
        Set<String> ret = new TreeSet<String>();
        for( String s : x )
            ret.add(s);
        for( String s : y )
            ret.add(s);
        return ret.toArray(new String[0]);
    }
    public static String[] intersect( String[] x, String[] y ) {
        Set<String> ret = new TreeSet<String>();
        for( String sx : x )
            for( String sy : y ) 
                if( sx.equals(sy) ) {
                    ret.add(sx);
                    break;
                }
        return ret.toArray(new String[0]);
    }
    public static String[] symmDiff( String[] x, String[] y ) {
        Set<String> ret = new TreeSet<String>();
        for( String s : x )
            ret.add(s);
        for( String s : y ) {
        	boolean contains = false;
            for( String t : x )
            	if( t.equals(s) ) {
            		contains = true;
            		break;
            	}
            if( contains )
                ret.remove(s);
            else
                ret.add(s);
        }
        return ret.toArray(new String[0]);
    }

    // interval indexes
    public static int pair( int x, int y ) {
        return (y<<16)|x;
    }
    public static int Y( int p ) {
        return p>>16;
    }
    public static int X( int p ) {
        return p&0xffff;
    }
    // x--; y--
    public static int decrPair( int p ) {
        return p-0x10001;
    }

    //-----------------------------------------
    public static long lPair( int x, int y ) {
        return ((long)y << 32) | (long)x;
    }

    public static int lY( long p ) {
        return (int) (p >> 32);
    }

    public static int lX( long p ) {
        return (int)p;
    }
    
    public static long addlX( long pair, int x ) {
    	return pair+x; 
    }
    
    public static long addlY( long pair, int y ) {
    	return pair+ (((long)y) << 32); 
    }    
    //----------------------------------
    
    // [14,5) -> 5
    public static int Y( String interval ) {
        return Integer.parseInt(interval.substring(interval.indexOf(',')+1,interval.length()-1));
    }
    // [14,5) -> 14
    public static int X( String interval ) {
        return Integer.parseInt(interval.substring(1,interval.indexOf(',')));
    }
    
    /*
     * State vector iterator
     */
    public static boolean next( int[] state, int limit ) {
        return next(state, limit, 0);
    }
    public static boolean next( int[] state, int limit, int init ) {
        for( int pos = 0; pos < state.length; pos++ ) {
            if( state[pos] < limit-1 ) {
                state[pos]++;
                return true;
            }
            state[pos] = init;                             
        }
        return false;
    }
    
    public static Object keyForAValue( Map m, Object value ) {
    	for( Object key : m.keySet() )
    		if( m.get(key).equals(value) )
    			return key;
    	return null;
    }

    public static void main( String[] args ) throws Exception {
        String[] src = {"a", "b", "c"};
        String[] target = {"x", "y"};
        int[] indexes = new int[src.length];
        for( int i = 0; i < indexes.length; i++ )
            indexes[i] = 0;
        do {
            for( int i = 0; i < indexes.length; i++ ) {
                System.out.print((i!=0?",":"")+src[i]+"->"+target[indexes[i]]);
            }
            System.out.println();
        } while ( next(indexes,target.length) );
    }

    public static String[] clone( String[] colNames ) {
        String[] ret = new String[colNames.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = colNames[i];
        }
        return ret;
    }

    public static Number max( Number x, Number y ) {
        if( x instanceof Long && y instanceof Long )
            return x.longValue() > y.longValue() ? x.longValue() : y.longValue();
        if( x instanceof Integer && y instanceof Integer )
            return x.intValue() > y.intValue() ? x.intValue() : y.intValue();
        if( x instanceof Double || y instanceof Double )
            return x.doubleValue() > y.doubleValue() ? x.doubleValue() : y.doubleValue();
        if( x instanceof Float || y instanceof Float )
            return x.floatValue() > y.floatValue() ? x.floatValue() : y.floatValue();
        //if( x instanceof BigDecimal && y instanceof BigDecimal )
            //return ((BigDecimal)y).gradd((BigDecimal)x);
        throw new AssertionError("? + ?");
    }
    public static Number plus( Number x, Number y ) {
        if( x instanceof Long && y instanceof Long )
        	return x.longValue()+y.longValue();
        if( x instanceof Integer && y instanceof Integer )
        	return x.intValue()+y.intValue();
        if( x instanceof Double || y instanceof Double )
        	return x.doubleValue()+y.doubleValue();
        if( x instanceof Float || y instanceof Float )
        	return x.floatValue()+y.floatValue();
        if( x instanceof BigDecimal && y instanceof BigDecimal )
        	return ((BigDecimal)y).add((BigDecimal)x);
        throw new AssertionError("? + ?");
    }
    public static Number minus( Number x, Number y ) {
        if( x instanceof Long && y instanceof Long )
        	return x.longValue()-y.longValue();
        if( x instanceof Integer && y instanceof Integer )
        	return x.intValue()-y.intValue();
        if( x instanceof Double || y instanceof Double )
        	return x.doubleValue()-y.doubleValue();
        if( x instanceof Float || y instanceof Float )
        	return x.floatValue()-y.floatValue();
        if( x instanceof BigDecimal && y instanceof BigDecimal )
        	return ((BigDecimal)y).subtract((BigDecimal)x);
        throw new AssertionError("? - ?");
    }
    public static Number times( Number x, Number y ) {
        if( x instanceof Long && y instanceof Long )
        	return x.longValue()*y.longValue();
        if( x instanceof Integer && y instanceof Integer )
        	return x.intValue()*y.intValue();
        if( x instanceof Double || y instanceof Double )
        	return x.doubleValue()*y.doubleValue();
        if( x instanceof Float || y instanceof Float )
        	return x.floatValue()*y.floatValue();
        if( x instanceof BigDecimal && y instanceof BigDecimal )
        	return ((BigDecimal)y).multiply((BigDecimal)x);
        throw new AssertionError("? * ?");
    }
    public static Number divide( Number x, Number y ) {
        if( x instanceof Long && y instanceof Integer 
         || x instanceof Integer && y instanceof Long 
         || x instanceof Integer && y instanceof Integer 
         || x instanceof Long && y instanceof Long 
        ) {
        	long ret = x.longValue()/y.longValue();
        	if( ret*y.longValue()==x.longValue() )
        		return ret;
        	return x.doubleValue()/y.doubleValue();
        } if( x instanceof Double || y instanceof Double )
        	return x.doubleValue()/y.doubleValue();
        if( x instanceof Float || y instanceof Float )
        	return x.floatValue()/y.floatValue();
        if( x instanceof BigDecimal && y instanceof BigDecimal )
        	return ((BigDecimal)y).divide((BigDecimal)x);
        throw new AssertionError("? - ?");
    }
}

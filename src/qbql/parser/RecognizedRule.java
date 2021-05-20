package qbql.parser;

import java.util.List;
import java.util.Map;

import qbql.parser.Parser.Tuple;

/**
 * *Partially* recognized tuple  
 * @author Dim
 *
 */
public class RecognizedRule extends RuleTuple {
	public int X = -1;
	public int Y = -1;
	public int weight = 0;
	
    public int pos; // Earley progressed till pos
    
    private Earley earley; 
    public RecognizedRule( Earley earley, String head, String[] rhs, int pos, int x, int y, int w ) {
        super(head, (String[])null);
        this.earley = earley;
        this.pos = pos;
        int len = rhs.length;
        if( pos+2 <= len  ) {
            len = pos+2;
            this.rhs = new String[len];
            for( int i = 0; i < pos+1; i++ ) 
                this.rhs[i] = rhs[i];
            this.rhs[pos+1] = "...";
        } else
            this.rhs = rhs;
        X = x;
        Y = y;
        weight = w;
    }
    public RecognizedRule( RecognizedRule src, int at, int[] replacement ) {
        super(src.head, (String[])null);
        if( at < src.pos )
            pos = src.pos+replacement.length-1;
        else
            pos = src.pos;
        int len = src.rhs.length+replacement.length-1;
        if( pos+2 <= len  )
            len = pos+2;
        rhs = new String[len];
        for( int i = 0; i < at; i++) 
            rhs[i] = src.rhs[i];
        for( int i = 0; i < replacement.length && at+i < len ; i++ ) 
            rhs[at+i] = earley.allSymbols[replacement[i]];
        for( int i = at+1; i < src.rhs.length && i+replacement.length-1 < len ; i++ )
            rhs[i+replacement.length-1] = src.rhs[i];
        if( len == pos+2 )
            rhs[len-1]="...";
        X = src.X;
        Y = src.Y;
        weight = src.weight;
    }
    public boolean isTruncated() {
        return pos==rhs.length-2 && "...".equals(rhs[rhs.length-1]);
    }
    /**
     * head: 'CREATE' 'TABLE' >!< identifier  <-- redundant
     * head: 'CREATE' 'TABLE' >!< identifier ...
     * @return
     */
    public boolean isRedundant( List<RecognizedRule> rules ) {
        boolean ret = false;
        for( RecognizedRule candidate : rules ) {
            if( !head.equals(candidate.head) )
                continue;
            if( pos != candidate.pos )
                continue;
            if( !candidate.isTruncated() )
                continue;
            if( rhs.length+1 != candidate.rhs.length )
                continue;
            ret = true;
            for( int i = 0; i < rhs.length; i++ ) {
                if( !rhs[i].equals(candidate.rhs[i]) ) {
                    ret = false;
                    break;
                }
            }
            if( ret == true )
                return ret;                
       }
       return ret;
    }
    
    public static void merge( List<RecognizedRule> rules, List<RecognizedRule> addendum ) {
        for( RecognizedRule candidate : addendum ) {
            boolean matches = false;
            for( RecognizedRule cmp : rules ) 
                if( cmp.equals(candidate) ) {
                    matches = true;
                    break;
                }
            if( !matches )
                rules.add(candidate);
        }
    }
    
    @Override
    public int compareTo( Object obj ) {
        int cmp = super.compareTo(obj);
        if( cmp != 0 )
            return cmp;
        int posDelta = pos - ((RecognizedRule)obj).pos;
        if( posDelta != 0 )
        	return posDelta;
        int xDelta = X - ((RecognizedRule)obj).X;
        if( xDelta != 0 )
        	return xDelta;
        int yDelta = Y - ((RecognizedRule)obj).Y;
        if( yDelta != 0 )
        	return yDelta;
        return weight - ((RecognizedRule)obj).weight;
    }
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        if( head!=null )
            b.append(head+":"); //$NON-NLS-1$
        int i = -1;
        for( String t: rhs ) {
            i++;
            if( i==pos )
                b.append(" >!<");
            b.append(" "+t); //$NON-NLS-1$
        }
        return b.toString();
    }

}

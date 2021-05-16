package qbql.bool;

public class Oper {
    public static boolean le( long a, long b ) {
        return (a & b) == a;
    }

    /**
     * Next boolean vector (next integer) ignoring masked components
     * @param x
     * @param mask
     * @return
     */
    public static long next( long x, long mask, long dimension ) {
        if( !le(mask,x) )
            throw new AssertionError("!le(mask,x)");
        long u = unmask(x,mask,dimension);
        return mask(u+1,mask,dimension+1);
    }
    private static long unmask( long x, long mask, long dimension ) {
        if( !le(mask,x) )
            throw new AssertionError("!le(mask,x)");
        long ret = 0;
        int shift = 0;
        for( int j = 0; j < dimension; j++ ) {
            long powJ = 1L << j;
            if( (powJ & mask) == powJ ) {
                shift++;
                continue;
            }
            if( (powJ & x) == powJ ) {
                long xComp = 1L << (j - shift);
                ret = ret | xComp;
            }
        }
        return ret;
    }
    private static long mask( long x, long mask, long dimension ) {
        long ret = mask;
        int shift = 0;
        for( int j = 0; j < dimension; j++ ) {
            long powJ = 1L << j;
            if( (powJ & mask) == powJ ) {
                shift++;
                continue;
            }
            long xComp = 1L << (j - shift);
            if( (xComp & x) == xComp ) {
                ret = ret | powJ;
            }
        }
        return ret;
    }
    
    static String toString( long x, int dim ) {
        StringBuilder ret = new StringBuilder();
        for( int j = dim-1; 0 <= j; j-- ) {
            long bit = 1L << j;
            if( bit == (bit & x) )
                ret.append('1');
            else
                ret.append('0');
        }
        return ret.toString();
    }


    public static void main( String[] args ) {
        for( int i = 0; i < 1024; i++ ) {
            int m = 43;
            if( unmask(mask(i,m,15),m,15)!=i )
                System.out.println( "mask("+i+")="+mask(i,m,15) );
            
        }
    }
}


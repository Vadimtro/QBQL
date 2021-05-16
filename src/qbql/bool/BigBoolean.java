package qbql.bool;

public class BigBoolean {
    long[] payload;
    int dimension;
    
    /**
     * @param size = dimension * 64
     */
    BigBoolean( int dimension ) {
        this.dimension = dimension;
        payload = new long[dimension/64+1];
    }
    BigBoolean( String src ) {
        dimension = src.length();
        int size = dimension/64+1;
        payload = new long[size];
        for( int i = 0; i < payload.length; i++ ) {            
            String chunk = src.substring(i*64);
            if( (i+1)*64 < src.length() )
                chunk = chunk.substring(0, 64);
            for( int j = 0; j < chunk.length(); j++ ) {
                char elem = chunk.charAt(j);
                if( '1' == elem ) {
                    long bit = 1L << j;
                    payload[i] = payload[i] | bit;
                }
            }
        }
    }
    // seems will be able to accomodate small vecors only 
    BigBoolean( long payload, int dimension ) {
        this.dimension = dimension;
        this.payload = new long[1];
        this.payload[0] = payload;
    }

    
    public static BigBoolean conjunction( BigBoolean a, BigBoolean b ) {
        if( a.dimension != b.dimension )
            throw new AssertionError("a.dimension != b.dimension");
        BigBoolean ret = new BigBoolean(a.dimension);
        for( int i = 0; i < a.payload.length; i++ ) 
            ret.payload[i] = a.payload[i] & b.payload[i];        
        return ret;
    }
    public BigBoolean complement() {
        BigBoolean ret = new BigBoolean(dimension);
        for( int i = 0; i < payload.length; i++ ) 
            ret.payload[i] = ~payload[i];        
        return ret;
    }
        
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for( int i = 0; i < payload.length; i++ ) {
            long chunk = payload[i];
            int dim = 64;
            if( i+1 == payload.length )
                dim = dimension - i*64;
            for( int j = 0; j < dim; j++ ) {
                long bit = 1L << j;
                if( bit == (bit & chunk) )
                    ret.append('1');
                else
                    ret.append('0');
            }
        }
        return ret.toString();
    }

    public static void main( String[] args ) {
        BigBoolean a = new BigBoolean("11001010101000011111000101101010101010100000001100011000011011011101001101010011");
        System.out.println(a.complement().toString());
        System.out.println(a.toString());
        BigBoolean b = new BigBoolean("10000100010101010100000010001000100010010010000010000100000101000001000001111100");
        System.out.println(b.toString());
        System.out.println(BigBoolean.conjunction(a, b).toString());
    }
    
}

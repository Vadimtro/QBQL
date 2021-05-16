package qbql.bool;


// up to 32 bit only
public class UnaryOperator  {
    int[] map;
    int dimension;
    
    UnaryOperator( int dimension ) {
        this.dimension = dimension;
        int size = 1 << dimension;
        map = new int[size];
        for( int i = 0; i < map.length; i++ ) {
			map[i]=i;
		}
    }
    
    UnaryOperator next() {
        for( int i = 0; i < map.length; i++ ) {
            for( int candidate = (int)Oper.next(map[i], i, dimension); candidate < map.length; candidate = (int)Oper.next(candidate, i, dimension)) {
                if( !validateIncrement(i, candidate) ) 
                    continue;
                if( i != candidate && candidate != map[candidate] ) // x``= x
                    continue;
                map[i] = candidate;
                for( int j = 0; j < i; j++ ) {
                    map[j] = j;
                }
                return this;
            }
        }
        return null; // exhausted all operator possibilities
    }

	private boolean validateIncrement( int i, long candidate ) {
		for( int j = 0; j < dimension; j++) {
			int increment = 1 << j;
			if( (i & increment) == increment )
				continue;
			int higherNeighbour = i | increment;
			if( map.length-1 < higherNeighbour )
				continue;
			if( !Oper.le(candidate, map[higherNeighbour]) )
				return false;
		}
		return true;
	}

    
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for( int i = 0; i < map.length; i++) {
            if( i == map[i] )
                continue;
            for( int j = dimension-1; 0 <= j; j-- ) {
                long bit = 1L << j;
                if( bit == (bit & i) )
                    ret.append('1');
                else
                    ret.append('0');
            }
            ret.append("->");
            for( int j = dimension-1; 0 <= j; j-- ) {
                long bit = 1L << j;
                if( bit == (bit & map[i]) )
                    ret.append('1');
                else
                    ret.append('0');
            }
            ret.append('\n');           
        }
        return ret.toString();
    }
    
    
} 

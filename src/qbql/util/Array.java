package qbql.util;


/**
 * Ordered array of integers/longs
 * @author Dim
 *
 */
public class Array {
	
	public static void main( String[] args ) throws Exception {
		/*int[] a = {2,3,6,10};
		System.out.println(indexOf(a, 0, a.length-1, -1));
		System.out.println(indexOf(a, 2));
		int[] b = insert(a,22);
		for( int i = 0; i < b.length; i++ ) {
			System.out.print(","+b[i]);
		}*/
		//Service.profile(10000, 50);
		
        /*int[] indexes = new int[4];
        for( int i = 0; i < indexes.length; i++ )
            indexes[i] = 0;
    	do {
    		System.out.println(""+indexes[3]+indexes[2]+indexes[1]+indexes[0]);
    		if( indexes[2]==1 ) {
    			skipPos(2, indexes, 3);
    			continue;
    		}
    	} while( Array.next(indexes,3) );
		*/
	    
	    int size = 1000000;
	    int[] a = new int[size/2];
        int[] b = new int[size/2];
        for( int i = 0; i < a.length; i++ ) 
            a[i] = 2*i;
        for( int i = 0; i < b.length; i++ ) 
            a[i] = 2*i+1;
        
        long t1 = System.nanoTime();        
        Array.merge(a, b);	            
	    System.out.println(System.nanoTime() - t1);
	}

	/**
	 * Find insertion point within an array
	 * @param array
	 * @param value
	 * @return
	 */
	public static int indexOf( int[] array, int value ) {
		return indexOf(array, 0, array.length-1, value);
	}
	public static int indexOf( long[] array, long value ) {
		return indexOf(array, 0, array.length-1, value);
	}
	
	/**
	 * Find insertion point within an array between values x and y
	 * @param array
	 * @param x
	 * @param y
	 * @param value
	 * @return
	 */
	private static int indexOf( int[] array, int xx, int yy, int value ) {
		if( xx+1 == yy || xx == yy )
			return array[xx] < value ? yy : xx;
		int mid = (xx+yy)/2;
		if( value < array[mid] )
			return indexOf(array, xx, mid, value);
		else
			return indexOf(array, mid, yy, value);
		/*for( int i = 0; i < array.length; i++ ) {
			if( x+1 == y || x == y )
				return array[x] < value ? y : x;
			int mid = (x+y)/2;
			if( value < array[mid] )
				y = mid;
			else
				x = mid;			
		}
		throw new AssertionError("indexOf xx="+xx+" yy="+yy);*/
	}
	private static int indexOf( long[] array, int xx, int yy, long value ) {
		if( xx+1 == yy || xx == yy )
			return array[xx] < value ? yy : xx;
		int mid = (xx+yy)/2;
		if( value < array[mid] )
			return indexOf(array, xx, mid, value);
		else
			return indexOf(array, mid, yy, value);
		/*for( int i = 0; i < array.length; i++ ) {
			if( x+1 == y || x == y )
				return array[x] < value ? y : x;
			int mid = (x+y)/2;
			if( value < array[mid] )
				y = mid;
			else
				x = mid;			
		}
		throw new AssertionError("indexOf xx="+xx+" yy="+yy);*/
	}
	
	public static int[] insert( int[] array, int value ) {
		if( array == null || array.length == 0 ) {
			array = new int[1];
			array[0] = value;
			return array;
		}
		
		int index = indexOf(array, 0, array.length, value);
		if( index < array.length && array[index] == value )
			return array;
		
		int[] ret = new int[array.length+1];
		for( int i = 0; i < index; i++ ) {
			ret[i] = array[i];
		}
		ret[index] = value; 
		for( int i = index+1; i < ret.length; i++ ) {
			ret[i] = array[i-1];
		}
		return ret;
	}
	public static long[] insert( long[] array, long value ) {
		if( array == null || array.length == 0 ) {
			array = new long[1];
			array[0] = value;
			return array;
		}
		
		int index = indexOf(array, 0, array.length, value);
		if( index < array.length && array[index] == value )
			return array;
		
		long[] ret = new long[array.length+1];
		for( int i = 0; i < index; i++ ) {
			ret[i] = array[i];
		}
		ret[index] = value; 
		for( int i = index+1; i < ret.length; i++ ) {
			ret[i] = array[i-1];
		}
		return ret;
	}
	
    public static int[] delete( int[] array, int value ) {
        int index = indexOf(array, 0, array.length, value);
        if( index == array.length || array[index] != value )
            return array;
        
        int[] ret = new int[array.length-1];
        for( int i = 0; i < index; i++ ) {
            ret[i] = array[i];
        }
        for( int i = index; i < ret.length; i++ ) {
            ret[i] = array[i+1];
        }
        return ret;
    }
    
    public static int[] delete( int[] array, int minVal, int maxVal ) {
    	if( minVal+1 == maxVal )
    		return delete(array, minVal);
    	
        int iMin = indexOf(array, 0, array.length, minVal);
        if( array[iMin] < minVal )
        	iMin++;
        int iMax = indexOf(array, 0, array.length, maxVal);
        if( array[iMax] > maxVal )
        	iMax--;
        
        if( iMin == iMax )
        	return array;
        
        int[] ret = new int[array.length-(iMax-iMin)];
        for( int i = 0; i < iMin; i++ ) {
            ret[i] = array[i];
        }
        for( int i = iMax; i < array.length; i++ ) {
            ret[i-(iMax-iMin)] = array[i];
        }
        return ret;
    }

    
	public static int[] merge( int[] x, int[] y ) {
		if( x == null )
			return y;
		if( y == null )
			return x;
		
		int m = x.length;
		int n = y.length;
		int[] tmp = new int[m+n];

		int i = 0;
		int j = 0;
		int k = 0;

		while( i < m && j < n ) 
			if( x[i] == y[j] ) {
				tmp[k++] = x[i++];
				j++;
			} else if (x[i] < y[j]) 
				tmp[k++] = x[i++];
			else 
				tmp[k++] = y[j++];
					

		if( i < m ) 
			for( int p = i; p < m; p++ ) 
				tmp[k++] = x[p];
		else
			for( int p = j; p < n; p++ ) 
				tmp[k++] = y[p];
		
		int[] ret = new int[k];
		for( int ii = 0; ii < k; ii++ ) 
			ret[ii] = tmp[ii];		

		return ret;
	}

    public static long[] merge( long[] x, long[] y ) {
        if( x == null )
            return y;
        if( y == null )
            return x;
        
        int m = x.length;
        int n = y.length;
        long[] tmp = new long[m+n];

        int i = 0;
        int j = 0;
        int k = 0;

        while( i < m && j < n ) 
            if( x[i] == y[j] ) {
                tmp[k++] = x[i++];
                j++;
            } else if (x[i] < y[j]) 
                tmp[k++] = x[i++];
            else 
                tmp[k++] = y[j++];
                    

        if( i < m ) 
            for( int p = i; p < m; p++ ) 
                tmp[k++] = x[p];
        else
            for( int p = j; p < n; p++ ) 
                tmp[k++] = y[p];
        
        long[] ret = new long[k];
        for( int ii = 0; ii < k; ii++ ) 
            ret[ii] = tmp[ii];      

        return ret;
    }

	
    /*
     * State vector iterator
     */
    public static boolean next( int[] state, int[] limit ) {
        return next(state, limit, 0);
    }
    public static boolean next( int[] state, int[] limit, int init ) {
        for( int pos = 0; pos < state.length; pos++ ) {
            if( state[pos] < limit[pos]-1 ) {
                state[pos]++;
                return true;
            }
            state[pos] = init;                             
        }
        return false;
    }

}

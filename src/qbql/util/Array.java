package qbql.util;

public class Array {
	
	public static void main( String[] args ) throws Exception {
		int[] a = {2,3,6,10};
		System.out.println(indexOf(a, 0, a.length-1, -1));
		int[] b = insert(a,22);
		for( int i = 0; i < b.length; i++ ) {
			System.out.print(","+b[i]);
		}
		//Service.profile(10000, 50);
	}

	public static int indexOf( int[] array, int value ) {
		return indexOf(array, 0, array.length-1, value);
	}
	
	public static int indexOf( int[] array, int x, int y, int value ) {
		if( x+1 == y || x == y )
			return array[x] < value ? y : x;
		int mid = (x+y)/2;
		if( value < array[mid] )
			return indexOf(array, x, mid, value);
		else
			return indexOf(array, mid, y, value);
	}
	private static int indexOf( long[] array, int x, int y, long value ) {
		if( x+1 == y || x == y )
			return array[x] < value ? y : x;
		int mid = (x+y)/2;
		if( value < array[mid] )
			return indexOf(array, x, mid, value);
		else
			return indexOf(array, mid, y, value);
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

}

package qbql.lattice;

public class Tuple implements Comparable {
    public Object[] data;

    public Tuple( Object[] data ) {
        super();
        this.data = data;
    }

    // careful: this works in the scope of one relation only, 
    // not between different relations
    // see the overloaded equals( Object obj, Relation x, Relation y )
    public boolean equals( Object obj ) {
        Tuple src = (Tuple) obj;
        if( src.data.length != data.length )
            return false;
        for( int i = 0; i < data.length; i++ ) {
        	if( src.data[i] instanceof Double || data[i] instanceof Double )
        		return false;
            if( !src.data[i].equals(data[i]) )
                return false;
        }
        return true;
    }
    /**
     * @param obj
     * @param th -- need to map named perspective into positional
     * @param sr -- the order of th (-->this) and sr (-->src) matters?
     * @return
     */
    public boolean equals( Object obj, Relation th, Relation sr ) {
        Tuple src = (Tuple) obj;
        if( src.data.length != data.length )
            return false;
        for( int i = 0; i < data.length; i++ ) {
            String colName = th.colNames[i];
            int j = sr.header.get(colName);
        	if( src.data[j] instanceof Double || data[i] instanceof Double )
        		return false;
            if( !data[i].equals(src.data[j]) )
                return false;
        }
        return true;
    }
    public boolean matches( Object obj, Relation th, Relation sr ) {
        Tuple src = (Tuple) obj;
        for( int i = 0; i < data.length; i++ ) {
            String colName = th.colNames[i];
            Integer j = sr.header.get(colName);
            if( j == null )
            	continue;
        	if( src.data[j] instanceof Double || data[i] instanceof Double )
        		return false;
            if( !data[i].equals(src.data[j]) )
                return false;
        }
        return true;
    }

    public int hashCode() {
        if( data.length == 0 )
            return 0;
        else
            return data[0].hashCode();
    }

    public int compareTo( Object o ) {
        Tuple src = (Tuple) o;
        return toString().compareTo(src.toString());
    }

    public String toString() {
        StringBuffer ret = new StringBuffer("<");
        for( int i = 0; i < data.length; i++ )
            ret.append((i==0?"":",")+data[i]);
        ret.append(">");
        return ret.toString();
    }


}

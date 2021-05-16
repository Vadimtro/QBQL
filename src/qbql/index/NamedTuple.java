package qbql.index;

import qbql.lattice.Relation;
import qbql.lattice.Tuple;

public class NamedTuple extends Tuple {
    String[] columns;
           
    public NamedTuple( String[] columns, Object[] data ) {
        super(data);
        assert(data.length==columns.length);
        this.columns = columns;
    }
    
    public Object get( String col ) {
        for( int i = 0; i < columns.length; i++ ) {
            if( col.equals(columns[i]) )
                return data[i];
        }
        return null;
    }

    public Relation toRelation() {
        Relation ret = new Relation(columns);
        ret.addTuple(data); 
        return ret;
    }
}

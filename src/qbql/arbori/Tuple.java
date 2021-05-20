package qbql.arbori;

import java.util.HashMap;
import java.util.Map;

import qbql.parser.ParseNode;

public class Tuple implements Comparable<Tuple> {
	ParseNode[] values;

	public Tuple( ParseNode[] attributes ) {
		this.values = attributes;
	}

	@Override
	public int compareTo( Tuple o ) {
		for( int i = 0; i < values.length; i++ ) {
			if( values[i] == null && o.values[i] == null )
				continue;
			if( values[i] == null )
				return -1;
			if( o.values[i] == null )
				return 1;
			int cmp = values[i].compareTo(o.values[i]);
			if( cmp != 0 )
				return cmp;
		}
		return 0;
	}
	
	/**
	 * Utility method to convert a object representtion into a map 
	 * This is useful because arbori callback methods represent tuple as Map<String,ParseNode>
	 * While explicit iteration MaterializedPredicate.getTuples() returns collection of Tuples 
	 * @param container
	 * @return
	 */
	public Map<String,ParseNode> decode( MaterializedPredicate container ) {
		Map<String,ParseNode> tuple = new HashMap<String,ParseNode>();
		for ( int j = 0; j < container.arity(); j++ ) {
			String colName = container.getAttribute(j);
			ParseNode node = container.getAttribute(this, colName); 
			tuple.put(colName, node);
		}
		return tuple;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("<");
		for ( int i = 0; i < values.length; i++ ) {
			if( i > 0 )
				ret.append(",");
			ret.append(values[i].interval());
		}
		return ret.toString()+">";
	}

	
	
}

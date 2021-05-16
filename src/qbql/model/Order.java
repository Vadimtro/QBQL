package qbql.model;

import java.util.HashSet;
import java.util.Set;

import qbql.util.Util;

public class Order {
	private int cardinality = -1;
	Set<Long> relation = new HashSet<Long>();
	
	public int cardinality() {
		if( 0 < cardinality )
			return cardinality;
		for( Long pair : relation ) {
			int x = Util.lX(pair);
			if( cardinality < x+1 )
				cardinality = x+1;
			int y = Util.lY(pair);
			if( cardinality < y+1 )
				cardinality = y+1;
		}
		return cardinality;
	}
	
	public static Order totalOrder( int n ) {
		Order ret = new Order();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= i; j++) {
				ret.relation.add(Util.lPair(i,j));
			}
		}
		return ret;
	}
			
	public static void main( String[] args ) {
		int n = 5;
		Order t = totalOrder(n);
		System.out.println(t.cardinality());
	}
		
}

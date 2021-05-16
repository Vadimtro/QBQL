package qbql.lang;

import java.math.BigDecimal;

import qbql.index.NamedTuple;

public class Exp {
    public static String[] getSymbolicNames() {
    	return new String[] {
        		"y = exp(x)",
        		"exp(x) = y",
        		"x = ln(y)",
        		"ln(y) = x",
    	};
    }

    public static NamedTuple x_y( Object x  ) {
        String[] columns = new String[]{"y"};
        if( x instanceof Integer  )
        	return new NamedTuple(columns,new Object[]{Math.exp((Integer)x)});
        if( x instanceof Float  )
        	return new NamedTuple(columns,new Object[]{Math.exp((Float)x)});
        if( x instanceof Double  )
        	return new NamedTuple(columns,new Object[]{Math.exp((Double)x)});
        //if( x instanceof BigDecimal & y instanceof BigDecimal )
        	//return new NamedTuple(columns,new Object[]{((BigDecimal)x).add((BigDecimal)y)});
        if( x instanceof String  ) {
        	Double d = Double.valueOf((String)x);
        	return new NamedTuple(columns,new Object[]{Math.exp((Double)d)});
        }
        throw new AssertionError("exp(?)");
    }

    public static NamedTuple y_x( Object y  ) {
        String[] columns = new String[]{"x"};
        if( y instanceof Integer  )
        	return new NamedTuple(columns,new Object[]{Math.log((Integer)y)});
        if( y instanceof Float  )
        	return new NamedTuple(columns,new Object[]{Math.log((Float)y)});
        if( y instanceof Double  )
        	return new NamedTuple(columns,new Object[]{Math.log((Double)y)});
        //if( x instanceof BigDecimal & y instanceof BigDecimal )
        	//return new NamedTuple(columns,new Object[]{((BigDecimal)x).add((BigDecimal)y)});
        if( y instanceof String  ) {
        	try { 
        		Double d = Double.valueOf((String)y);
        		return new NamedTuple(columns,new Object[]{Math.log((Double)d)});
        	} catch( Exception e ) {
        		throw new AssertionError("ln(var) -- postpone eval");
        	}
        }
        throw new AssertionError("ln(?)");
    }

}

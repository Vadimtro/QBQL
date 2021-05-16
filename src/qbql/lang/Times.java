package qbql.lang;

import java.math.BigDecimal;

import qbql.index.NamedTuple;
import qbql.util.Util;

public class Times {
    public static String[] getSymbolicNames() {
    	return new String[] {
        		"x * y = z",
        		"z / y = x",
        		"z = x * y",
        		"x = z / y",
    	};
    }

    public static NamedTuple x_y_z( 
            Object x, Object y 
    ) {
        String[] columns = new String[]{"z"};
        if( !(x instanceof Number) ) 
            throw new AssertionError("!(x instanceof Number)");
        if( !(y instanceof Number) ) 
            throw new AssertionError("!(y instanceof Number)");
        Number nX = (Number)x;
        Number nY = (Number)y;
        Number z = Util.times(nX, nY); 
        return new NamedTuple(columns,new Object[]{z});
    }
    public static NamedTuple x_z_y( 
    		Object x, Object z 
    ) {
        String[] columns = new String[]{"y"};       
        if( !(x instanceof Number) ) 
            throw new AssertionError("!(x instanceof Number)");
        if( !(z instanceof Number) ) 
            throw new AssertionError("!(z instanceof Number)");
        Number nX = (Number)x;
        Number nZ = (Number)z;
        Number y = Util.divide(nZ, nX); 
        return new NamedTuple(columns,new Object[]{y});
    }
    public static NamedTuple y_z_x( 
    		Object y, Object z 
    ) {
        String[] columns = new String[]{"x"};
        if( !(y instanceof Number) ) 
            throw new AssertionError("!(y instanceof Number)");
        if( !(z instanceof Number) ) 
            throw new AssertionError("!(z instanceof Number)");
        Number nY = (Number)y;
        Number nZ = (Number)z;
        Number x = Util.divide(nZ, nY); 
        return new NamedTuple(columns,new Object[]{x});
    }
}

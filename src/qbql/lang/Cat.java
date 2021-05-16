package qbql.lang;

import java.util.HashMap;
import java.util.Map;

import qbql.index.NamedTuple;
import qbql.lattice.Database;
import qbql.lattice.Relation;
import qbql.util.Util;

public class Cat {
    public static String[] getSymbolicNames() {
    	return new String[] {
        		"prefix +(from)+ postfix = source",
    	};
    }
    
    /*public Cat( Database d ) {
    }*/
    
    public static NamedTuple source_from_prefix_postfix( 
            String source, int from 
    ) {
        String[] columns = new String[]{"prefix","postfix"};
        Object[] data = new Object[]{
                source.substring(0,from),
                source.substring(from)};
        return new NamedTuple(columns,data);
    }
    public static NamedTuple prefix_postfix_source_from( 
            Object prefix, Object postfix 
    ) {
        String[] columns = new String[]{"source","from"};
        Object[] data = new Object[]{prefix.toString()+postfix.toString(),prefix.toString().length()};
        return new NamedTuple(columns,data);
    }
    public static Relation source_from_prefix_postfix( String source ) {
        Relation ret = new Relation(
            new String[]{"from","prefix","postfix"}
        );
        for( int i = 0; i <= source.length(); i++ ) {
            Map<String,Object> content = new HashMap<String,Object>();
            content.put("from", i);
            content.put("prefix", source.substring(0,i));
            content.put("postfix", source.substring(i));
            ret.addTuple(content);
        }
        return ret;
    }  
}

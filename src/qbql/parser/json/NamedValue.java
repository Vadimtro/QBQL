package qbql.parser.json;

import java.util.List;

public class NamedValue {
	public String name;
	public String atomic = null;
	public List<NamedValue> composite = null;

	public NamedValue( String n, String a ) {
        name = n;
        atomic = a;
    }

	public NamedValue( String n, List<NamedValue> c ) {
        name = n;
        composite = c;
    }

	public boolean isLiteral() {
        return name == null || name.equals(valueString());
    }
	
	public NamedValue child( String name ) {
		if( composite == null )
			return null;
		for( NamedValue c : composite ) {
			if( name.equals(c.name) )
				return c;
		}
		return null;
	}
	public NamedValue firstChild() {
		return child(0);
	}
	public NamedValue child( int index ) {
		if( composite == null )
			return null;
		return composite.get(index);
	}

	public String valueString()  {
        if( atomic != null )
            return atomic;

        if( composite != null ) {
            String openBr = "{";
            StringBuilder ret = new StringBuilder();
            int i = 0;
            for ( NamedValue v : composite ) {
                if( 0 == i++) {
                    if( v.isLiteral() )
                        openBr = "[";
                    ret.append(openBr);
                } else
                    ret.append(',');
                ret.append(v.toString());
            }
            if( openBr.equals("{") )
                ret.append("}");
            else
                ret.append("]");
            return ret.toString();
        }

        throw new AssertionError("atomic == map == null");
    }
	
	@Override
    public String toString() {
        if( name == null )
            return valueString();
        if( isLiteral() )
            return name;
        return name+"="+valueString();
    }
}

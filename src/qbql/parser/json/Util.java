package qbql.parser.json;


public class Util {
    public static String cleanText( String input ) {
    	String ret = input;
    	if( ret.startsWith("\"") )
    		ret = ret.substring(1,ret.length()-1);
    	String newLineEncoded = "^$%n%$^";
    	String rLineEncoded = "^$%r%$^";
    	String tLineEncoded = "^$%t%$^";
    	String bLineEncoded = "^$%b%$^";
    	String fLineEncoded = "^$%f%$^";
		ret = ret.replace("\\\\n", newLineEncoded);
		ret = ret.replace("\\\\r", rLineEncoded);
		ret = ret.replace("\\\\t", tLineEncoded);
		ret = ret.replace("\\\\b", bLineEncoded);
		ret = ret.replace("\\\\f", fLineEncoded);
    	ret = ret.replace("\\\"", "\"");
    	//? ret = ret.replace("\\\\", "\\");
    	ret = ret.replace("\\r", "\r");
    	ret = ret.replace("\\n", "\n");
    	ret = ret.replace("\\t", "\t");
    	ret = ret.replace("\\b", "\b");
    	ret = ret.replace("\\f", "\f");
    	ret = ret.replace(newLineEncoded, "\\\\n");
    	ret = ret.replace(rLineEncoded, "\\\\r");
    	ret = ret.replace(tLineEncoded, "\\\\t");
    	ret = ret.replace(bLineEncoded, "\\\\b");
    	ret = ret.replace(fLineEncoded, "\\\\f");
    	return ret;
    }
    public static String sugarcoatText( String input ) {
    	if( input == null )
    		return null;
    	String ret = input;
    	ret = ret.replace("\\", "\\\\");
    	ret = ret.replace("\r", "\\r");
    	ret = ret.replace("\n", "\\n");
    	ret = ret.replace("\t", "\\t");
    	ret = ret.replace("\b", "\\b");
    	ret = ret.replace("\f", "\\f");
    	ret = ret.replace("\"", "\\\"");
    	StringBuilder b = new StringBuilder();
        for (char c : ret.toCharArray()) {
            if (c >= 128)
                b.append("\\u").append(String.format("%04X", (int) c));
            else if (c < 32)
                b.append('?');
            else
                b.append(c);
        }
        return b.toString();
    }
}

package qbql.parser.json;

import qbql.lsp.Jsonable;

public class ResponseError implements Jsonable {
    int code = 0;
    String message = null;
    Object data = null;

    ResponseError() {}
    public ResponseError( int code, String message, Object data ) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
	@Override
	public String toString() {
		return "code="+code+",message="+message+",data="+data;
	}
	
	
	@Override
	public String toJson() {
		String d = "";
		if( data instanceof Jsonable )
			d = ((Jsonable) data).toJson();
		else if( data instanceof String )
			d = "\""+Util.sugarcoatText((String) data)+"\"";
		if( 0 < d.length())
			d = " , \"data\": "+d;
		return "{ \"code\": "+code+", \"message\": \""+message+"\""+d+" }";
	}
    
    
}

package qbql.arbori;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleBindings;

//https://stackoverflow.com/questions/35807683/capturing-nashorns-global-variables
public class GlobalMap extends SimpleBindings implements Closeable {

    private final static String NASHORN_GLOBAL = "nashorn.global";
    private Bindings global;
    private Set<String> original_keys;

    private int objRef;
    private static int objCnt = 0;    
    public GlobalMap( Map<String, Object> map ) {
        super(map);
        objRef = objCnt++;
    }

    @Override
    public Object put(String key, Object value) {
        if (key.equals(NASHORN_GLOBAL) && value instanceof Bindings) {
            global = (Bindings) value;
            original_keys = new HashSet<>(global.keySet());
            return null;
        }
        return super.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return key.equals(NASHORN_GLOBAL) ? global : super.get(key);
    }

    @Override
    public void close() {
        if (global != null) {
            Set<String> keys = new HashSet<>(global.keySet());
            keys.removeAll(original_keys);
            for (String key : keys)
                put(key, global.remove(key));
        }
    }
    
	@Override
	public String toString() {
		return getClass().getName()+"@"+objRef;
	}	

}
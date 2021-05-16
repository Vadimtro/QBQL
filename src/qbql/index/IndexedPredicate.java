package qbql.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import qbql.lattice.Database;
import qbql.lattice.EqualityPredicate;
import qbql.lattice.Program;
import qbql.lattice.Predicate;
import qbql.lattice.Relation;
import qbql.lattice.Tuple;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Token;
import qbql.util.Util;

public class IndexedPredicate extends Predicate {
    
    Class<?> implementation = null;
    
    private HashMap<String,String> renamed = new HashMap<String,String>(); //old_name->new_name
    private String newName( String col ) {
        return renamed.get(col);
    }
    private Set<String> oldNames( String col ) {
        Set<String> ret = new HashSet<String>();
        for( String newName : renamed.keySet() )
            if( col.equals(newName(newName)) )
                ret.add(newName);
        return ret;
    }
    
    private Method method( Set<String> output ) {
        Method ret = null;
        for( Method m : methods() ) {
            List<String> args = arguments(m,ArgType.OUTPUT);
            boolean inputsCovered = true;
            for( String i : output )
                if( !args.contains(i) ) {
                    inputsCovered = false;
                    break;
                }
            if( !inputsCovered )
                continue;
            ret = m;
            if( m.getReturnType() == NamedTuple.class )
                return ret;
        }
        return ret;
    }
    
    public void renameInPlace( String from, String to ) {       
        super.renameInPlace(from, to);
        Set<String> old = oldNames(from);
        for( String o : old ) {
        	renamed.remove(o);
        	renamed.put(o, to);
        }
    }
    public void eqInPlace( String from, String to ) {     
        super.eqInPlace(from, to);
        renameInPlace(from, to);
    }

    private Database db = null;
    public IndexedPredicate( Database db, String name ) throws Exception {
        this.db = db;
        if( !narrowPredicate(db, name) && !genericPredicate(db, name) )
        	throw new Exception("Failed to instantiate "+name);
        /*?*/db.addPredicate(name, this);
    }
	
    public IndexedPredicate( IndexedPredicate ip ) {
        super(Util.clone(ip.colNames)); 
        this.db = ip.db;
        implementation = ip.implementation;
        renamed = (HashMap<String, String>) ip.renamed.clone();
    }

    
    private Set<Method> methods() {
        Set<Method> ret = new HashSet<Method>();
        for( Method m : implementation.getDeclaredMethods() ) {
            if( !Modifier.isPublic(m.getModifiers()) ) 
                continue;
            Class<?> returnType = m.getReturnType();
            if( returnType!=Relation.class && returnType!=NamedTuple.class )
                continue;
            ret.add(m);
        }
        return ret;
    }
    static enum ArgType {INPUT,OUTPUT,BOTH}
    private static List<String> arguments( Method m, ArgType at ) {
        List<String> ret = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(m.getName(), "_", false);
        int pos = -1;
        while( st.hasMoreTokens() ) {
            pos++;
            String token = st.nextToken();
            if( at == ArgType.OUTPUT && pos < m.getParameterTypes().length )
                continue;
            if( at == ArgType.INPUT && m.getParameterTypes().length <= pos )
                break;
            ret.add(token);
        }
        return ret;
    }

    
    public static Relation join( Relation x, IndexedPredicate y ) {
        Set<String> header = new TreeSet<String>();
        header.addAll(x.header.keySet());
        header.addAll(y.header.keySet());               
        Relation ret = new Relation(header.toArray(new String[0]));
        Set<String> required = new HashSet<String>();
        for( String s : y.header.keySet() )
            required.addAll(y.oldNames(s));
        for( String s : x.header.keySet() )
            required.removeAll(y.oldNames(s));
        Method m = y.method(required);
        Object obj = null;
        try {
            Class partypes[] = new Class[1];
            partypes[0] = Database.class;
            Constructor ct = y.implementation.getConstructor(partypes);
            Object arglist[] = new Object[1];
            arglist[0] = y.db;
            obj = ct.newInstance(arglist);
        } catch( NoSuchMethodException e ) {            	
        } catch ( Exception e ) {
			throw new RuntimeException(e);
		}
        for( Tuple tupleX: x.getContent() ) {
            if( m == null ) { 
            	StringBuilder reqMap = new StringBuilder();
            	for( String s: required ) {
            		reqMap.append(" "+(s != null ? s+"->": "")+y.newName(s));
            	}
                throw new AssertionError("Didn't find a method for \n"
                		+reqMap+" in "+y.implementation.getName());
            }
            // postponed to facilitate joining with []
            
            List<String> inputs = arguments(m, ArgType.INPUT);
            
            Object[] args = new Object[m.getParameterTypes().length];
            int pos = -1;
            for( String origName : inputs ) {
                pos++;
                Integer xHeaderPos = x.header.get(y.newName(origName));
                args[pos] = tupleX.data[xHeaderPos];
            }
            
            Object o = null;
            try {
                o = m.invoke(obj, args);
            } catch( InvocationTargetException e ) {
                if( e.getCause() instanceof EmptySetException )
                    return ret;
                else if( e.getCause() instanceof AssertionError )
                	throw (AssertionError)e.getCause();
                else
                	throw new RuntimeException(e);
            } catch ( Exception e ) {
				throw new RuntimeException(e);
			}
            
            if( o instanceof NamedTuple ) {
                Object[] retTuple = new Object[header.size()];
                NamedTuple nt = (NamedTuple)o;
            retTuple_EQ_null:    
                for( String attr : ret.colNames ) {
                    Integer colRet = ret.header.get(attr);                    
                    Integer colX = x.header.get(attr);
                    String oldYattr = (String)Util.keyForAValue(y.renamed, attr);
                    if( colX != null ) {
                    	for( String yAttr : y.colNames )
                    		if( yAttr.equals(attr) ) {
                    			Object x1= tupleX.data[colX];
                    			Object x2= ((NamedTuple) o).get(oldYattr);
                    			if( x2!=null && !x1.equals(x2) ) {
                    				retTuple = null;
                    				break retTuple_EQ_null;
                    			}
                    		}
                    	retTuple[colRet] = tupleX.data[colX];
                    	continue;
                    }
                    
                    Object co = nt.get(oldYattr);
                    retTuple[colRet] = co;                    
                }
                if( retTuple != null )
                    ret.addTuple(retTuple);
            } else if( o instanceof Relation ) {
                Relation r = (Relation)o;
                for( Tuple t : r.getContent() ) {
                    Object[] retTuple = new Object[header.size()];
                    for( String attr : ret.colNames ) {
                        Integer colRet = ret.header.get(attr);
                        
                        Integer colX = x.header.get(attr);                    
                        if( colX != null ) {
                        	retTuple[colRet] = tupleX.data[colX];
                        	continue;
                        }
                        
                        String oldYattr = (String)Util.keyForAValue(y.renamed, attr);
                        Integer colR = r.header.get(oldYattr);
                        Object co = t.data[colR];
                        retTuple[colRet] = co;                         
                    }
                    //if( retTuple != null )
                        ret.addTuple(retTuple);
                }
            } else
                throw new RuntimeException("Wrong return type");
        }
        return ret;
    }
    
    public static IndexedPredicate union( Relation x, IndexedPredicate y )  {
        if( 0 < x.getContent().size() )
            throw new AssertionError("Not a projection: TODO");
        IndexedPredicate ret = new IndexedPredicate(y);
        Set<String> columns = new HashSet<String>();
        columns.addAll(x.header.keySet());
        columns.retainAll(y.header.keySet());
        HashMap<String,Integer> newHdr = new HashMap<String,Integer>();
        String[] newColNames = new String[columns.size()];
        int pos = -1;
        for( String col : columns ) {
            pos++;
            newColNames[pos] = col;
            newHdr.put(col, pos);
        }
        ret.header = newHdr;
        ret.colNames = newColNames;
        return ret; 
    }
    
    public static Relation setEQ( Relation x, IndexedPredicate y ) {
        Set<String> headerXmY = new TreeSet<String>();
        headerXmY.addAll(x.header.keySet());
        headerXmY.removeAll(y.header.keySet());            
        Set<String> headerYmX = new TreeSet<String>();
        headerYmX.addAll(y.header.keySet());
        headerYmX.removeAll(x.header.keySet());
        Set<String> headerSymDiff = new TreeSet<String>();
        headerSymDiff.addAll(headerXmY);
        headerSymDiff.addAll(headerYmX);
        Relation hdrXmY = new Relation(headerXmY.toArray(new String[0]));
        //Relation hdrYmX = new Relation(headerYmX.toArray(new String[0]));
        
        Relation ret = new Relation(headerSymDiff.toArray(new String[0]));
                
        Relation X = Relation.union(x,hdrXmY);
        
        Set<String> headerXY = new TreeSet<String>();
        headerXY.addAll(x.header.keySet());
        headerXY.retainAll(y.header.keySet());
        Relation hdrYX = new Relation(headerXY.toArray(new String[0]));
        
        
        Set<String> required = new HashSet<String>();
        for( String s : y.header.keySet() )
            required.addAll(y.oldNames(s));
        for( String s : x.header.keySet() )
            required.removeAll(y.oldNames(s));
        Method m = y.method(required);
        Object obj = null;
        try {
            Class partypes[] = new Class[1];
            partypes[0] = Database.class;
            Constructor ct = y.implementation.getConstructor(partypes);
            Object arglist[] = new Object[1];
            arglist[0] = y.db;
            obj = ct.newInstance(arglist);
        } catch( NoSuchMethodException e ) {            	
        } catch ( Exception e ) {
			throw new RuntimeException(e);
		}
        for( Tuple xi : X.getContent() ) {
            if( m == null ) { 
            	StringBuilder reqMap = new StringBuilder();
            	for( String s: required ) {
            		reqMap.append(" "+(s != null ? s+"->": "")+y.newName(s));
            	}
                throw new AssertionError("Didn't find a method for \n"
                		+reqMap+" in "+y.implementation.getName());
            }
            // postponed to facilitate joining with []
            
            Relation singleX = new Relation(X.colNames);
            singleX.addTuple(xi.data);
            Relation lft = Relation.union(Relation.join(singleX,x),hdrYX);
            
            Object o = null;
            try {
                Relation singleY = ((NamedTuple)m.invoke(obj, new Object[] {lft})).toRelation();
                for( String oldName : y.renamed.keySet() ) {
					String newName = y.newName(oldName);
					if( singleY.header.containsKey(oldName) )
						singleY.renameInPlace(oldName, newName);
				}

                Relation singleXY = Relation.join(singleX, singleY); 
                if( !singleXY.header.keySet().equals(ret.header.keySet()) )
                	throw new AssertionError("[Relation /= IndexedPredicate]="+ret.header.keySet()+" vs. \n"
                			+m.getName()+" "+singleXY.header.keySet());
				ret = Relation.union(ret, singleXY); 
            } catch( InvocationTargetException e ) {
                /*if( e.getCause() instanceof EmptySetException )
                    return ret;
                else*/
                	throw new RuntimeException(e);
            } catch ( Exception e ) {
				throw new RuntimeException(e);
			}
        }        
        return ret;      
    }

    
    private boolean narrowPredicate( Database db, String name ) {
    	Set<String> tmp = new TreeSet<String>();
    	try {
			implementation = Class.forName(db.pkg+"."+name);
		} catch( ClassNotFoundException e ) {
			return false;
		}
    	for( Method m : methods() ) {
    		tmp.addAll(arguments(m,ArgType.BOTH));
    	}
    	colNames = new String[tmp.size()];
    	int pos = 0;
    	for( String s : tmp ) {
    		colNames[pos] = s;
    		header.put(s,pos);
    		renamed.put(s, s);
    		pos++;
    	}
    	return true;
    }
    
    String[] files = null;
    private boolean genericPredicate( Database db, String predicate ) {
    	// http://www.javaworld.com/javaworld/javatips/jw-javatip113.html
    	if( files == null ) {
            String name = db.pkg;
            if( !name.startsWith("/") ) {
            	name = "/" + name;
            }        
            name = name.replace('.','/');

            URL url = IndexedPredicate.class.getResource(name);
            File directory = new File(url.getFile());
            if( directory.exists() ) {
            	files = directory.list();
            } else {
            	String jarName = directory.getPath();
            	jarName = jarName.substring("file:\\".length());
            	jarName = jarName.substring(0,jarName.indexOf('!'));
             	try {
					files = getClassesFromJARFile(jarName,name.substring(1)).toArray(new String[0]);
				} catch( IOException e ) {
					System.err.println(e.getMessage());
				}
            }
    	}
        for( int i=0; i<files.length; i++ ) {
            if( files[i].endsWith(".class") ) {
                String classname = files[i].substring(0,files[i].length()-6);
                try {
                    Class c = Class.forName(db.pkg+"."+classname);
                    Method getSymbolicNameMethod = c.getDeclaredMethod("getSymbolicNames");
                    String[] candidates = (String[])getSymbolicNameMethod.invoke(null);
                    for( String candidate: candidates ) {
                    	Map<String,String> matched = matchNames(candidate, predicate);
                    	if( matched == null )
                    		continue;
                    	implementation = c;
                    	Set<String> tmp = new TreeSet<String>();
                    	for( Method m : methods() ) {
                    		tmp.addAll(arguments(m,ArgType.BOTH));
                    	}
                    	
                    	boolean misMatched = false;
                    	for( String key : matched.keySet() ) 
                    		if( !key.equals(matched.get(key)) )
                    			if( !tmp.contains(key) ) {
                    				misMatched = true;
                    				break;
                    			}
                    	if( misMatched )
                    		continue;
                    	
                    	int pos = 0;
                    	Map<Integer,String> cols = new HashMap<Integer,String>();
                    	for( String s : tmp ) {
                    		String t = matched.get(s);
                    		if( t == null ) // e.g. Sum predicate has variable arity 
                    			continue;
                    		header.put(t,pos);
                    		renamed.put(s, t);
                    		cols.put(pos,t);
                    		pos++;
                    	}
                    	colNames = new String[pos];
                    	for( int j = 0; j < colNames.length; j++ ) {
                    		colNames[j] = cols.get(j);
						}
                    	return true;
                    }
                } catch ( Exception e ) {
                }
            }
        }
        return false;
    }
    
    private static List<String> getClassesFromJARFile( String jar, String packageName ) throws IOException {
    	final List<String> classes = new LinkedList<String>();
    	JarInputStream jarFile = null;
    	try {
    		jarFile = new JarInputStream(new FileInputStream(jar));
    		for( JarEntry jarEntry = jarFile.getNextJarEntry(); jarEntry != null ;jarEntry = jarFile.getNextJarEntry() ) {
    			String className = jarEntry.getName();
    			if( className.endsWith(".class") ) {
    				if (className.startsWith(packageName))
    					classes.add(className.substring(packageName.length()+1));
    			}    			
    		}
    	} finally {
    		if( jarFile != null )
    			jarFile.close();
    	}
    	return classes;
    }
    
    protected IndexedPredicate clone() {
        return new IndexedPredicate(this);
    }

}

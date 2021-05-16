package qbql.lattice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.util.Util;

public class Partition implements Comparable<Partition> {
    private Set<Block> blocks = new TreeSet<Block>();
    
    private Database db = null;
    
    public Partition() {}
    
    public Partition( int[] indexes ) {
        Map<Integer,Block> numberedBlocks = new HashMap<Integer,Block>();
        for( int i = 0; i < indexes.length; i++ ) {
            Integer partNo = indexes[i];
            Block block = numberedBlocks.get(partNo);
            if( block == null ) {
                block = new Block();
                numberedBlocks.put(partNo, block);
            }
            block.add(i);
        } 
        blocks.addAll(numberedBlocks.values());
    }
    
    public Partition( Relation r, Relation attributes, Database db ) {
    	this.db = db;
    	
        //Relation rjR11 = Relation.join(r, db.R11);
        Relation relVar = r;
        Map<Tuple, Integer> indexes = new HashMap<Tuple, Integer>(); 
        for( Tuple t1 : relVar.getContent() )
            for( Tuple t2 : relVar.getContent() ) {
                boolean equals = true;
                for( String attr : attributes.colNames ) {
                    Integer pos = relVar.header.get(attr);
                    if( pos == null )
                        continue;
                    if( !t1.data[pos].equals(t2.data[pos]) ) {
                        equals = false;
                        break;
                    }
                }
                if( equals ) {
                    Integer index = indexes.get(t1);
                    if( index == null ) {
                        index = indexes.size();
                        indexes.put(t1, index);
                    }
                    indexes.put(t2, index);
                }
            }
                           
        Map<Integer,Block> numberedBlocks = new HashMap<Integer,Block>();
        for( Tuple elem : indexes.keySet() ) {
            Integer partNo = indexes.get(elem);
            Block block = numberedBlocks.get(partNo);
            if( block == null ) {
                block = new Block();
                numberedBlocks.put(partNo, block);
            }
            block.add(elem);
        } 
        blocks.addAll(numberedBlocks.values());
    }
   
    /**
     * Convert partitions to equivalence relation, apply composition
     * If result is equivalence relation, then return it converted into partition
     */
    public static Partition composition( Partition x, Partition y ) {
    	Relation xx = new Relation(new String[]{"1","2"});
        for( Block bx : x.blocks ) 
        	for( Object ox1 : bx.content )
            	for( Object ox2 : bx.content )
            		xx.addTuple(new Object[]{ox1,ox2});
        
    	Relation yy = new Relation(new String[]{"2","3"});
        for( Block by : y.blocks ) 
        	for( Object oy1 : by.content )
            	for( Object oy2 : by.content )
            		yy.addTuple(new Object[]{oy1,oy2});
        
        Relation c = (Relation)Relation.setIX(xx, yy);
        // check symmetry
        for( Tuple t0 : c.content ) {
        	boolean matched = false;        
            for( Tuple t1 : c.content )
            	if( t0.data[0]==t1.data[1] && t0.data[1]==t1.data[0] ) {
            		matched = true;
            		break;
            	}
            if( !matched )
            	throw new AssertionError("!Symmetric; no match for "+t0.toString());
        }
        
        // check transitivity
        for( Tuple t12 : c.content ) 
            for( Tuple t23 : c.content ) {
            	if( t12.data[1]!=t23.data[0] )
            		continue;
            	boolean matched = false;        
            	for( Tuple t13 : c.content )
            		if( t12.data[0]==t13.data[0] && t23.data[1]==t13.data[1] ) {
            			matched = true;
            			break;
            		}
            if( !matched )
            	throw new AssertionError("!Transitive; no match for "+t12.toString()+" "+t23.toString());
        }
       
        Relation pc = Relation.union(c, new Relation(new String[]{"1"}));
        
        Relation gt = new Relation(new String[]{"1","3"}); 
        for( Tuple t1 : pc.content )
            for( Tuple t3 : pc.content )
            	if( t1.data[0].toString().compareTo(t3.data[0].toString())<0 )
            		gt.addTuple(new Object[]{t1.data[0],t3.data[0]});
        
        Relation gtJc = Relation.join(gt,c);
		Relation P1gtJc = Relation.union(gtJc, new Relation(new String[]{"1"}));
		Relation maxpc = (Relation)Relation.join(pc, x.db.complement(P1gtJc));
        
        Partition ret = new Partition();        
        for( Tuple t1 : maxpc.content ) {
        	Block b = new Block();
            Relation tmp = new Relation(new String[]{"1"});
            tmp.addTuple(new Object[]{t1.data[0]});
        	Relation matches = (Relation) Relation.setIX(c, tmp);
        	for( Tuple t2 : matches.content )
        		b.add(t2.data[0]);
            ret.blocks.add(b);
        }
        
        return ret;
    }
    

    public static Partition intersect( Partition x, Partition y ) {
        Partition ret = new Partition();
        for( Block bx : x.blocks )
            for( Block by : y.blocks ) {
                Block intersect = Block.intersect(bx,by);
                if( intersect.content.size() > 0 )
                    ret.blocks.add(intersect);
            }
        return ret;
    }
    public static Partition union( Partition x, Partition y ) {
        Partition ret = new Partition();
        Set<Block> allBlocks = new TreeSet<Block>();
        allBlocks.addAll(x.blocks);
        allBlocks.addAll(y.blocks); 
        
        while( allBlocks.size() > 0 ) {
            Block ab = null;
            for( Block b : allBlocks ) {
                ab = b;
                break;
            }
            allBlocks.remove(ab);
            
            Set<Block> additions = new HashSet<Block>();
            Set<Block> deletions = new HashSet<Block>();
            for( Block b : ret.blocks )         
                if( !Block.isDisjoint(ab, b) ) {
                    additions.add(Block.union(ab, b));
                    deletions.add(b);
                }
            
            if( additions.size() > 0 ) {
                ret.blocks.removeAll(deletions);
                ret.blocks.addAll(additions);
            } else
                ret.blocks.add(ab);
        }
        Set<Block> covered = new HashSet<Block>();
        for( Block b : ret.blocks )         
            for( Block bb : ret.blocks )
                if( bb != b && bb.content.containsAll(b.content) )
                    covered.add(b);
        ret.blocks.removeAll(covered);
        return ret;
    }
    public static boolean le( Partition x, Partition y ) {
        return y.compareTo(union(x,y))==0;
    }
    public static boolean ge( Partition x, Partition y ) {
        return x.compareTo(union(x,y))==0;
    }
    public static boolean comparable( Partition x, Partition y ) {
        return le(x,y) || le(y,x);
    }
    
    private String label = null;
    public String toString() {
        if( label != null )
            return label;
        StringBuilder ret = new StringBuilder();
        boolean firstIter = true;
        for( Block block : blocks ) {
            if( !firstIter )
                ret.append(" |");
            ret.append(block.toString());
            firstIter = false;
        }
        label = ret.toString();
        return label;
    }
    
    public int compareTo( Partition p ) {
        return toString().compareTo(p.toString());
    }
    
    public boolean equals( Object obj ) {
        Partition p = (Partition) obj;
        return compareTo(p)==0;
    }
    
    private static class Block implements Comparable<Block> {
        TreeSet content = new TreeSet();

        public int compareTo( Block b ) {
            return toString().compareTo(b.toString());
        }
        
        public static Block intersect( Block bx, Block by ) {
            Block ret = new Block();
            ret.content.addAll(bx.content);
            ret.content.retainAll(by.content);
            return ret;
        }

        public static boolean isDisjoint( Block bx, Block by ) {
            for( Object i : bx.content )
                for( Object j : by.content )
                    if( i.equals(j) )
                        return false;
            return true;
        }
        
        public static Block union( Block bx, Block by ) {
            Block ret = new Block();
            ret.content.addAll(bx.content);
            ret.content.addAll(by.content);
            return ret;
        }
        
        public void add( Object i ) {
            content.add(i);
        }
        private boolean contains( Object i ) {
        	return content.contains(i);
        }

        public String toString() {
            StringBuilder ret = new StringBuilder();
            for( Object i : content )
                ret.append(" "+i.toString());
            return ret.toString();
        }
    }
    
    private Block owner( Object o ) {
    	Block ret = null;
    	for( Block b : blocks )
    		if( b.contains(o) )
    			ret = b;
    	return ret;
    }
    
    public static Set<Partition> generate( int numElem ) {
        Set<Partition> ret = new TreeSet<Partition>();
        int[] indexes = new int[numElem];
        for( int i = 0; i < indexes.length; i++ )
            indexes[i] = 0;
        do {
            ret.add(new Partition(indexes));
        } while( Util.next(indexes,numElem) );
        return ret;
    }
    
    private static void generate() {
        for( int i = 8; i < 9; i++ ) { 
            System.out.print("\ni="+i);
            Set<Partition> partitions = generate(i);
            System.out.println(" ("+partitions.size()+")");
            for( Partition a : partitions ) {
                System.out.print(".");
                for( Partition c : partitions ) {
                    Partition jac = intersect(a,c);
                    if( jac.equals(a) || jac.equals(c) )
                        continue;
                    for( Partition b : partitions ) {
                        if( b.equals(a) )
                            continue;
                        if( !le(a,b) )
                            continue;
                        if( comparable(b,c) )
                            continue;
                        Partition jbc = intersect(b,c);
                        if( jbc.equals(c)
                         || jbc.equals(b) || jbc.equals(jac))
                            continue;
                        if( comparable(jbc,a) )
                            continue;
                        for( Partition d : partitions ) {
                            if( d.equals(c) )
                                continue;
                            if( !le(c,d) )
                                continue;
                            if( comparable(d,b) )
                                continue;
                            Partition jad = intersect(a,d);
                            if( comparable(jad, c) )
                                continue;
                            if( jad.equals(a) || jad.equals(b) || jad.equals(d)
                             || jad.equals(jac) || jad.equals(jbc)
                            )
                                continue;
                            Partition adVbc = union(jad,jbc);
                            if( 
                                adVbc.equals(b) || adVbc.equals(d)
                             || adVbc.equals(jac) || adVbc.equals(jbc)
                             || adVbc.equals(jad) 
                            )
                                continue;
                            if( comparable(adVbc, a) || comparable(adVbc, c) )
                                continue;
                            Partition r11 = intersect(b,d);
                            if( r11.equals(adVbc) )
                                continue;
                                                      
                            for( Partition r00 : partitions ) {
                                if( comparable(r00,a)
                                 || comparable(r00,b)
                                 || comparable(r00,c)
                                 || comparable(r00,d)
                                )
                                    continue;
                                Partition _a = intersect(a,r00);
                                if( !_a.equals(intersect(b,r00)) )
                                    continue;
                                Partition _c = intersect(c,r00);
                                if( !_c.equals(intersect(d,r00)) )
                                    continue;
                                if( comparable(_a,_c) )
                                    continue;
                                Partition r10 = intersect(_a,_c);
                                if( r10.equals(jac) )
                                    continue;
                                if( !r00.equals(union(_a,_c)) )
                                    continue;

                                System.out.println("a = "+a);
                                System.out.println("b = "+b);
                                System.out.println("c = "+c);
                                System.out.println("d = "+d);
                                System.out.println("r00 = "+r00);
                                System.out.println();
                                System.out.println("_a = "+_a);
                                System.out.println("_c = "+_c);
                                System.out.println("r10 = "+r10);
                                System.out.println("r11 = "+r11);
                                System.out.println("r01 = "+union(b,d));
                                System.out.println("a^c = "+jac);
                                System.out.println("a^d = "+jad);
                                System.out.println("b^c = "+jbc);
                                System.out.println("a^dVb^c = "+adVbc);
                                System.out.println("----------------------");
                                //return;
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        
        /*Partition p = construct(new int[]{1,2,3,1});
        Partition q = construct(new int[]{1,2,3,3});
        System.out.println(q.toString());
        System.out.println(p.toString());
        System.out.println(intersect(p,q).toString());
        System.out.println(union(p,q).toString());*/
        
        /*for( Partition p : generate(5) ) {
            System.out.println(p.toString());           
        }*/ 
        
        //generate(); 
        
        
    }
}

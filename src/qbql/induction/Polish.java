package qbql.induction;

import java.util.ArrayList;
import java.util.List;

public class Polish {
    List<TreeNode> code;
    
    public Polish( List<TreeNode> code ) {
        this.code = code;
    }
    
    
    TreeNode decode() {
        List<TreeNode> clone = new ArrayList<TreeNode>();
        for( TreeNode o : code )
            clone.add(o);
        while( step(clone) ) 
            ;
        if( clone.size() == 1 ) 
            return (TreeNode)clone.get(0).cloneLeaves();
        return null;      
    }
    static boolean step( List<TreeNode> code ) {
        TreeNode predecessor = null;
        TreeNode predpred = null;
        TreeNode insert = null;
        int pos = 0;
        for( TreeNode o : code ) {
            if( o == TreeNode.one ) {
                if( predecessor == null )
                    return false;
                insert = new TreeNode(predecessor,null);
                pos--;
                break;
            } else if( o == TreeNode.two ) {
                if( predpred == null || predecessor == null )
                    return false;
                insert = new TreeNode(predpred,predecessor);
                pos = pos-2;
                break;
            }
            predpred = predecessor;
            predecessor = o; 
            pos++;
        }
        if( insert != null ) {
            code.remove(pos);
            code.remove(pos);
            if( insert.right() != null ) 
                code.remove(pos);
            code.add(pos, insert);    
            return true;
        }
        return false;
    }
    
    void next() {
        boolean all2s = true;
        for( int i = 0; i < code.size(); i++ ) {
            TreeNode t = code.get(i);
            if( t != TreeNode.two ) {
                code.set(i, t.incr());
                all2s = false;
                break;
            } else
                code.set(i, t.zero());            
        }
        if( all2s )
            code.add(TreeNode.one);
    }
    
    static TreeNode leaf() {
        return new TreeNode(null,null);
    }
    
    public static void main( String[] args ) {
        
        /*ArrayList<TreeNode> l = new ArrayList<TreeNode>();
        l.add(leaf());
        l.add(leaf());
        l.add(leaf());
        l.add(TreeNode.one);
        l.add(TreeNode.two);
        l.add(TreeNode.two);
        TreeNode root = new Polish(l).decode();
        ExprGen.zilliaryOps = new String[] {"x","y"};
        ExprGen.init(root);
        for( int i = 0; i < 500; i++ ) {
            System.out.print(i+"=");
            root.print();
            if( !ExprGen.next(root) )
                break;
        }*/
        
        /*
        ArrayList<TreeNode> l = new ArrayList<TreeNode>();
        l.add(leaf());
        l.add(TreeNode.one);
        Polish num = new Polish(l);
        int cnt = 0; 
        for( int i = 0; i < 20; i++ ) {
            TreeNode n = num.decode();            
            if( n != null ) {
                System.out.println();
                System.out.println(l.toString());
                n.print();
                cnt++;
            } else {
                System.out.print('.');
            }
            num.next();
        }
        System.out.println(cnt);*/
        
        
    }
    
    boolean wellBuilt() {
        TreeNode predecessor = null;
        TreeNode predpred = null;
        for( TreeNode n : code ) {
            if( n == TreeNode.one && predecessor == TreeNode.one /*&& predpred == TreeNode.one*/ )
                return false;
            predpred = predecessor;
            predecessor = n; 
        }
        return true;
    }
}

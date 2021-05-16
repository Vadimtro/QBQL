package qbql.lattice;

import java.util.List;

import qbql.parser.LexerToken;
import qbql.parser.ParseNode;

public class Expr {
    String type; // operation name or variable
    Expr left;
    Expr right;

    public Expr( String type, Expr lft, Expr rgt ) {
        this.type = type;
        this.left = lft;
        this.right = rgt;
    }

    public Predicate eval( Database d ) {
        if( "^".equals(type) )
            return Predicate.join(left.eval(d),right.eval(d));
        else if( "v".equals(type) )
            return Predicate.union(left.eval(d),right.eval(d));
        else if( "⊗".equals(type) )
            return Relation.directProduct((Relation)left.eval(d),(Relation)right.eval(d));
        else if( "|v|".equals(type) )
            return Relation.count(left.eval(d),right.eval(d));
        else if( "'".equals(type) )
            return d.complement(left.eval(d));
        else if( "`".equals(type) )
            return d.inverse((Relation)left.eval(d));
        else if( "*".equals(type) )
            return ((Relation)left.eval(d)).CPclosure();
        else if( "+".equals(type) )
            return d.EQclosure(left.eval(d));
        else if( "/^".equals(type) )
            return Predicate.setIX(left.eval(d), right.eval(d));
        else if( "/=".equals(type) ) {
            Predicate lft = left.eval(d);
            Predicate rgt = right.eval(d);
            if( lft instanceof Relation && rgt instanceof Relation )
                return d.quantifier((Relation)lft,(Relation)rgt,Program.setEQ);
            else
                return Predicate.setEQ(lft, rgt);
        } else if( "/<".equals(type) )
            return d.quantifier((Relation)left.eval(d),(Relation)right.eval(d),Program.contains);
        else if( "/>".equals(type) )
            return d.quantifier((Relation)left.eval(d),(Relation)right.eval(d),Program.transpCont);
        else if( "/0".equals(type) )
            return d.quantifier((Relation)left.eval(d),(Relation)right.eval(d),Program.disjoint);
        else if( "/1".equals(type) )
            return d.quantifier((Relation)left.eval(d),(Relation)right.eval(d),Program.almostDisj);
        else if( "/!".equals(type) )
            return d.quantifier((Relation)left.eval(d),(Relation)right.eval(d),Program.big);

        else if( type.startsWith("<") ) {

            Expr e = d.getOperation(type);
            Predicate lft = d.getPredicate("?lft");
            Predicate rgt = d.getPredicate("?rgt");
            Predicate l = left.eval(d);
            Predicate r = right.eval(d);
            d.addPredicate("?lft",l);
            d.addPredicate("?rgt",r);
            Predicate ret = e.eval(d);
            d.removePredicate("?lft");
            d.removePredicate("?rgt");
            if( lft != null )
                d.addPredicate("?lft",lft);
            if( rgt != null )
                d.addPredicate("?rgt",rgt);
            return ret;

        } else { // variable        
            
            Predicate ret = d.lookup(type);
            if( ret == null )
                throw new AssertionError("Expr.eval() == null");
            return ret;
        }
    }

    static Expr convert( String l, String r, ParseNode root, List<LexerToken> src ) {
        if( root.from+1 == root.to ) {
            String var = root.content(src);
            if( l.equals(var) )
                return new Expr("?lft",null,null);
            else if( var.equals(r) )
                return new Expr("?rgt",null,null);
            else
                return new Expr(var,null,null); // e.g. R00
        } 

        if( root.contains(Program.parExpr) )
            for( ParseNode child : root.children() ) {
                if( !child.contains(Program.openParen) )
                    return convert(l,r,child,src);
            }

        String oper = null; 
        Expr lft = null;
        Expr rgt = null;
        for( ParseNode child : root.children() ) {
            if( lft == null && child.contains(Program.userOper) ) {
                lft = new Expr("R00",null,null);
                oper = child.content(src);
            } else if( lft == null && (
                    child.contains(Program.expr)  // <NOT> x
                    || root.contains(Program.complement)          // x'
                    || root.contains(Program.inverse)          
                    || root.contains(Program.CPclosure)          
                    || root.contains(Program.EQclosure)          
                    ) ) {
                lft = convert(l,r,child,src);
            } else if( child.contains(Program.parExpr) 
                    || child.contains(Program.expr) 
                    || child.contains(Program.identifier)  // user-defined unary expressions aren't expr
                    ) {
                rgt = convert(l,r,child,src);
            } else if( oper == null )
                oper = child.content(src);
            else if( "/".equals(oper) )
            	oper += child.content(src);
            else {   //oper += child.content(src); 
                root.printTree();System.out.println(root.content(src));
                throw new AssertionError("oper += child.content(src)");
            }
        }
        return new Expr(oper,lft,rgt);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if( left != null )
            sb.append('('+left.toString()+')');
        sb.append(type);
        if( right != null )
            sb.append('('+right.toString()+')');
        return sb.toString();
    }

}

package qbql.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import qbql.apps.Run;
import qbql.bool.Oper;
import qbql.lattice.Program;
import qbql.util.Array;
import qbql.util.Util;


public class Earley extends Parser {
    public Tuple[] rules;

    protected int identifier = -1; 
    protected int string_literal = -1; 
    protected int digits = -1; 


    protected int[] allXs = null;

    public static void main( String[] args ) throws Exception {
        //String input = "(Emps v [dept mgr]) /^ ((Emps /^\"mgr=mgr1\") v [dept mgr1]) < \"mgr=mgr1\".";
        String input = Util.readFile(qbql.bool.Program.class,"lattice.embeding");
        List<LexerToken> src =  (new Lex()).parse(input);

        Set<RuleTuple> wiki = new TreeSet<RuleTuple>();
        wiki.add(new RuleTuple("P",new String[]{"S"}));
        wiki.add(new RuleTuple("S",new String[]{"S","'+'","S"}));
        //wiki.add(new RuleTuple("S",new String[]{"M","'+'","S"}));
        //wiki.add(new RuleTuple("S",new String[]{"S","'+'","M"}));
        wiki.add(new RuleTuple("S",new String[]{"M"}));
        wiki.add(new RuleTuple("M",new String[]{"M","'*'","T"}));
        //wiki.add(new RuleTuple("M",new String[]{"T","'*'","M"}));
        wiki.add(new RuleTuple("M",new String[]{"T"}));
        wiki.add(new RuleTuple("T",new String[]{"digits"}));
        wiki.add(new RuleTuple("T",new String[]{"identifier"}));
        wiki.add(new RuleTuple("T",new String[]{"string_literal"}));

        /*wiki.add(new RuleTuple("G",new String[]{"A","B","C"}));
        wiki.add(new RuleTuple("A",new String[]{"digits","digits",}));
        wiki.add(new RuleTuple("B",new String[]{"identifier"}));
        wiki.add(new RuleTuple("C",new String[]{"string_literal","string_literal","string_literal"}));*/ 

        Set<RuleTuple> rules = qbql.bool.Program.latticeRules();
        //rules = wiki;

        /*final String symbol = "include";
        for( RuleTuple rule : rules ) 
            if( rule.head.contains(symbol) ) 
                System.out.println(rule.toString());
            else for( int i = 0; i < rule.rhs.length; i++ ) 
                if( rule.rhs[i].contains(symbol) ) {
                    System.out.println(rule.toString());
                    break;
                }*/


        Earley earley = new Earley(rules);
        Matrix matrix = new Matrix(earley);
        Visual visual = null;
        visual = new Visual(src, earley);

        long t1 = System.currentTimeMillis();
        earley.parse(src, matrix); 
        long t2 = System.currentTimeMillis();
        System.out.println("Earley parse time = "+(t2-t1)); // (authorized) //$NON-NLS-1$
        System.out.println("#tokens="+src.size());

        if( visual != null )
            visual.draw(matrix);
        ParseNode out = earley.forest(src, matrix);
        out.printTree();
    }


    public void parse( List<LexerToken> src, Matrix matrix ) {
        while( true ) {
            int before = matrix.size();
            if( !scan(matrix, src) )
                break;
            complete(matrix);
            predict(matrix);
            if( before == matrix.size() )
                break;
        }
    }


    public Earley( Set<RuleTuple> originalRules ) {
        super(originalRules);
        rules = new Tuple[originalRules.size()];
        int p = 0;
        for( RuleTuple t : originalRules ) {
            if( t.rhs.length == 0 )
                throw new AssertionError("empty production "+t.toString());
            int h = symbolIndexes.get(t.head);
            int[] rhs = new int[t.rhs.length];
            for( int i = 0; i < rhs.length; i++ ) {
                rhs[i] = symbolIndexes.get(t.rhs[i]);
            }
            rules[p++] = new Tuple(h,rhs);
        }
        identifier = symbolIndexes.get("identifier");
        string_literal = symbolIndexes.get("string_literal");
        try {
            digits = symbolIndexes.get("digits");
        } catch( NullPointerException e ) {} // no such symbol

        precomputePredictions();
        filterSingleRhsRules();
    }


    // precomputed  symbol -> predicted rules
    protected Map<Integer, int[]> predicts = new HashMap<Integer, int[]>();

    protected void precomputePredictions() {
        Map<Integer, int[]> closure = new HashMap<Integer, int[]>();
        Map<Integer, int[]> symbolHead2rules = new HashMap<Integer, int[]>();	

        for( int i = 0; i < rules.length; i++ ) {
            int[] tmp = closure.get(rules[i].head);
            int[] tmp1 = symbolHead2rules.get(rules[i].head);
            tmp = Array.insert(tmp, rules[i].rhs[0]);
            tmp1 = Array.insert(tmp1, i);
            closure.put(rules[i].head, tmp);
            symbolHead2rules.put(rules[i].head, tmp1);
        }
        //for( int k : symbolHead2rules.keySet() ) 
        //for( int n : symbolHead2rules.get(k) )
        //System.out.println(allSymbols[k]+" -> "+rules[n]);

        while( true ) {
            int before = size(closure);
            for( int k : closure.keySet() ) {
                final int[] v = closure.get(k);
                int[] tmp = Array.merge(v, new int[0]);
                for( int n : v ) {
                    tmp = Array.merge(tmp, closure.get(n));
                }
                closure.put(k, tmp);
            }
            if( before == size(closure) )
                break;
        }

        //for( int k : closure.keySet() ) 
        //for( int n : closure.get(k) )
        //System.out.println(allSymbols[k]+"->"+allSymbols[n]);

        for( int k : closure.keySet() ) {
            int[] tmp = symbolHead2rules.get(k);
            for( int n : closure.get(k) )
                tmp = Array.merge(tmp, symbolHead2rules.get(n));
            predicts.put(k, tmp);
        }

        //for( int k : predicts.keySet() ) 
        //for( int n : predicts.get(k) )
        //System.out.println(allSymbols[k]+" -> "+rules[n]);


    }

    private void filterSingleRhsRules() {
        Map<Integer, int[]> closure = new HashMap<Integer, int[]>();
        Map<Integer, int[]> symbolRhs02heads = new HashMap<Integer, int[]>();   

        for( int i = 0; i < allSymbols.length; i++ ) // reflexive
            closure.put(i, new int[]{i});

        for( int i = 0; i < rules.length; i++ ) {
            int[] tmp = closure.get(rules[i].rhs[0]);
            int[] tmp1 = symbolRhs02heads.get(rules[i].rhs[0]);
            tmp = Array.insert(tmp, rules[i].head);
            tmp1 = Array.insert(tmp1, rules[i].head);
            closure.put(rules[i].rhs[0], tmp);
            symbolRhs02heads.put(rules[i].rhs[0], tmp1);
        }
        //for( int k : symbolRhs02heads.keySet() ) 
        //for( int n : symbolRhs02heads.get(k) )
        //System.out.println(allSymbols[k]+" -> "+allSymbols[n]);

        while( true ) {
            int before = size(closure);
            for( int k : closure.keySet() ) {
                final int[] v = closure.get(k);
                int[] tmp = Array.merge(v, new int[0]);
                for( int n : v ) {
                    tmp = Array.merge(tmp, closure.get(n));
                }
                closure.put(k, tmp);
            }
            if( before == size(closure) )
                break;
        }

        //for( int k : closure.keySet() ) 
        //for( int n : closure.get(k) )
        //System.out.println(allSymbols[k]+"->"+allSymbols[n]);

        singleRhsRules = new HashSet[allSymbols.length];
        for( int k : closure.keySet() ) {
            Set<Integer> tmp = new HashSet<Integer>();
            for( int n : closure.get(k) )
                tmp.add(n);
            singleRhsRules[k] = tmp;
        }

    }




    private int size( Map<Integer, int[]> closure ) {
        int ret = 0;
        for( int[] tmp : closure.values() )
            ret += tmp.length;
        return ret;
    }


    EarleyCell insert( EarleyCell cell, int rule, int position ) {
        if( cell == null )
            cell = new EarleyCell(null);
        cell.content = Array.insert(cell.content, makeEarleyCell(rule, position));
        return cell;
    }

    protected boolean scan( Matrix matrix, List<LexerToken> src ) {
        long t1 = 0;
        if( Visual.visited != null )
            t1 = System.nanoTime();
        if( matrix.size() == 0 ) {
            int[] content = null;
            for( int i = 0; i < rules.length; i++ ) {
                Tuple t = rules[i];
                String head = allSymbols[t.head];
                if( head.charAt(head.length()-1) != ')' )
                    content = Array.insert(content,makeEarleyCell(i, 0));
            }
            matrix.put(Util.pair(0, 0), new EarleyCell(content));
            allXs = Array.insert(allXs,0);
            if( Visual.visited != null ) {
                long t2 = System.nanoTime();
                Visual.visited[0][0] += (int)(t2-t1);
            }
            return true;
        }

        Integer last = matrix.lastKey();
        int y = Util.Y(last);
        if( src.size() <= y  ) {
            return false;
        }

        LexerToken token = src.get(y);
        Integer suspect = symbolIndexes.get("'" + token.content + "'");

        for( int i = allXs.length-1; 0 <= i; i-- ) {
            int x = allXs[i]; 
            scan(matrix, y, token, suspect, x);
        }
        scan(matrix, y, token, suspect, y);
        return true;
    }
    private void scan( Matrix matrix, int y, LexerToken token, Integer suspect, int x ) {
        long t1 = 0;
        if( Visual.visited != null )
            t1 = System.nanoTime();
        int[] content = null;
        Cell candidateRules = matrix.get(Util.pair(x,y));
        if( candidateRules == null )
            return;
        for( int j = 0; j < candidateRules.size(); j++ ) {
            int pos = candidateRules.getPosition(j);
            int ruleNo = candidateRules.getRule(j);
            Tuple t = rules[ruleNo];

            if( t.size()-1 < pos )
                continue;
            int symbol = t.content(pos);
            if( isScannedSymbol(token, suspect, pos, t, symbol) )
                content = Array.insert(content,makeEarleyCell(ruleNo, pos+1));
        }
        if( content == null )
            return;
        if( Visual.visited != null ) {
            long t2 = System.nanoTime();
            Visual.visited[x][y+1] += (int)(t2-t1);
        }
        matrix.put(Util.pair(x, y+1), new EarleyCell(content));
        allXs = Array.insert(allXs,x);	
    }


    protected boolean isScannedSymbol( LexerToken token, Integer suspect, int pos,
            Tuple t, int symbol ) {
        return suspect != null && suspect == symbol 
                || isIdentifier(token, symbol, suspect) && (suspect==null||notConfusedAsId(suspect,t.head,pos))
                || symbol == digits && token.type == Token.DIGITS
                || symbol == string_literal && token.type == Token.QUOTED_STRING;
    }


    // symbol @ pos within the rule with head, e.g.
    // begin dummy1 : = 'N'
    // doesn't scan to 
    // object_d_rhs: constrained_type default_expr_opt
    protected boolean notConfusedAsId(int symbol, int head, int pos) {
        return true;
    }
    protected boolean isIdentifier( LexerToken token, int symbol, Integer suspect ) {
        return symbol == identifier && token.type == Token.IDENTIFIER
                ||   symbol == identifier && token.type == Token.DQUOTED_STRING;
    }

    void predict( Matrix matrix ) {
        long t1 = 0;
        if( Visual.visited != null )
            t1 = System.nanoTime();
        Integer last = matrix.lastKey();
        //int x = Util.X(last);
        int y = Util.Y(last);
        int yy = Util.pair(y, y);
        EarleyCell cell = (EarleyCell)matrix.get(yy);
        int[] content = null;
        if( cell != null )
            content = cell.content;

        SortedMap<Integer,Cell> range = matrix.subMap(Util.pair(0, y), true, last, true);
        for( int key : range.keySet() ) {
            //for( int rp : matrix.get(key) ) {
            //int pos = Util.X(rp);
            //int ruleNo = 
            Cell candidateRules = matrix.get(key);
            for( int j = 0; j < candidateRules.size(); j++ ) {
                int pos = candidateRules.getPosition(j);
                int ruleNo = candidateRules.getRule(j);
                Tuple t = rules[ruleNo];
                if( t.size() <= pos )
                    continue;
                int symbol = t.content(pos);
                int[] predictions = predicts.get(symbol);
                content = Array.merge(content,predictions);
            }
        }
        if( Visual.visited != null ) {
            long t2 = System.nanoTime();
            Visual.visited[y][y] += (int)(t2-t1);
        }
        if( content != null && content.length > 0 ) 
            matrix.put(yy, new EarleyCell(content));
        else
            return;

    }

    public boolean skipRanges = true;
    void complete( Matrix matrix ) {

        Integer last = matrix.lastKey();
        int y = Util.Y(last);
        //int[] newAllXs = new int[allXs.length];
        //System.arraycopy(allXs, 0, newAllXs, 0, allXs.length);
        //System.out.println("allXs="+Arrays.toString(allXs));
        for( int i = allXs.length-1; 0 <= i; i-- ) {
            int x = allXs[i]; 

            long t1 = 0;
            if( Visual.visited != null )
                t1 = System.nanoTime();

            EarleyCell content =  null;

            int skipTo = y;
            while( true ) {
                //for( int j = 0; j < 1; j++ ) {
                content = (EarleyCell) matrix.get(Util.pair(x, y));
                int before = content==null ? 0 : content.size();

                SortedMap<Integer,Cell> range = matrix.subMap(Util.pair(0, y), true, last, true);

                for( int key : range.keySet() ) {
                    //for( int j = allXs.length-1; 0 <= j && x <= allXs[j]; j-- ) {                	
                    //int mid = allXs[j];
                    int mid = Util.X(key);

                    Cell pres = matrix.get(Util.pair(x, mid));
                    if( pres == null )
                        continue;
                    Cell pos = matrix.get(/*Util.pair(mid, y)*/key);
                    if( pos == null )
                        continue;

                    Map<Integer,int[]> symPre2rulePos = new HashMap<Integer,int[]>();

                    boolean fancyJoin = 100 < pres.size() && 10 < pos.size();
                    if( fancyJoin ) {
                        for( int jj = 0; jj < pos.size(); jj++ ) {
                            int rulePost = pos.getRule(jj);                            
                            Tuple tPost = rules[rulePost];
                            symPre2rulePos.put(tPost.head, new int[0]);
                        }
                        for( int ii = 0; ii < pres.size(); ii++ ) {
                            int dotPre = pres.getPosition(ii);
                            int rulePre = pres.getRule(ii);
                            Tuple tPre = rules[rulePre];
                            if( tPre.size() == dotPre )
                                continue;
                            int symPre = tPre.content(dotPre);
                            int[] tmp = symPre2rulePos.get(symPre);
                            if( tmp == null )
                                continue;
                            int[] tmp1 = Array.insert(tmp, Util.pair(rulePre, dotPre));
                            symPre2rulePos.put(symPre, tmp1);
                        }
                    }

                    for( int jj = 0; jj < pos.size(); jj++ ) {
                        int dotPost = pos.getPosition(jj);
                        int rulePost = pos.getRule(jj);                            
                        Tuple tPost = rules[rulePost];
                        if( tPost.size() != dotPost ) 
                            continue;

                        if( fancyJoin ) {
                            int[] tmp = symPre2rulePos.get(tPost.head);
                            if( tmp == null )
                                continue;
                            for( int k : tmp ) {
                                int dotPre = Util.Y(k);
                                int rulePre = Util.X(k);
                                Tuple tPre = rules[rulePre];
                                if( tPre.size() == dotPre )
                                    continue;
                                int symPre = tPre.content(dotPre);
                                if( symPre != tPost.head )
                                    continue;
                                if( !lookaheadOK(tPre,dotPre+1) )
                                    continue;
                                content = insert(content,rulePre,dotPre+1);
                                if( skipRanges && rules[rulePre].rhs.length==dotPre+1 && mid < skipTo 
                                        && allSymbols[rules[rulePre].head].charAt(0)!='"'
                                        ) 
                                    skipTo = mid;
                                continue;
                            }
                        } else                        
                            for( int ii = 0; ii < pres.size(); ii++ ) {
                                int dotPre = pres.getPosition(ii);
                                int rulePre = pres.getRule(ii);
                                Tuple tPre = rules[rulePre];
                                if( tPre.size() == dotPre )
                                    continue;
                                int symPre = tPre.content(dotPre);
                                if( symPre != tPost.head )
                                    continue;
                                if( !lookaheadOK(tPre,dotPre+1) )
                                    continue;
                                content = insert(content,rulePre,dotPre+1);
                                if( skipRanges && rules[rulePre].rhs.length==dotPre+1 && mid < skipTo 
                                        && allSymbols[rules[rulePre].head].charAt(0)!='"'
                                        ) 
                                    skipTo = mid;
                                continue;
                            }
                    }

                }
                //System.out.println("x="+x+",y="+y+", skipTo="+skipTo);
                if( skipRanges && x < skipTo && skipTo < y ) {
                    for( int k = x+1; k < skipTo; k++ ) {
                        allXs = Array.delete(allXs,k);
                    }
                }

                if( Visual.visited != null ) {
                    long t2 = System.nanoTime();
                    Visual.visited[x][y]=Util.addlY(Visual.visited[x][y],(int)(t2-t1));
                }

                if( content == null || content.size() == before )
                    break;
                matrix.put(Util.pair(x, y),content);
            }
        }
    }

    protected boolean lookaheadOK( Tuple tPre, int pos ) {
        return true;
    }

    void toHtml( int ruleNo, int pos, boolean selected, 
            int x, int mid, int y, Matrix matrix, StringBuffer sb ) {
        Tuple rule = rules[ruleNo];
        String size = "+1";
        if( selected ) {
            sb.append("<b>");
            size = "+2";
        }
        sb.append("<font size="+size+" color=blue>"+allSymbols[rule.head]+":</font> ");
        final String greenish = "<font size="+size+" bgcolor=rgb(150,200,150))>";
        final String bluish = "<font size="+size+" bgcolor=rgb(150,150,200))>";
        sb.append(greenish);
        for( int i = 0; i < rule.rhs.length; i++ ) {			
            if( pos == i )
                sb.append("</font>"+bluish);
            sb.append(allSymbols[rule.rhs[i]]+" ");
        }
        sb.append("</font>");		
        if( selected ) 
            sb.append("</b>");

        if( mid == -1 )
            return;
        if( selected && x+y!=0) {
            if( mid < x || x == y ) {
                sb.append("<i> predict from </i>");
                Cell bc = matrix.get(Util.pair(mid, y));
                for( int j = 0; j < bc.size(); j++ ) {
                    int bp = bc.getPosition(j);
                    int br = bc.getRule(j);
                    Tuple bt = rules[br];
                    if( bp < bt.rhs.length && bt.rhs[bp] == rule.head ) {
                        //sb.append("<font size="+size+" color = green>");
                        //toString(br,bp,sb);
                        //sb.append("</font>");
                        toHtml(br,bp,false, -1,-1,-1, null, sb);
                        return;
                    }
                }
            } else if( y < mid ) {
                sb.append("<i> scan from </i>");
                Cell bc = matrix.get(Util.pair(x, y-1));
                for( int j = 0; j < bc.size(); j++ ) {
                    int bp = bc.getPosition(j);
                    int br = bc.getRule(j);
                    Tuple bt = rules[br];
                    if( br == ruleNo && bp+1 == pos ) {
                        //sb.append("<font size="+size+" color = green>");
                        //toString(br,bp,sb);
                        //sb.append("</font>");
                        toHtml(br,bp,false, -1,-1,-1, null, sb);
                        return;
                    }
                }
            } else {
                sb.append("<i> complete from </i>");
                boolean secondTime = false;
                Cell pre = matrix.get(Util.pair(x, mid));
                Cell post = matrix.get(Util.pair(mid, y));
                for( int i = 0; i < pre.size(); i++ ) 
                    for( int j = 0; j < post.size(); j++ ) {
                        int dotPre = pre.getPosition(i);
                        int dotPost = post.getPosition(j);
                        int rulePre = pre.getRule(i);
                        int rulePost = post.getRule(j);                            
                        Tuple tPre = rules[rulePre];
                        Tuple tPost = rules[rulePost];
                        if( tPre.size()!=dotPre && tPost.size()!=dotPost )
                            continue;
                        if( tPost.size() == dotPost ) {
                            if( rulePre != ruleNo )
                                continue;
                            if( dotPre+1 != pos )
                                continue;
                            int symPre = tPre.content(dotPre);
                            if( symPre != tPost.head )
                                continue;
                            if( secondTime )
                                sb.append("<b> or </b>");                                
                            toHtml(rulePre,dotPre,false, -1,-1,-1, null, sb);
                            sb.append("<i> and </i>");
                            toHtml(rulePost,dotPost,false, -1,-1,-1, null, sb);
                            secondTime = true;
                        }	        		
                    }
            }
        }
    }
    void toString( int ruleNo, int pos, StringBuffer sb ) {
        Tuple rule = rules[ruleNo];
        sb.append(rule.toString(pos));		
    }



    @Override
    public ParseNode forest(List<LexerToken> src, Matrix m) {
        explored = new HashSet<Long>();
        return super.forest(src, m);
    }


    @Override
    ParseNode treeForACell( List<LexerToken> src, Matrix m, Cell cell, int x, int y ) {
        int rule = -1;
        int pos = -1;
        for( int i = 0; i < cell.size(); i++ ) {
            rule = cell.getRule(i);
            pos = cell.getPosition(i);
            if( rules[rule].rhs.length == pos )
                return tree(src, m, x, y, rule,pos);
        }
        if( rule != -1 && pos != -1 && x+1 == y )
            return tree(src, m, x, y, rule,pos);
        return null;
    }

    private ParseNode tree( List<LexerToken> src, Matrix m, int x, int y, int rule, int pos ) {
        //System.out.println(" @["+x+","+y+") >>>>> "+rules[rule].toString(pos));
        //if(x==36&&y==65)
        //System.out.println();
        ParseNode ret = followScan(src, m,x,y,rule,pos); 
        if( ret != null )
            return ret;

        return followComplete(src,m, x, y, rule,pos);
    }


    private ParseNode followScan( List<LexerToken> src, Matrix m, int x, int y, int rule, int pos ) {
        //System.out.println("try scan");
        Cell pre = m.get(Util.pair(x,y-1));
        if( pre == null )
            return null;
        Cell cell = m.get(Util.pair(x,y));
        for( int i = 0; i < pre.size(); i++ ) {
            int rI = pre.getRule(i);
            int pI = pre.getPosition(i);
            if( rI != rule )
                continue;
            if( pI+1 != pos )
                continue;
            Tuple t = rules[rule];
            LexerToken token = src.get(y-1); 
            Integer suspect = symbolIndexes.get("'" + token.content + "'");
            int symbol = t.content(pI);
            if( !isScannedSymbol(token, suspect, pI, t, symbol) )
                continue;
            ParseNode branch = new ParseNode(y-1,y, rules[rI].rhs[pI],rules[rI].rhs[pI], this);
            if( x+1 == y ) {
                if( rules[rI].rhs.length == 1 )
                    branch.addContent(rules[rI].head);
                return branch;
            }
            int head = rules[rI].head;
            if( pos != rules[rI].rhs.length ) {
                head = -1;
            }
            ParseNode ret = new ParseNode(x,y,head,head, this);
            ret.lft = tree(src,m, x,y-1,rI,pI);
            ret.lft.parent = ret;
            ret.rgt = branch;
            ret.rgt.parent = ret;
            return ret;
        }
        return null;
    }

    private Set<Long> explored = new HashSet<Long>();
    private ParseNode followComplete( List<LexerToken> src, Matrix m, int x, int y, int rule, int pos ) {
        //System.out.println("try complete");
        for( int mid = y-1; x <= mid; mid-- ) {
            Cell pre = m.get(Util.pair(x,mid));
            if( pre == null )
                continue;
            Cell post = m.get(Util.pair(mid,y));
            if( post == null )
                continue;
            for( int i = 0; i < pre.size(); i++ ) {
                int rI = pre.getRule(i);
                int pI = pre.getPosition(i);
                if( rI != rule )
                    continue;
                if( pI+1 != pos )
                    continue;
                for( int j = 0; j < post.size(); j++ ) {
                //for( int j = post.size()-1; 0 <= j ; j-- ) {
                    int rJ = post.getRule(j);
                    int pJ = post.getPosition(j);
                    if( rules[rJ].rhs.length != pJ )
                        continue;
                    if( rules[rI].rhs.length <= pI )
                        continue;
                    if( rules[rJ].head != rules[rI].rhs[pI] )
                        continue;

                    // can degenerate into a cycle, example:
                    // subquery: subquery[439,451) 
                    // complete from 
                    // subquery: subquery[439,451) and subquery[439,451): subquery 
                    // or 
                    // subquery: subquery[439,451) and subquery[439,451): subquery subquery[443,450) 
                    long midYrJ = Util.lPair(Util.pair(mid, y), rJ);
                    if( explored.contains(midYrJ) )
                        continue;
                    explored.add(midYrJ);

                    int head = rules[rI].head;
                    if( rules[rI].rhs.length != pI+1 ) {
                        head = -1;
                    }
                    ParseNode ret = new ParseNode(x,y,head,head, this);
                    try {
                        if( x != mid ) {
                            ret.lft = tree(src,m, x,mid,rI,pI);
                            ret.lft.parent = ret;
                            ret.rgt = tree(src,m, mid,y,rJ,pJ);
                            ret.rgt.parent = ret;
                        } else {
                            ret = tree(src,m, mid,y,rJ,pJ);
                            if( head != -1 )
                                ret.addContent(head); 
                        }
                    } catch( AssertionError e ) {
                        if( e.getMessage().startsWith("unwind") )
                            continue;
                        throw e;
                    }
                    return ret;
                }
            }
        }
        throw new AssertionError("unwind "+rules[rule]+" @["+x+","+y+")");
    }

    public class Tuple implements Comparable<Tuple> {
        public int head;
        public int[] rhs;  
        public Tuple( int h, int[] r ) {
            head = h;
            rhs = r;
        }
        public int size() {
            return rhs.length; 
        }
        public int content( int i ) {
            return rhs[i];
        }

        public boolean equals(Object obj) {
            return (this == obj ) || ( obj instanceof Tuple &&  compareTo((Tuple)obj)==0);
        }
        public int hashCode() {
            throw new RuntimeException("hashCode inconssitent with equals"); //$NON-NLS-1$
        }              
        public int compareTo( Tuple src ) {
            if( head==0 || src.head==0 )
                throw new RuntimeException("head==0 || src.head==0"); //$NON-NLS-1$
            int cmp = head-src.head;
            if( cmp!=0 )
                return cmp;
            cmp = rhs.length-src.rhs.length;
            if( cmp!=0 )
                return cmp;
            for( int i = 0; i < rhs.length; i++ ) {				
                cmp = rhs[i]-src.rhs[i];
                if( cmp!=0 )
                    return cmp;                    
            }
            return  0;
        }
        public String toString() {
            StringBuilder s = new StringBuilder(allSymbols[head]+":");
            for( int i : rhs )
                s.append("  "+allSymbols[i]);
            s.append(";");
            return s.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        public String toString( int pos ) {
            StringBuilder s = new StringBuilder(allSymbols[head]+":");
            for( int i = 0; i < rhs.length; i++ ) {
                s.append(' ');
                if( pos == i )
                    s.append('!');
                s.append(allSymbols[rhs[i]]);
            }
            if( pos == rhs.length )
                s.append('!');
            s.append(";");
            return s.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    public String print( int rulePos ) {
        return rules[Util.X(rulePos)].toString(Util.Y(rulePos));
    }

    protected int makeEarleyCell( int rule, int position ) {
        return Util.pair(rule, position);
    }
    int ruleFromEarleyCell( int code ) {
        return Util.X(code);
    }
    public class EarleyCell implements Cell {
        int[] content = null;
        public EarleyCell( int[] content ) {
            this.content = content;
        }

        public int getSymbol( int index ) {
            throw new AssertionError("N/A");
        }

        public int getRule( int index ) {
            return Util.X(content[index]);
        }

        public int getPosition( int index ) {
            return Util.Y(content[index]);
        }

        public int size() {
            return content.length;
        }

        public int[] getContent() {
            throw new AssertionError("Legacy CYK method called by Earley");
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{ ");
            for( int i = 0; i < content.length; i++ ) {
                if( 0 < i ) 
                    sb.append(" , ");
                Tuple t = rules[getRule(i)]; 
                sb.append(t.toString(getPosition(i)));
            }
            sb.append(" }");
            return sb.toString();
        }
    }


}



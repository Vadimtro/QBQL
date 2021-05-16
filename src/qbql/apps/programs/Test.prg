include db/Figure1.db;
include udf.def;

"lft < rgt"^R00=[lft  rgt].

["^^^^^ Predicate Header Evaluation ^^^^^"];

R00 ^ (x v y v z) = R00 &
(x /^ (y ^ z)) ^ R00 = ((x /^ y) ^ (x /^ z)) ^ R00 ->
x /^ (y ^ z) = (x /^ y) ^ (x /^ z).

["^^^^^ CONDITIONAL DISTRIBUTIVITY ^^^^^"];

(<NOT>x) ^ x = x ^ R00.
(<NOT>x) v x = x v R11. 

(<INV>x) ^ x = x ^ R11.
(<INV>x) v x = x v R00. 

<NOT>R00 = <INV>R11.

["^^^^^ INV AXIOMS ^^^^^"];

(<INV>x) v (<INV>y) = <INV>(x <OR> y).
(<NOT>x) ^ (<NOT>y) = <NOT>(x <OR> y).

(<INV>x) <OR> (<INV>y) = <INV>(x v y).
(<INV><NOT>x) v (<INV><NOT>y) = <INV><NOT>(x ^ y).

["^^^^^ De Morgan ^^^^^"];

x ^ ((<INV>y) v (<INV>z)) = (x ^ (<INV>y)) v (x ^ (<INV>z)).

["^^^^^ Distributivity ^^^^^"];

x /> y = <NOT>((<NOT>x ^ y) <and> <INV>(y v x)).
x /> y  = <NOT>(<NOT>x /^ y).

["^^^^^ Relational Division/ set Joins ^^^^^"];

(y > x) ->  --FD(r,x,y) 
            (x^y > x^<NOT>y).
            
x^y > x^<NOT>y & --FD(r,y,z)
y^z > y^<NOT>z -> --FD(r,x,z)
x^z > x^<NOT>z. 
  
x^y > x^<NOT>y -> --FD(r,x^z,y^z)
            x^y^z > x^z^<NOT>(y^z).

["^^^^^ Fictional dependency ^^^^^"];

Cat ^ [source from] Hello 3 =[from  postfix  prefix  source]
                        3  lo  Hel  Hello
.
Cat ^ [source] Hello World ^ [from] 3=[from  postfix  prefix  source]
                                3  ld  Wor  World
                                3  lo  Hel  Hello
.
Cat ^ [] = [from  prefix  postfix   source].

[source from] Hello 3 /^ "from=f" /^ "source=s"=[f  s]
                                          3  Hello
.
(Cat /^ "source=src") ^ [src] Hello World ^ [from] 3
~
Cat ^ [source] Hello World ^ [from] 3
.
(Substr ^ [source] Hello World ^ [from to] 1 3) v [fragment]=[fragment]
                                                   el
                                                   or
.
(Substr ^ [source] Hello World ^ [fragment] "o" l) v [from fragment]=[fragment  from]
                                                           l  2
                                                           l  3
                                                           o  1
                                                           o  4
.

Substr1 = (Substr /^ "source=src1" /^ "from=from1") v [src1 from1 fragment];
Substr2 = (Substr /^ "source=src2" /^ "from=from2") v [src2 from2 fragment];
Substr1^[]=[src1 from1 fragment].
Substr2^[]=[src2 from2 fragment].

((Substr1 ^ [src1] Hello) ^ (Substr2 ^ [src2] World) ^ ([fragment]"")') 
v [from1 from2 fragment]=[fragment  from1  from2]
                          l  2  3
                          l  3  3
                          o  4  1
.


HelloFgmts = ("from=from1" /^ Substr /^ [source] Hello) v [from1 fragment];
WorldFgmts = ([source] World /^ Substr /^ "from=from2") v [from2 fragment];
HelloFgmts ^ WorldFgmts ^ ([fragment]"")'=[fragment  from1  from2]
                                       l  2  3
                                       l  3  3
                                       o  4  1
.

SubstrSrc = (Substr /^ [source] "Hello World" /^ [fragment]o) v [prefix postfix];
(Substr /^ (SubstrSrc /^ [fragment]"***")) v [source] = [source]
                                                 "Hell*** World"
                                                 "Hello W***rld"
.

["^^^^^ Predicates ^^^^^"]; 

[i a] 
  0 1 
  1 1 
  2 3 
 /= "sum += a[i]"=[sum] 5.

[a] 
 1 
 1 
 3 
 /= "sum += a" = [sum] 4.
 
["^^^^^ Aggregates ^^^^^"]; 

Points = [x y]
          0 1
          0 3
          2 2
          2 5
          3 1
          3 3
          4 2
          5 0
          6 3
;

Points ^ "x <= y" =[x  y]
                 0  1
                 0  3
                 2  2
                 2  5
                 3  3
. 
Points ^ "x <= 2"=[x  y]
                 0  1
                 0  3
                 2  2
                 2  5
.


AtOneSide 
/^ ([x1 y1] 0 1
 ^  [x2 y2] 2 2
 ^  [xa ya] 0 3
 ^  [xb yb] 3 1) = R00.
AtOneSide 
/^ ([x1 y1] 0 1
 ^  [x2 y2] 2 2
 ^  [xa ya] 0 3
 ^  [xb yb] 3 3) = R01.


(
     (Points /^ "x=x1" /^ "y=y1") 
   ^ (Points /^ "x=x2" /^ "y=y2") 
   ^ (Points /^ "x=xa" /^ "y=ya") 
   ^ (Points /^ "x=xb" /^ "y=yb") 
 ^ AtOneSide 
) /= (
     (Points /^ "x=xa" /^ "y=ya") 
   ^ (Points /^ "x=xb" /^ "y=yb")
)^ ("x1=x2" ^ "y1=y2")'
= [x1  x2  y1  y2]
   0  0  1  3
   0  0  3  1
   0  2  3  5
   0  5  1  0
   2  0  5  3
   2  6  5  3
   5  0  0  1
   5  6  0  3
   6  2  3  5
   6  5  3  0
.

["^^^^^ Convex Hull ^^^^^"];


"for(int i = 0; i<5; i++)" /^ "i+3=inc3"=[inc3]
                                        3
                                        4
                                        5
                                        6
                                        7
.

"i in [1,...,5)" ^ "i * i = i2" = [i  i2]
                               1  1
                               2  4
                               3  9
                               4  16
.
["^^^^^ User Defined Operations ^^^^^"]; 

Au=[i j s]
    1 1 3
    1 2 1
    2 1 1
    2 2 0
;
Ap=[i j1 j2]
    1 3  1
    2 1  0
;
Au =
((Ap /^ "j1=s") /^ [j]1)
v
((Ap /^ "j2=s") /^ [j]2)
. 
Ap =
((Au /^ "j1=s") /^ [j]1)
^
((Au /^ "j2=s") /^ [j]2)
.

["^^^^^ Pivot ^^^^^"];

"3 <*> 5 = p";
--=[p] "15.0".   -- need round relation

["^^^^^ Generic Predicates ^^^^^"]; 

(y=y <-> x=x).
!(y=x <-> x=x).

["^^^^^ Negated Assertion ^^^^^"];


["8507 < Time < 8686 (2500K 4.2Ghz) "];

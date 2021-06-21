-- 2 different styles of include
include "c:/users/dim/git/qbql/src/qbql/apps/db/Figure1.db"; 
--include "c:/users/dim/git/qbql/src/qbql/apps/db/DependencyTheory.db";
include udf.def;  
--include volume.db;


-- wrong "i in [1,...,20)" ^ "3 * x = i" ^ "5 * x = i" /^ "int x";

/*
Colored = [name color]
           A    green
--missing info:  B    color=?
           C    blue
;

"Colored(name,color)" = Colored <OR> ([name] B); 

Next = [lft rgt]
            A     B
            B     A
            B     C
            C     B
;

--"Colored(name,color)";
--"Next(from,to)";

GreenBlocksAtTheLeft = Colored /^ "name=lft" /^ [color]green;
GreenBlocksAtTheLeft;

CB = Colored v ([name]B ^ ([color])');

(CB /^ "name=name1" /^ [color]green) 
/^ Next /^ 
(CB /^ "name=name2" /^ ([color]green)');
*/

--x = (x <OR> y) <and> ( x <OR> <NOT>(<INV>(y)) ).

-- false: x < y & u < v -> x /^ u < y /^ v.

--<NOT>(y) /< <NOT>(x) = x /< y.

--x <OR> (y ^ <NOT> x) = y <OR> (x ^ <NOT> y). --=<NOT>((<NOT>(y) ^ <NOT>(x))).
--x ^ <NOT> (y <OR> z) = (x ^ <NOT> y) ^ <NOT> z.
--x ^ (y ^ <NOT> z) = (x ^ y) ^<NOT> (x ^ z).
/*
[p q]
 1 a
 2 b
<OR>
[r q]
 1 a
 0 c
;
*/


/*x /< y = <NOT>( <INV>(x v y) <and> (x ^ <NOT>y) ).

x <mult> y = x ^ y.
x <plus> y = x <OR> y.

-- semiring
x <plus> y = y <plus> x.
x <plus> (y <plus> z) = (x <plus> y) <plus> z.
x <plus> R00 = x.

x <mult> R01 = x.
x <mult> (y <mult> z) = (x <mult> y) <mult> z.
x <mult> (y <plus> z) = (x <mult> y) <plus> (x <mult> z).

x <mult> R00 = R00.

x ^ (y v z) = (x ^ (z v (R00 ^ y))) v (x ^ (y v (R00 ^ z))).
(x ^ y) v (x ^ z) =  x ^ ( ((x v z) ^ y) v ((x v y) ^ z) ).
(R00 ^ (x ^ (y v z))) v (y ^ z) = ((R00 ^ (x ^ y)) v z) ^ ((R00 ^ (x ^ z)) v y).
*/

/*
M_PQ=[p  q] v MaierP137;
M_PR=[p  r] v MaierP137;
M_PQjM_PR = M_PQ ^ M_PR;
M_RQ=[r  q] v MaierP137;
M_PQjM_RQ = M_PQ ^ M_RQ;
M_PRjM_RQ = M_PR ^ M_RQ;

MaierP137 = M_PQ ^ M_PRjM_RQ.
MaierP137 = M_PR ^ M_PQjM_RQ.
MaierP137 = 
(M_PQ v M_PQjM_RQ) ^ 
(M_PQ v M_PR) ^
(M_PRjM_RQ v M_PR) ^
(M_PRjM_RQ v M_PQjM_RQ). 
M_PQ v M_PQjM_RQ = M_PQ.
M_PQ v M_PR;
M_PRjM_RQ v M_PR = M_PR.
PQR2 = M_PRjM_RQ v M_PQjM_RQ;
--Q v [p  q];
--Q v [p  r];
PQR2_PQ = PQR2 v [p  q];
PQR2_PR = PQR2 v [p  r];
M_RQ = PQR2 v [r  q].
PQR2 = PQR2_PQ ^ M_RQ.
PQR2 = M_RQ ^ PQR2_PR.
--PQR2 = PQR2_PQ ^ PQR2_PR.
PQR2_PQ v M_RQ;
PQR2_PQ v PQR2_PR;
M_RQ v M_RQ = M_RQ.
M_RQ v PQR2_PR;
*/

/*
((s ^ t) ^ s) v (((s ^ t) v s) ^ R00)*  = (((R00 ^ s) v t) ^ s). -- thrm

x < x*.

x < y -> x* < y*.

x ^ x* = x.

(x*)*=x*.

x ^ y = x -> x* ^ y* =x*.

(x v y) ^ R00 = R00 
-> (x ^ y)* = x* ^ y*.

x ^ R00=x* ^ R00.
R00* = R00.


(((R00 ^ s) v t) ^ s) = s* & 
(((R00 ^ t) v s) ^ t) = t* & 
(((R00 ^ u) v w) ^ u) = u* & 
(((R00 ^ w) v u) ^ w) = w* & 
s ^ t = u ^ w ->
s ^ t = 
(s v w) ^ 
(s v u) ^
(t v u) ^
(t v w). 


(s <and> (s^t))* = s* & 
(t <and> (s^t))* = t* & 
(u <and> (s^t))* = u* & 
(w <and> (s^t))* = w* & 
s ^ t = u ^ w &
s ^ t = 
(s v w) ^ 
(s v u) ^
(t v u) ^
(t v w) 
-> s = u | s = w | t = u | t = w | (s v t)^R00 = R00
| (u v w)^R00 = R00 | s^t=(s^t)* | s=s* | t=t* | u=u* | w=w*
.
*/

/*
(x+)+ = x+.
x+ < x.
x < y -> x+ < y+.

x+ ^ y+ = (x ^ y)+. 

([p]1 2)+;         
*/


/*
y < x -> (r ^ y) < x.
(r ^ y) < x -> (r ^ y ^ z) < x ^ z.
(r ^ y) < x & (r ^ z) < y -> (r ^ z) < x.
*/

/*
<NOT>(y)^x < y <-> <NOT>(y)^x < y^x.

<INV>(x) ^ x < y <-> <NOT>(y)^x < y^x.

x^<NOT>y < y <-> (R11 ^ x) < y.

x^r < y <-> x^r < y^r.
*/

/*
x < y <-> R01 < x <"->"> y.
x ^ (x <"->"> y) < y.
(x <"->"> y) ^ <NOT>y < <NOT>x.
(x <"->"> y) ^ (y <"->"> z) < (x <"->"> z).
(x <"->"> y) ^ (x <"->"> z) < (x <"->"> (y^z)).

R01 = (x <"->"> (y <"->"> x)). 
R01 = ((x <"->"> (y <"->"> z)) <"->"> ((x <"->"> y) <"->"> (x <"->"> z))).
R01 = (((<NOT>x) <"->"> <NOT>y) <"->"> (((<NOT>x) <"->"> y) <"->"> x)). 
*/
/*
R11 < (x <"->"> (y <"->"> x)). -- <--> R11 < R11 v (y ^ x) 
R11 < ((x <"->"> (y <"->"> z)) <"->"> ((x <"->"> y) <"->"> (x <"->"> z))). -- <-->  R11 v ((z ^ x) ^ y)
R11 < (((<NOT>x) <"->"> <NOT>y) <"->"> (((<NOT>x) <"->"> y) <"->"> x)). -- <--> R00 /< (y ^ x), R11 v (y ^ x)
-- not interesting because:
x <"->"> (y <"->"> x) = ((<NOT>x) <"->"> <NOT>y) <"->"> (((<NOT>x) <"->"> y) <"->"> x).
*/

/*
R00 = <NOT>(x v <NOT>(x v <INV>x)).
R11 = <INV>(x v <NOT>(x v <INV>x)).
--R00 = (x v (x v x`)')'.
(x ^ y) v (x ^ z) = x ^ ( ((x v z) ^ y) v ((x v y) ^ z) ).
--false:(x ^ (y v (x ^ z))) ^ (x ^ (z v (x ^ y))) = x ^ (z v y).
--false:x ^ (y v z) = x ^ (y v ((x v y) ^ (z v (x ^ y)))).

--x <gn> y = (<NOT>y v <NOT>x) ^ (<INV>y v <INV>x).
--false:x <gn> x = x.
x <gn> y = y <gn> x.
-- false: x <gn> (y <gn> z) = (x <gn> y) <gn> z.
x <gn> R00 = <INV>x.
x <gn> R11 = <NOT>x.

x <gn2> y = (<NOT>(x) ^ <INV>(y)).
x <gn2> R11 = <NOT>x.
R00 <gn2> y = <INV>y.
*/

/*
p^R00=p & q^R00=q & r^R00=r & x^R00 < p^q^r
&
x /^ (x v t) = x v t`
&
x v (p ^ q) < (x v p) /< (x v q)
&
x v (q ^ r) < (x v q) /< (x v r)
->
x v (p ^ r) < (x v p) /< (x v r).
*/
/*
x^R00 < t --& t ^ R00 = t
&
(x v t) /= x = x v <INV>t 
&
x < (x v p) /< (x v q)
->
((x v <INV>t) v (p ^ q)) ^ ((x v <INV>t) v (p ^ <INV>q)) < x v <INV>t.
*/
/*
X=[p   q   r]
   =   =   !=     --(t1,t2)
   =   =   !=     --(t3,t4)
   ------------------------
   !=  =   =      --(t1,t5)
   ------------------------
   !=  =   !=     --(t2,t5)
   ------------------------
   !=  !=  =      --(t2,t4)
   !=  !=  =      --(t3,t5)
   !=  !=  =      --(t1,t3)
   ------------------------
   !=  !=  !=     --(t4,t5)
   !=  !=  !=     --(t2,t3)
   !=  !=  !=     --(t1,t4)
;*/

/*X=[p   q   r]
   0   a   0
   0   a   1
   0   b   0
   0   b   1
   1   a   0
;
X#R00;
X#[p];
X#[q];
X#[r];
X#[p q];
X#[q r];
X#[r p];
X#R10;
X=(X v [p r]) ^ (X v [q r]) ^ (X v [p q]).*/

/*y = [p  r]
     0  0
     0  1
     1  1
;
x = [r  s]
     1  1
     1  2
     2  2
;*/

/*
z = [q  r]
     b  0
;
y = [q  r]
     a  0
;
x = [p  r]
     0  0
;
y <and> z;
x <and> (y <and> z); 
x <and> y;
(x <and> y) <and> z;
*/

/*
R11 /^ x = <INV>x.
R00 /= x = <NOT>x.
x /= x = R01.
--x /^ x = R0?.
--(x /= x) ^ y = R00.
x /< y = <NOT>((<NOT>(y) /^ x)).
x /= y = <NOT>(((<NOT>(y) /^ x) v (<NOT>(x) ^ y))).
x /^ y = ((R00 ^ (y /= x)) v (y ^ x)).

x ^ (y /= y) = x.
x v (y /= y) = y /= y.
*/
/*
"i=2..N" = "i in [2,...,100)";

"N" = "i=2..N" /= "v i = N";

"j=2..SQRT(N)" = (("i=2..N" ^ "i * i = i2" ^ "N" ^ "i2 < N") v [i]) /^ "i=j";

"i=2..N" - (("i=2..N" ^ "j=2..SQRT(N)" ^ "k*j=i" ^ "int k" ^ "1 < k") v [i]);
*/

--"[q r]a0b0" ⊗ "[q r]a0b0a1";

/*x ⊗ y = y ⊗ x.
--x ⊗ (y ⊗ z) = (x ⊗ y) ⊗ z.
x ⊗ (y v z) = (x ⊗ y) v (x ⊗ z).
x < y -> x ⊗ z < y ⊗ z.
--fail: x ⊗ (y ^ z) = (x ⊗ y) ^ (x ⊗ z).
--fail: x ^ (y ⊗ z) = (x ^ y) ⊗ (x ^  z).
*/

a v b=R00 &
b v c=R00 &
a v c=R00
->
(a^c)v(((a^b)v r)^( (b^c)v( ((a^c)v r)
^
((a^b)v ( ((b^c)v r)^( (a^c)v(((b^c)v r)^((a^b)v r)) ) )) ) ) )=
(a^c)v( ((a^b)v ( ((b^c)v r)^( (a^c)v(((b^c)v r)^((a^b)v r)) ) ))
^
( (b^c)v(((a^b)v r)^((a^c)v r)) ) )
.


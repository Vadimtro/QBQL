/*
X=[p  q  r]
   0  a  0
   0  a  1
   1  c  0
   1  c  1
   2  a  0
;
X#R00;
X#[p];
X#[q];
X#[r];
X#[p q];
X#[q r];
X#[r p];
X#R10;

X#[p] /^ X#[p];
X#[p] /^ X#[q];
X#[p] /^ X#[r];
X#[p] /^ X#[p q];
X#[p] /^ X#[q r];
X#[p] /^ X#[r p];
X#[p] /^ X#R00;
X#[p] /^ X#R10;
*/


/*

r#(x ^ y) = (r#x) ^ (r#y). -- (^H)
r#(x v y) = (r#x) v (r#y). -- (vH)
r#x ^ (r#y v r#z) = (r#x ^ r#y) v (r#x ^ r#z). --follows from distributivity of headers
r^R00 = s^R00 -> (r^s)#x = (r#x)^(s#x).

(r ^ s)#x = (r#x) ^ (s#x).

[_reflexivity_];
x^R00 < y^R00 -> r#x < r#y. 
[_augmentation_];
r#x < r#y -> r#(x^z) < r#(y^z).
[_transitivity_];
r#x < r#y & r#y < r#z -> r#x < r#z.


x^R00=x & y^R00=y & r#x < r#y  ->
r = (r v (x^y)) ^ (r v (x^y`)).

x^R00=x & y^R00=y 
& r^R00<x & r^R00<y & x v y = R00 & (r^R00)v[s t]=R00 & x^y != r^R00 & x!=R00
& r = (r v (x^y)) ^ (r v (x^y`)) ->
r#x < r#y.
*/



/*
r = [p  q  r]
     0  a  0
     0  a  1
     0  c  0
     0  c  1
     1  a  0
;
s = [p  q  r]
     0  a  0
     0  a  1
     0  c  0
     1  a  0
;
x = [p];
y = [q];
z = [r];

r#x > r#y ^ r#z.
*/

--x^R00=x  ->
--r#(x ^ y) = (r#x) ^ (r#y). -- (^H)
--r#(x v y) = (r#x) v (r#y). -- (vH)
--(r^s)#x = (r#x)^(s#x).

--x#y = (R11^x)#y.
--R11#y = (R11 v y)#(R11 v y).

/*
(r ^ s)#x;
(r ^ s)'#(r ^ s);
r#x v r'#r;
s#x v s'#s;

r^R00=s^R00 ->
(r ^ s)#x v (r ^ s)'#(r ^ s) = (r#x v r'#r) ^ (s#x v s'#s).
r^R00=s^R00 ->
(r v s)#x v (r v s)'#(r v s) = (r#x v r'#r) v (s#x v s'#s).
*/



CntRelHdr = [cnt];
CardRelHdr = [card];
plpHdr = [plp];
t1=[t]1;
x <Prob> y = ((x |v| (y ^ CntRelHdr)) ^ (x |v| CardRelHdr)) /^ "cnt / card = prob".
x <Entropy> y = ((((x <Prob> y) ^ "ln(prob)=lp") /^ "prob*lp=plp") v plpHdr) /= "result += plp".
x <Gini> y = ((((x <Prob> y) ^ t1 ^ "t-prob=p1" ) /^ "prob*p1=plp") v plpHdr) /= "result += plp".

X=[t  x  y]
   0  0  0
   1  -1  2 --4
   2  0  2  --0
   3  3  0
;
-- x = t^2-2*t
-- y = x^2-3*x   vs. y = t-x
--X<Entropy>R00;
X<Entropy>[t];
X<Entropy>[x];
X<Entropy>[y];
X<Entropy>[t x];
X<Entropy>[x y];
X<Entropy>[t y];
X<Entropy>R10;
/*X#R00= <0,0,0> <1,-1,4> <2,0,0> <3,3,0>;
X#[t]= <0,0,0> | <1,-1,4> | <2,0,0> | <3,3,0>;
X#[x]= <0,0,0> <2,0,0> | <1,-1,4> | <3,3,0>;
X#[y]= <0,0,0> <2,0,0> <3,3,0> | <1,-1,4>;
X#[tx]= <0,0,0> | <1,-1,4> | <2,0,0> | <3,3,0>;
X#[xy]= <0,0,0> <2,0,0> | <1,-1,4> | <3,3,0>;
X#[ty]= <0,0,0> | <1,-1,4> | <2,0,0> | <3,3,0>;
X#R10= <0,0,0> | <1,-1,4> | <2,0,0> | <3,3,0>;*/
/*X#R00= <0,0,0> <1,-1,2> <2,0,2> <3,3,0>;
X#[t]= <0,0,0> | <1,-1,2> | <2,0,2> | <3,3,0>;
X#[x]= <0,0,0> <2,0,2> | <1,-1,2> | <3,3,0>;
X#[y]= <0,0,0> <3,3,0> | <1,-1,2> <2,0,2>;
X#[tx]= <0,0,0> | <1,-1,2> | <2,0,2> | <3,3,0>;
X#[xy]= <0,0,0> | <1,-1,2> | <2,0,2> | <3,3,0>;
X#[ty]= <0,0,0> | <1,-1,2> | <2,0,2> | <3,3,0>;
X#R10= <0,0,0> | <1,-1,2> | <2,0,2> | <3,3,0>;*/




--x ^ y < z <- x < (z /> y).

--?x ^ y ^ (z /< x) < z.


--x ^ (y <OR> z) < x ^ (y v z).
--(x ^ y) v (x ^ z) < x ^ (y v z).
--(x ^ y) <OR> (x ^ z) < (x ^ y) v (x ^ z).

--(x v y) ^ (x v z) > x v (y ^ z).
--(x <OR> y) ^ (x <OR> z) = x <OR> (y ^ z).

--(x <and> y) <OR> (x <and> z) > x <and> (y <OR> z).
--(x <OR> y) <and> (x <OR> z) < x <OR> (y <and> z).
--(x ^ (y v z)) < (x <OR> (y <and> z)).
--(x ^ y) v (x ^ z) < (x <OR> y) <and> (x <OR> z).

--(x ^ (y v z)) < ((x <OR> y) <and> (x <OR> z)).

--x <and> y > x @^ y.

--(x <OR> y) @^ (x <OR> z) > (x ^ y) v (x ^ z).



/*
((x ^ y) v (x ^ z)) v y = ((z ^ x) v y).
((x <OR> y) <and> (x <OR> z)) v y = (((R11 v z) ^ x) v y).
(x v (y ^ z)) v y = (y v x).
(x v (y ^ z)) ^ y = (((z ^ y) v x) ^ y).
((x <OR> y) <and> (x <OR> z)) ^ y = (((y <OR> x) <and> z) <OR> x) ^ y.
((x <OR> y) <and> (x <OR> z)) ^ y > (x ^ y) v (x ^ z).
doublechecked: (((R11 v x) ^ y) ^ z) v (x^y) = ((x <OR> y) <and> (x <OR> z)) ^ y.
expected implication the other way: ((R11 v x) ^ y) ^ z = x ^ y ^ z <- (x <OR> y) <and> (x <OR> z) = (x ^ y) v (x ^ z).
(x <OR> (y <and> z)) ^ (z v y) = (((z <and> y) <OR> x) ^ (z v y)).
(x <OR> (y <and> z)) v (z v y) = ((R11 ^ x) v y) v z.
(x v y)^(y v z) > y ^ ((x<OR>y)<and>(x<OR>z)).
(x v y)^(y v z) < (x v y) ^ (((R11 ^ x) v y) v z).
(x v y)^(y v z) ^ (y v ((x <OR> y) <and> (x <OR> z))) = (((z v y) ^ x) v y).
*/


/*
(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((((z v y) ^ y') ^ x)' v (z ^ x))'.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((((z v y) ^ z') ^ x)' v (y ^ x))'.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((((z v y) ^ x) ^ y')' v (z ^ x))'.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((((z v y) ^ x) ^ z')' v (y ^ x))'.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = (((y' ^ x) ^ (z v y))' v (z ^ x))'.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = (((z' ^ x) ^ (z v y))' v (y ^ x))'.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((z ^ x) v (y ^ x))' ^ (z v y) ^ x.

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((y' ^ x)' v (z ^ x))' ^ (z v y).

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((z' ^ x)' v (y ^ x))' ^ (z v y).

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = (((((z ^ x) v (y ^ x)))' ^ x) ^ (z v y)).

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((((z ^ x) v (y ^ x)))' ^ ((z v y) ^ x)).

(x ^ (y v z)) ^ ((x ^ y) v (x ^ z))' = ((((z ^ x) v (y ^ x)) v (((z v y) ^ x))'))'.


x ^ (y v z) = (x ^ (z v (R00 ^ y))) v (x ^ (y v (R00 ^ z))).

(R00 ^ (x ^ (y v z))) v (y ^ z) = ((R00 ^ (x ^ y)) v z) ^ ((R00 ^ (x ^ z)) v y).
(x ^ y) v (x ^ z) =  x ^ ( ((x v z) ^ y) v ((x v y) ^ z) ).
y v z > y <OR> z. --x ^ (y v z) < x ^ (y <OR> z).
x v y > x <OR> y.
x ^ y < x <and> y.
x <OR> y > x ^ y.
x v y > x <and> y.
(x ^ y) v (x ^ z) > x ^ (y <OR> z).



(y ^ R00) v ((y ^ x) v (y ^ x')) = y.
R11^((y ^ x) v (y ^ x')) = R11^y.

y v z = (y <OR> z) v ((y v z) ^ R00).

--R00 < (x v y v z) ^ ((x v y) <OR> (x v z))'.-- -> x ^ (y v z) = (x ^ y) v (x ^ z).
(x v y v z) = ((x v y) <OR> (x v z))' -> x ^ (y v z) = (x ^ y) v (x ^ z).

R00 ^ (x v y) = R00 ^ (x v z) -> R00 > (x v y v z) ^ ((x v y) <OR> (x v z))'.

x ^ (y v z) = (x ^ y) v (x ^ z) v ((x ^ (y v z)) ^ ((x ^ y) v (x ^ z))').

--(x ^ y) v (x ^ z) = (x ^ (y <OR> z)) v (((x ^ y) v (x ^ z)) ^ R00).


x <and> (x <and> y) = x <and> y.

--false:x <and> (y <and> z) = y <and> (x <and> z).

x <OR> y = y -> x <and> y = x.

x v R11 = x -> (x ^ y) v ((x v y) ^ R00) = (x^R00) v y.

x <and> y = (x ^ y) v ((x v y) ^ R00).
x <OR> y = (x' ^ y')'.


a v x = b &
a^R00=x^R00 &
(a^x)v R00 = R00 -> x = b ^ a'.



a ^ x = b &
a v R00=x v R00 &
(a v x)^ R00 = R00 -> x = b <and> a`.



((x' ^ y')')` = x` v y`.


x ^ (y` v z`) = (x ^ y`) v (x ^ z`).

x`=y`-> x^R00 = y^R00.

x`=y`-> x v R00 = y v R00.


(x` v y`) ^ (y` v x`)=(x ^ y)` v (x v y)`.


(x'^y) <and> (x v y`) = R00 <-> x > y.
x < y <-> R00 = ((y' ^ x)` ^ x)`.
x = y <-> R00 = ((((y)' <and> x))` v ((x)' ^ y)).

x < y & x <OR> y = y <-> x /< y = R01.


x = (x <OR> y) <and> ( x <OR> <NOT>(<INV>(y)) ).
x = (x ^ y) v ( x ^ <INV>(<NOT>(y)) ). 
-- false: x = (x ^ y) v ( x ^ <NOT>(<INV>(y)) ).




x = y <-> R00 = ((((y)' <and> x))` v ((x)' ^ y)).

(x')` = (x`)' <-> R00 > x | x > R11.

x ^ R00 = R00 -> x = R00 | x = R00'.

(x`)` = x <-> (x')` = (x`)'.

(x ^ x') <and> (x v x`) = R00.

R00'=R11`.



x' <OR> x` = R11.

x ^ (x`)' = R10.
x' v x` = R01. 
(x')` v x = R01.
(x`)' v x = R01.

(x`)` > x.
x < y -> (x`)` < (y`)`. % `` is closure
((x`)`)` = (x)`.        % (x``)``=x``

x v R00 = R01 & y v R00 = R01 -> y`<and>x = (y^R00)` v x.
(x`<and>y)^(y`<and>x) = (x v (y^R00)`) ^ (y v (x^R00)`).


x ^ (y v z) > (x ^ y) v (x ^ z).


(y')` = (y`)' v (y` ^ (y')`).
(y`)' = (y')` ^ (y` ^ (y')`)'.
(y`)' = (y')` ^ ((y`)' v ((y')`)').

x<OR>y = (x ^ R11) v (y ^ (x v R11)).
x = ( x ^ ( x v R00 ) ) ^ ( x v y ).
x = x v ( R00 ^ ( R11 ^ ( y ) ` ) ).
(((x)`)')` = ( x ) ' ^ ( ( x v R00 ) ) '.
(x`)` = (x v R11) ^ (x v R00). 

x <OR> (x <and> y) > x.
x <OR> (x ^ y) < x.
(x <OR> (x ^ y))^R11 = x^R11.
(x <OR> (x ^ y))v(x)` =  (x)` v (x ^ y).

x<OR>y = (R11 ^ y) v (x ^ y) v (x ^ R11).
x<and>y = (x ^ R00) v (x ^ y) v (R00 ^ y).

x<OR>y = (R00 ^ (x ^ y)) v (R11 ^ y) v (x ^ R11).
x<and>y = (x ^ y ^ R11) v ((x v y) ^ R00).


--false:x<and>y = (x' v y')' v (x` ^ y`)`.
x<OR>y = (x' ^ y')' ^ (x` v y`)`.
x<OR>y = (x' ^ y')' ^ (x v y).
x<and>y = (x' v y')' v (x ^ y).


x = (x ^ (y')`) v (x ^ y).
x` ^ y`    = (x <and> y)` <OR> (x` <and> y`).
(x` ^ y`)` = (x <and> y) v (x` <and> y`)`.
(x' ^ y')' = (x <OR> y) <OR> (x' <OR> y')'.




%false: x /^ y = (x <and> y`) ^ (x` <and> y).
x /^ y = (x ^ y) <and> (x` <OR> y`).
x /^ y = (x ^ y) <and> (x` ^ y`).
x /^ y = (x ^ y) <and> (x v y)`.
x /^ y = (y @^ x)` v (y ^ x). 
x /^ y = (R00 ^ (y v x))` v (y ^ x).
x /< y = ( (x v y)` <and> (x ^ y') )'.
x /< y = ( (y' @^ x)` v (y' ^ x) )'.
x /= y = (x /< y) ^ (x /> y).
x /= y = ( (x v y)` <and> ((x'^ y) v (x ^ y')) )'.

x = y <-> x /= y = R01.
x = y ^ R01 <-> x /= y = R01.
(x' /^ y)' = R01 <-> x /> y = R01.

(x /0 y) = (x /^ y)'.

x = y' <-> (x /! y)^(x /0 y) = R01.

x /0 y = ( (x v y)` <and> (x ^ y) )'.

x /! y = x' /0 y'.

x /! y = ( (x' v y')` <and> (x' ^ y') )'.

x /0 y = y /0 x.

x /! y = y /! x.

x /< y = y /> x.

(y)' /0 x = x /< y.

[_reflexivity_];
x < y & x = r v (x^R00) & y = r v (y^R00) ->  
(r <and> (x ^ y)) /1 (x` v y) = x.
[_augmentation_];
x = r v (x^R00) & y = r v (y^R00) & z = r v (z^R00) &  
(r <and> (x ^ y)) /1 (x` v y) = x ->
(r <and> (x^z^y)) /1 ((x^z)` v (y^z) ) = (x^z)<and>r.
[_transitivity_];
x = r v (x^R00) & y = r v (y^R00) & z = r v (z^R00) &
(r <and> (x ^ y)) /1 (x` v y) = x &
(r <and> (y ^ z)) /1 (y` v z) = y ->
(r <and> (x ^ z)) /1 (x` v z) = x.

-- FD example:  
r = [p  q]
     0  a
   --0  c
     1  a
     2  b
;
x = r v [p]; 
y = r v [q];


--x = r v (x^R00) & y = r v (y^R00) &
r < x & r < y &
(r <and> (x ^ y)) /1 (x` v y) = x -> r = (r <and> (x ^ y)) ^ (r <and> (x ^ y`)).

r = [p  q  r  s  t]
;
y = [p]
     1
;
x = [p]
     1
;
(r <and> (x ^ y));
(x` v y);
(r <and> (x ^ y)) /1 (x` v y);

--r < x & r < y ->
--(r <and> (x ^ y)) /1 (x` v y) = x.



(R00 ^ (x v y) = R00 ^ (x v z)) ->
x ^ (y <and> z) = (x ^ y) <and> (x ^ z).

x <and> (x <OR> y) = x.
x <OR> (x <and> y) > x.
x ^ (x <and> y) < x.
x < R00 | y < R00 ->
x <OR> (x <and> y) = x.
x < R00 | R11 < y ->
x ^ (x <and> y) = x.


x = [p  q]
     1  a
     1  b
     2  a
;
(x v [p]) /1 x;

x^[] < [p] ->
(x v [p]) /1 x = (x v [p]) /^ x |
(x v [p]) /1 x = (x v [p]) /= x |
(x v [p]) /1 x = (x v R10) v ([p])` |
(x v [p]) /1 x = (x ^ R00) v ([p])` |
(x v [p]) /1 x = (x v ([p])`)'.


dx = [p  q]
      1  a
      --2  a
;
dy = [p]
      --2
;
z = [p]
;
x = [p  q]
     1  a
     1  b
     2  a
;

z != []          &
z ^ [] = z       &       -- z is an empty relation (aka header)
--x v z = y      &       -- View def: y is projection of x onto z
x ^ [] = dx ^ [] &       -- x and dx have the same header
(x v z) ^ [] = dy ^ [] & -- y and dy have the same header
z > x ^ []       &       -- Atributes of z are contained in attributes of x
dx < x &                 -- decrement relation containment
dy < x v z &             -- ditto
(x ^ dx') v z = (x v z) ^ dy'  -- View definition after update
-> dx = dy ^ x. 


y /^ z = (y v z)` <and> (y ^ z).
y /^ z = ((R00 ^ z)` ^ y`) v (y ^ z).

(x v y v z)^R00=R00 ->   
x ^ (y /^ z) = (x ^ (R00 ^ z)` ^ y`) v (x ^ y ^ z).

--[] < x v y v z -> x /^ (y /^ z) = (x /^ y) /^ z.

--R00 ^ (x v y) = R00 ^ (x v z) <-> [] < x v y v z` & [] < x v z v y`. -- SDC criteria

--R00 ^ (x v y) = R00 ^ (x v z) -> x /^ (y v z) = (x /^ y) v (x /^ z).

--R00 ^ (x v y) = R00 ^ (y v z) -> x <and> (y <and> z) = (x <and> y) <and> z.

--[] < x v y` -> x <OR> (x <and> y) = x.

[] < x v y v z ->   
x /^ (y /^ z) = (R00 ^ ((x`^ y`) v (y`^ z`) v (x`^ z`)) ) v (x ^ y ^ z).


--R00 ^ (x v y) = R00 ^ (x v z) -> x ^ (y v z) = (x ^ y) v (x ^ z).

(x/=y = z) <-> (((x /> y) = z) & ((x /< y) = z)).

[] < (x v y v s) ^ (x v y v t) ^ (x v s v t) ^ (s v y v t) & 
(x /^ s) /^ s = x & (y /^ t) /^ t = x ->
((x /^ s) ^ (y /^ t)) = 
((x ^ y) /^ (s /^ t)) ^ ((x /^ y) /^ (s ^ t)) ^
((x ^ t) /^ (s /^ y)) ^ ((x /^ t) /^ (s ^ y))
.

--[] < (x v y v s) ^ (x v y v t) ^ (x v s v t) ^ (s v y v t) ->
--(x /^ s) ^ (y /^ t) < (x /^ y) /^ (s ^ t).

[] < ((x /^ s) v (y /^ t) v s) ^ ((x /^ s) v (y /^ t) v t) ^ ((x /^ s) v s v t) ^ (s v (y /^ t) v t)  ->
x ^ y < ((x /^ s) /^ (y /^ t)) /^ (s ^ t).

-- independence of "non-informative" attribute(s) "s" 
(x ^ s) ^ y = (x ^ y) ^ s.

s v R11 = s 
->   
(x ^ s) <OR> y = (x <OR> y) ^ s.

s v R11 = s 
& s v y = R01
->
(x ^ s) v y = x v y
. 

s v R11 = s 
& s v y = R01
->
(x ^ s) <and> y = x <and> y
.  

s v R11 = s 
& s v y = R01
->
(x ^ s) /^ y = (x /^ y) ^ s
. 

s v R11 = s 
& s v y = R01
->
(x ^ s) /= y = (x /= y) ^ s
.

["------1------"];
y > x -> (r^x)^y > (r^x)^y'.
["------2------"];
(r^x)^y > (r^x)^y' & (r^y)^z > (r^y)^z' -> (r^x)^z > (r^x)^z'.  
["------3------"]; 
(r^x)^y > (r^x)^y' -> (r^x^z)^(y^z) > (r^x^z)^(y^z)'.

--(r /^ y) /^ r < y
--& (r /^ y) /^ y = r
--& (r` /^ y) /^ y = r`
--& (r' /^ y) /^ r' = r'
(r^x)^y > (r^x)^y'
<-
r#x < r#y
.

r v x > s v x &
s v y > t v y &
x > y -> 
r v x > t v x.


r#x < r#y & r#s < r#t
-> r#(x^s) < r#(y^t).

*/

/*
z = [p  q]
     1  a
     1  b
     2  a
;
y = [p]
     1
;
y <OR> z;
y <and> z = y.
- (y <OR> z = z).
- (y <and> z = z).
- (y <OR> z = y).
*/
--(x ^ y) v (x ^ (y`)') = (y` ^ y')' <and> x.
--x = y <-> R00 = (((y ^ x)' ^ (y v x))` ^ (y v x))`.

--false:x /^ y < z <-> x < y /= z.

/* simplest Armstrong-like dependency
x < y -> R00^x < R00^y.
R00^x < R00^y -> R00^(x^z) < R00^(y^z).
R00^x < R00^y & R00^y < R00^z -> R00^x < R00^z. 
*/

/* 
(x^[]=[p q] &  
y^[]=[q r] & 
z^[]=[p r]) 
->(x /^ y < z <-> x < y /< z)
.*/

/* in partial order via /< set intersection join is galois connected with set containment join 
(losely speaking relational division being inverse of cartesian product)

left-adjoint:
c < a /< b 
-> 
c ^ a < b.

x /< x = R01.
x /< y = R01 & y /< x = R01 -> y=x.
x /< y = R01 & y /< z = R01 -> x /< z = R01.
(x /^ y) /< z = R01 <-> x /< (y /< z) = R01.
false: (x /^ y) /< z = x /< (y /< z).
Right cancellation:
((y /< z) /^ y) /< z = R01.
Upper-adjoint distribution:
x /< (z v y) = (x /< z) v (x /< y).
*/

/*
E=[p]
   2
; 

x = [t] (A) (B) (C) (D) (E);
lex = ((x /^ [t=lft]) ^ (x /^ [t=rgt])) ^ LE;
--lex;
--(x /^ [t=lft]) /=  lex;

--lex = ((x /^ [t=lft]) ^ (x /^ [t=rgt])) ^ LE;
--lex;
GE = LE' v [lft=rgt];
gex = ((x /^ [t=lft]) ^ (x /^ [t=rgt])) ^ GE;
(x /^ [t=rgt]) /=  gex; -- or /<
--plux = ((x /^ [t=x]) ^ (x /^ [t=y])) ^ Plus;
--plux;
--(plux /^ [t=x]) /= x;
*/






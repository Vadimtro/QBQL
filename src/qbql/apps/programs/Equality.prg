/*
V ~ W.
V ~ W1.

x ~ x.
x ~ y -> y ~ x.
x ~ y & y ~ z -> x ~ z.
x ~ y & x ^ R00 = y ^ R00 -> x = y.

(x v y) ^ R00 = (x v z) ^ R00 & y ~ z -> x ^ y ~ x ^ z.
(x v y) ^ R00 = (x v z) ^ R00 & y ~ z -> x v y ~ x v z.

x ^ R00 ~ R00.
x != R01 -> - (x ~ R01). 

[p  q]
 0  a
 1  b
^~
[p  q]
 0  a
 0  b
 1  a
 1  b
 1  c
;
*/

x^[]=[p q r] ->
(x \|/ [p=s]) ^ [q=t] = x \|/ ([p=s] \|/ [q=t]). 

x <~ y <-> y >~ x.
x <~ x.
x <~ y & y <~ x -> x ~ y.
 
z = R01;
y = R11 v [p q];
x = [p]
     1
;
x <~ y & y <~ z -> x <~ z.

/*
y=SUV;
x=R11;
y ^~ x;
x ^~ y;

[_idempotence_];
x ~ y -> x ^~ y ~ x.
[_symmetry_];
x ^~ y ~ y ^~ x.
[_associativity_];
(x ^~ y) ^~ z  ~  x ^~ (y ^~ z).
[_congruence_];
x ~ y -> x ^~ z ~ y ^~ z.
*/

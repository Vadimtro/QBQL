/*
        //String goal = "(x ^ (y v z)) /< ((x ^ y) v (x ^ z)) = expr.";
        //String goal = "0 ^ (x v z) = 0 -> x /^ (y /^ z) = (x /^ y) /^ z.";
        
        y <and> z = y <-> F(y,z). 
        ***Found: 
        y <and> z = y <-> (((R00 ^ (z v y)) v (z ^ y)) = y).
        Elapsed=101427
        evalTime=24971
        y <and> z = y <-> (((R11 ^ z) v (R00 ^ y)) = (z v y)).
        Elapsed=137405
        evalTime=32051
        y <and> z = y <-> (((R11 ^ z) v y) = ((R00 ^ y) v z)).
        Elapsed=147369
        evalTime=33376
        y <and> z = y <-> ((<NOT>(y) v z) = (R11 v y)).
        Elapsed=8338
        evalTime=7054
        Elapsed=6885 with nextOp optimization
        evalTime=6505

*/


--x <op> y = expr. 
--(x <OR> y) < z <-> x < (y <op> z).
--(x <OR> y) /< z = R01 <-> x /< (y <op> z) = R01.

--x ^ (y ^ <NOT> x) = expr.

--(x /= y)  = expr.
--(x /< y)  = expr.
--(x /< y)  = <NOT>(((<NOT>(y) ^ x) v <INV>(((R00 ^ x) <"and"> y)))).
--(x /< y)  = <NOT>(((<NOT>(y) ^ x) v <INV>((R00 ^ (y v x))))). 
--(x /< y)  = <NOT>(((<NOT>(y) ^ x) <and> <INV>((y v x)))).


--(x /! x) ^ x = expr.  -- = (<NOT>((<INV>((<NOT>(x) ^ <INV>(x))) ^ x)) ^ x).
--(x /< x) <and> y= expr.  -- = <INV>(y) v y
--((R11 v R00) ^ x) /*< y*/ = expr.
--<NOT>(R00 v <NOT>x) ^ x = expr.


/*!(FD(x,y) <-> <NOT>(R00 v <NOT>x) ^ x < y). 
!(FD(x,y) <-> <NOT>y < R00 v <NOT>x).
!(FD(x,y) <-> x^<NOT>y < y). -- <-> x^<NOT>y < x^y. <-> <INV>(x) ^ x < y.
!(FD(x,y) <-> R00^x<y).
!(FD(x,y) <-> x<y).
!(FD(x,y)).-- <-> x=x
(x < y -> FD(x,y)).
(FD(x,y) & FD(y,z) -> FD(x,z)).   
(FD(x,y) -> FD(x^z,y^z)).*/


/*!(FD(r,x,y) <-> ((r /1 r) ^ x) < y).
!(FD(r,x,y) <-> ((R00 v r) ^ x) < y).
!(FD(r,x,y) <-> <NOT>(R00 v <NOT>x) ^ x < y). 
!(FD(r,x,y) <-> <NOT>y < R00 v <NOT>x).
!(FD(r,x,y) <-> x^<NOT>y < y). -- <-> x^<NOT>y < x^y. <-> <INV>(x) ^ x < y.
!(FD(r,x,y) <-> R00^x^r < y).
!(FD(r,x,y) <-> x^<NOT>r < y).
!(FD(r,x,y) <-> x^<INV>r < y).
!(FD(r,x,y) <-> x^r < y).
!(FD(r,x,y) <-> R00^x<y).
!(FD(r,x,y) <-> x<y).
!(FD(r,x,y)).-- <-> x=x
!(FD(r,x,y) -> R00^x<y | R00^x<y).
(x < y -> FD(r,x,y)).
(FD(r,x,y) & FD(r,y,z) -> FD(r,x,z)).   
(FD(r,x,y) -> FD(r,x^z,y^z)).

(r v x) ^ (r v y) < r  <-> F(x,y,r).*/

--((r v (x ^ y))⊗(r v (x ^ y)))^(r v x) < r v (x ^ y) <-> F(x,y,r).

--((r v (x ^ y))⊗(r v (x ^ y)))^(r v x) = expr.

--(x ^ (y v z)) = ((x ^ y) v (x ^ z)) v expr.


/*
x <plus> y = expr. 
x <plus> R00 = x.
x <plus> y = y <plus> x.
x <plus> (y <plus> z) = (x <plus> y) <plus> z.
x <mult> (y <plus> z) = (x <mult> y) <plus> (x <mult> z).
*/

/*x <mult> y = expr. 
-- <mult> != ^.
x <mult> R11 = x.
--x <mult> R00 = R00.

x <mult> y = y <mult> x.
x <mult> (y <mult> z) = (x <mult> y) <mult> z.

x <mult> (y v z) = (x <mult> y) v (x <mult> z).
*/

--F^R00 = x^y 
-->
(FD(F,x,y) <-> F#x < F#y).

--(((F v (x^y^R00)) < (((x /^ (F v (x^y^R00))) v (F v (x^y^R00))) /1 (F v (x^y^R00))))) <-> FD(F,x,y).

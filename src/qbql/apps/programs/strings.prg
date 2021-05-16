

Emp = [EMPNO ENAME JOB MGR SAL DEPTNO]
 7369 SMITH  CLERK 7902 800 20 
 7499 ALLEN SALESMAN 7698 1600 30 
 7521 WARD SALESMAN 7698 1250 30 
 7566 JONES MANAGER 7839 2975  20 
 7654 MARTIN SALESMAN 7698 1250 30 
 7698 BLAKE MANAGER 7839 2850  30 
 7782 CLARK MANAGER 7839 2450  10 
 7788 SCOTT ANALYST 7566 3000  20 
 7839 KING PRESIDENT 7839 5000  10 
 7844 TURNER SALESMAN 7698 1500 30 
 7876 ADAMS CLERK 7788 1100  20 
 7900 JAMES CLERK 7698 950  30 
 7902 FORD ANALYST 7566 3000  20 
 7934 MILLER CLERK 7782 1300  10 
;
Dept = [DEPTNO DNAME LOC]
10 ACCOUNTING LONDON
20 SALES PARIS
30 RESEARCH ROME
40 OPERATIONS LONDON
;
Dept;

((Emp v [DEPTNO SAL]) /^ [SAL=summands]) /= Sum = [DEPTNO  result]
                                          10  8750
                                          20  7875
                                          30  8150
.
MySum = Sum /^ [result=sum];

((Emp v [DEPTNO SAL]) /^ [SAL=summands]) /= MySum = [DEPTNO  sum]
                                            10  8750
                                            20  7875
                                            30  8150
.
(Emp v [ENAME SAL]) /^ [SAL=x] /^ [y]10000 /^Plus=[ENAME  z]
                                           ADAMS  11100
                                           ALLEN  11600
                                           BLAKE  12850
                                           CLARK  12450
                                           FORD  13000
                                           JAMES  10950
                                           JONES  12975
                                           KING  15000
                                           MARTIN  11250
                                           MILLER  11300
                                           SCOTT  13000
                                           SMITH  10800
                                           TURNER  11500
                                           WARD  11250
.
(Emp v [ENAME SAL]) ^ [SAL=lft] /^ [rgt]1000 /^ LE=[ENAME  SAL]
                                           JAMES  950
                                           SMITH  800
.

--R = [x] 1 -3 0 ^ [x=rgt] ^ [lft]0 /^ LE ^ [x=abs]; 
--R;
--R = [x] 1 -3 0 ^ [z]0 ^ Plus ^ [y=rgt] ^ [lft]0 /^ LE /^ [y=abs]; 
--R;

Positive = LE /^ [lft]0 /^ [rgt=x];
Negative = LE /^ [rgt]0 /^ [lft=x];

Negation = Plus /^ [z]0;

Abs = (Positive ^ [x=abs]) v (Negation ^ Negative /^ [y=abs]);
[x] 1 -3 0 ^ Abs=[abs  x]
             0  0
             1  1
             3  -3
.

Incr = Plus /^ [y]1;
[x]4 /^ Incr /^ [z=x] /^ Incr /^ [z=x];

A=[i j1 j2]
   1 3 1
   2 1 0
;

(A /^ [j1=s] /^ [j]1)
v
(A /^ [j2=s] /^ [j]2)
;

A=[i j s]
   1 1 3
   1 2 1
   2 1 1
   2 2 0
;

(A /^ [j1=s] /^ [j]1)
^
(A /^ [j2=s] /^ [j]2)
; 

B=[i j s]
   1 1 4
   1 2 1
   2 1 1
   2 2 0
;
(A/^[j=k]/^[s=x]) /^ (B/^[i=k]/^[s=y]) /^ Multiply /^[z=summands]  /= Sum /^[result=s];  


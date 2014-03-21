#!/bin/bash

set -e

function run_test {
    TEST=$1
    ARG1=$2
    ARG2=$3
    rm -rf $TEST
    sbt "run-main scalapipe.test.$TEST $ARG1 $ARG2"
    cd $TEST
    make
    if [ $ARG1 -gt 0 ] ; then
        make sim | grep OUTPUT > ../test.out
    else
        ./proc_localhost | grep OUTPUT > ../test.out
    fi
    cd ..
    cmp test.out test.expected
    rm -rf $TEST
}

# Run unit tests.
sbt test

# Test configuration parameters.
echo "OUTPUT 0"     >  test.expected
echo "OUTPUT 1"     >> test.expected
echo "OUTPUT 2"     >> test.expected
echo "OUTPUT 3"     >> test.expected
echo "OUTPUT 4"     >> test.expected
echo "OUTPUT 5"     >> test.expected
echo "OUTPUT 6"     >> test.expected
echo "OUTPUT 7"     >> test.expected
echo "OUTPUT 8"     >> test.expected
echo "OUTPUT 9"     >> test.expected
rm -rf ConfigTest
sbt "run-main scalapipe.test.ConfigTest"
cd ConfigTest
make
./proc_localhost -mc 10 > ../test.out
cd ..
cmp test.out test.expected
rm -rf ConfigTest


# Test socket edges.
echo "OUTPUT 0"     >  test.expected
echo "OUTPUT 1"     >> test.expected
echo "OUTPUT 2"     >> test.expected
echo "OUTPUT 3"     >> test.expected
echo "OUTPUT 4"     >> test.expected
echo "OUTPUT 5"     >> test.expected
echo "OUTPUT 6"     >> test.expected
echo "OUTPUT 7"     >> test.expected
echo "OUTPUT 8"     >> test.expected
echo "OUTPUT 9"     >> test.expected
rm -rf SocketTest
sbt "run-main scalapipe.test.SocketTest"
cd SocketTest
make
./proc_localhost & ./proc_127.0.0.1 > ../test.out
cd ..
cmp test.out test.expected
rm -rf SocketTest

# Test cycles.
echo "OUTPUT 1"     >  test.expected
echo "OUTPUT 2"     >> test.expected
echo "OUTPUT 3"     >> test.expected
echo "OUTPUT 4"     >> test.expected
echo "OUTPUT 5"     >> test.expected
echo "OUTPUT 6"     >> test.expected
echo "OUTPUT 7"     >> test.expected
echo "OUTPUT 8"     >> test.expected
echo "OUTPUT 9"     >> test.expected
run_test CycleTest 0
run_test CycleTest 1

# Test control structures.
echo "OUTPUT 5" > test.expected
run_test ControlTest 0
run_test ControlTest 1

# Test arrays.
echo "OUTPUT 0: 0 1 2 3 4 5 6 7 "           >  test.expected
echo "OUTPUT 1: 8 9 10 11 12 13 14 15 "     >> test.expected
echo "OUTPUT 2: 16 17 18 19 20 21 22 23 "       >> test.expected
echo "OUTPUT 3: 24 25 26 27 28 29 30 31 "       >> test.expected
echo "OUTPUT 4: 32 33 34 35 36 37 38 39 "       >> test.expected
echo "OUTPUT 5: 40 41 42 43 44 45 46 47 "       >> test.expected
echo "OUTPUT 6: 48 49 50 51 52 53 54 55 "       >> test.expected
echo "OUTPUT 7: 56 57 58 59 60 61 62 63 "       >> test.expected
echo "OUTPUT 8: 64 65 66 67 68 69 70 71 "       >> test.expected
echo "OUTPUT 9: 72 73 74 75 76 77 78 79 "       >> test.expected
run_test ArrayTest 0
run_test ArrayTest 1

echo "OUTPUT 0"         >  test.expected
echo "OUTPUT 2"         >> test.expected
echo "OUTPUT 7"         >> test.expected
echo "OUTPUT 7"         >> test.expected
echo "OUTPUT 12"        >> test.expected
echo "OUTPUT 10"        >> test.expected
echo "OUTPUT 11"        >> test.expected
echo "OUTPUT 15"        >> test.expected
echo "OUTPUT 24"        >> test.expected
echo "OUTPUT 18"        >> test.expected
run_test ArrayTest2 0 0
run_test ArrayTest2 0 1
run_test ArrayTest2 0 2
run_test ArrayTest2 0 3
run_test ArrayTest2 1 0
run_test ArrayTest2 1 1
run_test ArrayTest2 1 2
run_test ArrayTest2 1 3

echo "OUTPUT 4950" > test.expected
run_test ArrayTest3 0
run_test ArrayTest3 1

# Test unions.
echo "OUTPUT 5 4" > test.expected
run_test UnionTest 0
run_test UnionTest 1

# Test structures.
echo "OUTPUT 0"     >  test.expected
echo "OUTPUT 1"     >> test.expected
echo "OUTPUT 2"     >> test.expected
echo "OUTPUT 3"     >> test.expected
echo "OUTPUT 4"     >> test.expected
echo "OUTPUT 5"     >> test.expected
echo "OUTPUT 6"     >> test.expected
echo "OUTPUT 7"     >> test.expected
echo "OUTPUT 8"     >> test.expected
echo "OUTPUT 9"     >> test.expected
run_test StructTest 0
run_test StructTest 1

# Test functions.
echo "OUTPUT 1"     >  test.expected
echo "OUTPUT 3"     >> test.expected
echo "OUTPUT 5"     >> test.expected
echo "OUTPUT 7"     >> test.expected
echo "OUTPUT 9"     >> test.expected
echo "OUTPUT 11"    >> test.expected
echo "OUTPUT 13"    >> test.expected
echo "OUTPUT 15"    >> test.expected
echo "OUTPUT 17"    >> test.expected
echo "OUTPUT 19"    >> test.expected
run_test FunctionTest 0
run_test FunctionTest 1
run_test FunctionTest 2
run_test FunctionTest 3

# Clean up.
rm test.expected

echo
echo "Success!"
echo

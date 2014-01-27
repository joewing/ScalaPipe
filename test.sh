#!/bin/bash

set -e

function run_test {
    TEST=$1
    ARG=$2
    rm -rf $TEST
    sbt "run-main autopipe.test.$TEST $ARG"
    cd $TEST
    make
    if [ $ARG -gt 0 ] ; then
        make sim | grep OUTPUT > test.out
    else
        ./proc_localhost | grep OUTPUT > ../test.out
    fi
    cd ..
    cmp test.out test.expected
    rm -rf $TEST
}

# Run unit tests.
sbt test

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


#!/bin/bash

set -e

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

function run_test {
    rm -rf FunctionTest
    sbt "run-main autopipe.test.FunctionTest $1"
    cd FunctionTest
    make
    if [ $1 -gt 0 ] ; then
        make sim | grep OUTPUT > test.out
    else
        ./proc_localhost | grep OUTPUT > ../test.out
    fi
    cd ..
    cmp test.out test.expected
    rm -rf FunctionTest
}

sbt test
run_test 0
run_test 1
run_test 2
run_test 3


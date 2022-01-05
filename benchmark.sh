#!/usr/bin/env bash

BASEDIR=$(dirname $0)
cd $BASEDIR

sbt ";++ 2.13.4 benchmark/clean ;++ 2.13.4 benchmark/stage" && \
    benchmark/target/universal/stage/bin/benchmark generate-sample && \
    echo "##############################################################" && \
    time benchmark/target/universal/stage/bin/benchmark circe -r 5 && \
    echo && \
    echo "##############################################################" && \
    time benchmark/target/universal/stage/bin/benchmark ghyll -r 5 
echo

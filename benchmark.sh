#!/usr/bin/env bash

BASEDIR=$(dirname $0)
cd $BASEDIR

sbt benchmark/stage

benchmark/target/universal/stage/bin/benchmark generate-sample
echo "##############################################################"
time benchmark/target/universal/stage/bin/benchmark circe -r 5
echo
echo "##############################################################"
time benchmark/target/universal/stage/bin/benchmark json-stream -r 5
echo
echo "##############################################################"
time benchmark/target/universal/stage/bin/benchmark json-stream2 -r 5
echo

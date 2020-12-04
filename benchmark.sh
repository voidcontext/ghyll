#!/usr/bin/env bash

BASEDIR=$(dirname $0)
cd $BASEDIR

sbt benchmark/stage

benchmark/target/universal/stage/bin/benchmark generate-sample
time benchmark/target/universal/stage/bin/benchmark circe -r 5
time benchmark/target/universal/stage/bin/benchmark json-stream -r 5

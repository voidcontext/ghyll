# json-stream [![Build Status](https://travis-ci.org/voidcontext/json-stream.svg?branch=main)](https://travis-ci.org/voidcontext/json-stream)
A library to process large JSON objects as (key, value) stream

## Problem Statement

In some cases datasets are not nicely organised into an array that's
easy to parse in a streaming fashion with existing libraries (e.g.
[circe-fs2](https://github.com/circe/circe-fs2)). When the dataset is
represented as a huge map/object/hash where keys are e.g. identifiers
it's not feasible to read the whole JSON into the memory and/or build
a full AST representing the data.

The only viable solution I found is [gson's mixed
streaming](https://sites.google.com/site/gson/streaming) but this is
pure Java, so I decided to build an idiomatic Scala wrapper based on
this idea.

## Overview

This library provides a way to parse and process large json objects,
where there are many keys and the payload under each key is smaller.

An example, that triggered this experiment. Parsing a 600MB JSON with
around 50K keys was impossible with circe even with 6GB of heap. The
first iteration of the code was able to parse the same JSON while max
memory usage was around 180MB.

The base idea is to start streaming the JSON tokens using once we
reach a key value pair that needs to be decoded the JSON is read and
converted to circe's JSON AST and then using that we can decode it
into the Scala representation. (This could be further optimised, but
it worked well for the POC).

### StreamingDecoder2

This experiment is trying to replace circe, with a bespoke,
semi-automatically derived decoder. I'd consider this POC succesful as
the memory usage is the same as in case of V1, but according to the
benchmarks it's even faster (this comparison might not be fair as this
decoder has only minimal number of features, just enough to run tests
and benchmarks). This version should really shine when there are big
parts of the JSON that could be discarded.

## TODO

[ ] Create benchmark that shows the strength of V2


## Benchmark results

```bash
$ ./benchmark.sh
[info] welcome to sbt 1.3.13 (Azul Systems, Inc. Java 11.0.9.1)

...

parse using circe
Right(496.2931715811804485143)
Current memory usage: 922
Right(496.2931715811804485143)
Current memory usage: 1093
Right(496.2931715811804485143)
Current memory usage: 2109
Right(496.2931715811804485143)
Current memory usage: 1270
Right(496.2931715811804485143)
Current memory usage: 1113
MemoryStats
Average: 1301
Max: 2109

real	0m14.712s
user	1m30.957s
sys	0m2.812s
parse using json-stream
Right(496.2931715811804485143)
Current memory usage: 149
Right(496.2931715811804485143)
Current memory usage: 187
Right(496.2931715811804485143)
Current memory usage: 223
Right(496.2931715811804485143)
Current memory usage: 38
Right(496.2931715811804485143)
Current memory usage: 74
MemoryStats
Average: 134
Max: 223

real	0m9.986s
user	0m11.839s
sys	0m0.471s
parse using json-stream v2
Right(496.2931715811804485143)
Current memory usage: 166
Right(496.2931715811804485143)
Current memory usage: 126
Right(496.2931715811804485143)
Current memory usage: 60
Right(496.2931715811804485143)
Current memory usage: 214
Right(496.2931715811804485143)
Current memory usage: 147
MemoryStats
Average: 142
Max: 214

real	0m6.167s
user	0m7.757s
sys	0m0.401s

```

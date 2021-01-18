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

##############################################################
parse using circe
Right(504.6476855415109732098)
Current memory usage: 894
Right(504.6476855415109732098)
Current memory usage: 1779
Right(504.6476855415109732098)
Current memory usage: 1882
Right(504.6476855415109732098)
Current memory usage: 1315
Right(504.6476855415109732098)
Current memory usage: 1013
MemoryStats
Average: 1376
Max: 1882

real	0m14.604s
user	1m32.937s
sys	0m3.113s

##############################################################
parse using json-stream
Right(504.6476855415109732098)
Current memory usage: 195
Right(504.6476855415109732098)
Current memory usage: 208
Right(504.6476855415109732098)
Current memory usage: 222
Right(504.6476855415109732098)
Current memory usage: 14
Right(504.6476855415109732098)
Current memory usage: 28
MemoryStats
Average: 133
Max: 222

real	0m10.376s
user	0m12.135s
sys	0m0.488s

##############################################################
parse using json-stream v2
Right(504.6476855415109732098)
Current memory usage: 48
Right(504.6476855415109732098)
Current memory usage: 67
Right(504.6476855415109732098)
Current memory usage: 220
Right(504.6476855415109732098)
Current memory usage: 154
Right(504.6476855415109732098)
Current memory usage: 87
MemoryStats
Average: 115
Max: 220

real	0m6.224s
user	0m7.695s
sys	0m0.422s
```

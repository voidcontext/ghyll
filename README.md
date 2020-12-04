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


## Benchmark results

```bash
$ ./benchmark.sh
[info] welcome to sbt 1.3.13 (Azul Systems, Inc. Java 11.0.9.1)

...

parse using circe
Right(517.211378369668927341746)
Current memory usage: 916
Right(517.211378369668927341746)
Current memory usage: 936
Right(517.211378369668927341746)
Current memory usage: 1872
Right(517.211378369668927341746)
Current memory usage: 1389
Right(517.211378369668927341746)
Current memory usage: 1039
MemoryStats
Average: 1230
Max: 1872

real	0m15.010s
user	1m35.808s
sys	0m3.162s
parse using json-stream
Right(517.211378369668927341746)
Current memory usage: 92
Right(517.211378369668927341746)
Current memory usage: 191
Right(517.211378369668927341746)
Current memory usage: 109
Right(517.211378369668927341746)
Current memory usage: 31
Right(517.211378369668927341746)
Current memory usage: 175
MemoryStats
Average: 119
Max: 191

real	0m9.152s
user	0m11.310s
sys	0m0.418s
```

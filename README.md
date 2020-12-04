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

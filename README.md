# ghyll
`ghyll` is a streaming JSON library for Scala

## Overview

In some cases datasets are not nicely organised into an array that's
easy to parse in a streaming fashion with existing libraries (e.g.
[circe-fs2](https://github.com/circe/circe-fs2)). When the dataset is
represented as a huge map/object/hash where keys are e.g. identifiers
it's not feasible to read the whole JSON into the memory and/or build
a full AST representing the data. Another scenario when this approach
can get problematic (or resource intensive) when only a small
subset/subtree of a JSON object needs to be decoded.

`ghyll` provides a way to parse and decode:

- large JSON objects as key-value pairs, where there are many keys and
the payload under each key is smaller
- large JSON arrays
- large JSON objects that only partially need to be decoded

__Note: `ghyll` currently doesn't support encoding data as JSON__

### How it works

Under the hood `ghyll` is using [gson](https://github.com/google/gson)
for parsing the JSON payload, then it's using its own `Decoder`
implementation to turn the JSON tokens emitted by `gson` into Scala
data structures.

## Disclaimer

`ghyll` is under active development and it's in an early stage when
its functionality is limited and the API can change substantially.


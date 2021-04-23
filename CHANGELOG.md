# Changelog

## [Unreleased]

### Added

- it is now possible to decode an object under a given JSON path
- `Decoder` can be fully automatically derived
- decode* shortcuts that can decode from `java.io.File`
- `Encoder` capabilities that match the `Decoder`'s functionality
  (auto derivation, etc)

### Fixed

- fixed decoding when an optional field was `null` in the JSON

## [0.1.0] - 2021-02-01

### Added
- Implement `decodeObject` and `decodeKeyValues` functions
- Implement `Decoder` instances for some scalar types
  - String
  - Int
  - Boolean
  - BigDecimal
  - LocalDate (java.time)
- Implement `Decoder` instances for `Option` and `List`
- Implement semi automatic `Decoder` derivation for case classes

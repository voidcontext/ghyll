# Changelog

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

# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 5.2.3 - 2025-08-05
### Changed
- Support for spring 3.5.4 and spring cloud 25.0.0
- Update dependencies

## 5.2.2 - 2025-01-28
### Changed
- Support for spring 3.4.2 and spring cloud 24.0.0

## 5.0.0 - 2023-06-05
### Changed
- Support for spring 3
- Rename packages caused by hand over to solace community

### Added
_n/a_

### Removed
- Support for spring 2

## 4.0.0 - 2023-05-30
### Changed
_n/a_

### Added
- STTRS-1127: support to send grouped multi response

### Removed
_n/a_

## 3.4.0 - 2023-05-10
### Changed
- update depends

### Added
- add AsyncApi spec to explain protocol
- STTRS-1101: reply with flux

### Removed
_n/a_

## 3.3.0 - 2023-05-04
### Changed
- update depends

### Added
_n/a_

### Removed
_n/a_

## 3.2.0 - 2023-05-03
### Changed
- support more than 64k items in multi responses

### Added
- support to received grouped multi response

### Removed
_n/a_

## 3.1.0 - 2023-05-01
### Changed
- all blocking methods may throw RequestReplyException

### Added
- add support for error responses

### Removed
_n/a_

## 2.10.0 - 2023-04-11
### Changed
- Migrate autoconfig to spring 2.7 (preparation for spring 3.0)

### Added
- STTRS-1066: support request multi reply

### Removed
- Support for spring <= 2.6

## 2.9.0 - 2023-04-11
### Changed
- update depends

### Added
_n/a_

### Removed
_n/a_

## 2.8.0 - 2023-02-24
### Changed
- update depends
- RCSPF-127827: support for custom message encoder for TibRv

### Added
_n/a_

### Removed
_n/a_

## 2.7.0 - 2022-09-13
### Changed
_n/a_

### Added
- RCSPF-119794 + RCSPF-120742 copy original header to response to enable TibRv support

### Removed
_n/a_

## 2.6.0 - 2022-06-14
### Changed
- support spring 2.6.6

### Added
_n/a_

### Removed
_n/a_

## 2.0.0 - 2022-02-17
### Changed
- support spring 2.4.0

### Added
- support static topics
- multi binder support

### Removed
_n/a_

## 1.5.0 - 2021-12-14
### Changed
- fix log4j zero-day-exploit (Log4Shell)

### Added
_n/a_

### Removed
_n/a_

## 1.4.0 - 2021-11-18
### Changed
- TPS-828: Fix dependency loop

### Added
_n/a_

### Removed
_n/a_

## 1.3.0 - 2021-09-20
### Changed
- RCSPF-105261: component scanning request-reply services

### Added
_n/a_

### Removed
_n/a_

## 1.2.0 - 2021-08-31
### Changed
_n/a_

### Added
- RCSPF-99458:: property replacement

### Removed
_n/a_

## 1.1.0 - 2021-07-07
### Changed
- solace depends are optional

### Added
_n/a_

### Removed
_n/a_

## 1.0.0 - 2021-03-19
### Changed
- updated parent

### Added
- initial version according to [RCSPF-92822](https://issues.sbb.ch/browse/RCSPF-92822)

### Removed
_n/a_

<hr/>

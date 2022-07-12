# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Upgraded
- "Keyple Service Library" to version `2.1.0`
- "Keyple Service Resource Library" to version `2.0.2`
- "Keyple Plugin Stub Library" to version `2.1.0`
### Added
- Calypso example #12 for Performance Measurement (embedded validation).


## [2022-06-09]
### Fixed
- Removal of the unused Jacoco plugin for compiling Android applications that had an unwanted side effect when the application was launched (stacktrace with warnings).
### Upgraded
- "Keyple Plugin Android NFC Library" to version `2.0.1`
- "Keyple Plugin Android OMAPI Library" to version `2.0.1`

## [2022-05-30]
### Changed
- Removal of the use of deprecated methods.
### Upgraded
- "Calypsonet Terminal Calypso API" to version `1.2.+`
- "Keyple Card Calypso Library" to version `2.2.0`
- "Keyple Util Library" to version `2.1.0`
### Added
- Calypso example #11 for Data signing (issue [#15]).

## [2022-04-06]
### Added
- PC/SC example #4 for "Transmit Control" mechanism (newly available since PcscPlugin 2.1.0).

## [2022-02-02]
### Added
- Calypso example #10 for Session Trace TN313 (issue [#9]).
### Upgraded
- "Calypsonet Terminal Calypso API" to version `1.1.+`
- "Keyple Card Calypso Library" to version `2.1.0`

## [2021-12-20]
### Upgraded
- "Keyple Card Calypso Library" to version `2.0.3`
- "Keyple Card Generic Library" to version `2.0.2`

## [2021-12-08]
### Upgraded
- "Keyple Service Library" to version `2.0.1`

## [2021-11-22]
### Added
- Log of the Calypso serial number in the examples of Example_Card_Calypso (issue [#5]).
- Main_ExplicitSelectionAid_Stub in Example_Card_Calypso (issue [#7]).
- Main_ScheduledSelection_Stub in Example_Card_Calypso (issue [#7]).
- Main_CardAuthentication_Stub in Example_Card_Calypso (issue [#7]).
### Upgraded
- "Keyple Service Resource Library" to version `2.0.1`
- "Keyple Card Calypso Library" to version `2.0.1`
- "Keyple Card Generic Library" to version `2.0.1`

## [2021-10-06]
### Added
- "CHANGELOG.md" file (issue [eclipse/keyple#6]).
- Example_Card_Calypso
- Example_Distributed_PoolReaderServerSide_Webservice
- Example_Distributed_ReaderClientSide_Webservice
- Example_Distributed_ReaderClientSide_Websocket
- Example_Plugin_Android_NFC
- Example_Plugin_Android_OMAPI
- Example_Plugin_PCSC
- Example_Service
- Example_Service_Resource
- Uses of released dependencies for all examples:
  - org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.0.+
  - org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.0.+
  - org.eclipse.keyple:keyple-common-java-api:2.0.+
  - org.eclipse.keyple:keyple-distributed-network-java-lib:2.0.0
  - org.eclipse.keyple:keyple-distributed-local-java-lib:2.0.0
  - org.eclipse.keyple:keyple-distributed-remote-java-lib:2.0.0
  - org.eclipse.keyple:keyple-service-java-lib:2.0.0
  - org.eclipse.keyple:keyple-service-resource-java-lib:2.0.0
  - org.eclipse.keyple:keyple-plugin-stub-java-lib:2.0.0
  - org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.0.0
  - org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:2.0.0
  - org.eclipse.keyple:keyple-plugin-android-omapi-java-lib:2.0.0
  - org.eclipse.keyple:keyple-card-generic-java-lib:2.0.0
  - org.eclipse.keyple:keyple-card-calypso-java-lib:2.0.0
  - org.eclipse.keyple:keyple-util-java-lib:2.+
    
[unreleased]: https://github.com/eclipse/keyple-java-example/compare/2022-06-09...HEAD
[2022-06-09]: https://github.com/eclipse/keyple-java-example/compare/2022-05-30...2022-06-09
[2022-05-30]: https://github.com/eclipse/keyple-java-example/compare/2022-04-06...2022-05-30
[2022-04-06]: https://github.com/eclipse/keyple-java-example/compare/2022-02-02...2022-04-06
[2022-02-02]: https://github.com/eclipse/keyple-java-example/compare/2021-12-20...2022-02-02
[2021-12-20]: https://github.com/eclipse/keyple-java-example/compare/2021-12-08...2021-12-20
[2021-12-08]: https://github.com/eclipse/keyple-java-example/compare/2021-11-22...2021-12-08
[2021-11-22]: https://github.com/eclipse/keyple-java-example/compare/2021-10-06...2021-11-22
[2021-10-06]: https://github.com/eclipse/keyple-java-example/releases/tag/2021-10-06

[#15]: https://github.com/eclipse/keyple-java-example/issues/15
[#9]: https://github.com/eclipse/keyple-java-example/issues/9
[#7]: https://github.com/eclipse/keyple-java-example/issues/7
[#5]: https://github.com/eclipse/keyple-java-example/issues/5

[eclipse/keyple#6]: https://github.com/eclipse/keyple/issues/6
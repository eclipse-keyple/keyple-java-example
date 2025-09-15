# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Changed
- Switched to [Keyple Java BOM](https://github.com/eclipse-keyple/keyple-java-bom) `2025.09.12` for dependency
  management, replacing individual Keyple component definitions.
- Restructured package names for better alignment with project conventions and clarity.

## [2025-07-21]
### Fixed
- Fixed bad preparation of the selection in PC/SC examples.
### Changed
- Migrated the CI pipeline from Jenkins to GitHub Actions.
### Upgraded
- `keyple-plugin-pcsc-java-lib:2.5.2`
- `keyple-card-calypso-crypto-pki-java-lib:0.2.2`

## [2025-04-18]
### Changed
- Switched license from `EPL 2.0` to `EDL 1.0` (issue [#32]).
  The license switch has been authorized through a restructuring review supervised by the « Eclipse Management
  Organization » team https://gitlab.eclipse.org/eclipsefdn/emo-team/emo/-/issues/908#note_3394156.
### Upgraded
- `keyple-common-java-api:2.0.2`
- `keyple-card-calypso-java-lib:3.1.8`

## [2025-03-21]
### Changed
- Replaced `prepareReadRecord` calls executed in session with `prepareReadRecords` in Calypso examples
- Various refactoring to clarify code
- Updated Android NFC plugin implementation to use new AndroidNfcConfig API
- Modernized Android OMAPI example
### Upgraded
- `keypop-calypso-crypto-legacysam-java-api:0.7.0`
- `keyple-service-java-lib:3.3.5`
- `keyple-service-resource-java-lib:3.1.0`
- `keyple-card-calypso-java-lib:3.1.6`
- `keyple-card-calypso-crypto-legacysam-java-lib:0.9.0`
- `keyple-card-calypso-crypto-pki-java-lib:0.2.1`
- `keyple-plugin-pcsc-java-lib:2.4.2`
- `keyple-distributed-network-java-lib:2.5.1`
- `keyple-distributed-local-java-lib:2.5.2`
- `keyple-distributed-remote-java-lib:2.5.1`
- `keyple-card-generic-java-lib:3.1.2`
- `keyple-plugin-android-nfc-java-lib:3.0.0`
- `keyple-plugin-android-omapi-java-lib:2.1.0`

## [2024-04-15]
### Changed
- Java source and target levels `1.6` -> `1.8`
### Upgraded
- Gradle `6.8.3` -> `7.6.4`
- `keypop-reader-java-api:2.0.1`
- `keypop-calypso-card-java-api:2.1.0`
- `keypop-calypso-crypto-legacysam-java-api:0.5.0`
- `keyple-common-java-api:2.0.1`
- `keyple-util-java-lib:2.4.0`
- `keyple-service-java-lib:3.2.1`
- `keyple-service-resource-java-lib:3.0.1`
- `keyple-distributed-network-java-lib:2.3.1`
- `keyple-distributed-local-java-lib:2.3.1`
- `keyple-distributed-remote-java-lib:2.3.1`
- `keyple-card-generic-java-lib:3.0.1`
- `keyple-card-calypso-java-lib:3.1.1`
- `keyple-card-calypso-crypto-legacysam-java-lib:0.6.0`
- `keyple-card-calypso-crypto-pki-java-lib:0.1.0`
- `keyple-plugin-pcsc-java-lib:2.2.1`
- `keyple-plugin-stub-java-lib:2.2.1`
- `keyple-plugin-android-nfc-java-lib:2.2.0`
### Removed
- Temporarily disabled the example implementing the OMAPI plugin, and it will not be updated until there is more clarity
  on the requirements surrounding this technology.

## [2023-11-30]
:warning: Major version! Following the migration of the "Calypsonet Terminal" APIs to the
[Eclipse Keypop project](https://keypop.org), this library now implements Keypop interfaces.
### Upgraded
- Calypsonet Terminal Reader API `1.2.0` -> Keypop Reader API `2.0.0`
- Calypsonet Terminal Calypso API `1.8.0` -> Keypop Calypso Card API `2.0.0`
- Calypsonet Terminal Crypto Legacy SAM API `1.0.0` -> Keypop Crypto Legacy SAM API `0.3.0`
- Keyple Service Library `2.2.0` -> `3.0.0`
- Keyple Service Resource Library `2.2.0` -> `3.0.0`
- Keyple Generic Card Library `2.0.2` -> `3.0.0`
- Keyple Calypso Card Library `2.3.4` -> `3.0.0`
- Keyple Calypso Crypto LegacySAM Library `0.3.0` -> `0.4.0`.
- Keyple Util Library `2.3.0` -> `2.3.1`
- Keyple Plugin PC/SC Library `2.1.1` -> `2.1.2`

## [2023-04-27]
### Upgraded
- "Keyple Service Library" to version `2.2.0`.
- "Keyple Service Resource Library" to version `2.1.1`.
- The use of wildcards ('+') in some dependency version definitions has been removed and replaced with the requirement 
to use the exact and latest version of each dependency. This ensures consistency and avoids compatibility issues.

## [2023-04-05]
### Upgraded
- "Calypsonet Terminal Calypso API" to `1.8.0`.
- "Calypsonet Terminal Calypso Crypto Legacy SAM API" version `0.2.0`.
- "Keyple Calypso Card Library" to version `2.3.4`.
- "Keyple Calypso Crypto Legacy SAM Library" version `0.3.0`.
- "Keyple Service Library" to version `2.1.4`.
- "Keyple Distributed Network Library" to version `2.2.0`.
- "Keyple Distributed Remote Library" to version `2.2.0`.
- "Keyple Distributed Local Library" to version `2.2.0`.
- "Keyple Plugin PC/SC Library" to version `2.1.1`

## [2023-02-23]
### Upgraded
- "Calypsonet Terminal Calypso API" to `1.6.0`
- "Keyple Calypso Card Library" to version `2.3.2`.
- "Keyple Service Library" to version `2.1.3`.
- "Keyple Distributed Remote Library" to version `2.1.0`.
- "Google Gson library" (com.google.code.gson) to version `2.10.1`.
### Added
- `UseCase15_ExtendedModeSession` example.

## [2023-01-10]
### Fixed
- Removed filtering by protocol in performance measurement tools.
### Upgraded
- "Calypsonet Terminal Reader API" to `1.2.0`
- "Keyple Service Library" to version `2.1.2`.
- "Keyple Calypso Card Library" to version `2.3.1`.

## [2022-12-13]
### Added
- "Calypsonet Terminal Calypso Crypto Legacy SAM API" version `0.1.0`.
- "Keyple Calypso Crypto Legacy SAM Library" version `0.2.0`.
- `UseCase14_ReadLegacySamCountersAndCeilings` example.

## [2022-12-06]
### Upgraded
- "Keyple Calypso Card Library" to version `2.3.0`.

## [2022-11-17]
### Fixed
- Command line argument parsing in TN313 Calypso example.
### Added
- Default KIF values for `PRIME_REVISION_2` cards in TN313 Calypso example.
### Upgraded
- "Keyple Calypso Card Library" to version `2.2.5`

## [2022-10-27]`
### Added
- Calypso example #12 for Performance Measurement (embedded validation).
- Calypso example #13 for Performance Measurement (distributed reloading).
### Changed
- Replaced the use of the Card Resource Service with a simple SAM selection in most Calypso examples.
### Upgraded
- "Calypsonet Terminal Reader API" to `1.1.0` 
- "Calypsonet Terminal Calypso API" to `1.4.0`
- "Keyple Service Library" to version `2.1.1`
- "Keyple Calypso Card Library" to version `2.2.3`

## [2022-07-26]
### Upgraded
- "Keyple Calypso Card Library" to version `2.2.1`
- "Keyple Plugin PC/SC Library" to version `2.1.0`
- "Keyple Service Library" to version `2.1.0`
- "Keyple Service Resource Library" to version `2.0.2`

## [2022-06-09]
### Fixed
- Removal of the unused Jacoco plugin for compiling Android applications that had an unwanted side effect when the application was launched (stacktrace with warnings).
### Upgraded
- "Keyple Plugin Android NFC Library" to version `2.0.1`
- "Keyple Plugin Android OMAPI Library" to version `2.0.1`

## [2022-05-30]
### Added
- Calypso example #11 for Data signing (issue [#15]).
### Changed
- Removal of the use of deprecated methods.
### Upgraded
- "Calypsonet Terminal Calypso API" to version `1.2.+`
- "Keyple Card Calypso Library" to version `2.2.0`
- "Keyple Util Library" to version `2.1.0`

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
- "CHANGELOG.md" file (issue [eclipse-keyple/keyple#6]).
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

[unreleased]: https://github.com/eclipse-keyple/keyple-java-example/compare/2025-07-21...HEAD
[2025-07-21]: https://github.com/eclipse-keyple/keyple-java-example/compare/2025-04-18...2025-07-21
[2025-04-18]: https://github.com/eclipse-keyple/keyple-java-example/compare/2025-03-21...2025-04-18
[2025-03-21]: https://github.com/eclipse-keyple/keyple-java-example/compare/2024-04-15...2025-03-21
[2024-04-15]: https://github.com/eclipse-keyple/keyple-java-example/compare/2023-11-30...2024-04-15
[2023-11-30]: https://github.com/eclipse-keyple/keyple-java-example/compare/2023-04-27...2023-11-30
[2023-04-27]: https://github.com/eclipse-keyple/keyple-java-example/compare/2023-04-05...2023-04-27
[2023-04-05]: https://github.com/eclipse-keyple/keyple-java-example/compare/2023-02-23...2023-04-05
[2023-02-23]: https://github.com/eclipse-keyple/keyple-java-example/compare/2023-01-10...2023-02-23
[2023-01-10]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-12-13...2023-01-10
[2022-12-13]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-12-06...2022-12-13
[2022-12-06]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-11-17...2022-12-06
[2022-11-17]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-10-27...2022-11-17
[2022-10-27]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-07-26...2022-10-27
[2022-07-26]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-06-09...2022-07-26
[2022-06-09]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-05-30...2022-06-09
[2022-05-30]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-04-06...2022-05-30
[2022-04-06]: https://github.com/eclipse-keyple/keyple-java-example/compare/2022-02-02...2022-04-06
[2022-02-02]: https://github.com/eclipse-keyple/keyple-java-example/compare/2021-12-20...2022-02-02
[2021-12-20]: https://github.com/eclipse-keyple/keyple-java-example/compare/2021-12-08...2021-12-20
[2021-12-08]: https://github.com/eclipse-keyple/keyple-java-example/compare/2021-11-22...2021-12-08
[2021-11-22]: https://github.com/eclipse-keyple/keyple-java-example/compare/2021-10-06...2021-11-22
[2021-10-06]: https://github.com/eclipse-keyple/keyple-java-example/releases/tag/2021-10-06

[#32]: https://github.com/eclipse-keyple/keyple-java-example/issues/32
[#15]: https://github.com/eclipse-keyple/keyple-java-example/issues/15
[#9]: https://github.com/eclipse-keyple/keyple-java-example/issues/9
[#7]: https://github.com/eclipse-keyple/keyple-java-example/issues/7
[#5]: https://github.com/eclipse-keyple/keyple-java-example/issues/5

[eclipse-keyple/keyple#6]: https://github.com/eclipse-keyple/keyple/issues/6
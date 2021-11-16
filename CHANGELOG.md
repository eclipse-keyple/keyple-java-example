# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Log of the Calypso serial number in the examples of Example_Card_Calypso (issue [#5]).
- Main_ExplicitSelectionAid_Stub in Example_Card_Calypso (issue [#7]).
- Main_ScheduledSelection_Stub in Example_Card_Calypso (issue [#7]).
- Main_CardAuthentication_Stub in Example_Card_Calypso (issue [#7]).

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

[unreleased]: https://github.com/eclipse/keyple-java-example/compare/2021-10-06...HEAD
[2021-10-06]: https://github.com/eclipse/keyple-java-example/releases/tag/2021-10-06

[#7]: https://github.com/eclipse/keyple-java-example/issues/7
[#5]: https://github.com/eclipse/keyple-java-example/issues/5

[eclipse/keyple#6]: https://github.com/eclipse/keyple/issues/6
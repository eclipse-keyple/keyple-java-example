# Card Calypso Examples

Those examples make use of the Keyple Calypso Extension library. They demonstrate the main features of the library's
API. We use a PCSC plugin for real smart cards, and a Stub Plugin to simulates Calypso smart card.

* Single or dual reader configuration (Card and SAM).
* Explicit and scheduled application selection.
* Calypso Card Secure Session in atomic and multiple mode.
* PIN verification.
* Stored Value debit and reload.

Each example can be run independently.

* Use Case Calypso 1 – Explicit Selection (
  Aid): [UseCase1_ExplicitSelectionAid](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase1_ExplicitSelectionAid)
    * Check if a card is in the reader, attempt to select a ISO 14443-4 Calypso card defined by its AID and read a file
      record following the selection (simple plain read, not involving a Calypso SAM).
    * _Explicit Selection_ means that the terminal application starts the card processing after the card presence has
      been checked.
    * Implementations:
        * For PC/SC plugin: [`Main_ExplicitSelectionAid_Pcsc.java`]
        * For Stub plugin: [`Main_ExplicitSelectionAid_Stub.java`]

* Use Case Calypso 2 – Scheduled
  Selection: [UseCase2_ScheduledSelection](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase2_ScheduledSelection)
    * Schedule a default selection of ISO 14443-4 Calypso card with a file record reading and set it to an observable
      reader, on card detection in case the Calypso selection is successful, notify the terminal application with the
      card data.
    * _Scheduled Selection_ means that the card selection process is automatically started when the card is detected.
    * Implementations:
        * For PC/SC plugin: [`Main_ScheduledSelection_Pcsc.java`]
        * For Stub plugin: [`Main_ScheduledSelection_Stub.java`]

* Use Case Calypso 3 – Selection of Calypso card Revision 1 (no
  AID): [UseCase3_Rev1Selection](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase3_Rev1Selection)
    * Check if a card is in the reader, select a Calypso card Rev1 identified by its communication protocol, operate a
      simple Calypso card transaction (simple plain read, not involving a Calypso SAM).
    * _Explicit Selection_ means that the terminal application starts the card processing after the card presence has
      been checked.
    * Implementations:
        * For PC/SC plugin: [`Main_Rev1Selection_Pcsc.java`]

* Use Case Calypso 4 - Card Authentication (certified reading of a file
  record):  [UseCase4_CardAuthentication](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase4_CardAuthentication)
    * Set up a card transaction using the Card Resource Service to process a basic Calypso Secure Session.
    * Real mode with PC/SC readers [`Main_CardAuthentication_Pcsc.java`]
    * Simulation mode  (Stub Secure Elements included) [`Main_CardAuthentication_Stub.java`]

* Use Case Calypso 5 - Multiple Session: illustrates the multiple session generation mechanism for managing the
  sending of modifying commands that exceed the capacity of the session
  buffer: [UseCase5_MultipleSession](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase5_MultipleSession)
    * Real mode with PC/SC readers [`Main_MultipleSession_Pcsc.java`]

* Use Case Calypso 6 - PIN management: presentation of the PIN, attempts counter
  reading: [UseCase6_VerifyPin](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase6_VerifyPin)
    * Real mode with PC/SC readers [`Main_VerifyPin_Pcsc.java`]

* Use Case Calypso 7 - Stored Value reloading (out of Secure Session):
   [UseCase7_StoredValue_SimpleReloading](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase7_StoredValue_SimpleReloading)
    * Real mode with PC/SC readers [`Main_StoredValue_SimpleReloading_Pcsc.java`]

* Use Case Calypso 8 - Stored Value debit within a Secure Session:
   [UseCase8_StoredValue_DebitInSession](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase8_StoredValue_DebitInSession)
    * Real mode with PC/SC readers [`Main_StoredValue_DebitInSession_Pcsc.java`]

* Use Case Calypso 9 - Change PIN:
   [UseCase9_ChangePin](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase9_ChangePin)
    * Real mode with PC/SC readers [`Main_ChangePin_Pcsc.java`]

* Use Case Calypso 10 - Session Trace TN #313:
   [UseCase10_SessionTrace_TN313](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase10_SessionTrace_TN313)
    * Real mode with PC/SC readers [`Main_SessionTrace_TN313_Pcsc.java`]

* Use Case Calypso 11 - Data signing:
  [UseCase11_DataSigning](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase11_DataSigning)
    * Real mode with PC/SC readers [`Main_DataSigning_Pcsc.java`]

* Use Case Calypso 12 - Performance measurement (embedded validation):
  [UseCase12_PerformanceMeasurement_EmbeddedValidation](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase12_PerformanceMeasurement_EmbeddedValidation)
    * Real mode with PC/SC readers [`Main_PerformanceMeasurement_EmbeddedValidation_Pcsc.java`]

* Use Case Calypso 13 - Performance measurement (distributed reloading):
  [UseCase13_PerformanceMeasurement_DistributedReloading](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase13_PerformanceMeasurement_DistributedReloading)
    * Real mode with PC/SC readers [`Main_PerformanceMeasurement_DistributedReloading_Pcsc.java`]

* Use Case Calypso 14 - Read SAM counters and ceilings:
  [UseCase14_ReadLegacySamCountersAndCeilings](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase14_ReadLegacySamCountersAndCeilings)
    * Real mode with PC/SC readers [`Main_ReadLegacySamCountersAndCeilings_Pcsc.java`]

* Use Case Calypso 15 - Secure session in extended mode with early authentication and data encryption:
  [UseCase15_ExtendedModeSession](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase15_ExtendedModeSession)
    * Real mode with PC/SC readers [`Main_ExtendedModeSession_Pcsc.java`]

* Use Case Calypso 16 - Secure session in PKI mode:
  [UseCase16_PkiModeSession](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase16_PkiModeSession)
    * Real mode with PC/SC readers [`Main_PkiModeSession_Pcsc.java`]

* Use Case Calypso 17 - PKI card pre-personalization:
  [UseCase17_PkiPrePersonalization](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Card_Calypso/src/main/java/org/eclipse/keyple/card/calypso/example/UseCase17_PkiPrePersonalization)
    * Real mode with PC/SC readers [`Main_CardKeyPairGeneratedByCard_Pcsc.java`]
    * Real mode with PC/SC readers [`Main_CardKeyPairGeneratedByLegacySam_Pcsc.java`]
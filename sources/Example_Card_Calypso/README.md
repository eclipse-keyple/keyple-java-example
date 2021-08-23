Getting Started - Calypso Examples
---

Those examples make use of the Keyple Calypso Extension library. They demonstrate the main features of the library's
API. We use a PCSC plugin for real smart cards, and a Stub Plugin to simulates Calypso smart card.

* Single or dual reader configuration (Card and SAM).
* Explicit and scheduled application selection.
* Calypso Card Secure Session in atomic and multiple mode.
* PIN verification.
* Stored Value debit and reload.

Each example can be run independently.

* Use Case ‘Calypso 1’ – Explicit Selection (
  Aid) : [UseCase1_ExplicitSelectionAid](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase1_ExplicitSelectionAid)
    * Check if a card is in the reader, attempt to select a ISO 14443-4 Calypso card defined by its AID and read a file
      record following the selection (simple plain read, not involving a Calypso SAM).
    * _Explicit Selection_ means that the terminal application starts the card processing after the card presence has
      been checked.
    * Implementations:
        * For PC/SC plugin: [`Main_ExplicitSelectionAid_Pcsc.java`]
        * For Stub plugin: [`Main_ExplicitSelectionAid_Stub.java`]
* Use Case ‘Calypso 2’ – Scheduled
  Selection [UseCase2_ScheduledSelection](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase2_ScheduledSelection)
    * Schedule a default selection of ISO 14443-4 Calypso card with a file record reading and set it to an observable
      reader, on card detection in case the Calypso selection is successful, notify the terminal application with the
      card data.
    * _Scheduled Selection_ means that the card selection process is automatically started when the card is detected.
    * Implementations:
        * For PC/SC plugin: [`Main_ScheduledSelection_Pcsc.java`]
        * For Stub plugin: [`Main_ScheduledSelection_Stub.java`]
* Use Case ‘Calypso 3’ – Selection of Calypso card Revision 1 (no
  AID) : [UseCase3_Rev1Selection](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase3_Rev1Selection)
    * Check if a card is in the reader, select a Calypso card Rev1 identified by its communication protocol, operate a
      simple Calypso card transaction (simple plain read, not involving a Calypso SAM).
    * _Explicit Selection_ means that the terminal application starts the card processing after the card presence has
      been checked.
    * Implementations:
        * For PC/SC plugin: [`Main_Rev1Selection_Pcsc.java`]
        * For Stub plugin: [`Main_Rev1Selection_Stub.java`]
* Use case 'Calypso 4' - Card Authentication (certified reading of a file
  record):  [UseCase4_CardAuthentication](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase4_CardAuthentication)
    * Set up a card transaction using the Card Resource Service to process a basic Calypso Secure Session.
    * Real mode with PC/SC readers [`Main_CardAuthentication_Pcsc.java`]
    * Simulation mode  (Stub Secure Elements included) [`Main_CardAuthentication_Stub.java`]

* Use case 'Calypso 5' - Multiple Session: illustrates the multiple session generation mechanism for managing the
  sending of modifying commands that exceed the capacity of the session
  buffer. [UseCase5_MultipleSession](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase5_MultipleSession)
    * Real mode with PC/SC readers [`Main_MultipleSession_Pcsc.java`]

* Use case 'Calypso 6' - PIN management: presentation of the PIN, attempts counter
  reading. [UseCase6_VerifyPin](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase6_VerifyPin)
    * Real mode with PC/SC readers [`Main_VerifyPin_Pcsc.java`]

* Use case 'Calypso 7' - Stored Value reloading (out of Secure Session)
  . [UseCase7_StoredValue_SimpleReload](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase7_StoredValue_SimpleReload)
    * Real mode with PC/SC readers [`Main_StoredValue_SimpleReload_Pcsc.java`]

* Use case 'Calypso 8' - Stored Value debit within a Secure Session.
  . [UseCase8_StoredValue_DebitInSession](https://github.com/eclipse/keyple-card-calypso-lib/tree/main/examples/src/main/java/org/eclipse/keyple/card/calypso/examples/UseCase8_StoredValue_DebitInSession)
    * Real mode with PC/SC readers [`Main_StoredValue_DebitInSession_Pcsc.java`]
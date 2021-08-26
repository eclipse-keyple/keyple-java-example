# Keyple Service examples

Those examples make use of the Keyple Java Service and the Keyple Java Card Generic extension. They demonstrate how to
select card and how to observe plugins and readers.

Each example can be run independently.

* Use Case Generic 1 – Basic
  selection: [UseCase1_BasicSelection](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase1_BasicSelection)
    * Demonstrates the most basic selection mode where no conditions are required to select a card.
    * Implementations:
        * For PC/SC plugin: [`Main_BasicSelection_Pcsc.java`]
        * For Stub plugin: [`Main_BasicSelection_Stub.java`]

* Use Case Generic 2 – Protocol based
  selection: [UseCase2_ProtocolBasedSelection](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase2_ProtocolBasedSelection)
    * Demonstrates the selection mode where the card's protocol is used as a differentiator.
    * Implementations:
        * For PC/SC plugin: [`Main_ProtocolBasedSelection_Pcsc.java`]
        * For Stub plugin: [`Main_ProtocolBasedSelection_Stub.java`]

* Use Case Generic 3 – Aid based
  selection: [UseCase3_AidBasedSelection](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase3_AidBasedSelection)
    * Demonstrates the selection mode where the card is selected by its DF Name with an appropriate AID.
    * Implementations:
        * For PC/SC plugin: [`Main_AidBasedSelection_Pcsc.java`]
        * For Stub plugin: [`Main_AidBasedSelection_Stub.java`]

* Use Case Generic 4 – Aid based
  selection: [UseCase4_ScheduledSelection](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase4_ScheduledSelection)
    * Demonstrates the selection is processed as soon as the card is detected by an observable reader.
    * Implementations:
        * For PC/SC plugin: [`Main_ScheduledSelection_Pcsc.java`]
        * For Stub plugin: [`Main_ScheduledSelection_Stub.java`]

* Use Case Generic 5 – Sequential multiple
  selection: [UseCase5_SequentialMultiSelection](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase5_SequentialMultiSelection)
    * Demonstrates multiple selections performed successively on the same card with the navigation options (FIRST/NEXT)
      defined by the ISO standard.
    * Implementations:
        * For PC/SC plugin: [`Main_SequentialMultiSelection_Pcsc.java`]
        * For Stub plugin: [`Main_SequentialMultiSelection_Stub.java`]

* Use Case Generic 6 – Grouped multiple
  selection: [UseCase6_GroupedMultiSelection](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase6_GroupedMultiSelection)
    * Demonstrates multiple selections made at once on the same card with the navigation options (FIRST/NEXT) defined by
      the ISO standard and returned as a single selection result.
    * Implementations:
        * For PC/SC plugin: [`Main_GroupedMultiSelection_Pcsc.java`]
        * For Stub plugin: [`Main_GroupedMultiSelection_Stub.java`]

* Use Case Generic 7 – Plugin and reader
  observation: [UseCase7_PluginAndReaderObservation](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service/src/main/java/org/eclipse/keyple/core/service/examples/UseCase7_PluginAndReaderObservation)
    * Demonstrates the observation of a plugin to monitor the connection/disconnection of readers and of the readers to
      monitor the insertion/removal of cards.
    * Implementations:
        * For PC/SC plugin: [`Main_PluginAndReaderObservation_Pcsc.java`]
        * For Stub plugin: [`Main_PluginAndReaderObservation_Stub.java`]
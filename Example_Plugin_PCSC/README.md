# Plugin PC/SC examples

Those examples make use of the Keyple Java PC/SC Plugin to demonstrate its specific features.

Each example can be run independently.

* Use Case PC/SC 1 – Reader type
  identification: [UseCase1_ReaderTypeAutoIdentification](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Plugin_PCSC/src/main/java/org/eclipse-keyple/keyple/plugin/pcsc/example/UseCase1_ReaderTypeAutoIdentification)
    * Demonstrates the reader type identification (contact/contactless) based on the reader's name.
    * [`Main_ReaderTypeAutoIdentification_Pcsc.java`]

* Use Case PC/SC 2 – Reader type explicit
  definition: [UseCase2_ExplicitReaderType](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Plugin_PCSC/src/main/java/org/eclipse-keyple/keyple/plugin/pcsc/example/UseCase2_ExplicitReaderType)
    * Demonstrates how set explicitly the reader type.
    * [`Main_ExplicitReaderType_Pcsc.java`]

* Use Case PC/SC 3 – Change of a protocol identification
  rule: [UseCase3_ChangeProtocolRules](https://github.com/eclipse-keyple/keyple-java-example/tree/main/Example_Plugin_PCSC/src/main/java/org/eclipse-keyple/keyple/plugin/pcsc/example/UseCase3_ChangeProtocolRules)
    * Demonstrates how to add a protocol rule to target a specific card technology by applying a regular expression on
      the ATR provided by the reader. This feature of the PC/SC plugin is useful for extending the set of rules already
      supported, but also for solving compatibility issues with some readers producing ATRs that do not work with the
      built-in rules.
    * It also shows how to set some PC/SC specific settings such as the Sharing mode or the ISO Card protocol.

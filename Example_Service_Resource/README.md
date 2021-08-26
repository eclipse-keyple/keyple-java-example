# Card Resource Service example

Those examples make use of the Keyple Java Service Resource and the Keyple Java Card Generic extension. They demonstrate how to
select card and how to observe plugins and readers.

Each example can be run independently.

* Use Case Generic 1 – Card resource service
  observation: [UseCase1_CardResourceService](https://github.com/eclipse/keyple-java-example/tree/main/Example_Service_Resource/src/main/java/org/eclipse/keyple/core/service/resource/example/UseCase1_CardResourceService)
    * Shows the implementation of the Card Resource service with simultaneous observation of the plugin and the readers,
      with the definition of two resource profiles.
    * Implementations:
        * For Stub plugin: [`Main_CardResourceService_Stub.java`]

# Use Case #1 : Reader Server Side with Sync Network Protocol (Web Socket) example

This projects aims at demonstrating how simple is the implementation of the asynchronous mode of the **Keyple Distributed** solution with a Web Socket protocol.
This configuration allows a **server** application to control a smart card reader available on a **client** (e.g. Card reader).
See the developer guide on [keyple.org](https://keyple.org) for more details.

The **local reader** is plugged on the client device which communicates with the server through a web socket.

Running the example, a complete scenario is executed :
- quarkus server starts up
- client and server components are initialized
- a Stub calypso Card is inserted in the local reader (client side)
- a remote transaction is executed by the server
- quarkus server shutdowns

By default, this example uses the keyple `StubPlugin` that emulates a local Reader and a smart card. You can activate the PCSC configuration by invoking ```initPcscReader()``` instead of  ```initStubReader()``` in the `clientApp.init()` method.      

## Running the example

This example is based on the Quarkus framework. To execute the example in dev mode, you need to install Quarkus dependencies : 
- JDK 1.8+ installed with JAVA_HOME configured appropriately
- gradle 

You can run the example in dev mode that enables live coding using:
```
./gradlew runExample
```

If you are interested in a synchronous protocol, checkout the webservice example.

## Packaging and running the application

The application can be packaged using `./gradlew quarkusBuild`.
It produces the `Example_Distributed_ReaderClientSide_Websocket-2.0.0-runner.jar` file in the `build` directory.
Be aware that it is a uber-jar as the dependencies are copied inside the jar.

The application is now runnable using `java -jar build/Example_Distributed_ReaderClientSide_Websocket-2.0.0-runner.jar`.

## Creating a native executable

You can create a native executable using: `./gradlew build -Dquarkus.package.type=native`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./build/Example_Distributed_ReaderClientSide_Websocket-2.0.0-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling#building-a-native-executable.

## About Quarkus

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

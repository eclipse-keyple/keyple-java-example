/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Distribution License 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ************************************************************************************** */
package org.eclipse.keyple.distributed.example.readerclientside.webservice;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javax.inject.Inject;
import org.eclipse.keyple.distributed.example.readerclientside.webservice.client.AppClient;
import org.eclipse.keyple.distributed.example.readerclientside.webservice.server.AppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String... args) {
    Quarkus.run(ExampleApp.class, args);
  }

  /** Main class of the server application. */
  public static class ExampleApp implements QuarkusApplication {

    /** The server application */
    @Inject AppServer appServer;

    /** The client application */
    @Inject AppClient appClient;

    /** {@inheritDoc} */
    @Override
    public int run(String... args) {

      logger.info("Server app init...");
      appServer.init();

      logger.info("Client app init...");
      appClient.init();

      logger.info("Launch client scenario...");
      Boolean isSuccessful = appClient.launchScenario();

      logger.info("Is scenario successful ? {}", isSuccessful);
      return 0;
    }
  }
}

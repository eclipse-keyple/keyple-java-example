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
package org.eclipse.keyple.distributed.example.readerclientside.websocket.server;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.distributed.RemotePluginServerFactory;
import org.eclipse.keyple.distributed.RemotePluginServerFactoryBuilder;

/** Example of a server side application. */
@ApplicationScoped
public class AppServer {

  public static final String REMOTE_PLUGIN_NAME = "REMOTE_PLUGIN_#1";

  /** The endpoint server */
  @Inject EndpointServer endpointServer;

  /**
   * Initialize the server components :
   *
   * <ul>
   *   <li>A {@link org.eclipse.keyple.distributed.RemotePluginServer} with a sync node and attach
   *       an observer that contains all the business logic.
   * </ul>
   */
  public void init() {

    // Init the remote plugin factory.
    RemotePluginServerFactory factory =
        RemotePluginServerFactoryBuilder.builder(REMOTE_PLUGIN_NAME)
            .withAsyncNode(endpointServer)
            .build();

    // Register the remote plugin to the smart card service using the factory.
    ObservablePlugin plugin =
        (ObservablePlugin) SmartCardServiceProvider.getService().registerPlugin(factory);

    // Init the remote plugin observer.
    plugin.setPluginObservationExceptionHandler(
        new PluginObservationExceptionHandlerSpi() {
          @Override
          public void onPluginObservationError(String pluginName, Throwable e) {
            // NOP
          }
        });
    plugin.addObserver(new RemotePluginServerObserver());
  }
}

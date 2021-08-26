/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.distributed.example.readerclientside.webservice.server;

import javax.enterprise.context.ApplicationScoped;
import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.distributed.RemotePluginServerFactory;
import org.eclipse.keyple.distributed.RemotePluginServerFactoryBuilder;

/** Example of a server side application. */
@ApplicationScoped
public class AppServer {

  public static final String REMOTE_PLUGIN_NAME = "REMOTE_PLUGIN_#1";

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
        RemotePluginServerFactoryBuilder.builder(REMOTE_PLUGIN_NAME).withSyncNode().build();

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

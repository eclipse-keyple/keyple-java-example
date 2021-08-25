/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.examples.UseCase7_PluginAndReaderObservation;

import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 7 – plugin and reader observation (PC/SC)</h1>
 *
 * <p>We demonstrate here the monitoring of an {@link ObservablePlugin} to be notified of reader
 * connection/disconnection, and also the monitoring of an {@link ObservableReader} to be notified
 * of card insertion/removal.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Launch the monitoring of the plugin, display potential already connected reader and already
 *       inserted cards.
 *   <li>Display any further reader connection/disconnection or card insertion/removal.
 *   <li>Automatically observe newly connected readers.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_PluginAndReaderObservation_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_PluginAndReaderObservation_Pcsc.class);

  public static void main(String[] args) throws Exception {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // We add an observer to each plugin (only one in this example) the readers observers will be
    // added dynamically upon plugin events notification. Nevertheless, here we provide the plugin
    // observer with the readers already present at startup in order to assign them a reader
    // observer.
    logger.info("Add observer PLUGINNAME = {}", plugin.getName());
    PluginObserver pluginObserver = new PluginObserver(plugin.getReaders());
    ((ObservablePlugin) plugin).setPluginObservationExceptionHandler(pluginObserver);
    ((ObservablePlugin) plugin).addObserver(pluginObserver);

    logger.info("Wait for reader or card insertion/removal");

    // Wait indefinitely. CTRL-C to exit.
    synchronized (waitForEnd) {
      waitForEnd.wait();
    }

    // unregister plugin
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program.");
  }

  /**
   * This object is used to freeze the main thread while card operations are handle through the
   * observers callbacks. A call to the notify() method would end the program (not demonstrated
   * here).
   */
  private static final Object waitForEnd = new Object();
}

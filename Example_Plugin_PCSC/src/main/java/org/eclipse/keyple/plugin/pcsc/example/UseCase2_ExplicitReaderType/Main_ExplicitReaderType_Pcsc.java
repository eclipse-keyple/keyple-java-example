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
package org.eclipse.keyple.plugin.pcsc.example.UseCase2_ExplicitReaderType;

import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case PC/SC 2 – Automatic reader type identification (PC/SC)</h1>
 *
 * <p>We demonstrate here how to configure the PC/SC plugin to allow explicit setting of
 * contact/contactless type for a reader.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Configure the plugin (via its factory builder) without specifying regular expressions.
 *   <li>Set the 'contactless' type for all connected readers.
 *   <li>Display the types of all connected readers.
 * </ul>
 *
 * <p><strong>Note #1:</strong> not all applications need to know what type of reader it is. This
 * parameter is only required if the application or card extension intends to call the {@link
 * CardReader#isContactless()} method.
 *
 * <p><strong>Note #2:</strong>: the Keyple Calypso Card extension requires this knowledge.
 *
 * <p><strong>Note #2:</strong>: in a production application, this setting must be applied to the
 * relevant reader.
 *
 * <p>All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_ExplicitReaderType_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_ExplicitReaderType_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, do not specify any regex for the type
    // identification (see use case 1), get the corresponding generic plugin in return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Set the contactless type to all connected readers through the specific method provided by
    // PC/SC reader's extension.
    for (CardReader reader : plugin.getReaders()) {
      plugin.getReaderExtension(PcscReader.class, reader.getName()).setContactless(true);
    }

    // Log the type of each connected reader
    for (CardReader reader : plugin.getReaders()) {
      logger.info(
          "The reader '{}' is a '{}' type",
          reader.getName(),
          reader.isContactless() ? "contactless" : "contact");
    }

    System.exit(0);
  }
}

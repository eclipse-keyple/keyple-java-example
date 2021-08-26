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
package org.eclipse.keyple.plugin.pcsc.example.UseCase1_ReaderTypeAutoIdentification;

import java.util.Set;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case PC/SC 1 – Automatic reader type identification (PC/SC)</h1>
 *
 * <p>We demonstrate here how to configure the PC/SC plugin to have an automatic detection of the
 * type of reader (contact/non-contact) from its name.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Configure the plugin (via its factory builder) to specify two regular expressions to apply
 *       to the reader names.
 *   <li>The first regular expression defines the names of readers that are of the contactless type.
 *   <li>The second regular expression defines the names of readers that are of the contactl type.
 *   <li>Display the types of all connected readers.
 * </ul>
 *
 * <p><strong>Note #1:</strong> not all applications need to know what type of reader it is. This
 * parameter is only required if the application or card extension intends to call the {@link
 * Reader#isContactless()} method.
 *
 * <p><strong>Note #2:</strong>: the Keyple Calypso Card extension requires this knowledge.
 *
 * <p><strong>Note #2:</strong>: In a production application, these regular expressions must be
 * adapted to the names of the devices used.
 *
 * <p>All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_ReaderTypeAutoIdentification_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ReaderTypeAutoIdentification_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, set the two regular expression matching
    // the expected devices, get the corresponding generic plugin in return.
    Plugin plugin =
        smartCardService.registerPlugin(
            PcscPluginFactoryBuilder.builder()
                .useContactlessReaderIdentificationFilter(
                    ".*ASK LoGO.*|.*HID OMNIKEY 5427 CK.*|.*contactless.*")
                .useContactReaderIdentificationFilter(
                    ".*Identive.*|.*HID Global OMNIKEY 3x21.*|(?=contact)(?!contactless)")
                .build());

    // Get all connected readers
    Set<Reader> readers = plugin.getReaders();

    // Log the type of each reader
    for (Reader reader : readers) {
      logger.info(
          "The reader '{}' is a '{}' type",
          reader.getName(),
          reader.isContactless() ? "contactless" : "contact");
    }

    System.exit(0);
  }
}

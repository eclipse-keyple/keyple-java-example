/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.plugin.pcsc.example.UseCase3_ChangeProtocolRules;

import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case PC/SC 3 – Change of a protocol identification rule (PC/SC)</h1>
 *
 * <p>Here we demonstrate how to add a protocol rule to target a specific card technology by
 * applying a regular expression on the ATR provided by the reader.
 *
 * <p>This feature of the PC/SC plugin is useful for extending the set of rules already supported,
 * but also for solving compatibility issues with some readers producing ATRs that do not work with
 * the built-in rules.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Configure the plugin to add a new protocol rule targeting Mifare Classic 4K cards.
 *   <li>Attempts to select a Mifare Classic 4K card with a protocol based selection.
 *   <li>Display the selection result.
 * </ul>
 *
 * In a real application, these regular expressions must be customized to the names of the devices
 * used.
 *
 * <p>All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_ChangeProtocolRules_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_ChangeProtocolRules_Pcsc.class);
  public static final String READER_PROTOCOL_MIFARE_CLASSIC_4_K = "MIFARE_CLASSIC_4K";
  public static final String CARD_PROTOCOL_MIFARE_CLASSIC_4_K = "MIFARE_CLASSIC_4K";

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, do not specify any regex for the type
    // identification (see use case 1), get the corresponding generic plugin in return.
    Plugin plugin =
        smartCardService.registerPlugin(
            PcscPluginFactoryBuilder.builder()
                .updateProtocolIdentificationRule(
                    READER_PROTOCOL_MIFARE_CLASSIC_4_K, "3B8F8001804F0CA0000003060300020000000069")
                .build());

    // Get the first available reader (we assume that a single contactless reader is connected)
    CardReader reader = plugin.getReaders().iterator().next();

    ((ConfigurableCardReader) reader)
        .activateProtocol(READER_PROTOCOL_MIFARE_CLASSIC_4_K, CARD_PROTOCOL_MIFARE_CLASSIC_4_K);

    // Configure the reader for contactless operations
    plugin
        .getReaderExtension(PcscReader.class, reader.getName())
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);

    // Get the generic card extension service
    GenericExtensionService cardExtension = GenericExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    // Check if a card is present in the reader
    if (!reader.isCardPresent()) {
      logger.error("No card is present in the reader.");
      System.exit(0);
    }

    // Retrieve the reader API factory.
    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a generic card selection with a MIFARE CLASSIC protocol filter
    CardSelector<BasicCardSelector> cardSelector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByCardProtocol(CARD_PROTOCOL_MIFARE_CLASSIC_4_K);

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(
        cardSelector, cardExtension.createGenericCardSelectionExtension());

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      logger.error("The selection of the card failed.");
      System.exit(0);
    }

    // Get the SmartCard resulting of the selection.
    SmartCard smartCard = selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", smartCard);

    System.exit(0);
  }
}

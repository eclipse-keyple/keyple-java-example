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
package org.eclipse.keyple.example.core.service.UseCase5_SequentialMultiSelection;

import org.eclipse.keyple.card.generic.GenericCardSelectionExtension;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.example.core.service.common.ConfigurationUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.*;
import org.eclipse.keypop.reader.selection.spi.IsoSmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 5 – Sequential selections based on an AID prefix (PC/SC)</h1>
 *
 * <p>We demonstrate here the selection of two applications in a single card, with both applications
 * selected sequentially using the same AID and the "FIRST" and "NEXT" navigation options.<br>
 * The result of the first selection is available to the application before the second selection is
 * executed.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card (here a card having two
 *       applications whose DF Names are prefixed by a specific AID [see AID_KEYPLE_PREFIX]).
 *   <li>Run an AID based application selection scenario (first occurrence).
 *   <li>Output collected smart card data (FCI and power-on data).
 *   <li>Run an AID based application selection scenario (next occurrence).
 *   <li>Output collected smart card data (FCI and power-on data).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_SequentialMultiSelection_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_SequentialMultiSelection_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the generic card extension service
    GenericExtensionService genericCardService = GenericExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(genericCardService);

    // Get the contactless reader whose name matches the provided regex
    CardReader cardReader = plugin.findReader(ConfigurationUtil.CONTACTLESS_READER_NAME_REGEX);

    // Configure the reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, cardReader.getName())
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ConfigurationUtil.ISO_CARD_PROTOCOL);

    logger.info(
        "=============== UseCase Generic #5: sequential selections based on an AID prefix ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", ConfigurationUtil.AID_KEYPLE_PREFIX);

    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    // AID based selection: get the first application occurrence matching the AID, keep the
    // physical channel open

    IsoCardSelector cardSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(ConfigurationUtil.AID_KEYPLE_PREFIX)
            .setFileOccurrence(CommonIsoCardSelector.FileOccurrence.FIRST);

    GenericCardSelectionExtension genericCardSelectionExtension =
        GenericExtensionService.getInstance().createGenericCardSelectionExtension();

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelector, genericCardSelectionExtension);

    // Do the selection and display the result
    doAndAnalyseSelection(cardReader, cardSelectionManager, 1);

    // New selection: get the next application occurrence matching the same AID, close the
    // physical channel after
    cardSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(ConfigurationUtil.AID_KEYPLE_PREFIX)
            .setFileOccurrence(CommonIsoCardSelector.FileOccurrence.NEXT);

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelector, genericCardSelectionExtension);

    // close the channel after the selection
    cardSelectionManager.prepareReleaseChannel();

    // Do the selection and display the result
    doAndAnalyseSelection(cardReader, cardSelectionManager, 2);

    logger.info("= #### End of the generic card processing.");

    System.exit(0);
  }

  /**
   * Performs the selection for the provided reader and logs its result.
   *
   * <p>The card selection manager must have been previously assigned a selection case.
   *
   * @param cardReader The reader.
   * @param cardSelectionsService The card selection manager.
   * @param index An int indicating the selection rank.
   */
  private static void doAndAnalyseSelection(
      CardReader cardReader, CardSelectionManager cardSelectionsService, int index) {
    CardSelectionResult cardSelectionsResult =
        cardSelectionsService.processCardSelectionScenario(cardReader);
    if (cardSelectionsResult.getActiveSmartCard() != null) {
      IsoSmartCard isoSmartCard = (IsoSmartCard) cardSelectionsResult.getActiveSmartCard();
      logger.info("The card matched the selection {}.", index);
      String powerOnData = isoSmartCard.getPowerOnData();
      String selectApplicationResponse = HexUtil.toHex(isoSmartCard.getSelectApplicationResponse());
      logger.info(
          "Selection status for case {}: \n\t\tpower-on data: {}\n\t\tSelect Application response: {}",
          index,
          powerOnData,
          selectApplicationResponse);
    } else {
      logger.info("The selection did not match for case {}.", index);
    }
  }
}

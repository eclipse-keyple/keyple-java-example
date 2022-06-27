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
package org.eclipse.keyple.core.service.example.UseCase6_GroupedMultiSelection;

import java.util.Map;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;
import org.eclipse.keyple.card.generic.GenericCardSelection;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 6 – Grouped selections based on an AID prefix (PC/SC)</h1>
 *
 * <p>We demonstrate here the selection of two applications in a single card, with both applications
 * selected using the same AID and the "FIRST" and "NEXT" navigation options but grouped in the same
 * selection process.<br>
 * Both selection results are available in the {@link CardSelectionResult} object returned by the
 * execution of the selection scenario.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card (here a card having two
 *       applications whose DF Names are prefixed by a specific AID [see AID_KEYPLE_PREFIX]).
 *   <li>Run a double AID based application selection scenario (first and next occurrence).
 *   <li>Output collected of all smart cards data (FCI and power-on data).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_GroupedMultiSelection_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_GroupedMultiSelection_Pcsc.class);

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
    String pcscContactlessReaderName =
        ConfigurationUtil.getCardReaderName(
            plugin, ConfigurationUtil.CONTACTLESS_READER_NAME_REGEX);
    CardReader cardReader = plugin.getReader(pcscContactlessReaderName);

    // Configure the reader with parameters suitable for contactless operations.
    ((PcscReader) cardReader)
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ConfigurationUtil.ISO_CARD_PROTOCOL);

    logger.info(
        "=============== UseCase Generic #6: Grouped selections based on an AID prefix ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", ConfigurationUtil.AID_KEYPLE_PREFIX);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();
    // Set the multiple selection mode
    cardSelectionManager.setMultipleSelectionMode();

    // First selection: get the first application occurrence matching the AID, keep the
    // physical channel open
    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(
        genericCardService
            .createCardSelection()
            .filterByDfName(ConfigurationUtil.AID_KEYPLE_PREFIX)
            .setFileOccurrence(GenericCardSelection.FileOccurrence.FIRST));

    // Second selection: get the next application occurrence matching the same AID, close the
    // physical channel after
    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(
        genericCardService
            .createCardSelection()
            .filterByDfName(ConfigurationUtil.AID_KEYPLE_PREFIX)
            .setFileOccurrence(GenericCardSelection.FileOccurrence.NEXT));

    // close the channel after the selection
    cardSelectionManager.prepareReleaseChannel();

    CardSelectionResult cardSelectionsResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // log the result
    for (Map.Entry<Integer, SmartCard> entry : cardSelectionsResult.getSmartCards().entrySet()) {
      SmartCard smartCard = entry.getValue();
      String powerOnData = smartCard.getPowerOnData();
      String selectApplicationResponse = HexUtil.toHex(smartCard.getSelectApplicationResponse());
      String selectionIsActive =
          smartCard == cardSelectionsResult.getActiveSmartCard() ? "true" : "false";
      logger.info(
          "Selection status for selection (indexed {}): \n\t\tActive smart card: {}\n\t\tpower-on data: {}\n\t\tSelect Application response: {}",
          entry.getKey(),
          selectionIsActive,
          powerOnData,
          selectApplicationResponse);
    }

    logger.info("= #### End of the generic card processing.");

    System.exit(0);
  }
}

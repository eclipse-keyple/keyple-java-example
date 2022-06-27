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
package org.eclipse.keyple.core.service.example.UseCase1_BasicSelection;

import java.util.List;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.calypsonet.terminal.reader.selection.spi.CardSelection;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;
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
 * <h1>Use Case Generic 1 – Basic Selection (PC/SC)</h1>
 *
 * <p>We demonstrate here a selection of cards without any condition related to the card itself. Any
 * card able to communicate with the reader must lead to a "selected" state.<br>
 * Note that in this case, no APDU "select application" is sent to the card.<br>
 * However, upon selection, an APDU command specific to Global Platform compliant cards is sent to
 * the card and may fail depending on the type of card presented.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card (a GlobalPlatform compliant
 *       card is expected here [e.g. EMV card or Javacard]).
 *   <li>Run a selection scenario without filter.
 *   <li>Output the collected smart card data (power-on data).
 *   <li>Send a additional APDUs to the card (get Card Production Life Cycle data [CPLC]).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_BasicSelection_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_BasicSelection_Pcsc.class);

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

    logger.info("=============== UseCase Generic #1: basic card selection ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      logger.error("No card is present in the reader.");
      System.exit(0);
    }

    logger.info("= #### Select the card with no conditions.");

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the generic card extension without specifying any filter
    // (protocol/power-on data/DFName).
    CardSelection cardSelection = genericCardService.createCardSelection();

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelection);

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the card failed.");
    }

    // Get the SmartCard resulting of the selection.
    SmartCard smartCard = selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", smartCard);

    // Execute an APDU to get CPLC Data (cf. Global Platform Specification)
    byte[] cplcApdu = HexUtil.toByteArray("80CA9F7F00");

    List<String> apduResponses =
        genericCardService
            .createCardTransaction(cardReader, smartCard)
            .prepareApdu(cplcApdu)
            .prepareReleaseChannel()
            .processApdusToHexStrings();

    logger.info("CPLC Data: '{}'", apduResponses.get(0));

    logger.info("= #### End of the generic card processing.");

    System.exit(0);
  }
}

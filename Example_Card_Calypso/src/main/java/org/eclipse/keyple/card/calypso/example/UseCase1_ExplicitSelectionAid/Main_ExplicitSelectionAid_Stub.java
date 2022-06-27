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
package org.eclipse.keyple.card.calypso.example.UseCase1_ExplicitSelectionAid;

import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.card.calypso.example.common.StubSmartCardFactory;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.stub.StubPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Calypso 1 – Explicit Selection Aid (Stub)</h1>
 *
 * <p>We demonstrate here the direct selection of a Calypso card inserted in a reader. No
 * observation of the reader is implemented in this example, so the card must be present in the
 * reader before the program is launched.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Checks if an ISO 14443-4 card is in the reader, enables the card selection manager.
 *   <li>Attempts to select the specified card (here a Calypso card characterized by its AID) with
 *       an AID-based application selection scenario, including reading a file record.
 *   <li>Output the collected data (FCI, ATR and file record content).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in a runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_ExplicitSelectionAid_Stub {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ExplicitSelectionAid_Stub.class);

  public static void main(String[] args) {
    final String CARD_READER_NAME = "Stub card reader";

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the StubPlugin with the SmartCardService, plug a Calypso card stub
    // get the corresponding generic plugin in return.
    Plugin plugin =
        smartCardService.registerPlugin(
            StubPluginFactoryBuilder.builder()
                .withStubReader(CARD_READER_NAME, true, StubSmartCardFactory.getStubCard())
                .build());

    CardReader cardReader = plugin.getReader(CARD_READER_NAME);

    ((ConfigurableCardReader) cardReader)
        .activateProtocol(ConfigurationUtil.ISO_CARD_PROTOCOL, ConfigurationUtil.ISO_CARD_PROTOCOL);

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    logger.info(
        "=============== UseCase Calypso #1: AID based explicit selection ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", CalypsoConstants.AID);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Prepare the selection by adding the created Calypso card selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(
        calypsoCardService
            .createCardSelection()
            .filterByDfName(CalypsoConstants.AID)
            .acceptInvalidatedCard()
            .prepareReadRecord(
                CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1));

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException(
          "The selection of the application '" + CalypsoConstants.AID + "' failed.");
    }

    // Get the SmartCard resulting of the selection.
    CalypsoCard calypsoCard = (CalypsoCard) selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoCard);

    logger.info(
        "Calypso Serial Number = {}", HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));

    logger.info(
        "File SFI {}h, rec 1: FILE_CONTENT = {}",
        String.format("%02X", CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER),
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

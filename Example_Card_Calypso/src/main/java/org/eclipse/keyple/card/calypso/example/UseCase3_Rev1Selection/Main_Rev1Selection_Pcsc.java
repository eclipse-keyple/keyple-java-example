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
package org.eclipse.keyple.card.calypso.example.UseCase3_Rev1Selection;

import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case ‘Calypso 3 – Selection a Calypso card Revision 1 (BPRIME protocol) (PC/SC)</h1>
 *
 * <p>We demonstrate here the direct selection of a Calypso card Revision 1 (Innovatron / B Prime
 * protocol) inserted in a reader. No observation of the reader is implemented in this example, so
 * the card must be present in the reader before the program is launched.
 *
 * <p>No AID is used here, the reading of the card data is done without any prior card selection
 * command as defined in the ISO standard.
 *
 * <p>The card selection (in the Keyple sense, i.e. retained to continue processing) is based on the
 * protocol.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO B Prime (Innovatron protocol) card is in the reader.
 *   <li>Send 2 additional APDUs to the card (one following the selection step, one after the
 *       selection, within a card transaction [without security here]).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_Rev1Selection_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_Rev1Selection_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding generic plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the contactless reader whose name matches the provided regex
    String pcscContactlessReaderName =
        ConfigurationUtil.getCardReaderName(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);
    CardReader cardReader = plugin.getReader(pcscContactlessReaderName);

    // Configure the reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, pcscContactlessReaderName)
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.INNOVATRON_B_PRIME_CARD.name(),
            ConfigurationUtil.INNOVATRON_CARD_PROTOCOL);

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    logger.info("=============== UseCase Calypso #3: selection of a rev1 card ==================");
    logger.info("= Card Reader  NAME = {}", cardReader.getName());

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select the card by its INNOVATRON protocol (no AID).");

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Prepare the selection by adding the created Calypso card selection to the card selection
    // scenario. No AID is defined, only the card protocol will be used to define the selection
    // case.
    cardSelectionManager.prepareSelection(
        calypsoCardService
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ConfigurationUtil.INNOVATRON_CARD_PROTOCOL)
            .prepareReadRecord(
                CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1));

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the B Prime card failed.");
    }

    // Get the SmartCard resulting of the selection.
    CalypsoCard calypsoCard = (CalypsoCard) selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoCard);

    String csn = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    logger.info("Calypso Serial Number = {}", csn);

    // Performs file reads using the card transaction manager in non-secure mode.

    calypsoCardService
        .createCardTransactionWithoutSecurity(cardReader, calypsoCard)
        .prepareReadRecord(CalypsoConstants.SFI_EVENT_LOG, 1)
        .prepareReleaseCardChannel()
        .processCommands();

    String sfiEnvHolder = HexUtil.toHex(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER);
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        sfiEnvHolder,
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));

    String sfiEventLog = HexUtil.toHex(CalypsoConstants.SFI_EVENT_LOG);
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        sfiEventLog,
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_EVENT_LOG));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

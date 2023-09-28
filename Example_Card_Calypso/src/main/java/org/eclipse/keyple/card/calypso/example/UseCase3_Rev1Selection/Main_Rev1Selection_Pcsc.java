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

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
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
 */
public class Main_Rev1Selection_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_Rev1Selection_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding generic plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the contactless reader whose name matches the provided regex
    CardReader cardReader =
        ConfigurationUtil.getCardReader(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);

    // Activate Innovatron protocol.
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

    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    CardSelector<IsoCardSelector> cardSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByCardProtocol(ConfigurationUtil.INNOVATRON_CARD_PROTOCOL);

    CalypsoCardApiFactory calypsoCardApiFactory = calypsoCardService.getCalypsoCardApiFactory();

    // Create a card selection using the Calypso card extension.
    // Prepare the selection by adding the created Calypso card selection to the card selection
    // scenario. No AID is defined, only the card protocol will be used to define the selection
    // case.
    cardSelectionManager.prepareSelection(
        cardSelector,
        calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()
            .acceptInvalidatedCard()
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

    calypsoCardApiFactory
        .createFreeTransactionManager(cardReader, calypsoCard)
        .prepareReadRecord(CalypsoConstants.SFI_EVENT_LOG, 1)
        .processCommands(ChannelControl.CLOSE_AFTER);

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

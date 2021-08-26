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

import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.getCardReader;

import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
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
 * <p>The card selection (in the Keyple sensein the Keyple sense, i.e. retained to continue
 * processing) is based on the protocol.
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

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    Reader cardReader = getCardReader(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);

    ((ConfigurableReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.INNOVATRON_B_PRIME_CARD.name(),
            ContactlessCardCommonProtocol.INNOVATRON_B_PRIME_CARD.name());

    // Get the Calypso card extension service
    CalypsoExtensionService cardExtension = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

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
        cardExtension
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ContactlessCardCommonProtocol.INNOVATRON_B_PRIME_CARD.name())
            .prepareReadRecordFile(
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

    // Performs file reads using the card transaction manager in non-secure mode.

    cardExtension
        .createCardTransactionWithoutSecurity(cardReader, calypsoCard)
        .prepareReadRecordFile(CalypsoConstants.SFI_EVENT_LOG, 1)
        .prepareReleaseCardChannel()
        .processCardCommands();

    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        String.format("%02X", CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER),
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        String.format("%02X", CalypsoConstants.SFI_EVENT_LOG),
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_EVENT_LOG));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

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
package org.eclipse.keyple.card.calypso.example.UseCase8_StoredValue_DebitInSession;

import static org.eclipse.keypop.calypso.card.WriteAccessLevel.DEBIT;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.*;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
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
 * <h1>Use Case Calypso 4 – Calypso Card authentication (PC/SC)</h1>
 *
 * <p>We demonstrate here the debit of the Stored Value counter of a Calypso card.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Checks if an ISO 14443-4 card is in the reader, enables the card selection manager.
 *   <li>Attempts to select a Calypso SAM (C1) in the contact reader.
 *   <li>Attempts to select the specified card (here a Calypso card characterized by its AID) with
 *       an AID-based application selection scenario.
 *   <li>Creates a {@link CardTransactionManager} using {@link CardSecuritySetting} referencing the
 *       selected SAM.
 *   <li>Displays the Stored Value status, debits the Store Value within a Secure Session.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_StoredValue_DebitInSession_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_StoredValue_DebitInSession_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding generic plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Get the card and SAM readers whose name matches the provided regexs
    CardReader cardReader =
        ConfigurationUtil.getCardReader(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);
    CardReader samReader =
        ConfigurationUtil.getSamReader(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX);

    logger.info("=============== UseCase Calypso #8: Stored Value debit ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    // Get the Calypso SAM SmartCard after selection.
    LegacySam sam = ConfigurationUtil.getSam(samReader);

    logger.info("= SAM = {}", sam);

    logger.info("= #### Select application with AID = '{}'.", CalypsoConstants.AID);

    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    CardSelector<IsoCardSelector> cardSelector =
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoConstants.AID);

    CalypsoCardApiFactory calypsoCardApiFactory = calypsoCardService.getCalypsoCardApiFactory();

    // Create a card selection using the Calypso card extension.
    // Prepare the selection by adding the created Calypso card selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(
        cardSelector,
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException(
          "The selection of the application " + CalypsoConstants.AID + " failed.");
    }

    // Get the SmartCard resulting of the selection.
    CalypsoCard calypsoCard = (CalypsoCard) selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoCard);

    String csn = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    logger.info("Calypso Serial Number = {}", csn);

    // Create security settings that reference the SAM
    SymmetricCryptoSecuritySetting cardSecuritySetting =
        calypsoCardApiFactory.createSymmetricCryptoSecuritySetting(
            LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createSymmetricCryptoTransactionManagerFactory(samReader, sam));

    // Performs file reads using the card transaction manager in non-secure mode.
    SecureRegularModeTransactionManager cardTransaction =
        calypsoCardApiFactory
            .createSecureRegularModeTransactionManager(cardReader, calypsoCard, cardSecuritySetting)
            .prepareOpenSecureSession(DEBIT)
            .prepareSvGet(SvOperation.DEBIT, SvAction.DO)
            .processCommands(ChannelControl.KEEP_OPEN);

    // Display the current SV status
    logger.info("Current SV status (SV Get for DEBIT):");
    logger.info(". Balance = {}", calypsoCard.getSvBalance());
    logger.info(". Last Transaction Number = {}", calypsoCard.getSvLastTNum());

    // Display the load and debit records.
    logger.info(". Load log record = {}", calypsoCard.getSvLoadLogRecord());
    logger.info(". Debit log record = {}", calypsoCard.getSvDebitLogLastRecord());

    // Prepare an SV Debit of 2 units
    cardTransaction
        .prepareSvDebit(2)
        .prepareCloseSecureSession()
        .processCommands(ChannelControl.CLOSE_AFTER);

    logger.info(
        "The Secure Session ended successfully, the stored value has been debited by 2 units.");

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

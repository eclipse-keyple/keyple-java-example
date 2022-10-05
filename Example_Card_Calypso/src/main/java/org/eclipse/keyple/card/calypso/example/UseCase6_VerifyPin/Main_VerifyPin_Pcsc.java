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
package org.eclipse.keyple.card.calypso.example.UseCase6_VerifyPin;

import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
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
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Calypso 6 – Calypso Card Verify PIN (PC/SC)</h1>
 *
 * <p>We demonstrate here the various operations around the PIN code checking.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Checks if an ISO 14443-4 card is in the reader, enables the card selection manager.
 *   <li>Attempts to select a Calypso SAM (C1) in the contact reader.
 *   <li>Attempts to select the specified card (here a Calypso card characterized by its AID) with
 *       an AID-based application selection scenario.
 *   <li>Creates a {@link CardTransactionManager} without security.
 *   <li>Verify the PIN code in plain mode with the correct code, display the remaining attempts
 *       counter.
 *   <li>Creates a {@link CardTransactionManager} using {@link CardSecuritySetting} referencing the
 *       selected SAM.
 *   <li>Verify the PIN code in session in encrypted mode with the code, display the remaining
 *       attempts counter.
 *   <li>Verify the PIN code in session in encrypted mode with a bad code, display the remaining
 *       attempts counter.
 *   <li>Cancel the card transaction, re-open a new one.
 *   <li>Verify the PIN code in session in encrypted mode with the code, display the remaining
 *       attempts counter.
 *   <li>Close the card transaction.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_VerifyPin_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_VerifyPin_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding generic plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

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
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ConfigurationUtil.ISO_CARD_PROTOCOL);

    // Get the contact reader dedicated for Calypso SAM whose name matches the provided regex
    String pcscContactReaderName =
        ConfigurationUtil.getCardReaderName(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX);
    CardReader samReader = plugin.getReader(pcscContactReaderName);

    // Configure the Calypso SAM reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, pcscContactReaderName)
        .setContactless(false)
        .setIsoProtocol(PcscReader.IsoProtocol.T0)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) samReader)
        .activateProtocol(
            PcscSupportedContactProtocol.ISO_7816_3_T0.name(), ConfigurationUtil.SAM_PROTOCOL);

    logger.info("=============== UseCase Calypso #5: Calypso card Verify PIN ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager = smartCardService.createCardSelectionManager();

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(calypsoCardService.createSamSelection());

    // SAM communication: run the selection scenario.
    CardSelectionResult samSelectionResult =
        samSelectionManager.processCardSelectionScenario(samReader);

    // Check the selection result.
    if (samSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Get the Calypso SAM SmartCard resulting of the selection.
    CalypsoSam calypsoSam = (CalypsoSam) samSelectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoSam);

    logger.info("= #### Select application with AID = '{}'.", CalypsoConstants.AID);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Prepare the selection by adding the created Calypso card selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(
        calypsoCardService
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByDfName(CalypsoConstants.AID));

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
    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setControlSamResource(samReader, calypsoSam)
            .setPinVerificationCipheringKey(
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KIF,
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KVC);

    // Create the card transaction manager in secure mode.
    CardTransactionManager cardTransaction =
        calypsoCardService.createCardTransactionWithoutSecurity(cardReader, calypsoCard);

    ////////////////////////////
    // Verification of the PIN (correct) out of a secure session in plain mode
    cardTransaction.processVerifyPin(CalypsoConstants.PIN_OK);
    logger.info("Remaining attempts #1: {}", calypsoCard.getPinAttemptRemaining());

    // create a secured card transaction
    cardTransaction =
        calypsoCardService.createCardTransaction(cardReader, calypsoCard, cardSecuritySetting);

    ////////////////////////////
    // Verification of the PIN (correct) out of a secure session in encrypted mode
    cardTransaction.processVerifyPin(CalypsoConstants.PIN_OK);
    // log the current counter value (should be 3)
    logger.info("Remaining attempts #2: {}", calypsoCard.getPinAttemptRemaining());

    ////////////////////////////
    // Verification of the PIN (incorrect) inside a secure session
    cardTransaction.processOpening(WriteAccessLevel.DEBIT);
    try {
      cardTransaction.processVerifyPin(CalypsoConstants.PIN_KO);
    } catch (Exception ex) {
      logger.error("PIN Exception: {}", ex.getMessage());
    }
    // log the current counter value (should be 2)
    logger.error("Remaining attempts #3: {}", calypsoCard.getPinAttemptRemaining());

    ////////////////////////////
    // Verification of the PIN (correct) inside a secure session with reading of the counter
    //////////////////////////// before
    cardTransaction.prepareCheckPinStatus();
    cardTransaction.processOpening(WriteAccessLevel.DEBIT);
    // log the current counter value (should be 2)
    logger.info("Remaining attempts #4: {}", calypsoCard.getPinAttemptRemaining());
    cardTransaction.processVerifyPin(CalypsoConstants.PIN_OK);
    cardTransaction.prepareReleaseCardChannel();
    cardTransaction.processClosing();

    // log the current counter value (should be 3)
    logger.info("Remaining attempts #5: {}", calypsoCard.getPinAttemptRemaining());

    logger.info("The Secure Session ended successfully, the PIN has been verified.");

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

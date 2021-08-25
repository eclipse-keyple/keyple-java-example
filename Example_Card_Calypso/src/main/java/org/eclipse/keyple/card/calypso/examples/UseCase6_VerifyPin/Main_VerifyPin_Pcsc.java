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
package org.eclipse.keyple.card.calypso.examples.UseCase6_VerifyPin;

import static org.eclipse.keyple.card.calypso.examples.common.ConfigurationUtil.getCardReader;
import static org.eclipse.keyple.card.calypso.examples.common.ConfigurationUtil.setupCardResourceService;

import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionException;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.examples.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.examples.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
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
 *   <li>Sets up the card resource service to provide a Calypso SAM (C1).
 *   <li>Checks if an ISO 14443-4 card is in the reader, enables the card selection manager.
 *   <li>Attempts to select the specified card (here a Calypso card characterized by its AID) with
 *       an AID-based application selection scenario.
 *   <li>Creates a {@link CardTransactionManager} without security.
 *   <li>Verify the PIN code in plain mode with the correct code, display the remaining attempts
 *       counter.
 *   <li>Creates a {@link CardTransactionManager} using {@link CardSecuritySetting} referencing the
 *       SAM profile defined in the card resource service.
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

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService cardExtension = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    // Get and setup the card reader
    // We suppose here, we use a ASK LoGO contactless PC/SC reader as card reader.
    Reader cardReader = getCardReader(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);

    // Configure the card resource service to provide an adequate SAM for future secure operations.
    // We suppose here, we use a Identive contact PC/SC reader as card reader.
    setupCardResourceService(
        plugin, ConfigurationUtil.SAM_READER_NAME_REGEX, CalypsoConstants.SAM_PROFILE_NAME);

    logger.info("=============== UseCase Calypso #5: Calypso card Verify PIN ==================");

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
        cardExtension
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

    // Create the card transaction manager in secure mode.
    CardTransactionManager cardTransaction =
        cardExtension.createCardTransactionWithoutSecurity(cardReader, calypsoCard);

    ////////////////////////////
    // Verification of the PIN (correct) out of a secure session in plain mode
    cardTransaction.processVerifyPin(CalypsoConstants.PIN_OK);
    logger.info("Remaining attempts #1: {}", calypsoCard.getPinAttemptRemaining());

    // Create security settings that reference the same SAM profile requested from the card resource
    // service, specifying the key ciphering key parameters.
    CardResource samResource =
        CardResourceServiceProvider.getService().getCardResource(CalypsoConstants.SAM_PROFILE_NAME);
    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setSamResource(samResource.getReader(), (CalypsoSam) samResource.getSmartCard())
            .setPinVerificationCipheringKey(
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KIF,
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KVC);

    try {
      // create a secured card transaction
      cardTransaction =
          cardExtension.createCardTransaction(cardReader, calypsoCard, cardSecuritySetting);

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
      } catch (CardTransactionException ex) {
        logger.error("PIN Exception: {}", ex.getMessage());
      }
      cardTransaction.processCancel();
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
    } finally {
      try {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      } catch (RuntimeException e) {
        logger.error("Error during the card resource release: {}", e.getMessage(), e);
      }
    }

    // log the current counter value (should be 3)
    logger.info("Remaining attempts #5: {}", calypsoCard.getPinAttemptRemaining());

    logger.info("The Secure Session ended successfully, the PIN has been verified.");

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

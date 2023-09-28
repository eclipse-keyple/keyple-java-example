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
package org.eclipse.keyple.card.calypso.example.UseCase9_ChangePin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
 * <h1>Use Case Calypso 9 – Calypso Card Change PIN (PC/SC)</h1>
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
 *   <li>Creates a {@link SecureRegularModeTransactionManager} using {@link
 *       SymmetricCryptoSecuritySetting} referencing the selected SAM.
 *   <li>Ask for the new PIN code.
 *   <li>Change the PIN code.
 *   <li>Verify the PIN code.
 *   <li>Close the card transaction.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_ChangePin_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_ChangePin_Pcsc.class);

  public static void main(String[] args) throws Exception {

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

    logger.info("=============== UseCase Calypso #5: Calypso card Verify PIN ==================");

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
        calypsoCardApiFactory
            .createSymmetricCryptoSecuritySetting(
                LegacySamExtensionService.getInstance()
                    .getLegacySamApiFactory()
                    .createSymmetricCryptoTransactionManagerFactory(samReader, sam))
            .setPinVerificationCipheringKey(
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KIF,
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KVC)
            .setPinModificationCipheringKey(
                CalypsoConstants.PIN_MODIFICATION_CIPHERING_KEY_KIF,
                CalypsoConstants.PIN_MODIFICATION_CIPHERING_KEY_KVC);

    // Create the card transaction manager in secure mode.
    SecureRegularModeTransactionManager cardTransaction =
        calypsoCardApiFactory.createSecureRegularModeTransactionManager(
            cardReader, calypsoCard, cardSecuritySetting);

    // Short delay to allow logs to be displayed before the prompt
    Thread.sleep(2000);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String inputString;
    byte[] newPinCode = new byte[4];
    boolean validPinCodeEntered = false;
    do {
      System.out.print("Enter new PIN code (4 numeric digits):");
      inputString = br.readLine();
      if (!inputString.matches("[0-9]{4}")) {
        System.out.println("Invalid PIN code.");
      } else {
        newPinCode = inputString.getBytes();
        validPinCodeEntered = true;
      }
    } while (!validPinCodeEntered);

    ////////////////////////////
    // Change the PIN (correct)
    cardTransaction.prepareChangePin(newPinCode).processCommands(ChannelControl.KEEP_OPEN);

    logger.info("PIN code value successfully updated to {}", inputString);

    ////////////////////////////
    // Verification of the PIN
    cardTransaction.prepareVerifyPin(newPinCode).processCommands(ChannelControl.CLOSE_AFTER);
    logger.info("Remaining attempts: {}", calypsoCard.getPinAttemptRemaining());

    logger.info("PIN {} code successfully presented.", inputString);

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

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
 *   <li>Creates a {@link CardTransactionManager} using {@link CardSecuritySetting} referencing the
 *       selected SAM.
 *   <li>Ask for the new PIN code.
 *   <li>Change the PIN code.
 *   <li>Verify the PIN code.
 *   <li>Close the card transaction.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
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
                CalypsoConstants.PIN_VERIFICATION_CIPHERING_KEY_KVC)
            .setPinModificationCipheringKey(
                CalypsoConstants.PIN_MODIFICATION_CIPHERING_KEY_KIF,
                CalypsoConstants.PIN_MODIFICATION_CIPHERING_KEY_KVC);

    // Create the card transaction manager in secure mode.
    CardTransactionManager cardTransaction =
        calypsoCardService.createCardTransaction(cardReader, calypsoCard, cardSecuritySetting);

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
    cardTransaction.processChangePin(newPinCode);

    logger.info("PIN code value successfully updated to {}", inputString);

    ////////////////////////////
    // Verification of the PIN
    cardTransaction.processVerifyPin(newPinCode);
    logger.info("Remaining attempts: {}", calypsoCard.getPinAttemptRemaining());

    logger.info("PIN {} code successfully presented.", inputString);

    cardTransaction.prepareReleaseCardChannel();

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

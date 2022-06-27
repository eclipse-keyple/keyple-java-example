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
package org.eclipse.keyple.card.calypso.example.UseCase4_CardAuthentication;

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
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
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
 * <h1>Use Case Calypso 4 – Calypso Card authentication (PC/SC)</h1>
 *
 * <p>We demonstrate here the authentication of a Calypso card using a Secure Session in which a
 * file from the card is read. The read is certified by verifying the signature of the card by a
 * Calypso SAM.
 *
 * <p>Two readers are required for this example: a contactless reader for the Calypso Card, a
 * contact reader for the Calypso SAM.
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
 *   <li>Read a file record in Secure Session.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_CardAuthentication_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_CardAuthentication_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Get the contactless reader whose name matches the provided regex
    String pcscContactlessCardReaderName =
        ConfigurationUtil.getCardReaderName(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);
    CardReader calypsoCardReader = plugin.getReader(pcscContactlessCardReaderName);

    // Configure the reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, pcscContactlessCardReaderName)
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);

    ((ConfigurableCardReader) calypsoCardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ConfigurationUtil.ISO_CARD_PROTOCOL);

    // Get the contact reader dedicated for Calypso SAM whose name matches the provided regex
    String pcscContactSamReaderName =
        ConfigurationUtil.getCardReaderName(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX);
    CardReader calypsoSamReader = plugin.getReader(pcscContactSamReaderName);

    // Configure the Calypso SAM reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, pcscContactSamReaderName)
        .setContactless(false)
        .setIsoProtocol(PcscReader.IsoProtocol.T0)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) calypsoSamReader)
        .activateProtocol(
            PcscSupportedContactProtocol.ISO_7816_3_T0.name(), ConfigurationUtil.SAM_PROTOCOL);

    logger.info(
        "=============== UseCase Calypso #4: Calypso card authentication ==================");

    // Check if a card is present in the reader
    if (!calypsoCardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", CalypsoConstants.AID);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a SAM selection using the Calypso card extension.
    cardSelectionManager.prepareSelection(calypsoCardService.createSamSelection());

    // SAM communication: run the selection scenario.
    CardSelectionResult samSelectionResult =
        cardSelectionManager.processCardSelectionScenario(calypsoSamReader);

    // Check the selection result.
    if (samSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Get the Calypso SAM SmartCard resulting of the selection.
    CalypsoSam calypsoSam = (CalypsoSam) samSelectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoSam);

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
        cardSelectionManager.processCardSelectionScenario(calypsoCardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException(
          "The selection of the application " + CalypsoConstants.AID + " failed.");
    }

    // Get the SmartCard resulting of the selection.
    CalypsoCard calypsoCard = (CalypsoCard) selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoCard);

    logger.info(
        "Calypso Serial Number = {}", HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));

    // Create security settings that reference the SAM
    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setControlSamResource(calypsoSamReader, calypsoSam);

    try {
      // Performs file reads using the card transaction manager in secure mode.
      calypsoCardService
          .createCardTransaction(calypsoCardReader, calypsoCard, cardSecuritySetting)
          .prepareReadRecords(
              CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER,
              CalypsoConstants.RECORD_NUMBER_1,
              CalypsoConstants.RECORD_NUMBER_1,
              CalypsoConstants.RECORD_SIZE)
          .processOpening(WriteAccessLevel.DEBIT)
          .prepareReleaseCardChannel()
          .processClosing();
    } finally {
      //      try {
      //        calypsoCardService
      //            .createSamTransaction(
      //                calypsoSamReader, calypsoSam, calypsoCardService.createSamSecuritySetting())
      //            .prepareReleaseCardChannel()
      //            .processCommands();
      //      } catch (RuntimeException e) {
      //        logger.error("Error during the card resource release: {}", e.getMessage(), e);
      //      }
    }

    logger.info(
        "The Secure Session ended successfully, the card is authenticated and the data read are certified.");
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        String.format("%02X", CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER),
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

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

import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.setupCardResourceService;

import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
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
 *   <li>Sets up the card resource service to provide a Calypso SAM (C1).
 *   <li>Checks if an ISO 14443-4 card is in the reader, enables the card selection manager.
 *   <li>Attempts to select the specified card (here a Calypso card characterized by its AID) with
 *       an AID-based application selection scenario.
 *   <li>Creates a {@link CardTransactionManager} using {@link CardSecuritySetting} referencing the
 *       SAM profile defined in the card resource service.
 *   <li>Read a file record in Secure Session.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_CardAuthentication_Pcsc_SamResourceService {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_CardAuthentication_Pcsc_SamResourceService.class);

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
    CardReader cardReader =
        ConfigurationUtil.getCardReader(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);

    // Configure the card resource service to provide an adequate SAM for future secure operations.
    // We suppose here, we use an Identive contact PC/SC reader as card reader.
    setupCardResourceService(
        plugin, ConfigurationUtil.SAM_READER_NAME_REGEX, CalypsoConstants.SAM_PROFILE_NAME);

    logger.info(
        "=============== UseCase Calypso #4: Calypso card authentication ==================");

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

    // Create security settings that reference the same SAM profile requested from the card resource
    // service.
    CardResource samResource =
        CardResourceServiceProvider.getService().getCardResource(CalypsoConstants.SAM_PROFILE_NAME);
    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setControlSamResource(
                samResource.getReader(), (CalypsoSam) samResource.getSmartCard());

    try {
      // Performs file reads using the card transaction manager in secure mode.
      calypsoCardService
          .createCardTransaction(cardReader, calypsoCard, cardSecuritySetting)
          .prepareReadRecords(
              CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER,
              CalypsoConstants.RECORD_NUMBER_1,
              CalypsoConstants.RECORD_NUMBER_1,
              CalypsoConstants.RECORD_SIZE)
          .processOpening(WriteAccessLevel.DEBIT)
          .prepareReleaseCardChannel()
          .processClosing();
    } finally {
      try {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      } catch (RuntimeException e) {
        logger.error("Error during the card resource release: {}", e.getMessage(), e);
      }
    }

    logger.info(
        "The Secure Session ended successfully, the card is authenticated and the data read are certified.");

    String sfiEnvHolder = HexUtil.toHex(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER);
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        sfiEnvHolder,
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }
}

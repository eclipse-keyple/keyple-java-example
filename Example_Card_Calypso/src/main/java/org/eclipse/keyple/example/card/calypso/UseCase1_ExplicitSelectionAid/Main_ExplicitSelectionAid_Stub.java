/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Distribution License 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ************************************************************************************** */
package org.eclipse.keyple.example.card.calypso.UseCase1_ExplicitSelectionAid;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.example.card.calypso.common.StubSmartCardFactory;
import org.eclipse.keyple.plugin.stub.StubPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the process of explicit selection of a Calypso card using the Stub plugin, without
 * implementing the observation of the reader. Ensure the Calypso card is inserted before launching
 * the program.
 *
 * <p>This class demonstrates the explicit selection of a Calypso card, including the initialization
 * of the card selection manager after verifying the presence of an ISO 14443-4 card in the reader,
 * and the attempt to select the specified Calypso card characterized by its AID.
 *
 * <p>It also demonstrates how to retrieve and output collected data such as serial number and file
 * record content after the selection scenario based on AID.
 *
 * <h2>Key Functionalities</h2>
 *
 * <ul>
 *   <li>Check for an ISO 14443-4 card in the reader and enable the card selection manager.
 *   <li>Attempt to select a specified Calypso card using AID-based application selection scenario.
 *   <li>Read and output the collected data including Calypso serial number and file record content.
 * </ul>
 *
 * <p>All operations and results are logged using slf4j for tracking and debugging. In the case of
 * unexpected behavior, a runtime exception is thrown.
 */
public class Main_ExplicitSelectionAid_Stub {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ExplicitSelectionAid_Stub.class);

  static final String CARD_READER_NAME = "Stub card reader";

  /** AID: Keyple test kit profile 1, Application 2 */
  private static final String AID = "315449432E49434131";

  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;

  // The plugin used to manage the reader.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader cardReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static CalypsoCardApiFactory calypsoCardApiFactory;

  public static void main(String[] args) {
    logger.info("= UseCase Calypso #1: AID based explicit selection ==================");

    // Initialize the context
    initKeypleService();
    initCardReader();
    initCalypsoCardExtensionService();

    logger.info(
        "=============== UseCase Calypso #1: AID based explicit selection ==================");

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", AID);

    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    IsoCardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(AID);
    CalypsoCardSelectionExtension calypsoCardSelectionExtension =
        calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()
            .acceptInvalidatedCard()
            .prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, 1);
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the application '" + AID + "' failed.");
    }

    // Get the SmartCard resulting of the selection.
    CalypsoCard calypsoCard = (CalypsoCard) selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", calypsoCard);

    String csn = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    logger.info("Calypso Serial Number = {}", csn);

    String sfiEnvHolder = HexUtil.toHex(SFI_ENVIRONMENT_AND_HOLDER);
    logger.info(
        "File SFI {}h, rec 1: FILE_CONTENT = {}",
        sfiEnvHolder,
        calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER));

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }

  /**
   * Initializes the Keyple service.
   *
   * <p>Gets an instance of the smart card service, registers the Stub plugin, and prepares the
   * reader API factory for use.
   *
   * <p>Retrieves the {@link ReaderApiFactory}.
   */
  private static void initKeypleService() {
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    // Register the StubPlugin with the SmartCardService and plug in stubs for a Calypso card.
    plugin =
        smartCardService.registerPlugin(
            StubPluginFactoryBuilder.builder()
                .withStubReader(CARD_READER_NAME, true, StubSmartCardFactory.getStubCard())
                .build());
    readerApiFactory = smartCardService.getReaderApiFactory();
  }

  /**
   * Initializes the card reader with specific configurations.
   *
   * <p>Prepares the card reader using a predefined set of configurations, including the card reader
   * name regex, ISO protocol, and sharing mode.
   */
  private static void initCardReader() {
    cardReader = plugin.getReader(CARD_READER_NAME);
  }

  /**
   * Initializes the Calypso card extension service.
   *
   * <p>Retrieves the {@link CalypsoCardApiFactory}.
   */
  private static void initCalypsoCardExtensionService() {
    CalypsoExtensionService calypsoExtensionService = CalypsoExtensionService.getInstance();
    SmartCardServiceProvider.getService().checkCardExtension(calypsoExtensionService);
    calypsoCardApiFactory = calypsoExtensionService.getCalypsoCardApiFactory();
  }
}

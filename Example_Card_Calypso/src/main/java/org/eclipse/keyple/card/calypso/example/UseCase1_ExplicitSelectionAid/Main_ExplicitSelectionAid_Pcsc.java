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
package org.eclipse.keyple.card.calypso.example.UseCase1_ExplicitSelectionAid;

import java.text.ParseException;
import java.util.Properties;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the process of explicit selection of a Calypso card using the PC/SC plugin, without
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
public class Main_ExplicitSelectionAid_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ExplicitSelectionAid_Pcsc.class);

  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";

  // Read the configuration to get the AID to use
  private static final String AID = getAidFromConfiguration();

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

  public static void main(String[] args) throws ParseException {

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
   * <p>Gets an instance of the smart card service, registers the PC/SC plugin, and prepares the
   * reader API factory for use.
   *
   * <p>Retrieves the {@link ReaderApiFactory}.
   */
  private static void initKeypleService() {
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());
    readerApiFactory = smartCardService.getReaderApiFactory();
  }

  /**
   * Initializes the card reader with specific configurations.
   *
   * <p>Prepares the card reader using a predefined set of configurations, including the card reader
   * name regex, ISO protocol, and sharing mode.
   */
  private static void initCardReader() {
    cardReader =
        getReader(
            plugin,
            CARD_READER_NAME_REGEX,
            true,
            PcscReader.IsoProtocol.T1,
            PcscReader.SharingMode.SHARED,
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ISO_CARD_PROTOCOL);
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

  /**
   * Configures and returns a card reader based on the provided parameters.
   *
   * <p>It finds the reader name by matching with a regular expression, then configures the reader
   * with the specified settings.
   *
   * @param plugin The plugin used to interact with the card reader.
   * @param readerNameRegex The regular expression to match the card reader's name.
   * @param isContactless A boolean indicating whether the card reader is contactless.
   * @param isoProtocol The ISO protocol used by the card reader.
   * @param sharingMode The sharing mode of the PC/SC reader.
   * @param physicalProtocolName The name of the protocol used by the reader to communicate with
   *     card.
   * @param logicalProtocolName The name of the protocol known by the application.
   * @return The configured card reader.
   */
  private static CardReader getReader(
      Plugin plugin,
      String readerNameRegex,
      boolean isContactless,
      PcscReader.IsoProtocol isoProtocol,
      PcscReader.SharingMode sharingMode,
      String physicalProtocolName,
      String logicalProtocolName) {
    CardReader reader = plugin.findReader(readerNameRegex);

    plugin
        .getReaderExtension(PcscReader.class, reader.getName())
        .setContactless(isContactless)
        .setIsoProtocol(isoProtocol)
        .setSharingMode(sharingMode);

    ((ConfigurableCardReader) reader).activateProtocol(physicalProtocolName, logicalProtocolName);

    return reader;
  }

  /**
   * Retrieves the "aid" property value from the configuration file.
   *
   * @return The value of the "aid" property if present; otherwise, null if the property is not
   *     found or an exception occurs during the file loading process.
   */
  static String getAidFromConfiguration() {
    try {
      Properties props = new Properties();
      props.load(
          Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
      return props.getProperty("aid");
    } catch (Exception e) {
      return null;
    }
  }
}

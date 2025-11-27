/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.example.card.calypso.UseCase10_SessionTrace_TN313;

import static org.eclipse.keypop.calypso.card.WriteAccessLevel.*;

import java.util.Scanner;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.*;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.transaction.*;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

/**
 * Handles the execution of Calypso Secure Session Trace as defined in Technical Note #313 (PC/SC)
 * using an observable reader and Calypso card extension service.
 *
 * <p>This class provides a comprehensive demonstration of the Calypso Secure Session established
 * with a specific Calypso card characterized by its AID. The session includes card selection, SAM
 * selection, and execution of the TN313 transaction scenario, ending by the closing of the physical
 * channel.
 *
 * <h2>Key Functionalities</h2>
 *
 * <ul>
 *   <li>Scheduling a selection scenario over an observable reader for a specific Calypso card AID,
 *       including reading a file record.
 *   <li>Attempting to select a Calypso SAM (C1) in the contact reader.
 *   <li>Starting the observation process and waiting for a card insertion.
 *   <li>Executing the TN313 transaction scenario within the reader event handler and closing the
 *       physical channel.
 * </ul>
 *
 * <p>Each step in the secure session is logged meticulously for monitoring, tracking, and debugging
 * purposes. In the event of unexpected behaviors or failures, runtime exceptions are thrown,
 * providing clear indications of issues encountered during the session’s execution.
 *
 * <p>Throws IllegalStateException if an error occurs during the card authentication or secure
 * session establishment, ensuring robust error management and security adherence.
 */
public class Main_SessionTrace_TN313_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_SessionTrace_TN313_Pcsc.class);

  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // A regular expression for matching common SAM readers. Adapt as needed.
  private static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  // The logical name of the protocol for communicating with the SAM (optional).
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";
  private static String cardReaderRegex = CARD_READER_NAME_REGEX;
  private static String samReaderRegex = SAM_READER_NAME_REGEX;

  /** AID: Keyple test kit profile 1, Application 2 */
  private static final String AID = "315449432E49434131";

  private static String cardAid = AID;
  private static boolean isVerbose;

  // The plugin used to manage the readers.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader cardReader;
  // The reader used to communicate with the SAM.
  private static CardReader samReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static CalypsoCardApiFactory calypsoCardApiFactory;
  // The security settings for the card transaction.
  private static SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting;

  public static void main(String[] args) {

    parseCommandLine(args);

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, isVerbose ? "TRACE" : "INFO");

    logger.info("=============== UseCase Calypso #10: session trace TN313 ==================");
    logger.info("Using parameters:");
    logger.info("  AID={}", cardAid);
    logger.info("  CARD_READER_REGEX={}", cardReaderRegex);
    logger.info("  SAM_READER_REGEX={}", samReaderRegex);

    // Initialize the context.
    initKeypleService();
    initCalypsoCardExtensionService();
    initCardReader();
    initSamReader();
    initSecuritySetting();

    logger.info("Select application with AID = '{}'", cardAid);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    IsoCardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(cardAid);

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    // Prepare the selection by adding the created Calypso selection to the card selection scenario.
    cardSelectionManager.prepareSelection(
        cardSelector,
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    // Schedule the selection scenario, request notification only if the card matches the selection
    // case.
    cardSelectionManager.scheduleCardSelectionScenario(
        (ObservableCardReader) cardReader, ObservableCardReader.NotificationMode.MATCHED_ONLY);

    // Create and add a card observer for this reader
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(
            plugin, cardReader, cardSelectionManager, symmetricCryptoSecuritySetting);

    ((ObservableCardReader) cardReader).setReaderObservationExceptionHandler(cardReaderObserver);
    ((ObservableCardReader) cardReader).addObserver(cardReaderObserver);
    ((ObservableCardReader) cardReader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info("Wait for a card...");

    Scanner sc = new Scanner(System.in);
    logger.info("Press ENTER to exit...");
    sc.nextLine();
    logger.info("Exit in progress...");

    // unregister plugin
    SmartCardServiceProvider.getService().unregisterPlugin(plugin.getName());

    logger.info("Exit program");

    System.exit(0);
  }

  /**
   * Analyses the command line and sets the specified parameters.
   *
   * @param args The command line arguments
   */
  private static void parseCommandLine(String[] args) {
    if (args.length == 0) {
      displayUsageAndExit();
      return;
    }

    for (String arg : args) {
      if (isDefaultArgument(arg)) {
        break;
      }

      if (isVerboseArgument(arg)) {
        isVerbose = true;
      } else {
        parseAdditionalArguments(arg);
      }
    }
  }

  private static boolean isDefaultArgument(String arg) {
    return arg.equals("-d") || arg.equals("--default");
  }

  private static boolean isVerboseArgument(String arg) {
    return arg.equals("-v") || arg.equals("--verbose");
  }

  private static void parseAdditionalArguments(String arg) {
    String[] argument = arg.split("=");
    if (argument.length != 2) {
      displayUsageAndExit();
      return;
    }

    String argKey = argument[0];
    String argValue = argument[1];

    if (argKey.equals("-a") || argKey.equals("--aid")) {
      parseAidArgument(argValue);
    } else if (argKey.equals("-c") || argKey.equals("--card")) {
      cardReaderRegex = argValue;
    } else if (argKey.equals("-s") || argKey.equals("--sam")) {
      samReaderRegex = argValue;
    } else {
      displayUsageAndExit();
    }
  }

  private static void parseAidArgument(String aid) {
    if (aid.length() < 10 || aid.length() > 32 || !HexUtil.isValid(aid)) {
      System.out.println("Invalid AID");
      displayUsageAndExit();
      return;
    }

    cardAid = aid;
  }

  /** Displays the expected options */
  private static void displayUsageAndExit() {
    System.out.println("Available options:");
    System.out.printf(
        " -d, --default                  use default values (is equivalent to -a=\"%s\" -c=\"%s\" -s=\"%s\")%n",
        AID, CARD_READER_NAME_REGEX, SAM_READER_NAME_REGEX);
    System.out.println(
        " -a, --aid=\"APPLICATION_AID\"    between 5 and 16 hex bytes (e.g. \"315449432E49434131\")");
    System.out.println(
        " -c, --card=\"CARD_READER_REGEX\" regular expression matching the card reader name (e.g. \"ASK Logo.*\")");
    System.out.println(
        " -s, --sam=\"SAM_READER_REGEX\"   regular expression matching the SAM reader name (e.g. \"HID.*\")");
    System.out.println(" -v, --verbose                  set the log level to TRACE");
    System.out.println(
        "PC/SC protocol is set to `\"ANY\" ('*') for the SAM reader, \"T1\" ('T=1') for the card reader.");
    System.exit(1);
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
            cardReaderRegex,
            true,
            PcscReader.IsoProtocol.T1,
            PcscReader.SharingMode.SHARED,
            PcscCardCommunicationProtocol.ISO_14443_4.name(),
            ISO_CARD_PROTOCOL);
  }

  /**
   * Initializes the SAM reader with specific configurations.
   *
   * <p>Prepares the SAM reader using a predefined set of configurations, including the card reader
   * name regex, ISO protocol, and sharing mode.
   */
  private static void initSamReader() {
    samReader =
        getReader(
            plugin,
            samReaderRegex,
            false,
            PcscReader.IsoProtocol.ANY,
            PcscReader.SharingMode.SHARED,
            PcscCardCommunicationProtocol.ISO_7816_3.name(),
            SAM_PROTOCOL);
  }

  /**
   * Initializes the security settings for the transaction.
   *
   * <p>Prepares the SAM reader, selects the SAM, and sets up the symmetric crypto security setting
   * for securing the transaction.
   */
  private static void initSecuritySetting() {
    LegacySam sam = selectSam(samReader);
    symmetricCryptoSecuritySetting =
        calypsoCardApiFactory
            .createSymmetricCryptoSecuritySetting(
                LegacySamExtensionService.getInstance()
                    .getLegacySamApiFactory()
                    .createSymmetricCryptoCardTransactionManagerFactory(samReader, sam))
            .assignDefaultKif(PERSONALIZATION, (byte) 0x21)
            .assignDefaultKif(LOAD, (byte) 0x27)
            .assignDefaultKif(DEBIT, (byte) 0x30)
            .enableRatificationMechanism();
    // Optimization: preload the SAM challenge for the next transaction
    symmetricCryptoSecuritySetting.initCryptoContextForNextTransaction();
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
   * Selects the SAM C1 for the transaction.
   *
   * <p>Creates a SAM selection manager, prepares the selection, and processes the SAM selection
   * scenario.
   *
   * @param reader The card reader used to communicate with the SAM.
   * @return The selected SAM for the transaction.
   * @throws IllegalStateException if SAM selection fails.
   */
  private static LegacySam selectSam(CardReader reader) {
    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a card selector without filer
    IsoCardSelector cardSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    LegacySamApiFactory legacySamApiFactory =
        LegacySamExtensionService.getInstance().getLegacySamApiFactory();

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        cardSelector, legacySamApiFactory.createLegacySamSelectionExtension());

    // SAM communication: run the selection scenario.
    CardSelectionResult samSelectionResult =
        samSelectionManager.processCardSelectionScenario(reader);

    // Check the selection result.
    if (samSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Get the Calypso SAM SmartCard resulting of the selection.
    return (LegacySam) samSelectionResult.getActiveSmartCard();
  }
}

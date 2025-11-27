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
package org.eclipse.keyple.example.card.calypso.UseCase12_PerformanceMeasurement_EmbeddedValidation;

import static org.eclipse.keypop.calypso.card.WriteAccessLevel.DEBIT;

import java.io.*;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.*;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ChannelControl;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

/**
 * Use Case Calypso 12 – Performance measurement: embedded validation (PC/SC)
 *
 * <p>This code is dedicated to performance measurement for an embedded validation type transaction.
 *
 * <p>It implements the scenario described <a
 * href="https://keypop.org/apis/keypop-calypso-card-api/#simple-secure-session-for-fast-embedded-performance">here</a>:
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_PerformanceMeasurement_EmbeddedValidation_Pcsc {
  private static Logger logger;

  // user interface management
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";

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

  // operating parameters
  private static String cardReaderRegex;
  private static String samReaderRegex;
  private static String cardAid;
  private static int counterDecrement;
  private static String logLevel;
  private static byte[] newEventRecord;
  private static String builtDate;
  private static String builtTime;

  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  // The logical name of the protocol for communicating with the SAM (optional).
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";

  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  private static final byte SFI_EVENT_LOG = (byte) 0x08;
  private static final byte SFI_CONTRACT_LIST = (byte) 0x1E;
  private static final byte SFI_CONTRACTS = (byte) 0x09;
  private static final byte SFI_COUNTERS = (byte) 0x19;
  private static final int RECORD_SIZE = 29;

  public static void main(String[] args) throws IOException {

    // load operating parameters
    readConfigurationFile();

    // Set log level
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, logLevel);

    // Create logger
    logger = LoggerFactory.getLogger(Main_PerformanceMeasurement_EmbeddedValidation_Pcsc.class);

    System.out.printf(
        "%s=============== Performance measurement: validation transaction ===============\n",
        ANSI_GREEN);
    System.out.printf("Using parameters:\n");
    System.out.printf("  CARD_READER_REGEX=%s\n", cardReaderRegex);
    System.out.printf("  SAM_READER_REGEX=%s\n", samReaderRegex);
    System.out.printf("  AID=%s\n", cardAid);
    System.out.printf("  Counter decrement=%d\n", counterDecrement);
    System.out.printf("  log level=%s\n", logLevel);
    System.out.printf("Build date: %s %s%s\n", builtDate, builtTime, ANSI_RESET);

    // Initialize the context
    initKeypleService();
    initCalypsoCardExtensionService();
    initCardReader();
    initSamReader();
    initSecuritySetting();

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      System.out.printf(
          "\n%s########################################################%s\n",
          ANSI_YELLOW, ANSI_RESET);
      System.out.printf(
          "%s## Press ENTER when the card is in the reader's field ##%s\n",
          ANSI_YELLOW, ANSI_RESET);
      System.out.printf(
          "%s## (or press 'q' + ENTER to exit)                     ##%s\n",
          ANSI_YELLOW, ANSI_RESET);
      System.out.printf(
          "%s########################################################%s\n",
          ANSI_YELLOW, ANSI_RESET);

      String input = bufferedReader.readLine();

      if (input.toLowerCase().contains("q")) {
        bufferedReader.close();
        break;
      }

      if (cardReader.isCardPresent()) {
        try {
          logger.info("Starting validation transaction...");
          logger.info("Select application with AID = '{}'", cardAid);

          // read the current time used later to compute the transaction time
          long timeStamp = System.currentTimeMillis();

          CalypsoCard calypsoCard = selectCard(cardReader, cardAid);
          if (calypsoCard == null) {
            throw new IllegalStateException("Card selection failed!");
          }

          // Create a transaction manager, open a Secure Session, read Environment and Event Log.
          // Specifying expected response lengths in read commands serves as a protective measure
          // for legacy cards.
          SecureRegularModeTransactionManager cardTransactionManager =
              calypsoCardApiFactory
                  .createSecureRegularModeTransactionManager(
                      cardReader, calypsoCard, symmetricCryptoSecuritySetting)
                  .prepareOpenSecureSession(DEBIT)
                  .prepareReadRecords(SFI_ENVIRONMENT_AND_HOLDER, 1, 1, RECORD_SIZE)
                  .processCommands(ChannelControl.KEEP_OPEN);

          byte[] environmentAndHolderData =
              calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER).getData().getContent(1);

          // TODO Place here the analysis of the context

          // Read the last event record
          cardTransactionManager
              .prepareReadRecords(SFI_EVENT_LOG, 1, 1, RECORD_SIZE)
              .processCommands(ChannelControl.KEEP_OPEN);

          byte[] eventLogData = calypsoCard.getFileBySfi(SFI_EVENT_LOG).getData().getContent(1);

          // TODO Place here the analysis of the last event log
          // Ratification and anti-passback management:
          // This section handles the scenario where a previous transaction occurred very recently:
          // - If the previous transaction has not been ratified, access is granted immediately.
          //   The only required action is to close the session, which ensures the authenticity of
          // the support.
          // - If the previous transaction has been ratified, access is denied according to the
          // anti-passback rule,
          //   preventing multiple successive illegal uses of the same support in a short time.

          // Read the contract list
          // Specifying expected response lengths in read commands serves as a protective measure
          // for legacy cards.
          cardTransactionManager
              .prepareReadRecords(SFI_CONTRACT_LIST, 1, 1, RECORD_SIZE)
              .processCommands(ChannelControl.KEEP_OPEN);

          byte[] contractListData =
              calypsoCard.getFileBySfi(SFI_CONTRACT_LIST).getData().getContent(1);

          // TODO Place here the analysis of the contract list

          // Read the elected contract
          // Specifying expected response lengths in read commands serves as a protective measure
          // for legacy cards.
          cardTransactionManager
              .prepareReadRecords(SFI_CONTRACTS, 1, 1, RECORD_SIZE)
              .processCommands(ChannelControl.KEEP_OPEN);

          byte[] contractData = calypsoCard.getFileBySfi(SFI_CONTRACTS).getData().getContent(1);

          // TODO Place here the analysis of the contract

          // read the contract counter
          cardTransactionManager
              .prepareReadCounter(SFI_COUNTERS, 1)
              .processCommands(ChannelControl.KEEP_OPEN);

          int counterValue =
              calypsoCard.getFileBySfi(SFI_CONTRACT_LIST).getData().getContentAsCounterValue(1);

          // TODO Place here the preparation of the card's content update

          // add an event record and close the Secure Session
          cardTransactionManager
              .prepareDecreaseCounter(SFI_COUNTERS, 1, counterDecrement)
              .prepareAppendRecord(SFI_EVENT_LOG, newEventRecord)
              .prepareCloseSecureSession()
              .processCommands(ChannelControl.KEEP_OPEN);

          // display transaction time
          System.out.printf(
              "%sTransaction succeeded. Execution time: %d ms%s\n",
              ANSI_GREEN, System.currentTimeMillis() - timeStamp, ANSI_RESET);

          // Optimization: preload the SAM challenge for the next transaction
          symmetricCryptoSecuritySetting.initCryptoContextForNextTransaction();
        } catch (Exception e) {
          System.out.printf(
              "%sTransaction failed with exception: %s%s", ANSI_RED, e.getMessage(), ANSI_RESET);
        }
      } else {
        System.out.printf("%sNo card detected%s", ANSI_RED, ANSI_RESET);
      }
    }
    logger.info("Exiting the program on user's request.");
  }

  /** Reads the configuration file and sets the specified parameters. */
  private static void readConfigurationFile() throws IOException {
    InputStream input = null;
    try {
      input = new FileInputStream("config.properties");
      Properties prop = new Properties();

      // load a properties file
      prop.load(input);

      cardReaderRegex = prop.getProperty("validation.reader.card");
      samReaderRegex = prop.getProperty("validation.reader.sam");
      cardAid = prop.getProperty("validation.aid");
      counterDecrement = Integer.parseInt(prop.getProperty("validation.decrement"));
      newEventRecord = HexUtil.toByteArray(prop.getProperty("validation.event"));
      logLevel = prop.getProperty("validation.log");
      InputStream stream =
          Main_PerformanceMeasurement_EmbeddedValidation_Pcsc.class.getResourceAsStream(
              "/META-INF/MANIFEST.MF");
      Manifest manifest = new Manifest(stream);
      Attributes attributes = manifest.getMainAttributes();
      builtDate = attributes.getValue("Build-Date");
      builtTime = attributes.getValue("Build-Time");
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null) {
        input.close();
      }
    }
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

  /**
   * Selects the Calypso card for the transaction based on the specified Application Identifier
   * (AID).
   *
   * <p>Creates a card selection manager, prepares the selection using the provided AID, and
   * processes the card selection scenario. The AID is used to identify and select the specific
   * application on the card for the subsequent transaction.
   *
   * @param reader The reader used to communicate with the card.
   * @param aid The Application Identifier (AID) used to select the application on the card. It
   *     cannot be {@code null} or empty.
   * @return The selected Calypso card ready for the transaction.
   * @throws IllegalStateException if the selection of the application identified by the provided
   *     AID fails.
   */
  private static CalypsoCard selectCard(CardReader reader, String aid) {
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    IsoCardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(aid);
    CalypsoCardSelectionExtension calypsoCardSelectionExtension =
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard();
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the application " + aid + " failed.");
    }

    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }
}

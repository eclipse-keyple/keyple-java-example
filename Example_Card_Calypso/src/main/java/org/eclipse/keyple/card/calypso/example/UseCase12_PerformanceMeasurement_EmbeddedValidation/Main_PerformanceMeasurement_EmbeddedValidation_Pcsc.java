/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.calypso.example.UseCase12_PerformanceMeasurement_EmbeddedValidation;

import static org.eclipse.keypop.calypso.card.WriteAccessLevel.DEBIT;

import java.io.*;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

/**
 * Use Case Calypso 12 – Performance measurement: embedded validation (PC/SC)
 *
 * <p>This code is dedicated to performance measurement for an embedded validation type transaction.
 *
 * <p>It implements the scenario described <a
 * href="https://terminal-api.calypsonet.org/apis/calypsonet-terminal-calypso-api/#simple-secure-session-for-fast-embedded-performance">here</a>:
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_PerformanceMeasurement_EmbeddedValidation_Pcsc {

  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // A regular expression for matching common SAM readers. Adapt as needed.
  private static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  // The logical name of the protocol for communicating with the card (optional).

  // user interface management
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";

  // operating parameters
  private static String cardReaderRegex;
  private static String samReaderRegex;
  private static String cardAid;
  private static int counterDecrement;
  private static String logLevel;
  private static byte[] newEventRecord;
  private static String builtDate;
  private static String builtTime;

  // File structure
  /** AID: Keyple test kit profile 1, Application 2 */
  private static final String AID = "315449432E49434131";

  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  private static final byte SFI_EVENT_LOG = (byte) 0x08;
  private static final byte SFI_CONTRACT_LIST = (byte) 0x1E;
  private static final byte SFI_CONTRACTS = (byte) 0x09;
  private static final byte SFI_COUNTERS = (byte) 0x19;

  public static void main(String[] args) throws IOException {

    // load operating parameters
    readConfigurationFile();

    // init logger
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, logLevel);
    Logger logger =
        LoggerFactory.getLogger(Main_PerformanceMeasurement_EmbeddedValidation_Pcsc.class);

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

    // Get the main Keyple service
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the card and SAM readers whose name matches the provided regexs
    CardReader cardReader = ConfigurationUtil.getCardReader(plugin, CARD_READER_NAME_REGEX);
    CardReader samReader = ConfigurationUtil.getSamReader(plugin, SAM_READER_NAME_REGEX);

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Get the Calypso SAM SmartCard after selection.
    LegacySam sam = ConfigurationUtil.getSam(samReader);

    logger.info("= SAM = {}", sam);

    logger.info("= #### Select application with AID = '{}'.", AID);

    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    CardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(AID);

    CalypsoCardApiFactory calypsoCardApiFactory = calypsoCardService.getCalypsoCardApiFactory();

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    // Prepare the selection by adding the selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(
        cardSelector,
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    SymmetricCryptoSecuritySetting cardSecuritySetting =
        calypsoCardApiFactory
            .createSymmetricCryptoSecuritySetting(
                LegacySamExtensionService.getInstance()
                    .getLegacySamApiFactory()
                    .createSymmetricCryptoTransactionManagerFactory(samReader, sam))
            .enableRatificationMechanism();

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

          // Process the card selection scenario
          CardSelectionResult cardSelectionResult =
              cardSelectionManager.processCardSelectionScenario(cardReader);
          CalypsoCard calypsoCard = (CalypsoCard) cardSelectionResult.getActiveSmartCard();
          if (calypsoCard == null) {
            throw new IllegalStateException("Card selection failed!");
          }

          // create a transaction manager, open a Secure Session, read Environment and Event Log.
          SecureRegularModeTransactionManager cardTransactionManager =
              calypsoCardApiFactory
                  .createSecureRegularModeTransactionManager(
                      cardReader, calypsoCard, cardSecuritySetting)
                  .prepareOpenSecureSession(DEBIT)
                  .prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, 1)
                  .prepareReadRecord(SFI_EVENT_LOG, 1)
                  .processCommands(ChannelControl.KEEP_OPEN);

          byte[] environmentAndHolderData =
              calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER).getData().getContent(1);

          byte[] eventLogData = calypsoCard.getFileBySfi(SFI_EVENT_LOG).getData().getContent(1);

          // TODO Place here the analysis of the context and the last event log

          // read the contract list
          cardTransactionManager
              .prepareReadRecord(SFI_CONTRACT_LIST, 1)
              .processCommands(ChannelControl.KEEP_OPEN);

          byte[] contractListData =
              calypsoCard.getFileBySfi(SFI_CONTRACT_LIST).getData().getContent(1);

          // TODO Place here the analysis of the contract list

          // read the elected contract
          cardTransactionManager
              .prepareReadRecord(SFI_CONTRACTS, 1)
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
}

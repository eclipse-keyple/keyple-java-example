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

import java.io.*;
import java.util.Properties;
import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
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

  public static void main(String[] args) throws IOException {
    // load operating parameters
    readConfigurationFile();

    // init logger
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, logLevel);
    Logger logger =
        LoggerFactory.getLogger(Main_PerformanceMeasurement_EmbeddedValidation_Pcsc.class);

    logger.info("=============== Performance measurement: validation transaction ===============");
    logger.info("Using parameters:");
    logger.info("  CARD_READER_REGEX={}", cardReaderRegex);
    logger.info("  SAM_READER_REGEX={}", samReaderRegex);
    logger.info("  AID={}", cardAid);
    logger.info("  Counter decrement={}", counterDecrement);
    logger.info("  log level={}", logLevel);

    // Get the main Keyple service
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the contactless reader whose name matches the provided regex and configure it
    String pcscContactlessReaderName = ConfigurationUtil.getCardReaderName(plugin, cardReaderRegex);
    plugin
        .getReaderExtension(PcscReader.class, pcscContactlessReaderName)
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ConfigurableCardReader cardReader =
        (ConfigurableCardReader) plugin.getReader(pcscContactlessReaderName);
    cardReader.activateProtocol(
        PcscSupportedContactlessProtocol.ISO_14443_4.name(), ConfigurationUtil.ISO_CARD_PROTOCOL);

    // Get the contact reader whose name matches the provided regex and configure it
    String pcscContactReaderName = ConfigurationUtil.getCardReaderName(plugin, samReaderRegex);
    plugin
        .getReaderExtension(PcscReader.class, pcscContactReaderName)
        .setContactless(false)
        .setIsoProtocol(PcscReader.IsoProtocol.T0)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ConfigurableCardReader samReader =
        (ConfigurableCardReader) plugin.getReader(pcscContactReaderName);
    samReader.activateProtocol(
        PcscSupportedContactProtocol.ISO_7816_3_T0.name(), ConfigurationUtil.SAM_PROTOCOL);

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Create a SAM selection manager, use it to select the SAM and retrieve a CalypsoSam.
    CardSelectionManager samSelectionManager = smartCardService.createCardSelectionManager();
    samSelectionManager.prepareSelection(calypsoCardService.createSamSelection());
    CardSelectionResult samSelectionResult =
        samSelectionManager.processCardSelectionScenario(samReader);
    CalypsoSam calypsoSam = (CalypsoSam) samSelectionResult.getActiveSmartCard();

    logger.info("Calypso SAM = {}", calypsoSam);

    // Check the selection result.
    if (calypsoSam == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Create a card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    // Prepare the selection by adding the selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(
        calypsoCardService
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ConfigurationUtil.ISO_CARD_PROTOCOL)
            .filterByDfName(cardAid));

    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setControlSamResource(samReader, calypsoSam);

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    boolean loop = true;    while (loop) {
      logger.info(
          "{}########################################################{}", ANSI_YELLOW, ANSI_RESET);
      logger.info(
          "{}## Press ENTER when the card is in the reader's field ##{}", ANSI_YELLOW, ANSI_RESET);
      logger.info(
          "{}## (or press 'q' + ENTER to exit)                     ##{}", ANSI_YELLOW, ANSI_RESET);
      logger.info(
          "{}########################################################{}", ANSI_YELLOW, ANSI_RESET);

      String input = bufferedReader.readLine();

      if (input.toLowerCase().contains("q")) {
        bufferedReader.close();
        loop = false;
        continue;
      }

      try {
        logger.info("Starting validation transaction...");

        logger.info("Select application with AID = '{}'", cardAid);

        // read the current time used later to compute the transaction time
        long timeStamp = System.currentTimeMillis();

        CardSelectionResult cardSelectionResult =
            cardSelectionManager.processCardSelectionScenario(cardReader);

        CalypsoCard calypsoCard = (CalypsoCard) cardSelectionResult.getActiveSmartCard();

        if (calypsoCard == null) {
          logger.info("Card selection failed!");
          continue;
        }
        // create a transaction manager, open a Secure Session, read Environment and Event Log.
        CardTransactionManager cardTransactionManager =
            CalypsoExtensionService.getInstance()
                .createCardTransaction(cardReader, calypsoCard, cardSecuritySetting)
                .prepareReadRecord(
                    CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1)
                .prepareReadRecord(CalypsoConstants.SFI_EVENT_LOG, CalypsoConstants.RECORD_NUMBER_1)
                .processOpening(WriteAccessLevel.DEBIT);

        /*
        Place for the analysis of the context and the last event log
        */
        byte[] environmentAndHolderData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        byte[] eventLogData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_EVENT_LOG)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        // read the contract list
        cardTransactionManager
            .prepareReadRecord(CalypsoConstants.SFI_CONTRACT_LIST, CalypsoConstants.RECORD_NUMBER_1)
            .processCommands();
        /*
        Place for the analysis of the contract list
        */
        byte[] contractListData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACT_LIST)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        // read the elected contract
        cardTransactionManager
            .prepareReadRecord(CalypsoConstants.SFI_CONTRACTS, CalypsoConstants.RECORD_NUMBER_1)
            .processCommands();

        /*
        Place for the analysis of the contract
        */
        byte[] contractData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACTS)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        // read the contract counter
        cardTransactionManager
            .prepareReadCounter(CalypsoConstants.SFI_COUNTERS, 1)
            .processCommands();

        /*
        Place for the preparation of the card's content update
        */
        int counterValue =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACT_LIST)
                .getData()
                .getContentAsCounterValue(1);

        // add an event record and close the Secure Session
        cardTransactionManager
            .prepareDecreaseCounter(CalypsoConstants.SFI_COUNTERS, 1, counterDecrement)
            .prepareAppendRecord(CalypsoConstants.SFI_EVENT_LOG, newEventRecord)
            .prepareReleaseCardChannel()
            .processClosing();

        // display transaction time
        logger.info(
            "{}Transaction succeeded. Execution time: {} ms{}",
            ANSI_GREEN,
            System.currentTimeMillis() - timeStamp,
            ANSI_RESET);
      } catch (Exception e) {
        logger.error(
            "{}Transaction failed with exception: {}{}", ANSI_RED, e.getMessage(), ANSI_RESET);
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

    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null) {
        input.close();
      }
    }
  }
}

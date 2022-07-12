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
package org.eclipse.keyple.card.calypso.example.UseCase13_PerformanceMeasurement_DistributedReload;

import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.setupCardResourceService;

import java.io.*;
import java.util.Properties;
import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.card.CalypsoCardSelection;
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
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

/**
 * Use Case Calypso 12 – Performance measurement: reloading (PC/SC)
 *
 * <p>This code is dedicated to performance measurement for a reloading type transaction.
 *
 * <p>It implements the scenario described <a
 * href="https://terminal-api.calypsonet.org/apis/calypsonet-terminal-calypso-api/#simple-secure-session-for-an-efficient-distributed-system">here</a>:
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_PerformanceMeasurement_DistributedReload_Pcsc {

  // user interface management
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";

  // operating parameters
  private static String cardReaderRegex;
  private static String samReaderRegex;
  private static String cardAid;
  private static int counterIncrement;
  private static String logLevel;
  private static byte[] newContractListRecord;
  private static byte[] newContractRecord;

  public static void main(String[] args) throws IOException {

    // load operating parameters
    readConfigurationFile();

    // init logger
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, logLevel);
    Logger logger =
        LoggerFactory.getLogger(Main_PerformanceMeasurement_DistributedReload_Pcsc.class);

    logger.info(
        "=============== Performance measurement: validation transaction ==================");

    logger.info("Using parameters:");
    logger.info("  CARD_READER_REGEX={}", cardReaderRegex);
    logger.info("  SAM_READER_REGEX={}", samReaderRegex);
    logger.info("  AID={}", cardAid);
    logger.info("  Counter increment={}", counterIncrement);
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

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    CalypsoCardSelection cardSelection =
        calypsoCardService
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ConfigurationUtil.ISO_CARD_PROTOCOL)
            .filterByDfName(cardAid)
            .prepareReadRecord(
                CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1)
            .prepareReadRecord(
                CalypsoConstants.SFI_CONTRACT_LIST, CalypsoConstants.RECORD_NUMBER_1);

    // Prepare the selection by adding the created Calypso selection to the card selection
    // scenario.
    cardSelectionManager.prepareSelection(cardSelection);

    // Configure the card resource service for the targeted SAM.
    setupCardResourceService(plugin, samReaderRegex, CalypsoConstants.SAM_PROFILE_NAME);

    // Create security settings that reference the same SAM profile requested from the card resource
    // service.
    CardResource samResource =
        CardResourceServiceProvider.getService().getCardResource(CalypsoConstants.SAM_PROFILE_NAME);

    CardSecuritySetting cardSecuritySetting =
        CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setControlSamResource(
                samResource.getReader(), (CalypsoSam) samResource.getSmartCard());

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    boolean loop = true;
    while (loop) {
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
        logger.info("Starting reloading transaction...");

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

        /*
        Place for the pre-analysis of the context and the contract list
        */
        byte[] environmentAndHolderData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        byte[] contractListData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACT_LIST)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        // create a transaction manager, open a Secure Session, read Environment and Event Log.
        CardTransactionManager cardTransactionManager =
            CalypsoExtensionService.getInstance()
                .createCardTransaction(cardReader, calypsoCard, cardSecuritySetting)
                .prepareReadRecord(
                    CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1)
                .prepareReadRecord(
                    CalypsoConstants.SFI_CONTRACT_LIST, CalypsoConstants.RECORD_NUMBER_1)
                .prepareReadRecords(
                    CalypsoConstants.SFI_CONTRACTS,
                    CalypsoConstants.RECORD_NUMBER_1,
                    CalypsoConstants.RECORD_NUMBER_2,
                    CalypsoConstants.RECORD_SIZE)
                .prepareReadCounter(CalypsoConstants.SFI_COUNTERS, 2)
                .processOpening(WriteAccessLevel.LOAD);

        /*
        Place for the analysis of the context, the contract list, the contracts and counters
        */
        environmentAndHolderData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        contractListData =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACT_LIST)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        byte[] contract1Data =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACTS)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_1);

        byte[] contract2Data =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACTS)
                .getData()
                .getContent(CalypsoConstants.RECORD_NUMBER_2);

        int counter1Value =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACT_LIST)
                .getData()
                .getContentAsCounterValue(1);

        int counter2Value =
            calypsoCard
                .getFileBySfi(CalypsoConstants.SFI_CONTRACT_LIST)
                .getData()
                .getContentAsCounterValue(2);

        // add an event record and close the Secure Session
        cardTransactionManager
            .prepareUpdateRecord(
                CalypsoConstants.SFI_CONTRACT_LIST,
                CalypsoConstants.RECORD_NUMBER_1,
                newContractListRecord)
            .prepareUpdateRecord(
                CalypsoConstants.SFI_CONTRACTS, CalypsoConstants.RECORD_NUMBER_1, newContractRecord)
            .prepareIncreaseCounter(CalypsoConstants.SFI_COUNTERS, 1, counterIncrement)
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

      cardReaderRegex = prop.getProperty("reloading.reader.card");
      samReaderRegex = prop.getProperty("reloading.reader.sam");
      cardAid = prop.getProperty("reloading.aid");
      counterIncrement = Integer.parseInt(prop.getProperty("reloading.increment"));
      newContractListRecord = HexUtil.toByteArray(prop.getProperty("reloading.contractlist"));
      newContractRecord = HexUtil.toByteArray(prop.getProperty("reloading.contract"));
      logLevel = prop.getProperty("reloading.log");

    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null) {
        input.close();
      }
    }
  }
}

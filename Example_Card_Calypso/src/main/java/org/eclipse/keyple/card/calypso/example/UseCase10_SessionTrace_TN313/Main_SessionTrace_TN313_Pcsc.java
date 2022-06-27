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
package org.eclipse.keyple.card.calypso.example.UseCase10_SessionTrace_TN313;

import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.setupCardResourceService;

import java.util.Scanner;
import org.calypsonet.terminal.calypso.card.CalypsoCardSelection;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
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
 * Use Case Calypso 10 – Calypso Secure Session Trace - Technical Note #313 (PC/SC)
 *
 * <p>This an implementation of the Calypso Secure Session described the technical note #313
 * defining a typical usage of a Calypso card and allowing performances comparison.
 *
 * <p>Scenario:
 *
 * <ul>
 *   <li>Schedule a selection scenario over an observable reader to target a specific card (here a
 *       Calypso card characterized by its AID) and including the reading of a file record.
 *   <li>Initialize and start the SAM resource service.
 *   <li>Start the observation and wait for a card insertion.
 *   <li>Within the reader event handler:
 *       <ul>
 *         <li>Do the TN313 transaction scenario.
 *         <li>Close the physical channel.
 *       </ul>
 * </ul>
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_SessionTrace_TN313_Pcsc {
  private static String cardReaderRegex = ConfigurationUtil.CARD_READER_NAME_REGEX;
  private static String samReaderRegex = ConfigurationUtil.SAM_READER_NAME_REGEX;
  private static String cardAid = CalypsoConstants.AID;
  private static boolean isVerbose;

  public static void main(String[] args) {

    parseCommandLine(args);

    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, isVerbose ? "TRACE" : "INFO");

    Logger logger = LoggerFactory.getLogger(Main_SessionTrace_TN313_Pcsc.class);

    logger.info("=============== UseCase Calypso #10: session trace TN313 ==================");

    logger.info("Using parameters:");
    logger.info("  AID={}", cardAid);
    logger.info("  CARD_READER_REGEX={}", cardReaderRegex);
    logger.info("  SAM_READER_REGEX={}", samReaderRegex);

    // Get the instance of the SmartCardService (singleton pattern)
    final SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin
    final Plugin plugin =
        smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Get the contactless reader whose name matches the provided regex
    String pcscContactlessReaderName = ConfigurationUtil.getCardReaderName(plugin, cardReaderRegex);
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

    logger.info("Select application with AID = '{}'", cardAid);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    CalypsoCardSelection cardSelection =
        calypsoCardService
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ConfigurationUtil.ISO_CARD_PROTOCOL)
            .filterByDfName(cardAid);

    // Prepare the selection by adding the created Calypso selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelection);

    // Schedule the selection scenario, request notification only if the card matches the selection
    // case.
    cardSelectionManager.scheduleCardSelectionScenario(
        (ObservableCardReader) cardReader,
        ObservableCardReader.DetectionMode.REPEATING,
        ObservableCardReader.NotificationMode.MATCHED_ONLY);

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

    // Create and add a card observer for this reader
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(cardReader, cardSelectionManager, cardSecuritySetting);

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
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program");

    System.exit(0);
  }

  /**
   * Analyses the command line and sets the specified parameters.
   *
   * @param args The command line arguments
   */
  private static void parseCommandLine(String[] args) {
    // command line arguments analysis
    if (args.length > 0) {
      // at least one argument
      for (String arg : args) {
        if (arg.equals("-d") || arg.equals("--default")) {
          break;
        }
        if (arg.equals("-v") || arg.equals("--verbose")) {
          isVerbose = true;
          continue;
        }
        String[] argument = arg.split("=");
        if (argument.length != 2) {
          displayUsageAndExit();
        }
        if (argument[0].equals("-a") || argument[0].equals("--aid")) {
          cardAid = argument[1];
          if (argument[1].length() < 10
              || argument[1].length() > 32
              || !HexUtil.isValid(argument[1])) {
            System.out.println("Invalid AID");
            displayUsageAndExit();
          }
        } else if (argument[0].equals("-c") || argument[0].equals("--card")) {
          cardReaderRegex = argument[1];
        } else if (argument[0].equals("-s") || argument[0].equals("--sam")) {
          samReaderRegex = argument[1];
        } else {
          displayUsageAndExit();
        }
      }
    } else {
      displayUsageAndExit();
    }
  }

  /** Displays the expected options */
  private static void displayUsageAndExit() {
    System.out.println("Available options:");
    System.out.println(
        String.format(
            " -d, --default                  use default values (is equivalent to -a=\"%s\" -c=\"%s\" -s=\"%s\")",
            CalypsoConstants.AID,
            ConfigurationUtil.CARD_READER_NAME_REGEX,
            ConfigurationUtil.SAM_READER_NAME_REGEX));
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
}

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

import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.getCardReader;
import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.setupCardResourceService;

import java.util.Scanner;
import org.calypsonet.terminal.calypso.card.CalypsoCardSelection;
import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_SessionTrace_TN313_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_SessionTrace_TN313_Pcsc.class);
  private static String cardReaderRegex = ConfigurationUtil.CARD_READER_NAME_REGEX;
  private static String samReaderRegex = ConfigurationUtil.SAM_READER_NAME_REGEX;
  private static String cardAid = CalypsoConstants.AID;

  public static void main(String[] args) {

    parseCommandLine(args);

    logger.info("Using parameters:");
    logger.info("cardReaderRegex = {}", cardReaderRegex);
    logger.info("samReaderRegex = {}", samReaderRegex);
    logger.info("application AID = {}", cardAid);

    // Get the instance of the SmartCardService (singleton pattern)
    final SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin
    final Plugin plugin =
        smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService cardExtension = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    // Retrieve the card reader
    Reader cardReader = getCardReader(plugin, cardReaderRegex);

    // Activate the ISO14443 card protocol.
    ((ConfigurableReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ContactlessCardCommonProtocol.ISO_14443_4.name());

    logger.info("=============== UseCase Calypso #10: session trace TN313 ==================");
    logger.info("Select application with AID = '{}'", cardAid);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    CalypsoCardSelection cardSelection =
        cardExtension
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ContactlessCardCommonProtocol.ISO_14443_4.name())
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
            .setSamResource(samResource.getReader(), (CalypsoSam) samResource.getSmartCard());

    // Create and add a card observer for this reader
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(cardReader, cardSelectionManager, cardSecuritySetting);

    ((ObservableCardReader) cardReader).setReaderObservationExceptionHandler(cardReaderObserver);
    ((ObservableCardReader) cardReader).addObserver(cardReaderObserver);
    ((ObservableCardReader) cardReader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info("Wait for a card...");

    Scanner sc = new Scanner(System.in);
    logger.info("Press enter to exit...");
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
        if (arg.equals("-default")) {
          break;
        }
        String[] argument = arg.split("=");
        if (argument[0].equals("-cardReaderRegex")) {
          cardReaderRegex = argument[1];
        } else if (argument[0].equals("-samReaderRegex")) {
          samReaderRegex = argument[1];
        } else if (argument[0].equals("-aid")) {
          cardAid = argument[1];
        } else {
          displayUsage();
        }
      }
    } else {
      displayUsage();
    }
  }

  /** Displays the expected options */
  private static void displayUsage() {
    System.out.println("Available options:");
    System.out.println(
        " -cardReaderRegex=CARD_READER_REGEX regular expression matching the card reader name, ex. \"ASK Logo.*\"");
    System.out.println(
        " -samReaderRegex=SAM_READER_REGEX   regular expression matching the SAM reader name, ex. \"HID.*\"");
    System.out.println(
        " -aid=APPLICATION_AID               at least 5 hex bytes, ex. \"315449432E49434131\"");
    System.out.println(" -default                           use default values,");
    System.out.println(
        "                                    is equivalent to -cardReaderRegex=.*ASK LoGO.*|.*Contactless.* -samReaderRegex=.*Identive.*|.*HID.* -aid=315449432E49434131");
    System.exit(1);
  }
}

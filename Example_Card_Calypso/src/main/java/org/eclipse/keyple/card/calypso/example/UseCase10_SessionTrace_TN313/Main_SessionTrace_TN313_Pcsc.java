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
package org.eclipse.keyple.card.calypso.example.UseCase10_SessionTrace_TN313;

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

import static org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil.getCardReader;

/**
 *
 *
 * <h1>Use Case Calypso 10 – Calypso Secure Session Trace - Technical Note #313 (PC/SC)</h1>
 *
 * <p>This an implementation of the Calypso Secure Session described the technical note #313 defining a typical usage of a Calypso card and allowing performances comparison.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Schedule a selection scenario over an observable reader to target a specific card (here a
 *       Calypso card characterized by its AID) and including the reading of a file record.
 *   <li>Start the observation and wait for a card insertion.
 *   <li>Within the reader event handler:
 *       <ul>
 *         <li>Output collected card data (FCI and ATR).
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

  public static void main(String[] args) throws InterruptedException {

    // Get the instance of the SmartCardService (singleton pattern)
    final SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    final Plugin plugin =
        smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the generic card extension service
    CalypsoExtensionService cardExtension = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    Reader cardReader = getCardReader(plugin, ConfigurationUtil.CARD_READER_NAME_REGEX);

    // Activate the ISO14443 card protocol.
    ((ConfigurableReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ContactlessCardCommonProtocol.ISO_14443_4.name());

    logger.info("=============== UseCase Generic #2: scheduled selection ==================");
    logger.info("= #### Select application with AID = '{}'.", CalypsoConstants.AID);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the Calypso card extension.
    // Select the card and read the record 1 of the file ENVIRONMENT_AND_HOLDER
    CalypsoCardSelection cardSelection =
        cardExtension
            .createCardSelection()
            .acceptInvalidatedCard()
            .filterByCardProtocol(ContactlessCardCommonProtocol.ISO_14443_4.name())
            .filterByDfName(CalypsoConstants.AID);

    // Prepare the selection by adding the created Calypso selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelection);

    // Schedule the selection scenario, request notification only if the card matches the selection
    // case.
    cardSelectionManager.scheduleCardSelectionScenario(
        (ObservableCardReader) cardReader,
        ObservableCardReader.DetectionMode.REPEATING,
        ObservableCardReader.NotificationMode.MATCHED_ONLY);

    // Create security settings that reference the same SAM profile requested from the card resource
    // service.
    CardResource samResource =
            CardResourceServiceProvider.getService().getCardResource(CalypsoConstants.SAM_PROFILE_NAME);
    CardSecuritySetting cardSecuritySetting =
            CalypsoExtensionService.getInstance()
                    .createCardSecuritySetting()
                    .setSamResource(samResource.getReader(), (CalypsoSam) samResource.getSmartCard());

    // Create and add an observer for this reader
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(cardReader, cardSelectionManager, cardSecuritySetting);

    ((ObservableCardReader) cardReader).setReaderObservationExceptionHandler(cardReaderObserver);
    ((ObservableCardReader) cardReader).addObserver(cardReaderObserver);
    ((ObservableCardReader) cardReader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info(
        "= #### Wait for a card. The default AID based selection to be processed as soon as the card is detected.");

    // Wait indefinitely. CTRL-C to exit.
    synchronized (waitForEnd) {
      waitForEnd.wait();
    }

    // unregister plugin
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program.");

    System.exit(0);
  }

  /**
   * This object is used to freeze the main thread while card operations are handle through the
   * observers callbacks. A call to the notify() method would end the program (not demonstrated
   * here).
   */
  private static final Object waitForEnd = new Object();
}

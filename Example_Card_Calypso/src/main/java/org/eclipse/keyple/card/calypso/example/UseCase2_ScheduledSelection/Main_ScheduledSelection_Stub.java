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
package org.eclipse.keyple.card.calypso.example.UseCase2_ScheduledSelection;

import org.calypsonet.terminal.calypso.card.CalypsoCardSelection;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.card.calypso.example.common.StubSmartCardFactory;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
import org.eclipse.keyple.plugin.stub.StubPluginFactoryBuilder;
import org.eclipse.keyple.plugin.stub.StubReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 2 – Scheduled Selection (Stub)</h1>
 *
 * <p>We demonstrate here the selection of a Calypso card using a scheduled scenario. The selection
 * operations are prepared in advance with the card selection manager and the Calypso extension
 * service, then the reader is observed. When a card is inserted, the prepared selection scenario is
 * executed and the observer is notified of a card insertion event including the selection data
 * collected during the selection process.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Schedule a selection scenario over an observable reader to target a specific card (here a
 *       Calypso card characterized by its AID) and including the reading of a file record.
 *   <li>Start the observation and wait for a card insertion.
 *   <li>Simulate the card insertion.
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
public class Main_ScheduledSelection_Stub {
  private static final Logger logger = LoggerFactory.getLogger(Main_ScheduledSelection_Stub.class);

  public static void main(String[] args) throws InterruptedException {
    final String CARD_READER_NAME = "Stub card reader";

    // Get the instance of the SmartCardService (singleton pattern)
    final SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the StubPlugin with the SmartCardService, plug a Calypso card stub
    // get the corresponding generic plugin in return.
    final Plugin plugin =
        smartCardService.registerPlugin(
            StubPluginFactoryBuilder.builder()
                .withStubReader(CARD_READER_NAME, true, null)
                .withMonitoringCycleDuration(100)
                .build());

    // Get the generic card extension service
    CalypsoExtensionService cardExtension = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    Reader cardReader = plugin.getReader(CARD_READER_NAME);

    // Activate the ISO14443 card protocol.
    ((ConfigurableReader) cardReader)
        .activateProtocol(
            ContactlessCardCommonProtocol.ISO_14443_4.name(),
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
            .filterByDfName(CalypsoConstants.AID)
            .prepareReadRecord(
                CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1);

    // Prepare the selection by adding the created Calypso selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelection);

    // Schedule the selection scenario, request notification only if the card matches the selection
    // case.
    cardSelectionManager.scheduleCardSelectionScenario(
        (ObservableReader) cardReader,
        ObservableCardReader.DetectionMode.REPEATING,
        ObservableCardReader.NotificationMode.MATCHED_ONLY);

    // Create and add an observer for this reader
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(cardReader, cardSelectionManager);
    ((ObservableReader) cardReader).setReaderObservationExceptionHandler(cardReaderObserver);
    ((ObservableReader) cardReader).addObserver(cardReaderObserver);
    ((ObservableReader) cardReader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info(
        "= #### Wait for a card. The default AID based selection to be processed as soon as the card is detected.");

    /* Wait a while. */
    Thread.sleep(100);

    logger.info("Insert stub card.");
    cardReader.getExtension(StubReader.class).insertCard(StubSmartCardFactory.getStubCard());

    /* Wait a while. */
    Thread.sleep(1000);

    logger.info("Remove stub card.");
    cardReader.getExtension(StubReader.class).removeCard();

    // unregister plugin
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program.");

    System.exit(0);
  }
}

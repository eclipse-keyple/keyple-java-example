/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.example.core.service.UseCase4_ScheduledSelection;

import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.example.common.ConfigurationUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 4 – Scheduled Selection (PC/SC)</h1>
 *
 * <p>We present here a selection of ISO-14443-4 cards including the transmission of a "select
 * application" APDU targeting EMV banking cards (AID PPSE). Any contactless EMV card should lead to
 * a "selected" state, any card with another DF Name should be ignored.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Schedule a selection scenario over an observable reader to target a specific card (here a
 *       EMV contactless card).
 *   <li>Start the observation and wait for a card.
 *   <li>Within the reader event handler:
 *       <ul>
 *         <li>Output collected smart card data (FCI and power-on data).
 *         <li>Close the physical channel.
 *       </ul>
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_ScheduledSelection_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_ScheduledSelection_Pcsc.class);

  public static void main(String[] args) throws InterruptedException {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the generic card extension service
    GenericExtensionService genericCardService = GenericExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(genericCardService);

    // Get the contactless reader whose name matches the provided regex
    ObservableCardReader observableCardReader =
        (ObservableCardReader) plugin.findReader(ConfigurationUtil.CONTACTLESS_READER_NAME_REGEX);

    // Configure the reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, observableCardReader.getName())
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) observableCardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ConfigurationUtil.ISO_CARD_PROTOCOL);

    logger.info(
        "=============== UseCase Generic #4: scheduled AID based selection ==================");

    logger.info("= #### Select application with AID = '{}'.", ConfigurationUtil.AID_EMV_PPSE);

    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();
    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    IsoCardSelector cardSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByCardProtocol(ConfigurationUtil.ISO_CARD_PROTOCOL)
            .filterByDfName(ConfigurationUtil.AID_EMV_PPSE);

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(
        cardSelector, GenericExtensionService.getInstance().createGenericCardSelectionExtension());

    // Schedule the selection scenario.
    cardSelectionManager.scheduleCardSelectionScenario(
        observableCardReader, ObservableCardReader.NotificationMode.MATCHED_ONLY);

    // Create and add an observer
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(observableCardReader, cardSelectionManager);
    observableCardReader.setReaderObservationExceptionHandler(cardReaderObserver);
    observableCardReader.addObserver(cardReaderObserver);
    observableCardReader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info(
        "= #### Wait for a card. The AID based selection scenario will be processed as soon as a card is detected.");

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
   * observers callbacks. A call to the "notify()" method would end the program (not demonstrated
   * here).
   */
  private static final Object waitForEnd = new Object();
}

/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.example.distributed.poolreaderserverside.webservice.client;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.card.ElementaryFile;
import org.eclipse.keypop.reader.ChannelControl;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains Calypso Ticketing Logic utilities. */
public class CalypsoTicketingServiceUtil {

  private static final Logger logger = LoggerFactory.getLogger(CalypsoTicketingServiceUtil.class);

  /** AID: Keyple */
  public static final String AID = "315449432E49434131";

  public static final byte RECORD_NUMBER_1 = 1;
  public static final byte SFI_EnvironmentAndHolder = (byte) 0x07;
  public static final byte SFI_EventLog = (byte) 0x08;
  private static final ReaderApiFactory readerApiFactory =
      SmartCardServiceProvider.getService().getReaderApiFactory();
  private static final CalypsoCardApiFactory calypsoCardApiFactory =
      CalypsoExtensionService.getInstance().getCalypsoCardApiFactory();

  private CalypsoTicketingServiceUtil() {}

  /**
   * Prepare a Selection object ready to select Calypso card and read environment file
   *
   * @return instance of Selection object
   */
  public static CardSelectionManager getCardSelection() {

    // Check the Calypso extension.
    SmartCardServiceProvider.getService().checkCardExtension(CalypsoExtensionService.getInstance());

    // ISO card selection
    IsoCardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(AID);

    // Calypso additional operations
    CalypsoCardSelectionExtension cardSelectionExtension =
        calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()
            .prepareReadRecord(SFI_EnvironmentAndHolder, RECORD_NUMBER_1);

    // Prepare Card Selection
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    // Add the selection case to the current selection
    cardSelectionManager.prepareSelection(cardSelector, cardSelectionExtension);
    return cardSelectionManager;
  }

  /**
   * Reads and returns content of event log file within a Portable Object Transaction.
   *
   * @param calypsoCard smartcard to read to the event log file
   * @param reader local reader where the smartcard is inserted
   * @return content of the event log file in Hexadecimal
   */
  public static String readEventLog(CalypsoCard calypsoCard, CardReader reader) {

    // Execute calypso session from a card selection
    logger.info(
        "Initial Card Content, power on data : {}, sn : {}",
        calypsoCard.getPowerOnData(),
        calypsoCard.getApplicationSerialNumber());

    // Retrieves the data read from the CalypsoCard updated during the transaction process.
    ElementaryFile efEnvironmentAndHolder = calypsoCard.getFileBySfi(SFI_EnvironmentAndHolder);
    String environmentAndHolder = HexUtil.toHex(efEnvironmentAndHolder.getData().getContent());

    // Logs the result.
    logger.info("EnvironmentAndHolder file data: {}", environmentAndHolder);

    // Go on with the reading of the first record of the EventLog file.
    logger.info("= #### reading transaction of the EventLog file.");

    // Prepares the reading order and keep the associated parser for later use once the
    // transaction has been processed.
    // Actual Card communication: send the prepared read order, then close the channel with
    // the Card.
    calypsoCardApiFactory
        .createFreeTransactionManager(reader, calypsoCard)
        .prepareReadRecord(SFI_EventLog, RECORD_NUMBER_1)
        .processCommands(ChannelControl.CLOSE_AFTER);
    logger.info("The reading of the EventLog has succeeded.");

    // Retrieves the data read from the CalypsoCard updated during the transaction process.
    ElementaryFile efEventLog = calypsoCard.getFileBySfi(SFI_EventLog);
    String eventLog = HexUtil.toHex(efEventLog.getData().getContent());

    // Logs the result.
    logger.info("EventLog file data: {}", eventLog);

    return eventLog;
  }
}

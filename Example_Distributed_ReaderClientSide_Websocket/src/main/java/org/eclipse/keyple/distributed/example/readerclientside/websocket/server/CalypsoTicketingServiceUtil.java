/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.distributed.example.readerclientside.websocket.server;

import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.card.ElementaryFile;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.spi.CardSelection;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
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

  private CalypsoTicketingServiceUtil() {}

  /**
   * Prepare a Selection object ready to select Calypso card and read environment file
   *
   * @return instance of Selection object
   */
  public static CardSelectionManager getCardSelection() {

    // Check the Calypso extension.
    SmartCardServiceProvider.getService().checkCardExtension(CalypsoExtensionService.getInstance());

    // Calypso selection
    CardSelection cardSelection =
        CalypsoExtensionService.getInstance()
            .createCardSelection()
            .filterByCardProtocol(ContactlessCardCommonProtocol.ISO_14443_4.name())
            .filterByDfName(AID)
            .prepareReadRecord(SFI_EnvironmentAndHolder, RECORD_NUMBER_1);

    // Prepare Card Selection
    CardSelectionManager cardSelectionManager =
        SmartCardServiceProvider.getService().createCardSelectionManager();

    // Add the selection case to the current selection
    cardSelectionManager.prepareSelection(cardSelection);
    return cardSelectionManager;
  }

  /**
   * Reads and returns content of event log file within a Portable Object Transaction.
   *
   * @param calypsoCard smartcard to read to the event log file
   * @param reader local reader where the smartcard is inserted
   * @return content of the event log file in Hexadecimal
   */
  public static String readEventLog(CalypsoCard calypsoCard, Reader reader) {

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
    CalypsoExtensionService.getInstance()
        .createCardTransactionWithoutSecurity(reader, calypsoCard)
        .prepareReadRecord(SFI_EventLog, RECORD_NUMBER_1)
        .prepareReleaseCardChannel()
        .processCommands();
    logger.info("The reading of the EventLog has succeeded.");

    // Retrieves the data read from the CalypsoCard updated during the transaction process.
    ElementaryFile efEventLog = calypsoCard.getFileBySfi(SFI_EventLog);
    String eventLog = HexUtil.toHex(efEventLog.getData().getContent());

    // Logs the result.
    logger.info("EventLog file data: {}", eventLog);

    return eventLog;
  }
}

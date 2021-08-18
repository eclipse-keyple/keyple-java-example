/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.example.generic.distributed.server.websocket.util;

import org.eclipse.keyple.calypso.transaction.*;
import org.eclipse.keyple.core.card.selection.CardResource;
import org.eclipse.keyple.core.card.selection.CardSelectionsService;
import org.eclipse.keyple.core.card.selection.CardSelector;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains Calypso Ticketing Logic utilities. */
public class CalypsoTicketingService {

  private static final Logger logger = LoggerFactory.getLogger(CalypsoTicketingService.class);

  /** AID: Keyple */
  public static final String AID = "315449432E49434131";

  public static final byte RECORD_NUMBER_1 = 1;
  public static final byte SFI_EnvironmentAndHolder = (byte) 0x07;
  public static final byte SFI_EventLog = (byte) 0x08;

  /**
   * Prepare a Selection object ready to select Calypso card and read environment file
   *
   * @return instance of Selection object
   */
  public static CardSelectionsService getCardSelection() {

    // Prepare Card Selection
    CardSelectionsService cardSelection = new CardSelectionsService();

    // Calypso selection
    PoSelection poSelectionRequest =
        new PoSelection(
            PoSelector.builder()
                .cardProtocol(ContactlessCardCommonProtocol.ISO_14443_4.name())
                .aidSelector(CardSelector.AidSelector.builder().aidToSelect(AID).build())
                .invalidatedPo(PoSelector.InvalidatedPo.REJECT)
                .build());

    // Prepare the reading order.
    poSelectionRequest.prepareReadRecordFile(SFI_EnvironmentAndHolder, RECORD_NUMBER_1);

    // Add the selection case to the current selection
    cardSelection.prepareSelection(poSelectionRequest);
    return cardSelection;
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
        calypsoCard.getAtr(),
        calypsoCard.getApplicationSerialNumber());

    // Retrieves the data read from the CalypsoCard updated during the transaction process.
    ElementaryFile efEnvironmentAndHolder = calypsoCard.getFileBySfi(SFI_EnvironmentAndHolder);
    String environmentAndHolder =
        ByteArrayUtil.toHex(efEnvironmentAndHolder.getData().getContent());

    // Logs the result.
    logger.info("EnvironmentAndHolder file data: {}", environmentAndHolder);

    // Go on with the reading of the first record of the EventLog file.
    logger.info("= #### reading transaction of the EventLog file.");

    PoTransaction poTransaction = new PoTransaction(new CardResource<>(reader, calypsoCard));

    // Prepares the reading order and keep the associated parser for later use once the
    // transaction has been processed.
    poTransaction.prepareReadRecordFile(SFI_EventLog, RECORD_NUMBER_1);

    // Actual Card communication: send the prepared read order, then close the channel with
    // the Card.
    poTransaction.prepareReleasePoChannel();
    poTransaction.processPoCommands();
    logger.info("The reading of the EventLog has succeeded.");

    // Retrieves the data read from the CalypsoCard updated during the transaction process.
    ElementaryFile efEventLog = calypsoCard.getFileBySfi(SFI_EventLog);
    String eventLog = ByteArrayUtil.toHex(efEventLog.getData().getContent());

    // Logs the result.
    logger.info("EventLog file data: {}", eventLog);

    return eventLog;
  }
}

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

import static org.calypsonet.terminal.reader.CardReaderEvent.Type.CARD_INSERTED;
import static org.calypsonet.terminal.reader.CardReaderEvent.Type.CARD_MATCHED;

import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.core.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)
 *
 * <p>A reader Observer handles card event such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED
 */
class CardReaderObserver
    implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);
  private final CardReader cardReader;
  private final CardSecuritySetting cardSecuritySetting;
  private final CardSelectionManager cardSelectionManager;
  private final byte[] newEventRecord =
      HexUtil.toByteArray("8013C8EC55667788112233445566778811223344556677881122334455");
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";
  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param cardReader The card reader.
   * @param cardSelectionManager The card selection manager.
   * @param cardSecuritySetting The card security settings.
   */
  CardReaderObserver(
      CardReader cardReader,
      CardSelectionManager cardSelectionManager,
      CardSecuritySetting cardSecuritySetting) {
    this.cardReader = cardReader;
    this.cardSelectionManager = cardSelectionManager;
    this.cardSecuritySetting = cardSecuritySetting;
  }

  /** {@inheritDoc} */
  @Override
  public void onReaderEvent(CardReaderEvent event) {
    switch (event.getType()) {
      case CARD_MATCHED:
        // read the current time used later to compute the transaction time
        long timeStamp = System.currentTimeMillis();
        try {
          // the selection matched, get the resulting CalypsoCard
          CalypsoCard calypsoCard =
              (CalypsoCard)
                  cardSelectionManager
                      .parseScheduledCardSelectionsResponse(
                          event.getScheduledCardSelectionsResponse())
                      .getActiveSmartCard();

          // create a transaction manager, open a Secure Session, read Environment, Event Log and
          // Contract List.
          CardTransactionManager cardTransactionManager =
              CalypsoExtensionService.getInstance()
                  .createCardTransaction(cardReader, calypsoCard, cardSecuritySetting)
                  .prepareReadRecord(
                      CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1)
                  .prepareReadRecord(
                      CalypsoConstants.SFI_EVENT_LOG, CalypsoConstants.RECORD_NUMBER_1)
                  .prepareReadRecord(
                      CalypsoConstants.SFI_CONTRACT_LIST, CalypsoConstants.RECORD_NUMBER_1)
                  .processOpening(WriteAccessLevel.DEBIT);

          /*
          Place for the analysis of the context and the list of contracts
          */

          // read the elected contract
          cardTransactionManager
              .prepareReadRecord(CalypsoConstants.SFI_CONTRACTS, CalypsoConstants.RECORD_NUMBER_1)
              .processCommands();

          /*
          Place for the analysis of the contracts
          */

          // add an event record and close the Secure Session
          cardTransactionManager
              .prepareAppendRecord(CalypsoConstants.SFI_EVENT_LOG, newEventRecord)
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

        break;

      case CARD_INSERTED:
        logger.error(
            "CARD_INSERTED event: should not have occurred because of the MATCHED_ONLY selection mode chosen");
        break;

      case CARD_REMOVED:
        logger.info("Card removed");
        break;
      default:
        break;
    }

    if (event.getType() == CARD_INSERTED || event.getType() == CARD_MATCHED) {

      // Informs the underlying layer of the end of the card processing, in order to manage the
      // removal sequence.
      ((ObservableCardReader) (cardReader)).finalizeCardProcessing();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
    logger.error("An exception occurred in plugin '{}', reader '{}'", pluginName, readerName, e);
  }
}

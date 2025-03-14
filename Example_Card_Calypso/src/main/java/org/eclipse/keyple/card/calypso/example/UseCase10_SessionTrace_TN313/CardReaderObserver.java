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

import static org.eclipse.keypop.calypso.card.WriteAccessLevel.DEBIT;
import static org.eclipse.keypop.reader.CardReaderEvent.Type.CARD_INSERTED;
import static org.eclipse.keypop.reader.CardReaderEvent.Type.CARD_MATCHED;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A reader Observer handles card event such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED */
class CardReaderObserver
    implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);
  private final Plugin plugin;
  private final CardReader cardReader;
  private final SymmetricCryptoSecuritySetting cardSecuritySetting;
  private final CardSelectionManager cardSelectionManager;
  private final byte[] newEventRecord =
      HexUtil.toByteArray("8013C8EC55667788112233445566778811223344556677881122334455");
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_BLACK = "\u001B[30m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_BLUE = "\u001B[34m";
  private static final String ANSI_PURPLE = "\u001B[35m";
  private static final String ANSI_CYAN = "\u001B[36m";
  private static final String ANSI_WHITE = "\u001B[37m";
  private final CalypsoCardApiFactory calypsoCardApiFactory;

  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  private static final byte SFI_EVENT_LOG = (byte) 0x08;
  private static final byte SFI_CONTRACT_LIST = (byte) 0x1E;
  private static final byte SFI_CONTRACTS = (byte) 0x09;
  private static final int RECORD_SIZE = 29;

  /**
   * Constructor.
   *
   * @param plugin
   * @param cardReader The card reader.
   * @param cardSelectionManager The card selection manager.
   * @param cardSecuritySetting The card security settings.
   */
  CardReaderObserver(
      Plugin plugin,
      CardReader cardReader,
      CardSelectionManager cardSelectionManager,
      SymmetricCryptoSecuritySetting cardSecuritySetting) {
    this.plugin = plugin;
    this.cardReader = cardReader;
    this.cardSelectionManager = cardSelectionManager;
    this.cardSecuritySetting = cardSecuritySetting;
    calypsoCardApiFactory = CalypsoExtensionService.getInstance().getCalypsoCardApiFactory();
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
          SecureRegularModeTransactionManager cardTransactionManager =
              calypsoCardApiFactory
                  .createSecureRegularModeTransactionManager(
                      cardReader, calypsoCard, cardSecuritySetting)
                  .prepareOpenSecureSession(DEBIT)
                  .prepareReadRecords(SFI_ENVIRONMENT_AND_HOLDER, 1, 1, RECORD_SIZE)
                  .prepareReadRecords(SFI_EVENT_LOG, 1, 1, RECORD_SIZE)
                  .prepareReadRecords(SFI_CONTRACT_LIST, 1, 1, RECORD_SIZE)
                  .processCommands(ChannelControl.KEEP_OPEN);

          /*
          Place for the analysis of the context and the list of contracts
          */

          // read the elected contract
          cardTransactionManager
              .prepareReadRecords(SFI_CONTRACTS, 1, 1, RECORD_SIZE)
              .processCommands(ChannelControl.KEEP_OPEN);

          /*
          Place for the analysis of the contracts
          */

          // add an event record and close the Secure Session
          cardTransactionManager
              .prepareAppendRecord(SFI_EVENT_LOG, newEventRecord)
              .prepareCloseSecureSession()
              .processCommands(ChannelControl.CLOSE_AFTER);

          // display transaction time
          logger.info(
              "{}Transaction succeeded. Execution time: {} ms{}",
              ANSI_GREEN,
              System.currentTimeMillis() - timeStamp,
              ANSI_RESET);

          // Optimization: preload the SAM challenge for the next transaction
          cardSecuritySetting.initCryptoContextForNextTransaction();

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

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
package org.eclipse.keyple.card.calypso.example.UseCase10_SessionTrace_TN313;

import org.calypsonet.terminal.calypso.WriteAccessLevel;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.CardTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.core.service.ObservableReader;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.calypsonet.terminal.reader.CardReaderEvent.Type.CARD_INSERTED;
import static org.calypsonet.terminal.reader.CardReaderEvent.Type.CARD_MATCHED;

/** A reader Observer handles card event such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED */
class CardReaderObserver
    implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);
  private final CardReader cardReader;
  private final CardSecuritySetting cardSecuritySetting;
  private final CardSelectionManager cardSelectionManager;

  /**
   * (package-private)<br>
   * Constructor.
   *  @param cardReader The card reader.
   * @param cardSelectionManager The card selection manager.
   * @param cardSecuritySetting The card security settings.
   */
  CardReaderObserver(CardReader cardReader, CardSelectionManager cardSelectionManager, CardSecuritySetting  cardSecuritySetting) {
    this.cardReader = cardReader;
    this.cardSelectionManager = cardSelectionManager;
    this.cardSecuritySetting = cardSecuritySetting;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onReaderEvent(CardReaderEvent event) {
    switch (event.getType()) {
      case CARD_MATCHED:
        // the selection has one target, get the result at index 0
        CalypsoCard calypsoCard =
            (CalypsoCard)
                cardSelectionManager
                    .parseScheduledCardSelectionsResponse(
                        event.getScheduledCardSelectionsResponse())
                    .getActiveSmartCard();

        CardTransactionManager cardTransactionManager = CalypsoExtensionService.getInstance()
                .createCardTransaction(cardReader, calypsoCard, cardSecuritySetting)
                .prepareReadRecordFile(
                        CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER, CalypsoConstants.RECORD_NUMBER_1)
                .prepareReadRecordFile(
                        CalypsoConstants.SFI_EVENT_LOG CalypsoConstants.RECORD_NUMBER_1)
                .processOpening(WriteAccessLevel.DEBIT);

//        cardTransactionManager.pre

        logger.info("= #### End of the card processing.");

        break;

      case CARD_INSERTED:
        logger.error(
            "CARD_INSERTED event: should not have occurred because of the MATCHED_ONLY selection mode chosen.");
        break;

      case CARD_REMOVED:
        logger.trace("There is no card inserted anymore. Return to the waiting state...");
        break;
      default:
        break;
    }

    if (event.getType() == CARD_INSERTED || event.getType() == CARD_MATCHED) {

      // Informs the underlying layer of the end of the card processing, in order to manage the
      // removal sequence.
      ((ObservableReader) (cardReader)).finalizeCardProcessing();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
    logger.error("An exception occurred in plugin '{}', reader '{}'.", pluginName, readerName, e);
  }
}

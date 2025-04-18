/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.core.service.example.UseCase4_ScheduledSelection;

import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the reader observation SPIs.<br>
 * A reader Observer to handle card events such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED
 *
 * @since 2.0.0.0
 */
class CardReaderObserver
    implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);
  private final ObservableCardReader observableCardReader;
  private final CardSelectionManager cardSelectionManager;

  CardReaderObserver(
      ObservableCardReader observableCardReader, CardSelectionManager cardSelectionManager) {
    this.observableCardReader = observableCardReader;
    this.cardSelectionManager = cardSelectionManager;
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
        SmartCard smartCard =
            cardSelectionManager
                .parseScheduledCardSelectionsResponse(event.getScheduledCardSelectionsResponse())
                .getActiveSmartCard();

        logger.info(
            "Observer notification: the selection of the card has succeeded and return the SmartCard = {}.",
            smartCard);

        logger.info("= #### End of the card processing.");

        break;
      case CARD_INSERTED:
        logger.error(
            "CARD_INSERTED event: should not have occurred due to the MATCHED_ONLY selection mode.");
        break;
      case CARD_REMOVED:
        logger.trace("There is no card inserted anymore. Return to the waiting state...");
        break;
      default:
        break;
    }
    if (event.getType() == CardReaderEvent.Type.CARD_INSERTED
        || event.getType() == CardReaderEvent.Type.CARD_MATCHED) {

      // Informs the underlying layer of the end of the card processing, in order to manage the
      // removal sequence.
      observableCardReader.finalizeCardProcessing();
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

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
package org.eclipse.keyple.card.calypso.examples.UseCase2_ScheduledSelection;

import static org.calypsonet.terminal.reader.CardReaderEvent.Type.CARD_INSERTED;
import static org.calypsonet.terminal.reader.CardReaderEvent.Type.CARD_MATCHED;

import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.card.calypso.examples.common.CalypsoConstants;
import org.eclipse.keyple.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A reader Observer handles card event such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED */
class CardReaderObserver
    implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);
  private final CardReader reader;
  private final CardSelectionManager cardSelectionManager;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * <p>Note: the reader is provided here for convenience but could also be retrieved from the
   * {@link SmartCardService} with its name and that of the plugin both present in the {@link
   * CardReaderEvent}.
   *
   * @param reader The card reader.
   * @param cardSelectionManager The card selection manager.
   */
  CardReaderObserver(CardReader reader, CardSelectionManager cardSelectionManager) {
    this.reader = reader;
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
        CalypsoCard calypsoCard =
            (CalypsoCard)
                cardSelectionManager
                    .parseScheduledCardSelectionsResponse(
                        event.getScheduledCardSelectionsResponse())
                    .getActiveSmartCard();

        logger.info(
            "Observer notification: card selection was successful and produced the smart card = {}",
            calypsoCard);
        logger.info("Data read during the scheduled selection process:");
        logger.info(
            "File {}h, rec 1: FILE_CONTENT = {}",
            String.format("%02X", CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER),
            calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));

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
      ((ObservableCardReader) (reader)).finalizeCardProcessing();
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

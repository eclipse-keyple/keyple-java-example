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
package org.eclipse.keyple.card.calypso.example.UseCase2_ScheduledSelection;

import static org.eclipse.keypop.reader.CardReaderEvent.Type.CARD_INSERTED;
import static org.eclipse.keypop.reader.CardReaderEvent.Type.CARD_MATCHED;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.FreeTransactionManager;
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
  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  private final CardReader reader;
  private final CardSelectionManager cardSelectionManager;
  private final SmartCardService smartCardService = SmartCardServiceProvider.getService();
  private final CalypsoExtensionService calypsoExtensionService =
      CalypsoExtensionService.getInstance();
  private final CalypsoCardApiFactory calypsoCardApiFactory =
      calypsoExtensionService.getCalypsoCardApiFactory();

  /**
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

  /** {@inheritDoc} */
  @Override
  public void onReaderEvent(CardReaderEvent event) {
    switch (event.getType()) {
      case CARD_MATCHED:
        CalypsoCard calypsoCard =
            (CalypsoCard)
                cardSelectionManager
                    .parseScheduledCardSelectionsResponse(
                        event.getScheduledCardSelectionsResponse())
                    .getActiveSmartCard();

        FreeTransactionManager transaction =
            calypsoCardApiFactory.createFreeTransactionManager(
                smartCardService.getReader(event.getReaderName()), calypsoCard);
        for (int i = 0; i < 25; i++) {
          transaction.prepareReadRecords(SFI_ENVIRONMENT_AND_HOLDER, 1, 1, 29);
        }
        transaction.processCommands(ChannelControl.CLOSE_AFTER);

        logger.info(
            "Observer notification: card selection was successful and produced the smart card = {}",
            calypsoCard);
        logger.info(
            "Calypso Serial Number = {}", HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));
        logger.info("Data read during the scheduled selection process:");
        logger.info(
            "File {}h, rec 1: FILE_CONTENT = {}",
            SFI_ENVIRONMENT_AND_HOLDER,
            calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER));

        logger.info("= #### End of the card processing.");

        break;

      case CARD_INSERTED:
        logger.error(
            "CARD_INSERTED event: should not have occurred because of the MATCHED_ONLY selection mode chosen.");
        break;

      case CARD_REMOVED:
        logger.info("There is no card inserted anymore. Return to the waiting state...");
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

  /** {@inheritDoc} */
  @Override
  public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
    logger.error("An exception occurred in plugin '{}', reader '{}'.", pluginName, readerName, e);
  }
}

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
package org.eclipse.keyple.example.core.service.UseCase7_PluginAndReaderObservation;

import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the reader observation SPIs.<br>
 * A reader Observer to handle card events such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED
 *
 * @since 2.0.0
 */
class ReaderObserver implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(ReaderObserver.class);
  private final SmartCardService smartCardService = SmartCardServiceProvider.getService();

  @Override
  public void onReaderEvent(CardReaderEvent event) {
    /* just log the event */
    String pluginName =
        smartCardService.getPlugin(smartCardService.getReader(event.getReaderName())).getName();
    logger.info(
        "Event: PLUGINNAME = {}, READERNAME = {}, EVENT = {}",
        pluginName,
        event.getReaderName(),
        event.getType().name());

    if (event.getType() != CardReaderEvent.Type.CARD_REMOVED) {
      ((ObservableCardReader)
              (smartCardService.getPlugin(pluginName).getReader(event.getReaderName())))
          .finalizeCardProcessing();
    }
  }

  @Override
  public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
    logger.error("An exception occurred in plugin '{}', reader '{}'.", pluginName, readerName, e);
  }
}

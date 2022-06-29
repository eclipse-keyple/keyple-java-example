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
package org.eclipse.keyple.core.service.example.UseCase7_PluginAndReaderObservation;

import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.core.service.ObservableReader;
import org.eclipse.keyple.core.service.ReaderEvent;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
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
    String pluginName = smartCardService.getPlugin(smartCardService.getReader(event.getReaderName())).getName();
    logger.info(
        "Event: PLUGINNAME = {}, READERNAME = {}, EVENT = {}",
        pluginName,
        event.getReaderName(),
        event.getType().name());

    if (event.getType() != CardReaderEvent.Type.CARD_REMOVED) {
      ((ObservableCardReader)
              (smartCardService
                  .getPlugin(pluginName)
                  .getReader(event.getReaderName())))
          .finalizeCardProcessing();
    }
  }

  @Override
  public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
    logger.error("An exception occurred in plugin '{}', reader '{}'.", pluginName, readerName, e);
  }
}

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

import java.util.Set;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implements the plugin observation SPIs. A plugin Observer to handle reader events such as
 * READER_CONNECTED or READER_DISCONNECTED.
 *
 * @since 2.0.0
 */
class PluginObserver implements PluginObserverSpi, PluginObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(PluginObserver.class);
  private final ReaderObserver readerObserver;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * <p>Add an observer to all provided readers that are observable.
   *
   * @param initialReaders The readers connected before the plugin is observed.
   * @since 2.0.0
   */
  PluginObserver(Set<? extends CardReader> initialReaders) {
    readerObserver = new ReaderObserver();
    for (CardReader reader : initialReaders) {
      if (reader instanceof ObservableCardReader) {
        addObserver(reader);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onPluginEvent(PluginEvent event) {
    for (String readerName : event.getReaderNames()) {
      // We retrieve the reader object from its name.
      CardReader reader =
          SmartCardServiceProvider.getService()
              .getPlugin(event.getPluginName())
              .getReader(readerName);

      logger.info(
          "PluginEvent: PLUGINNAME = {}, READERNAME = {}, Type = {}",
          event.getPluginName(),
          readerName,
          event.getType());

      switch (event.getType()) {
        case READER_CONNECTED:

          // We are informed here of a connection of a reader. We add an observer to this reader if
          // this is possible.
          logger.info("New reader! READERNAME = {}", readerName);

          // Configure the reader with parameters suitable for contactless operations.
          setupReader(reader);

          if (reader instanceof ObservableCardReader) {
            addObserver(reader);
          }
          break;

        case READER_DISCONNECTED:
          // We are informed here of a disconnection of a reader. The reader object still exists but
          // will be removed from the reader list right after. Thus, we can properly remove the
          // observer attached to this reader before the list update.
          logger.info("Reader removed. READERNAME = {}", readerName);

          if (reader instanceof ObservableCardReader) {
            logger.info("Clear observers of READERNAME = {}", readerName);
            ((ObservableCardReader) reader).clearObservers();
          }
          break;

        default:
          logger.info("Unexpected reader event. EVENT = {}", event.getType().name());
          break;
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onPluginObservationError(String pluginName, Throwable e) {
    logger.error("An exception occurred in plugin '{}.'", pluginName, e);
  }

  /**
   * Configure the reader to handle ISO14443-4 contactless cards
   *
   * @param cardReader The reader.
   */
  private void setupReader(CardReader cardReader) {

    try {
      KeypleReaderExtension readerExtension =
          SmartCardServiceProvider.getService()
              .getPlugin(cardReader)
              .getReaderExtension(KeypleReaderExtension.class, cardReader.getName());
      if (readerExtension instanceof PcscReader) {
        ((PcscReader) readerExtension)
            .setContactless(true)
            .setIsoProtocol(PcscReader.IsoProtocol.T1)
            .setSharingMode(PcscReader.SharingMode.SHARED);
      }
    } catch (Exception e) {
      logger.error("Exception raised while setting up the reader {}", cardReader.getName(), e);
    }

    // Activate the ISO14443 card protocol.
    ((ConfigurableCardReader) cardReader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ConfigurationUtil.ISO_CARD_PROTOCOL);
  }

  /**
   * Add the unique observer to the provided observable reader.
   *
   * @param reader An observable reader
   */
  private void addObserver(CardReader reader) {
    logger.info("Add observer READERNAME = {}", reader.getName());
    ((ObservableCardReader) reader).setReaderObservationExceptionHandler(readerObserver);
    ((ObservableCardReader) reader).addObserver(readerObserver);
    ((ObservableCardReader) reader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);
  }
}

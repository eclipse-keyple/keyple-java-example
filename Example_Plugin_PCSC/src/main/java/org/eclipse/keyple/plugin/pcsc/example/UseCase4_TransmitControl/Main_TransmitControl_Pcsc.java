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
package org.eclipse.keyple.plugin.pcsc.example.UseCase4_TransmitControl;

import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case PC/SC 4 – Transmit control command to the connected reader</h1>
 *
 * <p>Here we demonstrate how to transmit specific commands to a reader using the Transmit Control
 * mechanism offered by PC/SC.
 *
 * <p>This function of the PC/SC plugin is useful to access specific features of the reader such as
 * setting parameters, controlling LEDs, a buzzer or any other proprietary function defined by the
 * reader manufacturer.
 *
 * <p>Here, we show its use to change the color of the RGB LEDs and activate the buzzer of a
 * SpringCard "Puck One" reader.
 *
 * <h2>Scenario</h2>
 *
 * <ul>
 *   <li>Connect a Puck One reader
 *   <li>Run the program: the LED turns yellow
 *   <li>Present a card that matches the AID: the LED turns green as long as the card is present,
 *       and blue when the card is removed
 *   <li>Present a card that does not match the AID: the LED turns red as long as the card is
 *       present, and blue when the card is removed
 * </ul>
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.1.0
 */
public class Main_TransmitControl_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_TransmitControl_Pcsc.class);
  private static final String AID = "315449432E49434131";
  private static final byte[] CMD_SET_LED_RED = HexUtil.toByteArray("581E010000");
  private static final byte[] CMD_SET_LED_GREEN = HexUtil.toByteArray("581E000100");
  private static final byte[] CMD_SET_LED_BLUE = HexUtil.toByteArray("581E000001");
  private static final byte[] CMD_SET_LED_YELLOW = HexUtil.toByteArray("581E010100");
  private static final byte[] CMD_BUZZER_200MS = HexUtil.toByteArray("589300C8");
  private static final Object waitForEnd = new Object();

  public static void main(String[] args) throws InterruptedException {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the contactless reader (we assume that a SpringCard Puck One reader is connected)
    ObservableCardReader reader = null;
    for (CardReader r : plugin.getReaders()) {
      if (r.getName().toLowerCase().contains("contactless")) {
        reader = (ObservableCardReader) r;
      }
    }

    if (reader == null) {
      throw new IllegalStateException("Reader not found");
    }

    PcscReader pcscReader = plugin.getReaderExtension(PcscReader.class, reader.getName());

    // Change the LED color to yellow when no card is connected
    for (int i = 0; i < 3; i++) {
      pcscReader.transmitControlCommand(
          pcscReader.getIoctlCcidEscapeCommandId(), CMD_SET_LED_YELLOW);
      Thread.sleep(200);
      pcscReader.transmitControlCommand(pcscReader.getIoctlCcidEscapeCommandId(), CMD_SET_LED_BLUE);
      Thread.sleep(200);
    }

    // Get the generic card extension service
    GenericExtensionService genericExtensionService = GenericExtensionService.getInstance();

    // check the extension
    smartCardService.checkCardExtension(genericExtensionService);

    ReaderApiFactory readerApiFactory = smartCardService.getReaderApiFactory();
    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a card selection using the generic card extension without specifying any filter
    // (protocol/ATR/DFName).
    CardSelector<IsoCardSelector> cardSelector =
        readerApiFactory.createIsoCardSelector().filterByDfName(AID);

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelector, null);

    // Schedule the selection scenario, always notify card presence.
    cardSelectionManager.scheduleCardSelectionScenario(
        reader, ObservableCardReader.NotificationMode.ALWAYS);

    CardObserver cardObserver = new CardObserver(pcscReader);

    reader.setReaderObservationExceptionHandler(cardObserver);
    reader.addObserver(cardObserver);
    reader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    // Wait indefinitely. CTRL-C to exit.
    synchronized (waitForEnd) {
      waitForEnd.wait();
    }
  }

  /** Card observer class. */
  static class CardObserver
      implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {
    private final PcscReader pcscReader;

    /**
     * Constructor
     *
     * @param pcscReader The PcscReader is use.
     */
    public CardObserver(PcscReader pcscReader) {
      this.pcscReader = pcscReader;
    }

    /**
     * Changes the LED color depending on the event type.
     *
     * @param event The current event.
     */
    @Override
    public void onReaderEvent(CardReaderEvent event) {
      try {
        switch (event.getType()) {
          case CARD_INSERTED:
            pcscReader.transmitControlCommand(
                pcscReader.getIoctlCcidEscapeCommandId(), CMD_BUZZER_200MS);
            pcscReader.transmitControlCommand(
                pcscReader.getIoctlCcidEscapeCommandId(), CMD_SET_LED_RED);
            break;
          case CARD_MATCHED:
            pcscReader.transmitControlCommand(
                pcscReader.getIoctlCcidEscapeCommandId(), CMD_BUZZER_200MS);
            pcscReader.transmitControlCommand(
                pcscReader.getIoctlCcidEscapeCommandId(), CMD_SET_LED_GREEN);
            break;
          case CARD_REMOVED:
            pcscReader.transmitControlCommand(
                pcscReader.getIoctlCcidEscapeCommandId(), CMD_SET_LED_BLUE);
            break;
          case UNAVAILABLE:
            break;
        }
      } finally {
        if (event.getType() != CardReaderEvent.Type.CARD_REMOVED) {
          // indicates the end of the card processing
          // (not needed for a removal event)
          ((ObservableCardReader)
                  SmartCardServiceProvider.getService()
                      .getPlugins()
                      .iterator()
                      .next()
                      .getReader(event.getReaderName()))
              .finalizeCardProcessing();
        }
      }
    }

    @Override
    public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
      logger.error("An exception occurred in plugin '{}', reader '{}'", pluginName, readerName, e);
    }
  }
}

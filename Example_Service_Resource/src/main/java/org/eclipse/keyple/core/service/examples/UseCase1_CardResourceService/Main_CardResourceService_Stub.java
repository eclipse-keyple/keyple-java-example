/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.examples.UseCase1_CardResourceService;

import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.card.generic.GenericCardSelection;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.protocol.ContactCardCommonProtocol;
import org.eclipse.keyple.plugin.stub.StubPlugin;
import org.eclipse.keyple.plugin.stub.StubPluginFactoryBuilder;
import org.eclipse.keyple.plugin.stub.StubReader;
import org.eclipse.keyple.plugin.stub.StubSmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case "resource service 1" – Card resource service (Stub)</h1>
 *
 * <p>We demonstrate here the usage of the card resource service with a local pool of Stub readers.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>The card resource service is configured and started to observe the connection/disconnection
 *       of readers and the insertion/removal of cards.
 *   <li>A command line menu allows you to take and release the two defined types of card resources.
 *   <li>The log and console printouts show the operation of the card resource service.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0
 */
public class Main_CardResourceService_Stub {
  private static final Logger logger = LoggerFactory.getLogger(Main_CardResourceService_Stub.class);
  private static final String READER_A = "READER_A";
  private static final String READER_B = "READER_B";
  public static final String ATR_CARD_A = "3B3F9600805A4880C120501711AABBCC829000";
  public static final String ATR_CARD_B = "3B3F9600805A4880C120501722AABBCC829000";
  public static final String ATR_REGEX_A = "^3B3F9600805A4880C120501711[0-9A-F]{6}829000$";
  public static final String ATR_REGEX_B = "^3B3F9600805A4880C120501722[0-9A-F]{6}829000$";
  public static final String RESOURCE_A = "RESOURCE_A";
  public static final String RESOURCE_B = "RESOURCE_B";
  public static final String READER_NAME_REGEX_A = ".*_A";
  public static final String READER_NAME_REGEX_B = ".*_B";

  public static void main(String[] args) throws InterruptedException {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the StubPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(StubPluginFactoryBuilder.builder().build());

    // Get the generic card extension service
    GenericExtensionService cardExtension = GenericExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    logger.info(
        "=============== UseCase Resource Service #1: card resource service ==================");

    // Create a card resource extension A expecting a card having power-on data matching the regex
    // A.
    GenericCardSelection cardSelectionA =
        GenericExtensionService.getInstance()
            .createCardSelection()
            .filterByPowerOnData(ATR_REGEX_A);

    CardResourceProfileExtension cardResourceExtensionA =
        GenericExtensionService.getInstance().createCardResourceProfileExtension(cardSelectionA);

    // Create a card resource extension B expecting a card having power-on data matching the regex
    // B.
    GenericCardSelection cardSelectionB =
        GenericExtensionService.getInstance()
            .createCardSelection()
            .filterByPowerOnData(ATR_REGEX_B);

    CardResourceProfileExtension cardResourceExtensionB =
        GenericExtensionService.getInstance().createCardResourceProfileExtension(cardSelectionB);

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    PluginAndReaderExceptionHandler pluginAndReaderExceptionHandler =
        new PluginAndReaderExceptionHandler();

    // Configure the card resource service:
    // - allocation mode is blocking with a 100 milliseconds cycle and a 10 seconds timeout.
    // - the readers are searched in the Stub plugin, the observation of the plugin (for the
    // connection/disconnection of readers) and of the readers (for the insertion/removal of cards)
    // is activated.
    // - two card resource profiles A and B are defined, each expecting a specific card
    // characterized by its power-on data and placed in a specific reader.
    // - the timeout for using the card's resources is set at 5 seconds.
    cardResourceService
        .getConfigurator()
        .withBlockingAllocationMode(100, 10000)
        .withPlugins(
            PluginsConfigurator.builder()
                .addPluginWithMonitoring(
                    plugin,
                    new ReaderConfigurator(),
                    pluginAndReaderExceptionHandler,
                    pluginAndReaderExceptionHandler)
                .withUsageTimeout(5000)
                .build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(RESOURCE_A, cardResourceExtensionA)
                .withReaderNameRegex(READER_NAME_REGEX_A)
                .build(),
            CardResourceProfileConfigurator.builder(RESOURCE_B, cardResourceExtensionB)
                .withReaderNameRegex(READER_NAME_REGEX_B)
                .build())
        .configure();

    cardResourceService.start();

    plugin.getExtension(StubPlugin.class).plugReader(READER_A, true, null);
    plugin.getExtension(StubPlugin.class).plugReader(READER_B, true, null);

    // sleep for a moment to let the readers being detected
    Thread.sleep(2000);

    Reader readerA = plugin.getReader(READER_A);
    Reader readerB = plugin.getReader(READER_B);

    logger.info("= #### Connect/disconnect readers, insert/remove cards, watch the log.");

    boolean loop = true;
    CardResource cardResourceA = null;
    CardResource cardResourceB = null;
    while (loop) {
      char c = getInput();
      switch (c) {
        case '1':
          readerA
              .getExtension(StubReader.class)
              .insertCard(
                  StubSmartCard.builder()
                      .withPowerOnData(ByteArrayUtil.fromHex(ATR_CARD_A))
                      .withProtocol(ContactCardCommonProtocol.ISO_7816_3_T0.name())
                      .build());
          break;
        case '2':
          readerA.getExtension(StubReader.class).removeCard();
          break;
        case '3':
          readerB
              .getExtension(StubReader.class)
              .insertCard(
                  StubSmartCard.builder()
                      .withPowerOnData(ByteArrayUtil.fromHex(ATR_CARD_B))
                      .withProtocol(ContactCardCommonProtocol.ISO_7816_3_T0.name())
                      .build());
          break;
        case '4':
          readerB.getExtension(StubReader.class).removeCard();
          break;
        case '5':
          cardResourceA = cardResourceService.getCardResource(RESOURCE_A);
          if (cardResourceA != null) {
            logger.info(
                "Card resource A is available: reader {}, smart card {}",
                cardResourceA.getReader().getName(),
                cardResourceA.getSmartCard());
          } else {
            logger.info("Card resource A is not available");
          }
          break;
        case '6':
          if (cardResourceA != null) {
            logger.info("Release card resource A.");
            cardResourceService.releaseCardResource(cardResourceA);
          } else {
            logger.error("Card resource A is not available");
          }
          break;
        case '7':
          cardResourceB = cardResourceService.getCardResource(RESOURCE_B);
          if (cardResourceB != null) {
            logger.info(
                "Card resource B is available: reader {}, smart card {}",
                cardResourceB.getReader().getName(),
                cardResourceB.getSmartCard());
          } else {
            logger.info("Card resource B is not available");
          }
          break;
        case '8':
          if (cardResourceB != null) {
            logger.info("Release card resource B.");
            cardResourceService.releaseCardResource(cardResourceB);
          } else {
            logger.error("Card resource B is not available");
          }
          break;
        case 'q':
          loop = false;
          break;
        default:
          break;
      }
    }

    // unregister plugin
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program.");
  }

  /**
   * Reader configurator used by the card resource service to setup the SAM reader with the required
   * settings.
   */
  private static class ReaderConfigurator implements ReaderConfiguratorSpi {
    private static final Logger logger = LoggerFactory.getLogger(ReaderConfigurator.class);

    /**
     * (private)<br>
     * Constructor.
     */
    private ReaderConfigurator() {}

    /** {@inheritDoc} */
    @Override
    public void setupReader(Reader reader) {
      // Configure the reader with parameters suitable for contactless operations.
      try {
        ((ConfigurableReader) reader)
            .activateProtocol(
                ContactCardCommonProtocol.ISO_7816_3_T0.name(),
                ContactCardCommonProtocol.ISO_7816_3_T0.name());
      } catch (Exception e) {
        logger.error("Exception raised while setting up the reader {}", reader.getName(), e);
      }
    }
  }

  /** Class implementing the exception handler SPIs for plugin and reader monitoring. */
  private static class PluginAndReaderExceptionHandler
      implements PluginObservationExceptionHandlerSpi, CardReaderObservationExceptionHandlerSpi {

    @Override
    public void onPluginObservationError(String pluginName, Throwable e) {
      logger.error("An exception occurred while monitoring the plugin '{}'.", pluginName, e);
    }

    @Override
    public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
      logger.error(
          "An exception occurred while monitoring the reader '{}/{}'.", pluginName, readerName, e);
    }
  }

  public static char getInput() {

    int key = 0;

    System.out.println("Options:");
    System.out.println("    '1': Insert stub card A");
    System.out.println("    '2': Remove stub card A");
    System.out.println("    '3': Insert stub card B");
    System.out.println("    '4': Remove stub card B");
    System.out.println("    '5': Get resource A");
    System.out.println("    '6': Release resource A");
    System.out.println("    '7': Get resource B");
    System.out.println("    '8': Release resource B");
    System.out.println("    'q': quit");
    System.out.print("Select an option: ");

    try {
      key = System.in.read();
    } catch (java.io.IOException e) {
      logger.error("Input error");
    }

    try {
      while ((System.in.available()) != 0) System.in.read();
    } catch (java.io.IOException e) {
      logger.error("Input error");
    }

    return (char) key;
  }
}

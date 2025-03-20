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
package org.eclipse.keyple.card.calypso.example.UseCase2_ScheduledSelection;

import java.util.Properties;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the process of a Calypso card selection using the PC/SC plugin and a scheduled selection
 * scenario.
 *
 * <p>This class demonstrates the advanced selection of a Calypso card, where the selection
 * operations are prepared ahead of time. Using the card selection manager and Calypso extension
 * service, a scheduled scenario is created and the reader is set to observation mode. Upon card
 * insertion, the prepared selection scenario is automatically executed, and the observer is
 * notified with the selection data collected.
 *
 * <p>The class also highlights the utility of scheduling selection operations in advance for
 * efficient and automated card processing upon insertion, minimizing the latency between card
 * detection and data availability.
 *
 * <p>
 *
 * <h2>Key Functionalities</h2>
 *
 * <ul>
 *   <li>Prepare a scheduled selection scenario targeting a specific Calypso card using its AID, and
 *       include operations to read a file record.
 *   <li>Initiate the observation of the reader for card insertion events.
 *   <li>Handle card insertion events by executing the prepared selection scenario, collecting, and
 *       outputting card data such as FCI and ATR.
 *   <li>Close the physical channel after data collection to ensure resource availability and
 *       security.
 * </ul>
 *
 * <p>All operations and results are logged using slf4j for tracking and debugging purposes. In case
 * of unexpected behavior or errors during the selection process, runtime exceptions are thrown to
 * ensure fail-fast and informative error handling.
 */
public class Main_ScheduledSelection_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_ScheduledSelection_Pcsc.class);
  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";

  // Read the configuration to get the AID to use
  private static final String AID = getAidFromConfiguration();

  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;

  // The plugin used to manage the reader.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader cardReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static CalypsoCardApiFactory calypsoCardApiFactory;

  public static void main(String[] args) throws InterruptedException {

    logger.info("= UseCase Generic #2: scheduled selection ==================");

    // Initialize the context.
    initKeypleService();
    initCardReader();
    initCalypsoCardExtensionService();

    logger.info("= #### Select application with AID = '{}'.", AID);

    // Retrieve the core card selection manager from the Reader API factory.
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create an ISO card selector and set a filter by the specific AID.
    IsoCardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(AID);

    // Initialize a Calypso card selection extension read the record 1 of the file
    // ENVIRONMENT_AND_HOLDER.
    CalypsoCardSelectionExtension calypsoCardSelectionExtension =
        calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()
            .acceptInvalidatedCard()
            .prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, 1);

    // Prepare the card selection scenario by associating the card selector with the Calypso card
    // selection extension.
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    // Schedule the card selection scenario with a configuration to notify only if a card matches
    // the selection criteria.
    cardSelectionManager.scheduleCardSelectionScenario(
        (ObservableCardReader) cardReader, ObservableCardReader.NotificationMode.MATCHED_ONLY);

    // Establish a card reader observer to manage card reader events and errors and initiate the
    // card detection process.
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(cardReader, cardSelectionManager);
    ((ObservableCardReader) cardReader).setReaderObservationExceptionHandler(cardReaderObserver);
    ((ObservableCardReader) cardReader).addObserver(cardReaderObserver);
    ((ObservableCardReader) cardReader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info(
        "= #### Wait for a card. The default AID based selection to be processed as soon as the card is detected.");

    // The program will remain active indefinitely until interrupted by the user (CTRL-C to exit).
    synchronized (waitForEnd) {
      waitForEnd.wait();
    }

    // Clean up by unregistering the plugin before program termination.
    SmartCardServiceProvider.getService().unregisterPlugin(plugin.getName());

    logger.info("Exit program.");

    System.exit(0);
  }

  /**
   * Initializes the Keyple service.
   *
   * <p>Gets an instance of the smart card service, registers the PC/SC plugin, and prepares the
   * reader API factory for use.
   *
   * <p>Retrieves the {@link ReaderApiFactory}.
   */
  private static void initKeypleService() {
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());
    readerApiFactory = smartCardService.getReaderApiFactory();
  }

  /**
   * Initializes the card reader with specific configurations.
   *
   * <p>Prepares the card reader using a predefined set of configurations, including the card reader
   * name regex, ISO protocol, and sharing mode.
   */
  private static void initCardReader() {
    cardReader =
        getReader(
            plugin,
            CARD_READER_NAME_REGEX,
            true,
            PcscReader.IsoProtocol.T1,
            PcscReader.SharingMode.SHARED,
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ISO_CARD_PROTOCOL);
  }

  /**
   * Initializes the Calypso card extension service.
   *
   * <p>Retrieves the {@link CalypsoCardApiFactory}.
   */
  private static void initCalypsoCardExtensionService() {
    CalypsoExtensionService calypsoExtensionService = CalypsoExtensionService.getInstance();
    SmartCardServiceProvider.getService().checkCardExtension(calypsoExtensionService);
    calypsoCardApiFactory = calypsoExtensionService.getCalypsoCardApiFactory();
  }

  /**
   * Configures and returns a card reader based on the provided parameters.
   *
   * <p>It finds the reader name by matching with a regular expression, then configures the reader
   * with the specified settings.
   *
   * @param plugin The plugin used to interact with the card reader.
   * @param readerNameRegex The regular expression to match the card reader's name.
   * @param isContactless A boolean indicating whether the card reader is contactless.
   * @param isoProtocol The ISO protocol used by the card reader.
   * @param sharingMode The sharing mode of the PC/SC reader.
   * @param physicalProtocolName The name of the protocol used by the reader to communicate with
   *     card.
   * @param logicalProtocolName The name of the protocol known by the application.
   * @return The configured card reader.
   */
  private static CardReader getReader(
      Plugin plugin,
      String readerNameRegex,
      boolean isContactless,
      PcscReader.IsoProtocol isoProtocol,
      PcscReader.SharingMode sharingMode,
      String physicalProtocolName,
      String logicalProtocolName) {
    CardReader reader = plugin.findReader(readerNameRegex);

    plugin
        .getReaderExtension(PcscReader.class, reader.getName())
        .setContactless(isContactless)
        .setIsoProtocol(isoProtocol)
        .setSharingMode(sharingMode);

    ((ConfigurableCardReader) reader).activateProtocol(physicalProtocolName, logicalProtocolName);

    return reader;
  }

  /**
   * Retrieves the "aid" property value from the configuration file.
   *
   * @return The value of the "aid" property if present; otherwise, null if the property is not
   *     found or an exception occurs during the file loading process.
   */
  static String getAidFromConfiguration() {
    try {
      Properties props = new Properties();
      props.load(
          Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
      return props.getProperty("aid");
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * This object is used to freeze the main thread while card operations are handle through the
   * observers callbacks. A call to the "notify()" method would end the program (not demonstrated
   * here).
   */
  private static final Object waitForEnd = new Object();
}

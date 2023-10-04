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
package org.eclipse.keyple.core.service.example.UseCase2_ProtocolBasedSelection;

import org.eclipse.keyple.card.generic.GenericCardSelectionExtension;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.*;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 2 – Protocol Based Selection (PC/SC)</h1>
 *
 * <p>We demonstrate here a selection of cards with the only condition being the type of
 * communication protocol they use, in this case the Mifare Classic. Any card of the Mifare Classic
 * type must lead to a "selected" status, any card using another protocol must be ignored.<br>
 * Note that in this case, no APDU "select application" is sent to the card.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card (a Mifare Classic card is
 *       expected here).
 *   <li>Run a selection scenario with the MIFARE CLASSIC protocol filter.
 *   <li>Output the collected smart card data (power-on data).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_ProtocolBasedSelection_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ProtocolBasedSelection_Pcsc.class);

  public static final String CONTACTLESS_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  public static final String MIFARE_CLASSIC_PROTOCOL = "MIFARE_CLASSIC_CARD";
  private static Plugin plugin;
  private static ReaderApiFactory readerApiFactory;
  private static CardReader cardReader;

  public static void main(String[] args) {

    logger.info("= UseCase Generic #2: protocol based card selection ==================");

    initKeypleService();
    initGenericCardExtensionService();
    initCardReader();

    // Check if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      logger.error("No card is present in the reader.");
      System.exit(0);
    }

    logger.info("= #### Select the card if the protocol is '{}'.", MIFARE_CLASSIC_PROTOCOL);

    SmartCard smartCard = selectCard(cardReader);

    if (smartCard == null) {
      logger.error("The selection of the card failed.");
      System.exit(0);
    }

    logger.info("= SmartCard = {}", smartCard);

    logger.info("= #### End of the generic card processing.");

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

  /** Initializes the generic card extension service. */
  private static void initGenericCardExtensionService() {
    GenericExtensionService genericExtensionService = GenericExtensionService.getInstance();
    SmartCardServiceProvider.getService().checkCardExtension(genericExtensionService);
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
            CONTACTLESS_READER_NAME_REGEX,
            true,
            PcscReader.IsoProtocol.T1,
            PcscReader.SharingMode.EXCLUSIVE);
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
   * @return The configured card reader.
   */
  private static CardReader getReader(
      Plugin plugin,
      String readerNameRegex,
      boolean isContactless,
      PcscReader.IsoProtocol isoProtocol,
      PcscReader.SharingMode sharingMode) {
    String readerName = getReaderName(plugin, readerNameRegex);
    CardReader reader = plugin.getReader(readerName);

    plugin
        .getReaderExtension(PcscReader.class, readerName)
        .setContactless(isContactless)
        .setIsoProtocol(isoProtocol)
        .setSharingMode(sharingMode);

    ((ConfigurableCardReader) reader)
        .activateProtocol(PcscSupportedContactlessProtocol.ISO_14443_4.name(), ISO_CARD_PROTOCOL);
    ((ConfigurableCardReader) reader)
        .activateProtocol(
            PcscSupportedContactlessProtocol.MIFARE_CLASSIC.name(), MIFARE_CLASSIC_PROTOCOL);

    return reader;
  }

  /**
   * Searches for and retrieves the name of the reader from the provided plugin's available reader
   * names that matches the given regular expression.
   *
   * <p>This method iterates through the reader names available to the provided plugin, returning
   * the first reader name that matches the supplied regular expression. If no match is found, an
   * IllegalStateException is thrown, indicating the absence of a matching reader name.
   *
   * @param plugin The plugin containing the available reader names to search through.
   * @param readerNameRegex The regular expression used to find a matching reader name among the
   *     available names provided by the plugin.
   * @return The name of the reader that matches the given regular expression from the available
   *     reader names of the provided plugin.
   * @throws IllegalArgumentException if the provided plugin is null, or if the reader name regex is
   *     null or empty.
   * @throws IllegalStateException if no reader name from the available names of the provided plugin
   *     matches the given regular expression.
   */
  private static String getReaderName(Plugin plugin, String readerNameRegex) {
    if (plugin == null) {
      throw new IllegalArgumentException("Plugin cannot be null");
    }

    if (readerNameRegex == null || readerNameRegex.trim().isEmpty()) {
      throw new IllegalArgumentException("Reader name regex cannot be null or empty");
    }

    for (String readerName : plugin.getReaderNames()) {
      if (readerName.matches(readerNameRegex)) {
        logger.info("Card reader found, plugin: {}, name: {}", plugin.getName(), readerName);
        return readerName;
      }
    }

    String errorMsg =
        String.format(
            "Reader matching '%s' not found in plugin '%s'", readerNameRegex, plugin.getName());
    logger.error(errorMsg);
    throw new IllegalStateException(errorMsg);
  }

  /**
   * Selects any card for the transaction.
   *
   * <p>Creates a card selection manager, prepares the selection using no filter.
   *
   * @param reader The reader used to communicate with the card.
   * @return The selected smart card ready for the transaction.
   * @throws IllegalStateException if the selection of the application fails.
   */
  private static SmartCard selectCard(CardReader reader) {
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    CardSelector<BasicCardSelector> cardSelector =
        readerApiFactory.createBasicCardSelector().filterByCardProtocol(MIFARE_CLASSIC_PROTOCOL);
    GenericCardSelectionExtension genericCardSelectionExtension =
        GenericExtensionService.getInstance().createGenericCardSelectionExtension();
    cardSelectionManager.prepareSelection(cardSelector, genericCardSelectionExtension);

    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the card failed.");
    }

    return selectionResult.getActiveSmartCard();
  }
}

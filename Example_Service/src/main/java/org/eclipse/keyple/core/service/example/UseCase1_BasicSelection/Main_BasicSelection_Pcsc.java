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
package org.eclipse.keyple.core.service.example.UseCase1_BasicSelection;

import java.util.List;
import org.eclipse.keyple.card.generic.ChannelControl;
import org.eclipse.keyple.card.generic.GenericCardSelectionExtension;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 1 – Basic Selection (PC/SC)</h1>
 *
 * <p>We demonstrate here a selection of cards without any condition related to the card itself. Any
 * card able to communicate with the reader must lead to a "selected" state.<br>
 * Note that in this case, no APDU "select application" is sent to the card.<br>
 * However, upon selection, an APDU command specific to Global Platform compliant cards is sent to
 * the card and may fail depending on the type of card presented.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card (a GlobalPlatform compliant
 *       card is expected here [e.g. EMV card or Javacard]).
 *   <li>Run a selection scenario without filter.
 *   <li>Output the collected smart card data (power-on data).
 *   <li>Send a additional APDUs to the card (get Card Production Life Cycle data [CPLC]).
 * </ul>
 *
 * <p>All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0.0
 */
public class Main_BasicSelection_Pcsc {
  public static final String CONTACTLESS_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  private static final Logger logger = LoggerFactory.getLogger(Main_BasicSelection_Pcsc.class);
  private static Plugin plugin;
  private static ReaderApiFactory readerApiFactory;
  private static CardReader cardReader;

  public static void main(String[] args) {

    logger.info("= UseCase Generic #1: basic card selection ==================");

    initKeypleService();
    initGenericCardExtensionService();
    initCardReader();

    // CHek the card presence
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select the card with no conditions.");

    SmartCard smartCard = selectCard(cardReader);

    logger.info("= SmartCard = {}", smartCard);

    // Execute an APDU to get CPLC Data (cf. Global Platform Specification)
    byte[] cplcApdu = HexUtil.toByteArray("80CA9F7F00");

    List<String> apduResponses =
        GenericExtensionService.getInstance()
            .createCardTransaction(cardReader, smartCard)
            .prepareApdu(cplcApdu)
            .processApdusToHexStrings(ChannelControl.CLOSE_AFTER);

    logger.info("CPLC Data: '{}'", apduResponses.get(0));

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
            PcscReader.SharingMode.EXCLUSIVE,
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ISO_CARD_PROTOCOL);
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
    String readerName = getReaderName(plugin, readerNameRegex);
    CardReader reader = plugin.getReader(readerName);

    plugin
        .getReaderExtension(PcscReader.class, readerName)
        .setContactless(isContactless)
        .setIsoProtocol(isoProtocol)
        .setSharingMode(sharingMode);

    ((ConfigurableCardReader) reader).activateProtocol(physicalProtocolName, logicalProtocolName);

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
    CardSelector<IsoCardSelector> cardSelector = readerApiFactory.createIsoCardSelector();
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

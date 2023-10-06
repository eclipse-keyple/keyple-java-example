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
package org.eclipse.keyple.card.calypso.example.UseCase14_ReadLegacySamCountersAndCeilings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.FreeTransactionManager;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the reading of the counters and ceilings of a legacy SAM (PC/SC) using the Legacy SAM
 * extension service.
 *
 * <p>This class demonstrates how to set up a simple Legacy SAM transaction to read counters and
 * ceilings.
 *
 * <p>Operations and results are systematically logged via slf4j, facilitating comprehensive
 * monitoring, tracking, and debugging. In the occurrence of unexpected behaviors or anomalies,
 * runtime exceptions are generated, offering clear insights into issues for prompt resolution.
 *
 * <p>Throws IllegalStateException if errors emerge in the signature generation, verification, or
 * resource management processes, fortifying error handling, and security protocols.
 */
public class Main_ReadLegacySamCountersAndCeilings_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ReadLegacySamCountersAndCeilings_Pcsc.class);
  // A regular expression for matching common SAM readers. Adapt as needed.
  private static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  // The logical name of the protocol for communicating with the SAM (optional).
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";

  // The plugin used to manage the readers.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader samReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static LegacySamApiFactory legacySamApiFactory;

  public static void main(String[] args) {

    // Initialize the context
    initKeypleService();
    initLegacySamExtensionService();
    initSamReader();

    // Get the Calypso legacy SAM SmartCard resulting of the selection.
    LegacySam sam = selectSam(samReader);

    // Create a transaction manager
    FreeTransactionManager samTransactionManager =
        legacySamApiFactory.createFreeTransactionManager(samReader, sam);

    // Process the transaction to read counters and ceilings
    samTransactionManager.prepareReadAllCountersStatus().processCommands();

    // Output results
    logger.info("\nSAM event counters =\n{}", gson.toJson(sam.getCounters()));
    logger.info("\nSAM event ceilings =\n{}", gson.toJson(sam.getCounterCeilings()));
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
   * Initializes the SAM reader with specific configurations.
   *
   * <p>Prepares the SAM reader using a predefined set of configurations, including the card reader
   * name regex, ISO protocol, and sharing mode.
   */
  private static void initSamReader() {
    samReader =
        getReader(
            plugin,
            SAM_READER_NAME_REGEX,
            false,
            PcscReader.IsoProtocol.ANY,
            PcscReader.SharingMode.SHARED,
            PcscSupportedContactProtocol.ISO_7816_3_T0.name(),
            SAM_PROTOCOL);
  }

  /**
   * Initializes the Calypso card extension service.
   *
   * <p>Retrieves the {@link CalypsoCardApiFactory}.
   */
  private static void initLegacySamExtensionService() {
    LegacySamExtensionService legacySamExtensionService = LegacySamExtensionService.getInstance();
    SmartCardServiceProvider.getService().checkCardExtension(legacySamExtensionService);
    legacySamApiFactory = legacySamExtensionService.getLegacySamApiFactory();
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
   * Selects the SAM C1 for the transaction.
   *
   * <p>Creates a SAM selection manager, prepares the selection, and processes the SAM selection
   * scenario.
   *
   * @param reader The card reader used to communicate with the SAM.
   * @return The selected SAM for the transaction.
   * @throws IllegalStateException if SAM selection fails.
   */
  private static LegacySam selectSam(CardReader reader) {
    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a card selector without filer
    CardSelector<IsoCardSelector> cardSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    LegacySamApiFactory legacySamApiFactory =
        LegacySamExtensionService.getInstance().getLegacySamApiFactory();

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        cardSelector, legacySamApiFactory.createLegacySamSelectionExtension());

    // SAM communication: run the selection scenario.
    CardSelectionResult samSelectionResult =
        samSelectionManager.processCardSelectionScenario(reader);

    // Check the selection result.
    if (samSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Get the Calypso SAM SmartCard resulting of the selection.
    return (LegacySam) samSelectionResult.getActiveSmartCard();
  }
}

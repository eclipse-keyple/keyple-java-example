/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.calypso.example.common;

import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.sam.CalypsoSamSelection;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ConfigurableCardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing methods for configuring readers and the card resource service used across
 * several examples.
 */
public class ConfigurationUtil {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

  // Common reader identifiers
  // These two regular expressions can be modified to fit the names of the readers used to run these
  // examples.
  public static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  public static final String SAM_PROTOCOL = "ISO_7816_3_T0";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  public static final String INNOVATRON_CARD_PROTOCOL = "INNOVATRON_B_PRIME_CARD";

  /**
   * (private)<br>
   * Constructor.
   */
  private ConfigurationUtil() {}

  private static String getReaderName(Plugin plugin, String readerNameRegex) {
    for (String readerName : plugin.getReaderNames()) {
      if (readerName.matches(readerNameRegex)) {
        logger.info("Card reader, plugin; {}, name: {}", plugin.getName(), readerName);
        return readerName;
      }
    }
    throw new IllegalStateException(
        String.format("Reader '%s' not found in plugin '%s'", readerNameRegex, plugin.getName()));
  }

  /**
   * Retrieves the contactless card reader the first available reader in the provided plugin whose
   * name matches the provided regular expression.
   *
   * @param plugin The plugin to which the reader belongs.
   * @param readerNameRegex A regular expression matching the targeted reader.
   * @return The found card reader.
   * @throws IllegalStateException If the reader is not found.ConfigurationUtil.getSamReader(plugin,
   *     ConfigurationUtil.SAM_READER_NAME_REGEX)
   */
  public static CardReader getCardReader(Plugin plugin, String readerNameRegex) {
    // Get the contactless reader whose name matches the provided regex
    String pcscContactlessReaderName = getReaderName(plugin, readerNameRegex);
    CardReader cardReader = plugin.getReader(pcscContactlessReaderName);

    // Configure the reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, pcscContactlessReaderName)
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setSharingMode(PcscReader.SharingMode.SHARED);

    ((ConfigurableCardReader) cardReader)
        .activateProtocol(PcscSupportedContactlessProtocol.ISO_14443_4.name(), ISO_CARD_PROTOCOL);
    return cardReader;
  }

  /**
   * Retrieves the contact SAM reader the first available reader in the provided plugin whose name
   * matches the provided regular expression.
   *
   * @param plugin The plugin to which the reader belongs.
   * @param readerNameRegex A regular expression matching the targeted reader.
   * @return The found SAM reader.
   * @throws IllegalStateException If the reader is not found.ConfigurationUtil.getSamReader(plugin,
   *     ConfigurationUtil.SAM_READER_NAME_REGEX)
   */
  public static CardReader getSamReader(Plugin plugin, String readerNameRegex) {
    // Get the contact reader dedicated for Calypso SAM whose name matches the provided regex
    String pcscContactReaderName = getReaderName(plugin, readerNameRegex);
    CardReader samReader = plugin.getReader(pcscContactReaderName);

    // Configure the Calypso SAM reader with parameters suitable for contactless operations.
    plugin
        .getReaderExtension(PcscReader.class, pcscContactReaderName)
        .setContactless(false)
        .setIsoProtocol(PcscReader.IsoProtocol.ANY)
        .setSharingMode(PcscReader.SharingMode.SHARED);
    ((ConfigurableCardReader) samReader)
        .activateProtocol(PcscSupportedContactProtocol.ISO_7816_3_T0.name(), SAM_PROTOCOL);
    return samReader;
  }

  /**
   * Attempts to select a SAM and return the {@link CalypsoSam} in case of success.
   *
   * @param samReader The reader in which the SAM is inserted
   * @return A {@link CalypsoSam}.
   * @throws IllegalStateException when the selection of the SAM
   *     failed.ConfigurationUtil.getSamReader(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX)
   */
  public static CalypsoSam getSam(CardReader samReader) {

    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager =
        SmartCardServiceProvider.getService().createCardSelectionManager();

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        CalypsoExtensionService.getInstance().createSamSelection());

    // SAM communication: run the selection scenario.
    CardSelectionResult samSelectionResult =
        samSelectionManager.processCardSelectionScenario(samReader);

    // Check the selection result.
    if (samSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Get the Calypso SAM SmartCard resulting of the selection.
    return (CalypsoSam) samSelectionResult.getActiveSmartCard();
  }

  /**
   * Set up the {@link CardResourceService} to provide a Calypso SAM C1 resource when requested.
   *
   * @param plugin The plugin to which the SAM reader belongs.
   * @param readerNameRegex A regular expression matching the expected SAM reader name.
   * @param samProfileName A string defining the SAM profile.
   * @throws IllegalStateException If the expected card resource is not found.
   */
  public static void setupCardResourceService(
      Plugin plugin, String readerNameRegex, String samProfileName) {

    // Create a card resource extension expecting a SAM "C1".
    CalypsoSamSelection samSelection =
        CalypsoExtensionService.getInstance()
            .createSamSelection()
            .filterByProductType(CalypsoSam.ProductType.SAM_C1);
    CardResourceProfileExtension samCardResourceExtension =
        CalypsoExtensionService.getInstance().createSamResourceProfileExtension(samSelection);

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    // Create a minimalist configuration (no plugin/reader observation)
    cardResourceService
        .getConfigurator()
        .withPlugins(
            PluginsConfigurator.builder().addPlugin(plugin, new ReaderConfigurator()).build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(samProfileName, samCardResourceExtension)
                .withReaderNameRegex(readerNameRegex)
                .build())
        .configure();
    cardResourceService.start();

    // verify the resource availability
    CardResource cardResource = cardResourceService.getCardResource(samProfileName);

    if (cardResource == null) {
      throw new IllegalStateException(
          String.format(
              "Unable to retrieve a SAM card resource for profile '%s' from reader '%s' in plugin '%s'",
              samProfileName, readerNameRegex, plugin.getName()));
    }

    // release the resource
    cardResourceService.releaseCardResource(cardResource);
  }

  /**
   * Reader configurator used by the card resource service to set up the SAM reader with the
   * required settings.
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
    public void setupReader(CardReader cardReader) {
      // Configure the reader with parameters suitable for contactless operations.
      try {
        KeypleReaderExtension readerExtension =
            SmartCardServiceProvider.getService()
                .getPlugin(cardReader)
                .getReaderExtension(KeypleReaderExtension.class, cardReader.getName());
        if (readerExtension instanceof PcscReader) {
          ((PcscReader) readerExtension)
              .setContactless(false)
              .setIsoProtocol(PcscReader.IsoProtocol.ANY)
              .setSharingMode(PcscReader.SharingMode.SHARED);
        }
      } catch (Exception e) {
        logger.error("Exception raised while setting up the reader {}", cardReader.getName(), e);
      }
    }
  }
}

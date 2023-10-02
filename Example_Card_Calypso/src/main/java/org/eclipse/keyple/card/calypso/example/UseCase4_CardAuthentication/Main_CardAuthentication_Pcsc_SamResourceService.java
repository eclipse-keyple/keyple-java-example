/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.calypso.example.UseCase4_CardAuthentication;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.card.calypso.example.common.CalypsoConstants;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySamSelectionExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the process of a Calypso card authentication using the PC/SC plugin and the Calypso Card
 * Extension Service.
 *
 * <p>This class demonstrates the card authentication process, including the initialization of the
 * Smart Card Service, registering the PC/SC plugin, checking the compatibility of the Calypso card
 * extension service, and performing operations with the Reader and Calypso Card APIs.
 *
 * <p>The class also demonstrates how to set up the Card Resource Service to manage the SAM and how
 * to operate the security of transactions.
 *
 * <h2>Key Functionalities</h2>
 *
 * <ul>
 *   <li>Initialization of the Smart Card Service and registering the PC/SC plugin.
 *   <li>Configuration of the card and SAM readers.
 *   <li>Selection of the card and the SAM.
 *   <li>Authentication of the Calypso Card by reading its content in a secure session.
 * </ul>
 *
 * <p>Each operation is logged for tracking and debugging purposes. In case of an error during the
 * card authentication process, an {@link IllegalStateException} is thrown and logged to the
 * console.
 */
public class Main_CardAuthentication_Pcsc_SamResourceService {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_CardAuthentication_Pcsc_SamResourceService.class);

  public static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  public static final String SAM_PROTOCOL = "ISO_7816_3_T0";

  public static final String SAM_PROFILE_NAME = "SAM C1";

  private static CardReader cardReader;
  private static ReaderApiFactory readerApiFactory;
  private static CalypsoCardApiFactory calypsoCardApiFactory;
  private static SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting;
  private static Plugin plugin;

  /**
   * The main method to execute the card authentication process.
   *
   * <p>Initializes the Keyple service, card reader, security settings, and Calypso card extension
   * service. It checks the card presence, selects the card, operates the transaction, and logs the
   * results.
   *
   * @param args the command-line arguments (not used)
   */
  public static void main(String[] args) {
    logger.info("= UseCase Calypso #4: Calypso card authentication ==================");

    // Initialize the context
    initKeypleService();
    initCalypsoCardExtensionService();
    initCardReader();
    initSamResourceService();
    initSecuritySetting();

    // CHek the card presence
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    // Select the card
    CalypsoCard calypsoCard = selectCard(cardReader, CalypsoConstants.AID);

    // Operate the transaction
    calypsoCardApiFactory
        .createSecureRegularModeTransactionManager(
            cardReader, calypsoCard, symmetricCryptoSecuritySetting)
        .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
        .prepareReadRecords(
            CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER,
            CalypsoConstants.RECORD_NUMBER_1,
            CalypsoConstants.RECORD_NUMBER_1,
            CalypsoConstants.RECORD_SIZE)
        .prepareCloseSecureSession()
        .processCommands(ChannelControl.CLOSE_AFTER);

    logger.info(
        "The Secure Session ended successfully, the card is authenticated and the data read are certified.");

    String serialNumberString = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    logger.info("Calypso Serial Number = {}", serialNumberString);
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER,
        calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENVIRONMENT_AND_HOLDER));
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
            PcscReader.SharingMode.EXCLUSIVE,
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ISO_CARD_PROTOCOL);
  }

  /**
   * Initializes the SAM Resource Service making a SAM resource available under the SAM_PROFILE_NAME
   * name.
   */
  private static void initSamResourceService() {
    // Retrieve the reader API factory
    ReaderApiFactory readerApiFactory = SmartCardServiceProvider.getService().getReaderApiFactory();

    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a card selector without filer
    CardSelector<BasicCardSelector> cardSelector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    LegacySamApiFactory legacySamApiFactory =
        LegacySamExtensionService.getInstance().getLegacySamApiFactory();

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        cardSelector, legacySamApiFactory.createLegacySamSelectionExtension());

    // Create a card resource extension expecting a SAM "C1".
    LegacySamSelectionExtension samSelection =
        legacySamApiFactory.createLegacySamSelectionExtension();

    CardResourceProfileExtension samCardResourceExtension =
        LegacySamExtensionService.getInstance()
            .createLegacySamResourceProfileExtension(samSelection);

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    // Create a minimalist configuration (no plugin/reader observation)
    cardResourceService
        .getConfigurator()
        .withPlugins(
            PluginsConfigurator.builder().addPlugin(plugin, new ReaderConfigurator()).build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(SAM_PROFILE_NAME, samCardResourceExtension)
                .withReaderNameRegex(SAM_READER_NAME_REGEX)
                .build())
        .configure();
    cardResourceService.start();

    // verify the resource availability
    CardResource cardResource = cardResourceService.getCardResource(SAM_PROFILE_NAME);

    if (cardResource == null) {
      throw new IllegalStateException(
          String.format(
              "Unable to retrieve a SAM card resource for profile '%s' from reader '%s' in plugin '%s'",
              SAM_PROFILE_NAME, SAM_READER_NAME_REGEX, plugin.getName()));
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

  /**
   * Initializes the security settings for the transaction.
   *
   * <p>Uses the Card Resource Service to retrieve the SAM reader, the SAM, and sets up the
   * symmetric crypto security setting for securing the transaction.
   */
  private static void initSecuritySetting() {
    CardResource samResource =
        CardResourceServiceProvider.getService().getCardResource(CalypsoConstants.SAM_PROFILE_NAME);
    symmetricCryptoSecuritySetting =
        calypsoCardApiFactory.createSymmetricCryptoSecuritySetting(
            LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createSymmetricCryptoTransactionManagerFactory(
                    samResource.getReader(), (LegacySam) samResource.getSmartCard()));
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

  /**
   * Selects the Calypso card for the transaction based on the specified Application Identifier
   * (AID).
   *
   * <p>Creates a card selection manager, prepares the selection using the provided AID, and
   * processes the card selection scenario. The AID is used to identify and select the specific
   * application on the card for the subsequent transaction.
   *
   * @param reader The reader used to communicate with the card.
   * @param aid The Application Identifier (AID) used to select the application on the card. It
   *     cannot be {@code null} or empty.
   * @return The selected Calypso card ready for the transaction.
   * @throws IllegalStateException if the selection of the application identified by the provided
   *     AID fails.
   */
  private static CalypsoCard selectCard(CardReader reader, String aid) {
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    CardSelector<IsoCardSelector> cardSelector =
        readerApiFactory.createIsoCardSelector().filterByDfName(aid);
    CalypsoCardSelectionExtension calypsoCardSelectionExtension =
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard();
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the application " + aid + " failed.");
    }

    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }
}

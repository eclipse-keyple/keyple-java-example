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
import org.eclipse.keypop.calypso.crypto.legacysam.spi.LegacySamDynamicUnlockDataProviderSpi;
import org.eclipse.keypop.calypso.crypto.legacysam.spi.LegacySamStaticUnlockDataProviderSpi;
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
 * extension service, and performing operations with the Keypop Reader and Calypso Card APIs.
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

  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // A regular expression for matching common SAM readers. Adapt as needed.
  private static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  // The logical name of the protocol for communicating with the SAM (optional).
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";

  /** AID: Keyple test kit profile 1, Application 2 */
  private static final String AID = "315449432E49434131";

  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  private static final int RECORD_SIZE = 29;

  // The name of the SAM resource provided by the Card Resource Manager and used during the card
  // transaction.
  private static final String SAM_PROFILE_NAME = "SAM C1";

  // The plugin used to manage the readers.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader cardReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static CalypsoCardApiFactory calypsoCardApiFactory;
  // The security settings for the card transaction.
  private static SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting;

  private static class StaticUnlockDataProvider implements LegacySamStaticUnlockDataProviderSpi {

    @Override
    public byte[] getUnlockData(byte[] samSerialNumber) {
      logger.debug(
          "StaticUnlockDataProvider.getUnlockData: samSerialNumber = {}",
          HexUtil.toHex(samSerialNumber));
      return HexUtil.toByteArray("00112233445566778899AABBCCDDEEFF");
    }
  }

  private static class DynamicUnlockDataProvider implements LegacySamDynamicUnlockDataProviderSpi {

    @Override
    public byte[] getUnlockData(byte[] samSerialNumber, byte[] samChallenge) {
      logger.debug(
          "DynamicUnlockDataProvider.getUnlockData: samSerialNumber = {}",
          HexUtil.toHex(samSerialNumber));
      logger.debug(
          "DynamicUnlockDataProvider.getUnlockData: samChallenge = {}",
          HexUtil.toHex(samChallenge));
      return HexUtil.toByteArray("0011223344556677");
    }
  }

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
    logger.info(
        "= UseCase Calypso #4: Calypso card authentication (Card Resource Service) ==================");

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
    CalypsoCard calypsoCard = selectCard(cardReader, AID);

    // Execute the transaction: the environment file is read within a secure session to ensure data
    // authenticity.
    // Specifying expected response lengths in read commands serves as a protective measure for
    // legacy cards.
    calypsoCardApiFactory
        .createSecureRegularModeTransactionManager(
            cardReader, calypsoCard, symmetricCryptoSecuritySetting)
        .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
        .prepareReadRecords(SFI_ENVIRONMENT_AND_HOLDER, 1, 1, RECORD_SIZE)
        .prepareCloseSecureSession()
        .processCommands(ChannelControl.CLOSE_AFTER);

    logger.info(
        "The secure session has ended successfully; the card is authenticated, and the read data is certified.");

    String serialNumberString = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    logger.info("Calypso Serial Number = {}", serialNumberString);
    logger.info(
        "File {}h, rec 1: FILE_CONTENT = {}",
        SFI_ENVIRONMENT_AND_HOLDER,
        calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER));
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
    // Retrieve the Legacy SAM factory to create the SAM selection and profile extensions.
    LegacySamApiFactory legacySamApiFactory =
        LegacySamExtensionService.getInstance().getLegacySamApiFactory();

    // Create a card resource extension expecting a SAM "C1".
    //    LegacySamSelectionExtension samSelection =
    //        legacySamApiFactory
    //            .createLegacySamSelectionExtension()
    //            .setStaticUnlockDataProvider(new StaticUnlockDataProvider())
    //            .prepareReadAllCountersStatus();
    LegacySamSelectionExtension samSelection =
        legacySamApiFactory
            .createLegacySamSelectionExtension()
            .setDynamicUnlockDataProvider(new DynamicUnlockDataProvider())
            .prepareReadAllCountersStatus();
    //    LegacySamSelectionExtension samSelection =
    //            legacySamApiFactory
    //                    .createLegacySamSelectionExtension()
    //                    .prepareReadAllCountersStatus();
    CardResourceProfileExtension samCardResourceExtension =
        LegacySamExtensionService.getInstance()
            .createLegacySamResourceProfileExtension(
                samSelection,
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    // Get the card resource service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    // Set up a basic configuration without plugin/reader observation.
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

    // Verify if the card resource is available.
    CardResource cardResource = cardResourceService.getCardResource(SAM_PROFILE_NAME);

    if (cardResource == null) {
      throw new IllegalStateException(
          String.format(
              "Failed to retrieve a SAM card resource. No card resource found for profile '%s' with reader matching '%s' in plugin '%s'.",
              SAM_PROFILE_NAME, SAM_READER_NAME_REGEX, plugin.getName()));
    }

    // Release the card resource.
    cardResourceService.releaseCardResource(cardResource);
  }

  /**
   * Reader configurator used by the card resource service to set up the SAM reader with the
   * required settings.
   */
  private static class ReaderConfigurator implements ReaderConfiguratorSpi {

    /** Constructor. */
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
        CardResourceServiceProvider.getService().getCardResource(SAM_PROFILE_NAME);
    symmetricCryptoSecuritySetting =
        calypsoCardApiFactory.createSymmetricCryptoSecuritySetting(
            LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createSymmetricCryptoCardTransactionManagerFactory(
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
    IsoCardSelector cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(aid);
    CalypsoCardSelectionExtension calypsoCardSelectionExtension =
        calypsoCardApiFactory.createCalypsoCardSelectionExtension();
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the application " + aid + " failed.");
    }

    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }
}

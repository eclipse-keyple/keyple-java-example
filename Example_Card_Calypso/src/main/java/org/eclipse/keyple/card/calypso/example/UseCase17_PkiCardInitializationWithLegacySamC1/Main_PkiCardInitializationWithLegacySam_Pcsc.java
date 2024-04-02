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
package org.eclipse.keyple.card.calypso.example.UseCase17_PkiCardInitializationWithLegacySamC1;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.PutDataTag;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.GetDataTag;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.CardCertificateComputationData;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.KeyPairContainer;
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
 * Illustrates the loading of a key pair and Calypso certificates into a PKI card with SAM C1.
 *
 * <p>Each operation is logged for tracking and debugging purposes. In case of an error during the
 * card authentication process, an {@link IllegalStateException} is thrown and logged to the
 * console.
 */
public class Main_PkiCardInitializationWithLegacySam_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_PkiCardInitializationWithLegacySam_Pcsc.class);
  private static byte[] PCA_PUBLIC_KEY_REFERENCE =
      HexUtil.toByteArray("0BA000000291A0000101B0010000000000000000000000000000000002");
  private static byte[] PCA_PUBLIC_KEY =
      HexUtil.toByteArray(
          "C2494557ECE5979A497424833489CCCACF4DEE3FD7576A99C3999D8F468174E7"
              + "6F393D4E5C3802AC6C3CB192EB687F5505D24EBA01FFC60D5752CE6910D50B4A"
              + "DAC8C93159165109C3901FCA383A9F6603D576390FD59899A10873936D3A369B"
              + "3EB8403ADFF476547B039ACC7DCB3C1FAF4F954E29A8C2E2AED7721272AF5CDC"
              + "0A3B2994715261A4364EC1256D00004E084914DC4727349D715C3848D7C54AD5"
              + "8DB0F6907549FED51D564E3A853D44F071A852AB536356C7974B16FC03E1FFE9"
              + "DEE7527FBADDA5BC1116156DBFA5C13F06ACBBCDCEE3F9F4564034A8AD20F407"
              + "32B2AB414891D940ED96DA6DA6E98F766A1CDBC7FD0C17A708BD5F68B816AA47");
  private static final String AID = "A000000291FF9101";
  // A regular expression for matching common contact and contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  private static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";
  private static Plugin plugin;
  private static CardReader cardReader;
  private static CardReader samReader;
  private static ReaderApiFactory readerApiFactory;
  private static CalypsoCardApiFactory calypsoCardApiFactory;
  private static LegacySamApiFactory legacySamApiFactory;
  private static SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting;

  public static void main(String[] args) {
    // Log a message indicating the start of the Use Case
    logger.info("= UseCase Calypso #17: PKI card initialization with a SAM C1 ==================");

    // Initialize services and readers required for the operation
    initServicesAndReaders();

    // Select the Calypso card using the card reader and the target AID
    CalypsoCard calypsoCard = selectCard(cardReader, AID);

    // Optional step: reset PKI data by rewriting symmetric key 1
    calypsoCardApiFactory
        .createSecureRegularModeTransactionManager(
            cardReader, calypsoCard, symmetricCryptoSecuritySetting)
        .prepareChangeKey(1, (byte) 0x21, (byte) 0x74, (byte) 0x21, (byte) 0x74)
        .processCommands(ChannelControl.KEEP_OPEN);

    // Create a container to hold the generated key pair
    KeyPairContainer keyPairContainer = legacySamApiFactory.createKeyPairContainer();

    // Get today's date
    Calendar startDate = new GregorianCalendar();
    startDate.setTime(new Date());

    // Create a copy of the start date to modify for the end date
    Calendar endDate = (Calendar) startDate.clone();

    // Set the end date to 5 years from today, minus 1 day for validity period
    endDate.add(Calendar.YEAR, 5);
    endDate.add(Calendar.DATE, -1);

    // Create the data object for certificate computation
    CardCertificateComputationData cardCertificateComputationData =
        legacySamApiFactory
            .createCardCertificateComputationData()
            // Set the card AID (DF name)
            .setCardAid(calypsoCard.getDfName())
            // Set the card's serial number
            .setCardSerialNumber(calypsoCard.getApplicationSerialNumber())
            // Set the certificate start date using the start date Calendar object
            .setStartDate(
                startDate.get(Calendar.YEAR),
                startDate.get(Calendar.MONTH) + 1, // Month is 0-indexed, so add 1
                startDate.get(Calendar.DAY_OF_MONTH))
            // Set the certificate end date using the modified end date Calendar object
            .setEndDate(
                endDate.get(Calendar.YEAR),
                endDate.get(Calendar.MONTH) + 1, // Month is 0-indexed, so add 1
                endDate.get(Calendar.DAY_OF_MONTH))
            // Set the card startup information
            .setCardStartupInfo(calypsoCard.getStartupInfoRawData());

    // Select the SAM device using the SAM reader
    LegacySam sam = selectSam(samReader);

    // Create a transaction manager for the SAM device
    legacySamApiFactory
        .createFreeTransactionManager(samReader, sam)
        // Prepare to retrieve the CA certificate from the SAM
        .prepareGetTag(GetDataTag.CA_CERTIFICATE)
        // Prepare to generate a key pair by the SAM
        .prepareGenerateCardAsymmetricKeyPair(keyPairContainer)
        // Prepare to compute the card certificate by the SAM using the created data
        .prepareComputeCardCertificate(cardCertificateComputationData)
        // Execute all prepared commands on the SAM device
        .processCommands();

    // Create a transaction manager for the Calypso card
    calypsoCardApiFactory
        .createFreeTransactionManager(cardReader, calypsoCard)
        // Prepare to store the CA certificate on the card
        .preparePutData(PutDataTag.CA_CERTIFICATE, sam.getCaCertificate())
        // Prepare to store the generated key pair on the card
        .preparePutData(PutDataTag.CARD_KEY_PAIR, keyPairContainer.getKeyPair())
        // Prepare to store the computed card certificate on the card
        .preparePutData(
            PutDataTag.CARD_CERTIFICATE, cardCertificateComputationData.getCertificate())
        // Execute all prepared commands on the Calypso card and close the channel afterwards
        .processCommands(ChannelControl.CLOSE_AFTER);
  }

  private static void initServicesAndReaders() {
    initKeypleService();
    initCalypsoCardExtensionService();
    initLegacySamExtensionService();
    initCardReader();
    initSamReader();
    initSecuritySetting();
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
   * Initializes the security settings for the transaction.
   *
   * <p>Prepares the SAM reader, selects the SAM, and sets up the symmetric crypto security setting
   * for securing the transaction.
   */
  private static void initSecuritySetting() {
    LegacySam sam = selectSam(samReader);
    symmetricCryptoSecuritySetting =
        calypsoCardApiFactory.createSymmetricCryptoSecuritySetting(
            LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createSymmetricCryptoCardTransactionManagerFactory(samReader, sam));
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

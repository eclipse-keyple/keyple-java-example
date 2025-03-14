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
package org.eclipse.keyple.card.calypso.example.UseCase16_PkiModeSession;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.pki.CertificateType;
import org.eclipse.keyple.card.calypso.crypto.pki.PkiExtensionService;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.*;
import org.eclipse.keypop.calypso.card.transaction.spi.AsymmetricCryptoCardTransactionManagerFactory;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the process of a Calypso card in PKI Mode using the PC/SC plugin and the Calypso Card
 * Extension Service.
 *
 * <p>This class demonstrates how to operate the card and data strong authentication using the PKI
 * mode.
 *
 * <p>Each operation is logged for tracking and debugging purposes. In case of an error during the
 * card authentication process, an {@link IllegalStateException} is thrown and logged to the
 * console.
 */
public class Main_PkiModeSession_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_PkiModeSession_Pcsc.class);

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

  private static byte[] CA_CERTIFICATE =
      HexUtil.toByteArray(
          "90010BA000000291A0000101B00100000000000000000000000000000000020B"
              + "A000000291A00100024001000000000000000000AEC8E0EA0000000020240222"
              + "00000000090100000000FF00000000000000000000000000000000000000AE0E"
              + "22FC13DA303EDEC0B02E89FC5BCDD1CED8123BAD3877C2C68BDB162C5C63DF6F"
              + "A9BE454ADD615D42D1FD4372A87F0368F0F2603C6CB12CFE3583891D2DA71185"
              + "FC9E3EB9894BD60447CA88200ED35E42AB08EC8606E0782D6005AEE9D282EE1B"
              + "98510E39D747C5070E383E8519720CD79F123B584E3DB31E05A6348369347EF0"
              + "D8C4E38A4553C26B518F235E4459534A990C680F596A19DF87C08F8124B8EA64"
              + "E1245A38BA31A2D400B36CEC7E72C5EE4EDD4C3FA7D2C8BB2A631609C341EF91"
              + "87FF80D21CF417EBE9328D07CA64F4AA40250B285559041BC64D24F5CCCC90B0"
              + "6C8EFFF0C80BADAB4D2D2ABBD21241490805A27AF1B41A282D67D61885CBDD23"
              + "F87271ABD1989C954B3146AE38AE2581DEFE8D48840F9075B9430CDD8ECB1916");

  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";

  // The logical name of the protocol for communicating with the SAM (optional).

  // File structure
  /** AID: Keyple test kit profile 1, Application 2 */
  private static final String AID = "A000000291FF9101";

  private static final byte SFI_EVENT_LOG = (byte) 0x08;
  private static final byte SFI_CONTRACT_LIST = (byte) 0x1E;
  private static final byte SFI_CONTRACTS = (byte) 0x09;
  private static final int RECORD_SIZE = 29;

  // The plugin used to manage the readers.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader cardReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static CalypsoCardApiFactory calypsoCardApiFactory;
  // The security settings for the card transaction.
  private static AsymmetricCryptoSecuritySetting asymmetricCryptoSecuritySetting;

  public static void main(String[] args) {

    logger.info("= UseCase Calypso #16: PKI Mode Session ==================");

    // Initialize the context
    initKeypleService();
    initCalypsoCardExtensionService();
    initCardReader();
    initSecuritySetting();

    // CHek the card presence
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    // Select the card
    CalypsoCard calypsoCard = selectCard(cardReader, AID);

    logger.info("= SmartCard = {}", calypsoCard);

    if (!calypsoCard.isPkiModeSupported()) {
      throw new IllegalStateException("This Calypso card does not support the PKI mode.");
    }

    // Performs file reads using the card transaction manager in non-secure mode.
    SecurePkiModeTransactionManager cardTransaction =
        calypsoCardApiFactory.createSecurePkiModeTransactionManager(
            cardReader, calypsoCard, asymmetricCryptoSecuritySetting);

    processTransaction(cardTransaction, ChannelControl.KEEP_OPEN);

    // the transaction is done twice for performance measurement purpose (avoids the "first load"
    // effect)
    logger.info("============================== NEW TRANSACTION ==============================");

    long currentTimeMillis = System.currentTimeMillis();

    calypsoCard = selectCard(cardReader, AID);

    cardTransaction =
        calypsoCardApiFactory.createSecurePkiModeTransactionManager(
            cardReader, calypsoCard, asymmetricCryptoSecuritySetting);

    processTransaction(cardTransaction, ChannelControl.CLOSE_AFTER);

    long executionTime = System.currentTimeMillis() - currentTimeMillis;

    logger.info("Execution time: {} ms", executionTime);

    logger.info(
        "The secure session has ended successfully, all read data have been authenticated.");

    logger.info("= #### End of the Calypso card processing.");

    System.exit(0);
  }

  private static void processTransaction(
      SecurePkiModeTransactionManager cardTransaction, ChannelControl channelControl) {
    // Operates the transaction.
    // Specifying expected response lengths in read commands serves as a protective measure for
    // legacy cards.
    cardTransaction
        // .prepareGetData(GetDataTag.CA_CERTIFICATE)
        .prepareOpenSecureSession()
        .prepareReadRecords(SFI_CONTRACT_LIST, 1, 1, RECORD_SIZE)
        .prepareReadRecords(SFI_CONTRACTS, 1, 1, RECORD_SIZE)
        .prepareCloseSecureSession()
        .processCommands(channelControl);
  }

  private static void initSecuritySetting() {
    PkiExtensionService pkiExtensionService = PkiExtensionService.getInstance();
    pkiExtensionService.setTestMode();
    AsymmetricCryptoCardTransactionManagerFactory transactionManagerFactory =
        pkiExtensionService.createAsymmetricCryptoCardTransactionManagerFactory();
    asymmetricCryptoSecuritySetting =
        calypsoCardApiFactory.createAsymmetricCryptoSecuritySetting(transactionManagerFactory);
    asymmetricCryptoSecuritySetting
        .addPcaCertificate(
            pkiExtensionService.createPcaCertificate(PCA_PUBLIC_KEY_REFERENCE, PCA_PUBLIC_KEY))
        // Uncomment the following line to inject the CA certificate into the parameters and avoid
        // reading it from the card.
        // .addCaCertificate(pkiExtensionService.createCaCertificate(CA_CERTIFICATE))
        .addCaCertificateParser(
            pkiExtensionService.createCaCertificateParser(CertificateType.CALYPSO_LEGACY))
        .addCardCertificateParser(
            pkiExtensionService.createCardCertificateParser(CertificateType.CALYPSO_LEGACY));
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
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard();
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the application " + aid + " failed.");
    }

    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }
}

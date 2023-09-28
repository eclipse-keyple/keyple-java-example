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
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
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
 * Handles the process of a Calypso card authentication using the PC/SC plugin and the Calypso Card
 * Extension Service.
 *
 * <p>This class demonstrates the card authentication process, including the initialization of the
 * Smart Card Service, registering the PC/SC plugin, checking the compatibility of the Calypso card
 * extension service, and performing operations with the Reader and Calypso Card APIs.
 *
 * <p>The class also demonstrates how to retrieve the card and SAM readers using regular expressions
 * to match their names and how to operate the security of transactions using SAM.
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
public class Main_CardAuthentication_PcscRefactored {
  private static final Logger logger = LoggerFactory.getLogger(Main_CardAuthentication_Pcsc.class);

  public static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  public static final String SAM_PROTOCOL = "ISO_7816_3_T0";

  private final CalypsoCardApiFactory calypsoCardApiFactory;
  private final CardReader cardReader;
  private final CardReader samReader;
  private final ReaderApiFactory readerApiFactory;
  private final LegacySam sam;

  /**
   * Constructor for the Main_CardAuthentication_PcscRefactored class.
   *
   * <p>This constructor performs the initial setup required for the card authentication
   * demonstrated here.
   *
   * <p>It initializes the smart card service, registers the PC/SC plugin and checks the Calypso
   * Card Extension Service compatibility with the current service.
   *
   * <p>It then prepares the factories for the Reader and Calypso Card APIs, and retrieves the card
   * and SAM readers based on the provided regular expressions for their names.
   *
   * <p>Finally it retrieve the SAM that will be used to operate the security of the transaction.
   *
   * <ul>
   *   <li>Obtains an instance of the SmartCardService
   *   <li>Registers the PC/SC plugin with the smart card service
   *   <li>Gets the CalypsoExtensionService instance and checks its compatibility with the
   *       SmartCardService
   *   <li>Initializes the ReaderApiFactory and CalypsoCardApiFactory for further operations
   *   <li>Retrieves the card and SAM readers based on name matching with the provided regular
   *       expressions
   *   <li>Selects the SAM C1
   * </ul>
   */
  public Main_CardAuthentication_PcscRefactored() {
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();
    smartCardService.checkCardExtension(calypsoCardService);
    readerApiFactory = smartCardService.getReaderApiFactory();
    calypsoCardApiFactory = calypsoCardService.getCalypsoCardApiFactory();
    cardReader = getCardReader(plugin);
    samReader = getSamReader(plugin);
    sam = selectSam();
  }

  /**
   * The entry point of the application.
   *
   * <p>It creates an instance of {@link Main_CardAuthentication_PcscRefactored} and initiates the
   * card authentication process.
   *
   * @param args The command-line arguments. Not used in this implementation.
   */
  public static void main(String[] args) {
    logger.info(
        "=============== UseCase Calypso #4: Calypso card authentication ==================");
    new Main_CardAuthentication_PcscRefactored().runCardAuthentication(CalypsoConstants.AID);
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
  private CardReader getReader(
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
   * Retrieves and configures the card reader.
   *
   * @param plugin The plugin used for interactions with the card reader.
   * @return The configured card reader.
   */
  private CardReader getCardReader(Plugin plugin) {
    return getReader(
        plugin,
        CARD_READER_NAME_REGEX,
        true,
        PcscReader.IsoProtocol.T1,
        PcscReader.SharingMode.EXCLUSIVE,
        PcscSupportedContactlessProtocol.ISO_14443_4.name(),
        ISO_CARD_PROTOCOL);
  }

  /**
   * Retrieves and configures the SAM reader.
   *
   * @param plugin The plugin used for interactions with the SAM reader.
   * @return The configured SAM reader.
   */
  private CardReader getSamReader(Plugin plugin) {
    return getReader(
        plugin,
        SAM_READER_NAME_REGEX,
        false,
        PcscReader.IsoProtocol.ANY,
        PcscReader.SharingMode.SHARED,
        PcscSupportedContactProtocol.ISO_7816_3_T0.name(),
        SAM_PROTOCOL);
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
  private String getReaderName(Plugin plugin, String readerNameRegex) {
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
   * Executes the card authentication process for the specified Application Identifier (AID).
   *
   * <p>This method logs the steps and results of the authentication process and handles exceptions
   * that might occur during execution. The AID is used to identify the expected application on the
   * card to be authenticated.
   *
   * @param aid The Application Identifier (AID) used to select the expected application on the
   *     card. It cannot be {@code null} or empty.
   */
  private void runCardAuthentication(String aid) {
    try {
      if (!cardReader.isCardPresent()) {
        throw new IllegalStateException("No card is present in the reader.");
      }

      logger.info("= #### Select application with AID = '{}'.", aid);

      CalypsoCard calypsoCard = selectCalypsoCard(aid);
      readCardDataInASecureSession(calypsoCard, sam);

      logger.info("= #### End of the Calypso card processing.");
    } catch (Exception e) {
      logger.error("Error during card authentication: {}", e.getMessage());
    }
  }

  /**
   * Selects the SAM C1 for the transaction.
   *
   * <p>Creates a SAM selection manager, prepares the selection, and processes the SAM selection
   * scenario.
   *
   * @return The selected SAM for the transaction.
   * @throws IllegalStateException if SAM selection fails.
   */
  private LegacySam selectSam() {
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
        samSelectionManager.processCardSelectionScenario(samReader);

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
   * @param aid The Application Identifier (AID) used to select the application on the card. It
   *     cannot be {@code null} or empty.
   * @return The selected Calypso card ready for the transaction.
   * @throws IllegalStateException if the selection of the application identified by the provided
   *     AID fails.
   */
  private CalypsoCard selectCalypsoCard(String aid) {
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    CardSelector<IsoCardSelector> cardSelector =
        readerApiFactory.createIsoCardSelector().filterByDfName(aid);
    CalypsoCardSelectionExtension calypsoCardSelectionExtension =
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard();
    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension);

    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the application " + aid + " failed.");
    }

    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }

  /**
   * Reads data from the Calypso card in a secure session.
   *
   * <p>Prepares and processes command to open a secure session, read some files, close the secure
   * session and logs the results.
   *
   * @param calypsoCard The Calypso card to read data from.
   * @param sam The SAM (Secure Access Module) used for the secure transaction.
   */
  private void readCardDataInASecureSession(CalypsoCard calypsoCard, LegacySam sam) {
    SymmetricCryptoSecuritySetting cardSecuritySetting =
        calypsoCardApiFactory.createSymmetricCryptoSecuritySetting(
            LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createSymmetricCryptoTransactionManagerFactory(samReader, sam));

    calypsoCardApiFactory
        .createSecureRegularModeTransactionManager(cardReader, calypsoCard, cardSecuritySetting)
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
}

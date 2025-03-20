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
package org.eclipse.keyple.card.calypso.example.UseCase9_ChangePin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
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
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.calypso.card.transaction.ChannelControl;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the process of changing the PIN code of a Calypso card using the PC/SC plugin,
 * demonstrating both plain and encrypted PIN verification methods.
 *
 * <p>This class demonstrates the various steps involved in PIN code verification for a Calypso
 * card, detailing operations of plain and encrypted PIN verification.
 *
 * <h2>Key Functionalities</h2>
 *
 * <ul>
 *   <li>Identify the presence of an ISO 14443-4 card and activate the card selection manager for
 *       targeted operations.
 *   <li>Initiate an AID-based application selection scenario to pinpoint and select a specific
 *       Calypso card.
 *   <li>Set ciphering keys needed for the PIN change operation.
 *   <li>Create a Secure Regular Mode Transaction Manager for encrypted PIN change operations.
 *   <li>Prompt the user to enter a new 4-digit numeric PIN code and validate the input.
 *   <li>Prepare and process the command to change the PIN code.
 *   <li>Prepare and process the command to verify the new PIN code.
 * </ul>
 *
 * <strong>Warning:</strong> On recent Windows versions, delayed PIN entry can cause loss of
 * communication with the smart card (the card is reset after 5 seconds of inactivity).
 *
 * <p>Operations and outcomes are meticulously logged using slf4j, offering detailed insights for
 * tracking and debugging. In scenarios where unexpected behaviors or errors emerge, runtime
 * exceptions are triggered, offering precise, actionable feedback for timely resolution.
 */
public class Main_ChangePin_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_ChangePin_Pcsc.class);

  // A regular expression for matching common contactless card readers. Adapt as needed.
  private static final String CARD_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  // A regular expression for matching common SAM readers. Adapt as needed.
  private static final String SAM_READER_NAME_REGEX = ".*Identive.*|.*HID.*|.*SAM.*";
  // The logical name of the protocol for communicating with the card (optional).
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  // The logical name of the protocol for communicating with the SAM (optional).
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";

  // Read the configuration to get the AID to use
  private static final String AID = getAidFromConfiguration();

  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;

  private static final byte PIN_MODIFICATION_CIPHERING_KEY_KIF = (byte) 0x21;
  private static final byte PIN_MODIFICATION_CIPHERING_KEY_KVC = (byte) 0x79;
  private static final byte PIN_VERIFICATION_CIPHERING_KEY_KIF = (byte) 0x30;
  private static final byte PIN_VERIFICATION_CIPHERING_KEY_KVC = (byte) 0x79;
  // The plugin used to manage the readers.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static CardReader cardReader;
  // The reader used to communicate with the SAM.
  private static CardReader samReader;
  // The factory used to create the selection manager and card selectors.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static CalypsoCardApiFactory calypsoCardApiFactory;
  // The security settings for the card transaction.
  private static SymmetricCryptoSecuritySetting symmetricCryptoSecuritySetting;

  public static void main(String[] args) throws InterruptedException, IOException {
    logger.info("= UseCase Calypso #9: Calypso card Change PIN ==================");

    // Initialize the context
    initKeypleService();
    initCalypsoCardExtensionService();
    initCardReader();
    initSamReader();
    initSecuritySetting();

    // Verify if a card is present in the reader
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    // Select the card
    CalypsoCard calypsoCard = selectCard(cardReader, AID);

    logger.info("= SmartCard = {}", calypsoCard);

    // Retrieve and log the Calypso card's Serial Number
    String csn = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    logger.info("Calypso Serial Number = {}", csn);

    // Add the key identifiers needed for ciphering the PIN.
    symmetricCryptoSecuritySetting
        .setPinVerificationCipheringKey(
            PIN_VERIFICATION_CIPHERING_KEY_KIF, PIN_VERIFICATION_CIPHERING_KEY_KVC)
        .setPinModificationCipheringKey(
            PIN_MODIFICATION_CIPHERING_KEY_KIF, PIN_MODIFICATION_CIPHERING_KEY_KVC);

    // Instantiate a Secure Regular Mode Transaction Manager to handle encrypted PIN verification
    // and secure operations.
    SecureRegularModeTransactionManager cardTransaction =
        calypsoCardApiFactory.createSecureRegularModeTransactionManager(
            cardReader, calypsoCard, symmetricCryptoSecuritySetting);

    // Short delay to allow logs to be displayed before the prompt
    Thread.sleep(2000);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String inputString;
    byte[] newPinCode = new byte[4];
    boolean validPinCodeEntered = false;
    do {
      System.out.print("Enter new PIN code (4 numeric digits):");
      inputString = br.readLine();
      if (!inputString.matches("[0-9]{4}")) {
        System.out.println("Invalid PIN code.");
      } else {
        newPinCode = inputString.getBytes();
        validPinCodeEntered = true;
      }
    } while (!validPinCodeEntered);

    // Change the PIN (correct)
    cardTransaction.prepareChangePin(newPinCode).processCommands(ChannelControl.KEEP_OPEN);

    logger.info("PIN code value successfully updated to {}", inputString);

    // Verification of the PIN
    cardTransaction.prepareVerifyPin(newPinCode).processCommands(ChannelControl.CLOSE_AFTER);
    logger.info("Remaining attempts: {}", calypsoCard.getPinAttemptRemaining());

    logger.info("PIN {} code successfully presented.", inputString);

    logger.info("= #### End of the Calypso card processing.");

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
    IsoCardSelector cardSelector =
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

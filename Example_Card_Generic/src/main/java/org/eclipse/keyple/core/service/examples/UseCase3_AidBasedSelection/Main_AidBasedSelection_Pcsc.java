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
package org.eclipse.keyple.core.service.examples.UseCase3_AidBasedSelection;

import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.calypsonet.terminal.reader.selection.spi.CardSelection;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.examples.common.ConfigurationUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Generic 3 – AID Based Selection (PC/SC)</h1>
 *
 * <p>We present here a selection of cards including the transmission of a "select application" APDU
 * targeting a specific DF Name. Any card with an application whose DF Name starts with the provided
 * AID should lead to a "selected" state, any card with another DF Name should be ignored.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card with the specified AID (here
 *       the EMV PPSE AID).
 *   <li>Run a selection scenario with the DF Name filter.
 *   <li>Output the collected smart card data (power-on data).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0
 */
public class Main_AidBasedSelection_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_AidBasedSelection_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the generic card extension service
    GenericExtensionService cardExtension = GenericExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    // Get the contactless reader whose name matches the provided regex
    Reader reader =
        ConfigurationUtil.getCardReader(plugin, ConfigurationUtil.CONTACTLESS_READER_NAME_REGEX);

    logger.info("=============== UseCase Generic #3: AID based card selection ==================");

    // Check if a card is present in the reader
    if (!reader.isCardPresent()) {
      logger.error("No card is present in the reader.");
      System.exit(0);
    }

    logger.info(
        "= #### Select the card if its DF Name matches '{}'.", ConfigurationUtil.AID_EMV_PPSE);

    // Get the core card selection manager.
    CardSelectionManager cardSelectionManager = smartCardService.createCardSelectionManager();

    // Create a card selection using the generic card extension without specifying any filter
    // (protocol/power-on data/DFName).
    CardSelection cardSelection =
        cardExtension.createCardSelection().filterByDfName(ConfigurationUtil.AID_EMV_PPSE);

    // Prepare the selection by adding the created generic selection to the card selection scenario.
    cardSelectionManager.prepareSelection(cardSelection);

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult = cardSelectionManager.processCardSelectionScenario(reader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      logger.error("The selection of the card failed.");
      System.exit(0);
    }

    // Get the SmartCard resulting of the selection.
    SmartCard smartCard = selectionResult.getActiveSmartCard();

    logger.info("= SmartCard = {}", smartCard);

    logger.info("= #### End of the generic card processing.");

    System.exit(0);
  }
}

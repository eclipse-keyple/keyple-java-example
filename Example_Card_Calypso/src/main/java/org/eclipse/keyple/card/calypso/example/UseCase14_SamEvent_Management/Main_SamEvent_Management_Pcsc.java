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
package org.eclipse.keyple.card.calypso.example.UseCase14_SamEvent_Management;

import org.calypsonet.terminal.calypso.crypto.legacysam.sam.LegacySam;
import org.calypsonet.terminal.calypso.crypto.legacysam.sam.LegacySamSelection;
import org.calypsonet.terminal.calypso.crypto.legacysam.transaction.LegacySamFreeTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.crypto.legacysam.CardSelectionFactoryProvider;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamTransactionManagerFactoryProvider;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main_SamEvent_Management_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_SamEvent_Management_Pcsc.class);

  public static void main(String[] args) {
    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding PC/SC plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Retrieve the SAM reader
    CardReader samReader =
        ConfigurationUtil.getSamReader(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX);

    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager =
        SmartCardServiceProvider.getService().createCardSelectionManager();

    // Create a SAM selection.
    LegacySamSelection samSelection =
        CardSelectionFactoryProvider.getFactory().createSamSelection();

    // Provide the SAM selection to the card selection manager
    samSelectionManager.prepareSelection(samSelection);

    // SAM communication: run the selection scenario.
    CardSelectionResult samSelectionResult =
        samSelectionManager.processCardSelectionScenario(samReader);

    // Check the selection result.
    if (samSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("The selection of the SAM failed.");
    }

    // Get the Calypso legacy SAM SmartCard resulting of the selection.
    LegacySam sam = (LegacySam) samSelectionResult.getActiveSmartCard();

    LegacySamFreeTransactionManager samTransactionManager =
        LegacySamTransactionManagerFactoryProvider.getFactory()
            .createFreeTransactionManager(samReader, sam);

    samTransactionManager
        .prepareReadEventCounters(0, 26)
        .prepareReadEventCeilings(0, 26)
        .processCommands();

    logger.info("SAM content: {}", JsonUtil.toJson(sam));
  }
}

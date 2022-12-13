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
import org.calypsonet.terminal.calypso.crypto.legacysam.sam.LegacySam;
import org.calypsonet.terminal.calypso.crypto.legacysam.sam.LegacySamSelection;
import org.calypsonet.terminal.calypso.crypto.legacysam.transaction.LSFreeTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamCardExtensionService;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use Case Calypso 14 – Read counters and ceilings of a legacy SAM (PC/SC)
 *
 * <p>This example shows how to set up a simple Legacy SAM transaction to read counters and
 * ceilings.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_ReadLegacySamCountersAndCeilings_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_ReadLegacySamCountersAndCeilings_Pcsc.class);
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public static void main(String[] args) {
    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding PC/SC plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the legacy SAM card extension service
    LegacySamCardExtensionService legacySamCardExtensionService =
        LegacySamCardExtensionService.getInstance();

    // Check the APIs compliance (optional)
    smartCardService.checkCardExtension(legacySamCardExtensionService);

    // Retrieve the SAM reader
    CardReader samReader =
        ConfigurationUtil.getSamReader(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX);

    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager =
        SmartCardServiceProvider.getService().createCardSelectionManager();

    // Create a SAM selection.
    LegacySamSelection samSelection =
        legacySamCardExtensionService.getLegacySamSelectionFactory().createSamSelection();

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

    // Create a transaction manager
    LSFreeTransactionManager samTransactionManager =
        legacySamCardExtensionService
            .getTransactionManagerFactory()
            .createFreeTransactionManager(samReader, sam);

    // Process the transaction to read counters and ceilings
    samTransactionManager
        .prepareReadEventCounters(0, 26)
        .prepareReadEventCeilings(0, 26)
        .processCommands();

    // Output results
    logger.info("\nSAM event counters =\n{}", gson.toJson(sam.getEventCounters()));
    logger.info("\nSAM event ceilings =\n{}", gson.toJson(sam.getEventCeilings()));
  }
}

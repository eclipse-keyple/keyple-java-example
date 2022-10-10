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

import org.calypsonet.terminal.calypso.sam.CalypsoSam;
import org.calypsonet.terminal.calypso.transaction.SamSecuritySetting;
import org.calypsonet.terminal.calypso.transaction.SamTransactionManager;
import org.calypsonet.terminal.reader.CardReader;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
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

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    CardReader samReader =
        ConfigurationUtil.getSamReader(plugin, ConfigurationUtil.SAM_READER_NAME_REGEX);

    // Get the Calypso SAM SmartCard after selection.
    CalypsoSam calypsoSam = ConfigurationUtil.getSam(samReader);

    logger.info("= SAM = {}", JsonUtil.toJson(calypsoSam));

    SamSecuritySetting samSecuritySetting =
        CalypsoExtensionService.getInstance().createSamSecuritySetting();

    SamTransactionManager samTransactionManager =
        CalypsoExtensionService.getInstance()
            .createSamTransaction(samReader, calypsoSam, samSecuritySetting);

    samTransactionManager.prepareReadEventCounters(0, 26);
    samTransactionManager.prepareReadEventCeilings(0, 26);
    samTransactionManager.processCommands();

    logger.info("= SAM = {}", JsonUtil.toJson(calypsoSam));
  }
}

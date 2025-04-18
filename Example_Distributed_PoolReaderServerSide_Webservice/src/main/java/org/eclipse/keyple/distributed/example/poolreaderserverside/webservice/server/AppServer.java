/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Distribution License 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ************************************************************************************** */
package org.eclipse.keyple.distributed.example.poolreaderserverside.webservice.server;

import javax.enterprise.context.ApplicationScoped;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.distributed.LocalServiceClient;
import org.eclipse.keyple.distributed.LocalServiceServerFactory;
import org.eclipse.keyple.distributed.LocalServiceServerFactoryBuilder;
import org.eclipse.keyple.plugin.stub.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Example of a server side application. */
@ApplicationScoped
public class AppServer {

  private static final Logger logger = LoggerFactory.getLogger(AppServer.class);
  public static final String LOCAL_SERVICE_NAME = "LOCAL_SERVICE_#1";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";

  private PoolPlugin poolPlugin;

  /**
   * Initialize the server components :
   *
   * <ul>
   *   <li>A {@link PoolPlugin} with a {@link StubPoolPlugin} with a inserted card,
   *   <li>A {@link LocalServiceClient} with a sync node bind to a {@link
   *       org.eclipse.keyple.distributed.spi.SyncEndpointClientSpi} endpoint.
   * </ul>
   */
  public void init() {

    // Init a local pool plugin.
    initStubPoolPlugin();

    // Init the local service factory.
    LocalServiceServerFactory factory =
        LocalServiceServerFactoryBuilder.builder(LOCAL_SERVICE_NAME)
            .withSyncNode() // HTTP webservice needs a server sync node configuration
            .withPoolPlugins(poolPlugin.getName()) // use the registered ReaderPoolPlugin
            .build();

    // Init the local service using the associated factory.
    SmartCardServiceProvider.getService().registerDistributedLocalService(factory);
  }

  /** Init a local pool plugin with a stub pool plugin and an inserted card */
  private void initStubPoolPlugin() {

    String STUB_PLUGIN_NAME = "stubPoolPlugin";
    String STUB_READER_NAME = "stubReader";
    String REFERENCE_GROUP = "group1";

    // Registers the plugin to the smart card service.
    poolPlugin =
        (PoolPlugin)
            SmartCardServiceProvider.getService()
                .registerPlugin(StubPoolPluginFactoryBuilder.builder().build());

    // Plug manually to the plugin a local reader associated in a group reference.
    poolPlugin
        .getExtension(StubPoolPlugin.class)
        .plugPoolReader(REFERENCE_GROUP, STUB_READER_NAME, getStubCard());

    logger.info(
        "Server - Local plugin was configured with a STUB reader : {} in group reference : {}",
        STUB_READER_NAME,
        REFERENCE_GROUP);
  }

  /**
   * Returns a new instance of a stub card.
   *
   * @return A new instance.
   */
  private StubSmartCard getStubCard() {
    return StubSmartCard.builder()
        .withPowerOnData(HexUtil.toByteArray("3B8880010000000000718100F9"))
        .withProtocol(ISO_CARD_PROTOCOL)
        /* Select Application */
        .withSimulatedCommand("00A4040005AABBCCDDEE00", "6A82")
        /* Select Application */
        .withSimulatedCommand(
            "00A4040009315449432E4943413100",
            "6F238409315449432E49434131A516BF0C13C708000000001122334453070A3C23121410019000")
        /* Read Records - EnvironmentAndHolder (SFI=07)) */
        .withSimulatedCommand(
            "00B2013C00", "24B92848080000131A50001200000000000000000000000000000000009000")
        /* Open Secure Session V3.1 */
        .withSimulatedCommand(
            "008A0B4104C1C2C3C400",
            "030490980030791D01112233445566778899AABBCCDDEEFF00112233445566778899AABBCC9000")
        /* Open Secure Session V3.1 */
        .withSimulatedCommand(
            "008A0B3904C1C2C3C400",
            "0308306C00307E1D24B928480800000606F0001200000000000000000000000000000000009000")
        /* Read Records */
        .withSimulatedCommand(
            "00B2014400", "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC9000")
        /* Read Records */
        .withSimulatedCommand(
            "00B201F400", "00000000000000000000000000000000000000000000000000000000009000")
        /* Read Records */
        .withSimulatedCommand(
            "00B2014C00", "00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF9000")
        /* Read Records */
        .withSimulatedCommand(
            "00B2014D00",
            "011D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF021D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF031D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF041D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF9000")
        /* Append Record */
        .withSimulatedCommand(
            "00E200401D00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC", "9000")
        /* Append Record */
        .withSimulatedCommand(
            "00E200401D01112233445566778899AABBCCDDEEFF00112233445566778899AABBCC", "9000")
        /* Close Secure Session */
        /* no ratification asked */
        .withSimulatedCommand("008E0000040506070800", "010203049000")
        /* ratification asked */
        .withSimulatedCommand("008E8000040506070800", "010203049000")
        /* Ratification */
        .withSimulatedCommand("00B2000000", "6B00")
        .build();
  }
}

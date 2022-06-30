/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.distributed.example.poolreaderserverside.webservice.client;

import java.util.SortedSet;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.distributed.RemotePluginClientFactory;
import org.eclipse.keyple.distributed.RemotePoolPluginClientFactoryBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** Example of a client side application. */
@ApplicationScoped
public class AppClient {

  public static final String REMOTE_PLUGIN_NAME = "REMOTE_PLUGIN_#1";

  /** The endpoint client */
  @Inject @RestClient EndpointClient endpointClient;

  /**
   * Initialize the client components :
   *
   * <ul>
   *   <li>A pool {@link org.eclipse.keyple.distributed.RemotePluginClient} with a sync node.
   * </ul>
   */
  public void init() {

    // Init the remote plugin factory.
    RemotePluginClientFactory factory =
        RemotePoolPluginClientFactoryBuilder.builder(REMOTE_PLUGIN_NAME)
            .withSyncNode(endpointClient)
            .build();

    // Register the remote plugin to the smart card service using the factory.
    SmartCardServiceProvider.getService().registerPlugin(factory);
  }

  /**
   * Executes a simple scenario : allocate a reader, execute a transaction, release the reader.
   *
   * @return true if the transaction was successful
   */
  public Boolean launchScenario() {

    // Retrieves the pool remote plugin.
    PoolPlugin poolRemotePlugin =
        (PoolPlugin) SmartCardServiceProvider.getService().getPlugin(REMOTE_PLUGIN_NAME);

    // Retrieves the reader group references available.
    SortedSet<String> groupReferences = poolRemotePlugin.getReaderGroupReferences();

    // Allocates a remote reader.
    CardReader remoteReader = poolRemotePlugin.allocateReader(groupReferences.first());

    // Execute a ticketing transaction :
    // 1. perform a remote explicit selection
    CardSelectionManager cardSelectionManager = CalypsoTicketingServiceUtil.getCardSelection();
    CardSelectionResult cardSelectionResult =
        cardSelectionManager.processCardSelectionScenario(remoteReader);

    // 2. Reads the content of event log file
    CalypsoCard calypsoCard = (CalypsoCard) cardSelectionResult.getActiveSmartCard();
    String eventLog = CalypsoTicketingServiceUtil.readEventLog(calypsoCard, remoteReader);

    // Releases the remote reader.
    poolRemotePlugin.releaseReader(remoteReader);

    return !eventLog.isEmpty();
  }
}

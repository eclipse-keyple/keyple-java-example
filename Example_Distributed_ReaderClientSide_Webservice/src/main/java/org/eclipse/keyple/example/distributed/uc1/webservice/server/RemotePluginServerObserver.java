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
package org.eclipse.keyple.example.distributed.uc1.webservice.server;

import org.calypsonet.terminal.calypso.card.CalypsoCard;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.PluginEvent;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.distributed.RemotePluginServer;
import org.eclipse.keyple.distributed.RemoteReaderServer;
import org.eclipse.keyple.example.distributed.uc1.webservice.common.InputDataDto;
import org.eclipse.keyple.example.distributed.uc1.webservice.common.OutputDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of a {@link RemotePluginServer} observer.
 *
 * <p>It contains the business logic of the remote service execution.
 */
public class RemotePluginServerObserver implements PluginObserverSpi {

  private static final Logger logger = LoggerFactory.getLogger(RemotePluginServerObserver.class);

  /** {@inheritDoc} */
  @Override
  public void onPluginEvent(PluginEvent event) {

    // For a RemotePluginServer, the events can only be of type READER_CONNECTED.
    // So there is no need to analyze the event type.
    logger.info(
        "Event received {} {} {}",
        event.getType(),
        event.getPluginName(),
        event.getReaderNames().first());

    // Retrieves the remote plugin using the plugin name contains in the event.
    ObservablePlugin plugin =
        (ObservablePlugin) SmartCardServiceProvider.getService().getPlugin(event.getPluginName());
    RemotePluginServer pluginExtension = plugin.getExtension(RemotePluginServer.class);

    // Retrieves the name of the remote reader using the first reader name contains in the event.
    // Note that for a RemotePluginServer, there can be only one reader per event.
    String readerName = event.getReaderNames().first();

    // Retrieves the remote reader from the plugin using the reader name.
    Reader reader = plugin.getReader(readerName);
    RemoteReaderServer readerExtension = reader.getExtension(RemoteReaderServer.class);

    // Analyses the Service ID contains in the reader to find which business service to execute.
    // The Service ID was specified by the client when executing the remote service.
    Object userOutputData;
    if ("EXECUTE_CALYPSO_SESSION_FROM_REMOTE_SELECTION".equals(readerExtension.getServiceId())) {

      // Executes the business service using the remote reader.
      userOutputData = executeCalypsoSessionFromRemoteSelection(reader, readerExtension);

    } else {
      throw new IllegalArgumentException("Service ID not recognized");
    }

    // Terminates the business service by providing the reader name and the optional output data.
    pluginExtension.endRemoteService(readerName, userOutputData);
  }

  /**
   * Executes the business service having the Service ID
   * "EXECUTE_CALYPSO_SESSION_FROM_REMOTE_SELECTION".
   *
   * <p>This is an example of a ticketing transaction :
   *
   * <ol>
   *   <li>Perform a remote explicit selection,
   *   <li>Read the content of event log file.
   * </ol>
   *
   * @param reader The remote reader on where to execute the business logic.
   * @param readerExtension The reader extension.
   * @return a nullable reference to the user output data to transmit to the client.
   */
  private Object executeCalypsoSessionFromRemoteSelection(
      Reader reader, RemoteReaderServer readerExtension) {

    // Retrieves the optional userInputData specified by the client when executing the remote
    // service.
    InputDataDto userInputData = readerExtension.getInputData(InputDataDto.class);

    // Performs a remote explicit selection.
    CardSelectionManager cardSelectionManager = CalypsoTicketingServiceUtil.getCardSelection();
    CardSelectionResult cardSelectionResult =
        cardSelectionManager.processCardSelectionScenario(reader);
    CalypsoCard calypsoCard = (CalypsoCard) cardSelectionResult.getActiveSmartCard();

    // Read the content of event log file.
    try {
      CalypsoTicketingServiceUtil.readEventLog(calypsoCard, reader);

      // Return a successful transaction result.
      return new OutputDataDto().setUserId(userInputData.getUserId()).setSuccessful(true);

    } catch (Exception e) {
      // If an exception is thrown, then return an unsuccessful transaction result.
      return new OutputDataDto().setUserId(userInputData.getUserId()).setSuccessful(false);
    }
  }
}

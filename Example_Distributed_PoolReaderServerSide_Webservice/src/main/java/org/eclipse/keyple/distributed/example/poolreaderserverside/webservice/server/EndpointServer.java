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

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.distributed.LocalServiceServer;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.SyncNodeServer;
import org.eclipse.keyple.distributed.example.poolreaderserverside.webservice.client.EndpointClient;

/**
 * Example of a Server Controller.
 *
 * <p>Responds to {@link EndpointClient} requests.
 */
@Path("/pool-local-service")
public class EndpointServer {

  /**
   * The unique endpoint access.
   *
   * @param message The request.
   * @return a list of response messages.
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<MessageDto> processMessage(MessageDto message) {

    // Retrieves the node associated to the remote plugin.
    SyncNodeServer node =
        SmartCardServiceProvider.getService()
            .getDistributedLocalService(AppServer.LOCAL_SERVICE_NAME)
            .getExtension(LocalServiceServer.class)
            .getSyncNode();

    // Forwards the message to the node and returns the response to the client.
    return node.onRequest(message);
  }
}

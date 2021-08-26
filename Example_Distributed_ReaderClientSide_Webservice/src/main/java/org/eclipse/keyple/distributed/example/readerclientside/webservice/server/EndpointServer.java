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
package org.eclipse.keyple.distributed.example.readerclientside.webservice.server;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.RemotePluginServer;
import org.eclipse.keyple.distributed.SyncNodeServer;
import org.eclipse.keyple.distributed.example.readerclientside.webservice.client.EndpointClient;

/**
 * Example of a Server Controller.
 *
 * <p>Responds to {@link EndpointClient} requests.
 */
@Path("/remote-plugin")
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
            .getPlugin(AppServer.REMOTE_PLUGIN_NAME)
            .getExtension(RemotePluginServer.class)
            .getSyncNode();

    // Forwards the message to the node and returns the response to the client.
    return node.onRequest(message);
  }
}

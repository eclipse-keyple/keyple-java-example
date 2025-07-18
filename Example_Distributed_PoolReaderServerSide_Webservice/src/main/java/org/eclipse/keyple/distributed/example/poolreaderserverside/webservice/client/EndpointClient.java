/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.distributed.example.poolreaderserverside.webservice.client;

import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.example.poolreaderserverside.webservice.server.EndpointServer;
import org.eclipse.keyple.distributed.spi.SyncEndpointClientSpi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Example of a {@link SyncEndpointClientSpi} implementation using Web Services.
 *
 * <p>Sends requests to the {@link EndpointServer}.
 */
@RegisterRestClient(configKey = "pool-local-service-api")
public interface EndpointClient extends SyncEndpointClientSpi {

  @POST
  @Path("/pool-local-service")
  @Produces("application/json")
  @Override
  List<MessageDto> sendRequest(MessageDto messageDto);
}

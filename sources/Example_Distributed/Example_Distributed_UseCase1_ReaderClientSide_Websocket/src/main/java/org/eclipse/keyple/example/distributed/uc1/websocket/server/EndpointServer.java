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
package org.eclipse.keyple.example.distributed.uc1.websocket.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keyple.distributed.AsyncNodeServer;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.RemotePluginServer;
import org.eclipse.keyple.distributed.spi.AsyncEndpointServerSpi;
import org.eclipse.keyple.example.distributed.uc1.websocket.client.EndpointClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of a {@link org.eclipse.keyple.distributed.spi.AsyncEndpointServerSpi} implementation
 * using Web Sockets.
 *
 * <p>Interacts with the {@link EndpointClient}.
 */
@ApplicationScoped
@ServerEndpoint("/remote-plugin")
public class EndpointServer implements AsyncEndpointServerSpi {

  private static final Logger logger = LoggerFactory.getLogger(EndpointServer.class);

  /** Map of opened sessions by session id */
  private final Map<String, Session> openedSessions;

  /** constructor */
  public EndpointServer() {
    openedSessions = new ConcurrentHashMap<String, Session>();
  }

  /**
   * Is invoked by the framework when a server session is opened.
   *
   * @param session the server session which is opened.
   */
  @OnOpen
  public void onOpen(Session session) {

    // Retrieves the session id from the query.
    String sessionId = session.getQueryString();
    logger.trace("Server - Opened socket for sessionId {} : ", sessionId);

    // Associates the server session to its session id.
    openedSessions.put(sessionId, session);
  }

  /** {@inheritDoc} */
  @Override
  public void sendMessage(MessageDto messageDto) {

    // Retrieves the session id from the provided message.
    String sessionId = messageDto.getSessionId();

    // Retrieves the opened server session using the session id.
    Session session = openedSessions.get(sessionId);

    // Serialize the message to send.
    String data = JsonUtil.getParser().toJson(messageDto);

    // Send the message.
    session.getAsyncRemote().sendText(data);
  }

  /**
   * Is invoked by the framework when a message is received from the client.
   *
   * @param data The incoming message.
   */
  @OnMessage
  public void onMessage(String data) {

    logger.trace("Server - Received message {} : ", data);

    // Deserialise the incoming message.
    MessageDto message = JsonUtil.getParser().fromJson(data, MessageDto.class);

    // Retrieves the async node associated to the local service.
    AsyncNodeServer node =
        SmartCardServiceProvider.getService()
            .getPlugin(AppServer.REMOTE_PLUGIN_NAME)
            .getExtension(RemotePluginServer.class)
            .getAsyncNode();

    // Forward the message to the node.
    node.onMessage(message);
  }

  /**
   * Is invoked by the framework when the server session is closed.
   *
   * @param session The server session which is getting closed.
   */
  @OnClose
  public void onClose(Session session) {

    // Retrieves the session id from the query.
    String sessionId = session.getQueryString();
    logger.trace("Server - Closed socket for sessionId {}", sessionId);

    // Clean the map of opened sessions.
    openedSessions.remove(sessionId);

    // Retrieves the async node associated to the local service.
    AsyncNodeServer node =
        SmartCardServiceProvider.getService()
            .getPlugin(AppServer.REMOTE_PLUGIN_NAME)
            .getExtension(RemotePluginServer.class)
            .getAsyncNode();

    // Forward the event to the node.
    node.onClose(sessionId);
  }

  /**
   * Is invoked by the framework when a session error occurs.
   *
   * @param session The server session which is getting closed.
   * @param error The error.
   */
  @OnError
  public void onError(Session session, Throwable error) {

    // Retrieves the session id from the query.
    String sessionId = session.getQueryString();
    logger.trace("Server - Error socket for sessionId {}", sessionId);

    // Clean the map of opened sessions.
    openedSessions.remove(sessionId);

    // Retrieves the async node associated to the local service.
    AsyncNodeServer node =
        SmartCardServiceProvider.getService()
            .getPlugin(AppServer.REMOTE_PLUGIN_NAME)
            .getExtension(RemotePluginServer.class)
            .getAsyncNode();

    // Forward the error to the node.
    node.onError(sessionId, error);
  }
}

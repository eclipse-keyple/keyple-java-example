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
package org.eclipse.keyple.distributed.example.readerclientside.webservice.common;

/**
 * Example of POJO which contains the <b>input data</b> associated to the <b>ReaderClientSide</b>
 * API.
 */
public class InputDataDto {

  private String userId;

  public String getUserId() {
    return userId;
  }

  public InputDataDto setUserId(String userId) {
    this.userId = userId;
    return this;
  }
}

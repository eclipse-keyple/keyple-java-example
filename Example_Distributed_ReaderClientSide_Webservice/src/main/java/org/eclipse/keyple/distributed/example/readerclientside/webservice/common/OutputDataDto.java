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
 * Example of POJO which contains the <b>output data</b> associated to the <b>ReaderClientSide</b>
 * API.
 */
public class OutputDataDto {

  private Boolean isSuccessful;
  private String userId;

  public OutputDataDto setSuccessful(Boolean successful) {
    isSuccessful = successful;
    return this;
  }

  public Boolean isSuccessful() {
    return isSuccessful;
  }

  public String getUserId() {
    return userId;
  }

  public OutputDataDto setUserId(String userId) {
    this.userId = userId;
    return this;
  }
}

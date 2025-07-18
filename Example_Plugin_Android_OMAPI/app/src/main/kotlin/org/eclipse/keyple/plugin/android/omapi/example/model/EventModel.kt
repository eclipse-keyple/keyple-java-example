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
package org.eclipse.keyple.plugin.android.omapi.example.model

open class EventModel(val type: Int, val text: String) {
  companion object {
    const val TYPE_HEADER = 0
    const val TYPE_ACTION = 1
    const val TYPE_RESULT = 2
    const val TYPE_MULTICHOICE = 3
  }
}

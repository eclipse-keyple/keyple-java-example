/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.example.plugin.android.nfc

/**
 * Represents a message, consisting of a type and text.
 *
 * @property type The type of the message.
 * @property text The text of the message.
 */
open class Message(val type: Int, val text: String) {
  companion object {
    const val TYPE_HEADER = 0
    const val TYPE_ACTION = 1
    const val TYPE_RESULT = 2
  }
}

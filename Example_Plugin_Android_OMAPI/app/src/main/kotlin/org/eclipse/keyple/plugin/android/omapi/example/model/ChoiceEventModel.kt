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

data class ChoiceEventModel(
    val title: String,
    val choices: List<String> = arrayListOf(),
    val callback: (choice: String) -> Unit
) : EventModel(TYPE_MULTICHOICE, title)

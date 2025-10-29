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
package org.eclipse.keyple.example.plugin.android.omapi.util

import android.content.Context
import android.os.Build

/** Extensions to improve readability of example code */
fun Context.getColorResource(id: Int): Int {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    resources.getColor(id, null)
  } else {
    resources.getColor(id)
  }
}

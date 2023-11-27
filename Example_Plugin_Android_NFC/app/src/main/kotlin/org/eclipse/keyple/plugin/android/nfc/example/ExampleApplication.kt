/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.nfc.example

import android.app.Application
import androidx.multidex.MultiDex
import timber.log.Timber

class ExampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    MultiDex.install(this)
    Timber.plant(Timber.DebugTree())
  }
}

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

import android.app.Application
import androidx.multidex.MultiDex
import timber.log.Timber

/**
 * The `ExampleApplication` class is a subclass of the `Application` class. It is responsible for
 * initializing the application with necessary setup.
 */
class ExampleApplication : Application() {
  /**
   * This method is called when the activity is first created. It is responsible for initializing
   * various components of the application.
   */
  override fun onCreate() {
    super.onCreate()
    MultiDex.install(this)
    Timber.plant(Timber.DebugTree())
  }
}

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
package org.eclipse.keyple.plugin.android.omapi.example.activity

import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import java.io.IOException
import org.eclipse.keyple.R
import org.eclipse.keyple.databinding.ActivityCoreExamplesBinding
import org.eclipse.keyple.plugin.android.omapi.example.adapter.EventAdapter
import org.eclipse.keyple.plugin.android.omapi.example.model.ChoiceEventModel
import org.eclipse.keyple.plugin.android.omapi.example.model.EventModel
import org.eclipse.keypop.reader.CardReaderEvent
import timber.log.Timber

abstract class AbstractExampleActivity :
    AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

  /** Use to modify event update behaviour regarding current use case execution */
  interface UseCase {
    fun onEventUpdate(event: CardReaderEvent?)
  }

  /** Variables for event window */
  private lateinit var adapter: RecyclerView.Adapter<*>
  private lateinit var layoutManager: RecyclerView.LayoutManager
  protected val events = arrayListOf<EventModel>()
  protected lateinit var binding: ActivityCoreExamplesBinding

  protected var useCase: UseCase? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initContentView()

    /** Init recycler view */
    adapter = EventAdapter(events)
    layoutManager = LinearLayoutManager(this)
    binding.eventRecyclerView.layoutManager = layoutManager
    binding.eventRecyclerView.adapter = adapter

    /** Init menu */
    binding.navigationView.setNavigationItemSelectedListener(this)
    val toggle =
        ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.open_navigation_drawer,
            R.string.close_navigation_drawer)
    binding.drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
    if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
      binding.drawerLayout.openDrawer(GravityCompat.START)
    }
  }

  protected fun initActionBar(toolbar: Toolbar, title: String, subtitle: String) {
    setSupportActionBar(toolbar)
    val actionBar = supportActionBar
    actionBar?.title = title
    actionBar?.subtitle = subtitle
  }

  protected fun clearEvents() {
    events.clear()
    adapter.notifyDataSetChanged()
  }

  protected fun addHeaderEvent(message: String) {
    events.add(EventModel(EventModel.TYPE_HEADER, message))
    adapter.notifyItemInserted(events.lastIndex)
    Timber.d("Header: %s", message)
  }

  protected fun addActionEvent(message: String) {
    events.add(EventModel(EventModel.TYPE_ACTION, message))
    adapter.notifyItemInserted(events.lastIndex)
    Timber.d("Action: %s", message)
  }

  protected fun addResultEvent(message: String) {
    events.add(EventModel(EventModel.TYPE_RESULT, message))
    adapter.notifyItemInserted(events.lastIndex)
    Timber.d("Result: %s", message)
  }

  protected fun addChoiceEvent(
      title: String,
      choices: List<String>,
      callback: (choice: String) -> Unit
  ) {
    events.add(ChoiceEventModel(title, choices, callback))
    adapter.notifyItemInserted(events.lastIndex)
    Timber.d("Choice: %s: %s", title, choices.toString())
  }

  @Throws(IOException::class)
  protected fun checkNfcAvailability() {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(this)

    if (nfcAdapter == null) {
      throw IOException("Your device does not support NFC")
    } else {
      if (!nfcAdapter.isEnabled) {
        throw IOException("Please enable NFC to communicate with NFC Elements\"")
      }
    }
  }

  abstract fun initContentView()
}

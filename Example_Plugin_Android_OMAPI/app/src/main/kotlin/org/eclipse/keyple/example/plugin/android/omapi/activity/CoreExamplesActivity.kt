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
package org.eclipse.keyple.example.plugin.android.omapi.activity

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.GravityCompat
import org.eclipse.keyple.card.generic.GenericExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPlugin
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactoryProvider
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import org.eclipse.keyple.plugin.android.omapi.example.R
import org.eclipse.keyple.plugin.android.omapi.example.databinding.ActivityCoreExamplesBinding
import org.eclipse.keyple.example.plugin.android.omapi.util.CalypsoClassicInfo
import org.eclipse.keypop.reader.CardCommunicationException
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ReaderCommunicationException
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.spi.IsoSmartCard

class CoreExamplesActivity : AbstractExampleActivity() {

  private lateinit var reader: CardReader
  private lateinit var cardSelectionManager: CardSelectionManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /** Register Android OMAPI plugin */
    AndroidOmapiPluginFactoryProvider(this) {
      val plugin = SmartCardServiceProvider.getService().registerPlugin(it)
      reader = plugin.getReader(AndroidOmapiReader.READER_NAME_SIM_1)
      Toast.makeText(this@CoreExamplesActivity, "Inited", Toast.LENGTH_LONG).show()
    }
  }

  override fun onDestroy() {
    SmartCardServiceProvider.getService().unregisterPlugin(AndroidOmapiPlugin.PLUGIN_NAME)
    super.onDestroy()
  }

  override fun initContentView() {
    binding = ActivityCoreExamplesBinding.inflate(layoutInflater)
    setContentView(binding.root)
    initActionBar(binding.toolbar, "NFC Plugins", "Core Examples")
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
      binding.drawerLayout.closeDrawer(GravityCompat.START)
    }
    when (item.itemId) {
      R.id.usecase1 -> {
        clearEvents()
        configureUseCase1ExplicitSelectionAid()
      }
    }
    return true
  }

  private fun configureUseCase1ExplicitSelectionAid() {
    addHeaderEvent("UseCase Generic #1: Explicit AID selection")

    with(reader) {
      addHeaderEvent("Reader  NAME = $name")

      if (isCardPresent) {

        val smartCardService = SmartCardServiceProvider.getService()

        /** Prepare a card selection */
        var readerApiFactory = smartCardService.readerApiFactory
        cardSelectionManager = readerApiFactory.createCardSelectionManager()

        /** Get the generic card extension service */
        val cardExtension = GenericExtensionService.getInstance()

        /** Verify that the extension's API level is consistent with the current service. */
        smartCardService.checkCardExtension(cardExtension)

        /**
         * Setting of an AID based selection (in this example a Calypso REV3 PO)
         *
         * Select the first application matching the selection AID whatever the card communication
         * protocol keep the logical channel open after the selection
         */
        val aid = CalypsoClassicInfo.AID_NAVIGO_2013

        /**
         * Generic selection: configures a CardSelector with all the desired attributes to make the
         * selection and read additional information afterwards
         */
        val cardSelector = readerApiFactory.createIsoCardSelector().filterByDfName(aid)

        /** Create a card selection extension using the generic card extension. */
        val cardSelectionExtension =
            GenericExtensionService.getInstance().createGenericCardSelectionExtension()
        cardSelectionManager.prepareSelection(cardSelector, cardSelectionExtension)

        /** Release the channel after the selection is done */
        cardSelectionManager.prepareReleaseChannel()

        /** We won't be listening for event update within this use case */
        useCase = null

        addActionEvent("Calypso PO selection: $aid")
        try {
          val cardSelectionsResult = cardSelectionManager.processCardSelectionScenario(this)

          if (cardSelectionsResult.activeSmartCard != null) {
            val matchedCard = cardSelectionsResult.activeSmartCard
            addResultEvent("The selection of the card has succeeded.")
            addResultEvent(
                "Application FCI = ${HexUtil.toHex((matchedCard as IsoSmartCard).selectApplicationResponse)}")
            addResultEvent("End of the generic card processing.")
          } else {
            addResultEvent("The selection of the card has failed.")
          }
        } catch (e: CardCommunicationException) {
          addResultEvent("Error: ${e.message}")
        } catch (e: ReaderCommunicationException) {
          addResultEvent("Error: ${e.message}")
        }
      } else {
        addResultEvent("No cards were detected.")
        addResultEvent("The card must be in the field when starting this use case")
      }
      binding.eventRecyclerView.smoothScrollToPosition(events.size - 1)
    }
  }
}

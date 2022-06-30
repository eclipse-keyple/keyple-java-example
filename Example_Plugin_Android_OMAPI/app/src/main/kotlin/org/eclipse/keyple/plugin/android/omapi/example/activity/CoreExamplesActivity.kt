/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.omapi.example.activity

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.activity_core_examples.drawerLayout
import kotlinx.android.synthetic.main.activity_core_examples.eventRecyclerView
import kotlinx.android.synthetic.main.activity_core_examples.toolbar
import org.calypsonet.terminal.reader.CardCommunicationException
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.ReaderCommunicationException
import org.calypsonet.terminal.reader.selection.CardSelectionManager
import org.eclipse.keyple.card.generic.GenericExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPlugin
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactoryProvider
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import org.eclipse.keyple.plugin.android.omapi.example.R
import org.eclipse.keyple.plugin.android.omapi.example.util.CalypsoClassicInfo

class CoreExamplesActivity : AbstractExampleActivity() {

    private lateinit var reader: CardReader
    private lateinit var cardSelectionManager: CardSelectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Register Android OMAPI plugin
         */
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
        setContentView(R.layout.activity_core_examples)
        initActionBar(toolbar, "NFC Plugins", "Core Examples")
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
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

                /**
                 * Prepare a card selection
                 */
                cardSelectionManager = smartCardService.createCardSelectionManager()

                /**
                 * Get the generic card extension service
                 */
                val cardExtension = GenericExtensionService.getInstance()

                /**
                 * Verify that the extension's API level is consistent with the current service.
                 */
                smartCardService.checkCardExtension(cardExtension)

                /**
                 * Setting of an AID based selection (in this example a Calypso REV3 PO)
                 *
                 * Select the first application matching the selection AID whatever the card communication
                 * protocol keep the logical channel open after the selection
                 */
                val aid = CalypsoClassicInfo.AID_NAVIGO_2013

                /**
                 * Generic selection: configures a CardSelector with all the desired attributes to make
                 * the selection and read additional information afterwards
                 */
                val cardSelection = cardExtension.createCardSelection()
                    .filterByDfName(aid)

                /**
                 * Create a card selection using the generic card extension.
                 */
                cardSelectionManager.prepareSelection(cardSelection)

                /**
                 * Release the channel after the selection is done
                 */
                cardSelectionManager.prepareReleaseChannel()

                /**
                 * We won't be listening for event update within this use case
                 */
                useCase = null

                addActionEvent("Calypso PO selection: $aid")
                try {
                    val cardSelectionsResult = cardSelectionManager.processCardSelectionScenario(this)

                    if (cardSelectionsResult.activeSmartCard != null) {
                        val matchedCard = cardSelectionsResult.activeSmartCard
                        addResultEvent("The selection of the card has succeeded.")
                        addResultEvent("Application FCI = ${HexUtil.toHex(matchedCard.selectApplicationResponse)}")
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
            eventRecyclerView.smoothScrollToPosition(events.size - 1)
        }
    }
}

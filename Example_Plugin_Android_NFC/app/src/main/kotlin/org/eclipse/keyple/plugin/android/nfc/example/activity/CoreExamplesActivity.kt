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
package org.eclipse.keyple.plugin.android.nfc.example.activity

import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.GravityCompat
import java.io.IOException
import kotlinx.android.synthetic.main.activity_core_examples.drawerLayout
import kotlinx.android.synthetic.main.activity_core_examples.eventRecyclerView
import kotlinx.android.synthetic.main.activity_core_examples.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.calypsonet.terminal.reader.CardCommunicationException
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.CardReaderEvent
import org.calypsonet.terminal.reader.ConfigurableCardReader
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.ReaderCommunicationException
import org.eclipse.keyple.card.generic.GenericExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.nfc.example.R
import org.eclipse.keyple.plugin.android.nfc.example.util.CalypsoClassicInfo
import timber.log.Timber

/**
 * Examples of Keyple API usage relying on keyple-plugin-android-nfc-java-lib
 */
class CoreExamplesActivity : AbstractExampleActivity() {

    private lateinit var reader: CardReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Register AndroidNfc plugin Factory
         */
        val plugin = SmartCardServiceProvider.getService().registerPlugin(AndroidNfcPluginFactoryProvider(this).getFactory())

        /**
         * Configure Nfc Reader
         */
        with(plugin.getReader(AndroidNfcReader.READER_NAME) as ObservableCardReader) {
            setReaderObservationExceptionHandler(this@CoreExamplesActivity)
            addObserver(this@CoreExamplesActivity)

            // with this protocol settings we activate the nfc for ISO1443_4 protocol
            (this as ConfigurableCardReader).activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name, "ISO_14443_4_CARD")
            reader = this
        }
    }

    override fun onDestroy() {
        SmartCardServiceProvider.getService().unregisterPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        super.onDestroy()
    }

    override fun initContentView() {
        setContentView(R.layout.activity_core_examples)
        initActionBar(toolbar, "NFC Plugins", "Core Examples")
    }

    override fun onResume() {
        super.onResume()
        try {
            checkNfcAvailability()
            if (intent.action != null && intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) run {

                Timber.d("Handle ACTION TECH intent")
                // notify reader that card detection has been launched
                (reader as ObservableCardReader).startCardDetection(ObservableCardReader.DetectionMode.SINGLESHOT)
                initFromBackgroundTextView()
                (reader as AndroidNfcReader).processIntent(intent)
                configureUseCase1ExplicitSelectionAid()
            } else {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                // enable detection
                (reader as ObservableCardReader).startCardDetection(ObservableCardReader.DetectionMode.SINGLESHOT)
            }
        } catch (e: IOException) {
            showAlertDialog(e)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        when (item.itemId) {
            R.id.usecase2 -> {
                clearEvents()
                (reader as ObservableCardReader).startCardDetection(ObservableCardReader.DetectionMode.REPEATING)
                configureUseCase2DefaultSelectionNotification()
            }
        }
        return true
    }

    private fun configureUseCase2DefaultSelectionNotification() {
        addHeaderEvent("UseCase Generic #2: AID based default selection")

        with(reader as ObservableCardReader) {

            addHeaderEvent("Reader  NAME = $name")

            /**
             * Prepare a card selection
             */
            cardSelectionManager = SmartCardServiceProvider.getService().createCardSelectionManager()

            /**
             * Setting of an AID based selection
             *
             * Select the first application matching the selection AID whatever the card communication
             * protocol keep the logical channel open after the selection
             */
            val aid = CalypsoClassicInfo.AID_CD_LIGHT_GTML

            /**
             * Generic selection: configures a CardSelector with all the desired attributes to make the
             * selection
             */
            val cardSelection = GenericExtensionService.getInstance().createCardSelection()
                .filterByCardProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                .filterByDfName(aid)

            /**
             * Add the selection case to the current selection (we could have added other cases here)
             */
            cardSelectionManager.prepareSelection(cardSelection)

            cardSelectionManager.scheduleCardSelectionScenario(reader as ObservableCardReader, ObservableCardReader.DetectionMode.REPEATING, ObservableCardReader.NotificationMode.MATCHED_ONLY)

            useCase = object : UseCase {
                override fun onEventUpdate(event: CardReaderEvent) {
                    CoroutineScope(Dispatchers.Main).launch {
                        when (event.type) {
                            CardReaderEvent.Type.CARD_MATCHED -> {
                                addResultEvent("CARD_MATCHED event: A card corresponding to request has been detected")
                                val selectedCard = cardSelectionManager.parseScheduledCardSelectionsResponse(event.scheduledCardSelectionsResponse).activeSmartCard
                                if (selectedCard != null) {
                                    addResultEvent("Observer notification: the selection of the card has succeeded. End of the card processing.")
                                    addResultEvent("Application FCI = ${HexUtil.toHex(selectedCard.selectApplicationResponse)}")
                                } else {
                                    addResultEvent("The selection of the card has failed. Should not have occurred due to the MATCHED_ONLY selection mode.")
                                }
                                (reader as ObservableCardReader).finalizeCardProcessing()
                            }

                            CardReaderEvent.Type.CARD_INSERTED -> {
                                addResultEvent("CARD_INSERTED event: should not have occurred due to the MATCHED_ONLY selection mode.")
                                (reader as ObservableCardReader).finalizeCardProcessing()
                            }

                            CardReaderEvent.Type.CARD_REMOVED -> {
                                addResultEvent("CARD_REMOVED event: There is no PO inserted anymore. Return to the waiting state...")
                            }
                        }
                        eventRecyclerView.smoothScrollToPosition(events.size - 1)
                    }
                    eventRecyclerView.smoothScrollToPosition(events.size - 1)
                }
            }
            addActionEvent("Waiting for a card... The default AID based selection to be processed as soon as the card is detected.")
        }
    }

    private fun configureUseCase1ExplicitSelectionAid() {
        addHeaderEvent("UseCase Generic #1: Explicit AID selection")

        with(reader as ObservableCardReader) {
            addHeaderEvent("Reader  NAME = $name")

            if (isCardPresent) {

                val smartCardService = SmartCardServiceProvider.getService()

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
                val aid = CalypsoClassicInfo.AID_CD_LIGHT_GTML

                /**
                 * Generic selection: configures a CardSelector with all the desired attributes to make
                 * the selection and read additional information afterwards
                 */
                val cardSelection = cardExtension.createCardSelection()
                    .filterByCardProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                    .filterByDfName(aid)

                /**
                 * Create a card selection using the generic card extension.
                 */
                cardSelectionManager.prepareSelection(cardSelection)

                /**
                 * Provide the Reader with the selection operation to be processed when a card is inserted.
                 */
                cardSelectionManager.scheduleCardSelectionScenario(reader as ObservableCardReader, ObservableCardReader.DetectionMode.SINGLESHOT, ObservableCardReader.NotificationMode.MATCHED_ONLY)

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
                    (reader as ObservableCardReader).finalizeCardProcessing()
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

    override fun onReaderEvent(cardReaderEvent: CardReaderEvent) {
        Timber.i("New ReaderEvent received : ${cardReaderEvent.type}")
        useCase?.onEventUpdate(cardReaderEvent)
    }
}

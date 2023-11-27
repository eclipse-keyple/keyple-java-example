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

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.databinding.ActivityMainBinding
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.CardIOException
import org.eclipse.keypop.calypso.card.transaction.ChannelControl
import org.eclipse.keypop.calypso.card.transaction.FreeTransactionManager
import org.eclipse.keypop.reader.*
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

/** Activity launched on app start up that display the only screen available on this example app. */
class MainActivity :
    AppCompatActivity(), CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private lateinit var binding: ActivityMainBinding
  private lateinit var adapter: RecyclerView.Adapter<*>
  private lateinit var layoutManager: RecyclerView.LayoutManager
  private val messages = arrayListOf<Message>()
  private lateinit var reader: ObservableCardReader
  private var calypsoExtensionService = CalypsoExtensionService.getInstance()
  private lateinit var cardSelectionManager: CardSelectionManager
  private val readerApiFactory: ReaderApiFactory =
      SmartCardServiceProvider.getService().readerApiFactory
  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      calypsoExtensionService.getCalypsoCardApiFactory()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeViews()
    setupActionBar()
    initRecyclerView()
    configureContactlessReader()
    prepareCardSelection()
    startCardDetection()
  }

  override fun onDestroy() {
    super.onDestroy()
    SmartCardServiceProvider.getService().unregisterPlugin(AndroidNfcPlugin.PLUGIN_NAME)
  }

  private fun initializeViews() {
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)
  }

  private fun setupActionBar() {
    supportActionBar?.title = "Keyple NFC plugin"
    supportActionBar?.subtitle = "Test application"
  }

  private fun initRecyclerView() {
    adapter = MessageDisplayAdapter(messages)
    layoutManager = LinearLayoutManager(this)
    binding.messageRecyclerView.layoutManager = layoutManager
    binding.messageRecyclerView.adapter = adapter
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  private fun configureContactlessReader() {
    val plugin =
        SmartCardServiceProvider.getService()
            .registerPlugin(AndroidNfcPluginFactoryProvider(this).getFactory())
    reader = plugin.getReader(AndroidNfcReader.READER_NAME) as ObservableCardReader
    reader.setReaderObservationExceptionHandler(this)
    reader.addObserver(this)
    (reader as ConfigurableCardReader).activateProtocol(
        AndroidNfcSupportedProtocols.ISO_14443_4.name, "ISODEP")
  }

  private fun prepareCardSelection() {
    cardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Create a generic ISO selector

    // Create a generic ISO selector
    val cardSelector =
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoConstants.KEYPLE_KIT_AID)

    // Create a specific Calypso card selection extension
    val calypsoCardSelectionExtension =
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard()

    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension)
  }

  private fun startCardDetection() {
    cardSelectionManager.scheduleCardSelectionScenario(
        reader, ObservableCardReader.NotificationMode.ALWAYS)

    reader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)

    addActionMessage("Waiting for card insertion...")
  }

  private fun addHeaderMessage(message: String) {
    messages.add(Message(Message.TYPE_HEADER, message))
    updateList()
    Timber.d("Header: %s", message)
  }

  private fun addActionMessage(message: String) {
    messages.add(Message(Message.TYPE_ACTION, message))
    updateList()
    Timber.d("Action: %s", message)
  }

  private fun addResultMessage(message: String) {
    messages.add(Message(Message.TYPE_RESULT, message))
    updateList()
    Timber.d("Result: %s", message)
  }

  private fun updateList() {
    CoroutineScope(Dispatchers.Main).launch {
      adapter.notifyDataSetChanged()
      adapter.notifyItemInserted(messages.lastIndex)
      binding.messageRecyclerView.smoothScrollToPosition(messages.size - 1)
    }
  }

  override fun onReaderEvent(readerEvent: CardReaderEvent?) {
    CoroutineScope(Dispatchers.Main).launch {
      readerEvent?.let { event ->
        when (event.type) {
          CardReaderEvent.Type.CARD_INSERTED -> handleCardInsertedEvent(event)
          CardReaderEvent.Type.CARD_MATCHED -> handleCardMatchedEvent(event)
          CardReaderEvent.Type.CARD_REMOVED -> handleCardRemovedEvent()
          else -> {
            // Handle other event types if necessary
          }
        }
      }
      binding.messageRecyclerView.smoothScrollToPosition(messages.size - 1)
    }
  }

  private fun handleCardMatchedEvent(readerEvent: CardReaderEvent) {
    addHeaderMessage("Calypso card detected")

    val calypsoCard = parseCalypsoCard(readerEvent.scheduledCardSelectionsResponse)

    addResultMessage(
        "Type = ${calypsoCard.productType.name}" +
            "\nS/N = ${HexUtil.toHex(calypsoCard.applicationSerialNumber)}" +
            "\nFile structure = ${HexUtil.toHex(calypsoCard.applicationSubtype)}" +
            "\nExtended mode available = ${calypsoCard.isExtendedModeSupported}")

    addActionMessage("Reading card content...")

    val cardTransactionManager = createCardTransactionManager(reader, calypsoCard)
    try {
      cardTransactionManager
          .prepareReadRecord(CalypsoConstants.SFI_Environment, CalypsoConstants.RECORD_NUMBER_1)
          .prepareReadRecord(CalypsoConstants.SFI_EventsLog, CalypsoConstants.RECORD_NUMBER_1)
          .processCommands(ChannelControl.CLOSE_AFTER)

      addResultMessage(
          "Environment file = ${HexUtil.toHex(calypsoCard.getFileBySfi(CalypsoConstants.SFI_Environment).data.getContent(
            CalypsoConstants.RECORD_NUMBER_1))}")
      addResultMessage(
          "Events log file = ${HexUtil.toHex(calypsoCard.getFileBySfi(CalypsoConstants.SFI_EventsLog).data.getContent(
            CalypsoConstants.RECORD_NUMBER_1))}")

      addActionMessage("Waiting for card removal...")
    } catch (e: CardIOException) {
      addResultMessage("Card communication link lost.")
    } catch (e: Exception) {
      addResultMessage("An unexpected error occurred ${e.message}.")
    }

    reader.finalizeCardProcessing()
  }

  private fun handleCardInsertedEvent(readerEvent: CardReaderEvent) {
    addHeaderMessage("Unknown card detected")
    reader.finalizeCardProcessing()
  }

  private fun handleCardRemovedEvent() {
    addHeaderMessage("Card removed")
    addActionMessage("Waiting for card insertion...")
  }

  private fun parseCalypsoCard(response: ScheduledCardSelectionsResponse): CalypsoCard {
    return cardSelectionManager.parseScheduledCardSelectionsResponse(response).activeSmartCard
        as CalypsoCard
  }

  private fun createCardTransactionManager(
      reader: CardReader,
      calypsoCard: CalypsoCard
  ): FreeTransactionManager {
    return calypsoCardApiFactory.createFreeTransactionManager(reader, calypsoCard)
  }

  override fun onReaderObservationError(contextInfo: String?, readerName: String?, e: Throwable?) {
    addResultMessage("Error: ${e!!.message}")
  }
}

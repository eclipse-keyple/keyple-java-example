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
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConfig
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
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
  private lateinit var cardSelectionManager: CardSelectionManager
  private val readerApiFactory: ReaderApiFactory =
      SmartCardServiceProvider.getService().readerApiFactory
  private var calypsoExtensionService = CalypsoExtensionService.getInstance()
  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      calypsoExtensionService.getCalypsoCardApiFactory()

  /**
   * Called when the activity is starting or when the activity's instance is recreated.
   *
   * @param savedInstanceState The Bundle instance containing the previously saved state.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeViews()
    setupActionBar()
    initRecyclerView()
    configureContactlessReader()
    prepareCardSelection()
    startCardDetection()
  }

  /**
   * Performs necessary clean-up and unregistration of the SmartCard plugin when the activity is
   * being destroyed.
   *
   * This method should be invoked when the activity that overrides it is being destroyed. It is
   * responsible for unregistering the SmartCard plugin provided by the AndroidNfcPlugin with the
   * SmartCardServiceProvider.
   *
   * @see android.app.Activity.onDestroy
   * @see SmartCardServiceProvider.unregisterPlugin
   */
  override fun onDestroy() {
    super.onDestroy()
    SmartCardServiceProvider.getService().unregisterPlugin(AndroidNfcConstants.PLUGIN_NAME)
  }

  /** Initializes the views for the activity. */
  private fun initializeViews() {
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)
  }

  /** Sets up the action bar with custom title and subtitle */
  private fun setupActionBar() {
    supportActionBar?.title = "Keyple NFC plugin"
    supportActionBar?.subtitle = "Test application"
  }

  /**
   * Initializes the RecyclerView for displaying messages. This method sets up the adapter, layout
   * manager, and attaches them to the RecyclerView. It also adds a flag to keep the screen on while
   * the RecyclerView is active.
   */
  private fun initRecyclerView() {
    adapter = MessageDisplayAdapter(messages)
    layoutManager = LinearLayoutManager(this)
    binding.messageRecyclerView.layoutManager = layoutManager
    binding.messageRecyclerView.adapter = adapter
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  /**
   * Configures the contactless card reader.
   *
   * This method registers the Android NFC plugin factory and obtains the reader instance. It also
   * sets the reader observation exception handler and adds the current instance as an observer.
   * Finally, it activates the ISO 14443-4 protocol with the "ISODEP" logical name.
   *
   * @throws CardReaderException if there is an error while configuring the contactless card reader.
   */
  private fun configureContactlessReader() {
    val plugin =
        SmartCardServiceProvider.getService()
            .registerPlugin(AndroidNfcPluginFactoryProvider.provideFactory(AndroidNfcConfig(this)))
    reader = plugin.getReader(AndroidNfcConstants.READER_NAME) as ObservableCardReader
    reader.setReaderObservationExceptionHandler(this)
    reader.addObserver(this)
    (reader as ConfigurableCardReader).activateProtocol(
        AndroidNfcSupportedProtocols.ISO_14443_4.name, "ISODEP")
  }

  /**
   * Prepares the card selection process. Initializes the cardSelectionManager and sets up the card
   * selector and card selection extension. The card selector filters the cards based on the AID.
   */
  private fun prepareCardSelection() {
    cardSelectionManager = readerApiFactory.createCardSelectionManager()
    // Create a generic ISO selector
    val cardSelector =
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoConstants.KEYPLE_KIT_AID)

    // Create a specific Calypso card selection extension
    val calypsoCardSelectionExtension = calypsoCardApiFactory.createCalypsoCardSelectionExtension()

    cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension)
  }

  /**
   * Starts the card detection process.
   *
   * This method schedules the card selection scenario and starts the card detection mode.
   */
  private fun startCardDetection() {
    cardSelectionManager.scheduleCardSelectionScenario(
        reader, ObservableCardReader.NotificationMode.ALWAYS)

    reader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)

    addActionMessage("Waiting for card insertion...")
  }

  /**
   * Adds a header message to the list of messages.
   *
   * @param message The header message to be added.
   */
  private fun addHeaderMessage(message: String) {
    messages.add(Message(Message.TYPE_HEADER, message))
    updateList()
    Timber.d("Header: %s", message)
  }

  /**
   * Adds an action message to the list of messages.
   *
   * @param message The action message to be added.
   */
  private fun addActionMessage(message: String) {
    messages.add(Message(Message.TYPE_ACTION, message))
    updateList()
    Timber.d("Action: %s", message)
  }

  /**
   * Adds a result message to the list of messages.
   *
   * @param message The result message to be added.
   */
  private fun addResultMessage(message: String) {
    messages.add(Message(Message.TYPE_RESULT, message))
    updateList()
    Timber.d("Result: %s", message)
  }

  /**
   * Update the list of messages in the RecyclerView and scroll to the last message.
   *
   * This method should be called after adding a new message to the list.
   */
  private fun updateList() {
    CoroutineScope(Dispatchers.Main).launch {
      adapter.notifyDataSetChanged()
      adapter.notifyItemInserted(messages.lastIndex)
      binding.messageRecyclerView.smoothScrollToPosition(messages.size - 1)
    }
  }

  /**
   * Method to handle card reader events.
   *
   * @param readerEvent The card reader event to be handled.
   */
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

  /**
   * Handles the CardMatched event.
   *
   * @param readerEvent The CardReaderEvent containing the information about the matched card.
   */
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

  /**
   * Handles the event when a card is inserted into the card reader.
   *
   * @param readerEvent The CardReaderEvent object representing the event.
   */
  private fun handleCardInsertedEvent(readerEvent: CardReaderEvent) {
    addHeaderMessage("Unknown card detected")
    reader.finalizeCardProcessing()
  }

  /** Handles the event when a card is removed. */
  private fun handleCardRemovedEvent() {
    addHeaderMessage("Card removed")
    addActionMessage("Waiting for card insertion...")
  }

  /**
   * Parses the ScheduledCardSelectionsResponse object and returns the active CalypsoCard.
   *
   * @param response The ScheduledCardSelectionsResponse object to be parsed.
   * @return The active CalypsoCard parsed from the ScheduledCardSelectionsResponse.
   */
  private fun parseCalypsoCard(response: ScheduledCardSelectionsResponse): CalypsoCard {
    return cardSelectionManager.parseScheduledCardSelectionsResponse(response).activeSmartCard
        as CalypsoCard
  }

  /**
   * Creates a card transaction manager using the provided card reader and calypso card.
   *
   * @param reader The card reader to be used for card transactions.
   * @param calypsoCard The calypso card to be used for card transactions.
   * @return The created card transaction manager.
   */
  private fun createCardTransactionManager(
      reader: CardReader,
      calypsoCard: CalypsoCard
  ): FreeTransactionManager {
    return calypsoCardApiFactory.createFreeTransactionManager(reader, calypsoCard)
  }

  /**
   * Callback method invoked when there is an error occurred during the observation of a reader.
   *
   * @param contextInfo additional context information about the error, if available.
   * @param readerName the name of the reader that encountered the error.
   * @param e the throwable object representing the error.
   */
  override fun onReaderObservationError(contextInfo: String?, readerName: String?, e: Throwable?) {
    addResultMessage("Error: ${e!!.message}")
  }
}

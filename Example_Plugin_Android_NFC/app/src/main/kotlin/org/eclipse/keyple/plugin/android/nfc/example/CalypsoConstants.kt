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

/**
 * Helper class to provide specific elements to handle Calypso cards.
 * * AIDs for application selection
 * * Files info (SFI, rec number, etc) for
 * * Environment and Holder
 * * Event Log
 * * Contract List
 * * Contracts
 * * Counters
 */
internal object CalypsoConstants {
  /** Calypso default AID */
  const val KEYPLE_KIT_AID = "315449432e49434131"

  const val RECORD_NUMBER_1 = 1
  const val RECORD_NUMBER_2 = 2
  const val RECORD_NUMBER_3 = 3
  const val RECORD_NUMBER_4 = 4
  const val SFI_Environment = 0x07.toByte()
  const val SFI_EventsLog = 0x08.toByte()
  const val SFI_ContractList = 0x1E.toByte()
  const val SFI_Contracts = 0x09.toByte()
  const val SFI_Counter1 = 0x19.toByte()

  const val RECORD_SIZE = 29
}

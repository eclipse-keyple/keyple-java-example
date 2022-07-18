/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.calypso.example.common;

/**
 * Helper class to provide specific constants to manipulate Calypso cards from the Keyple demo kit.
 *
 * <ul>
 *   <li>AID application selection (default Calypso AID)
 *   <li>File
 *   <li>File definitions and identifiers (SFI)
 *   <li>Sample data
 *   <li>Security settings
 * </ul>
 */
public final class CalypsoConstants {

  /**
   * (private)<br>
   * Constructor.
   */
  private CalypsoConstants() {}

  // Application
  /** AID: Keyple test kit profile 1, Application 2 */
  public static final String AID = "315449432E49434131";

  // File structure
  public static final int RECORD_SIZE = 29;

  public static final byte RECORD_NUMBER_1 = 1;
  public static final byte RECORD_NUMBER_2 = 2;
  public static final byte RECORD_NUMBER_3 = 3;
  public static final byte RECORD_NUMBER_4 = 4;

  // File identifiers
  public static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  public static final byte SFI_EVENT_LOG = (byte) 0x08;
  public static final byte SFI_CONTRACT_LIST = (byte) 0x1E;
  public static final byte SFI_CONTRACTS = (byte) 0x09;
  public static final byte SFI_COUNTERS = (byte) 0x19;

  public static final short LID_DF_RT = 0x2000;
  public static final short LID_EVENT_LOG = 0x2010;

  // Sample data
  public static final String EVENT_LOG_DATA_FILL =
      "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC";

  // Security settings
  public static final String SAM_PROFILE_NAME = "SAM C1";

  public static final byte PIN_MODIFICATION_CIPHERING_KEY_KIF = (byte) 0x21;
  public static final byte PIN_MODIFICATION_CIPHERING_KEY_KVC = (byte) 0x79;
  public static final byte PIN_VERIFICATION_CIPHERING_KEY_KIF = (byte) 0x30;
  public static final byte PIN_VERIFICATION_CIPHERING_KEY_KVC = (byte) 0x79;

  public static final byte[] PIN_OK = {(byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30};
  public static final byte[] PIN_KO = {(byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x31};
}

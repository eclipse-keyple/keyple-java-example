/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.card.calypso.example.common;

import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.stub.StubSmartCard;

/** Factory for a Calypso Card emulation via a smart card stub */
public class StubSmartCardFactory {
  private static final String CARD_POWER_ON_DATA = "3B888001000000009171710098";
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  private static final String SAM_PROTOCOL = "ISO_7816_3_T0";
  private static final StubSmartCard stubCard =
      StubSmartCard.builder()
          .withPowerOnData(HexUtil.toByteArray(CARD_POWER_ON_DATA))
          .withProtocol(ISO_CARD_PROTOCOL)
          // select application
          .withSimulatedCommand(
              "00A4040009315449432E4943413100",
              "6F238409315449432E49434131A516BF0C13C70800000000AABBCCDD53070A3C23051410019000")
          // read records
          .withSimulatedCommand(
              "00B2013C00", "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC9000")
          // open secure session
          .withSimulatedCommand(
              "008A0B39040011223300",
              "0308D1810030791D00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC9000")
          // close secure session
          .withSimulatedCommand("008E8000041234567800", "876543219000")
          // ping command (used by the card removal procedure)
          .withSimulatedCommand("00C0000000", "9000")
          .build();

  private static final String SAM_POWER_ON_DATA = "3B3F9600805A0080C120000012345678829000";
  private static final StubSmartCard stubSam =
      StubSmartCard.builder()
          .withPowerOnData(HexUtil.toByteArray(SAM_POWER_ON_DATA))
          .withProtocol(SAM_PROTOCOL)
          // select diversifier
          .withSimulatedCommand("801400000800000000AABBCCDD", "9000")
          // get challenge
          .withSimulatedCommand("8084000004", "001122339000")
          // digest init
          .withSimulatedCommand(
              "808A00FF2730790308D1810030791D00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC",
              "9000")
          // digest close
          .withSimulatedCommand("808E000004", "123456789000")
          // digest authenticate
          .withSimulatedCommand("808200000487654321", "9000")
          .build();

  /** Constructor */
  private StubSmartCardFactory() {}

  /**
   * Get the stub smart card for a Calypso card
   *
   * @return A not null reference
   */
  public static StubSmartCard getStubCard() {
    return stubCard;
  }

  /**
   * Get the stub smart card for a Calypso SAM
   *
   * @return A not null reference
   */
  public static StubSmartCard getStubSam() {
    return stubSam;
  }
}

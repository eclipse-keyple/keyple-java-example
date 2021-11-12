/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
import org.eclipse.keyple.plugin.stub.StubSmartCard;

/** Factory for a Calypso Card emulation via a smart card stub */
public class CalypsoCardStubFactory {
  private static final String POWER_ON_DATA = "3B888001000000009171710098";
  private static final String PROTOCOL = ContactlessCardCommonProtocol.ISO_14443_4.name();
  private static StubSmartCard stubSmartCard =
      StubSmartCard.builder()
          .withPowerOnData(ByteArrayUtil.fromHex(POWER_ON_DATA))
          .withProtocol(PROTOCOL)
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
          .build();

  /**
   * (private)<br>
   * Constructor
   */
  private CalypsoCardStubFactory() {}

  /**
   * Get the stub smart card
   *
   * @return A not null reference
   */
  public static StubSmartCard getStubSmartCard() {
    return stubSmartCard;
  }
}

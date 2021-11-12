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
import org.eclipse.keyple.core.util.protocol.ContactCardCommonProtocol;
import org.eclipse.keyple.plugin.stub.StubSmartCard;

/** Factory for a Calypso SAM emulation via a smart card stub */
public class CalypsoSamStubFactory {
  private static final String POWER_ON_DATA = "3B3F9600805A0080C120000012345678829000";
  private static final String PROTOCOL = ContactCardCommonProtocol.ISO_7816_3_T0.name();
  private static StubSmartCard stubSmartCard =
      StubSmartCard.builder()
          .withPowerOnData(ByteArrayUtil.fromHex(POWER_ON_DATA))
          .withProtocol(PROTOCOL)
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

  /**
   * (private)<br>
   * Constructor
   */
  private CalypsoSamStubFactory() {}

  /**
   * Get the stub smart card
   *
   * @return A not null reference
   */
  public static StubSmartCard getStubSmartCard() {
    return stubSmartCard;
  }
}

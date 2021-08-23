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
package org.eclipse.keyple.example.generic.distributed.server.websocket.util;

import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.plugin.stub.StubSmartCard;

/**
 * Example of a Stub Implementation of SecureElement. It works with the protocol PROTOCOL_ISO14443_4
 * and simulates a Calypso Card with an Hoplink application
 */
public class CalypsoSmartCard extends StubSmartCard {

  private static final String PROTOCOL = "ISO_14443_4";
  private static final String ATR_HEX = "3B8880010000000000718100F9";

  public CalypsoSmartCard() {
    /* Select Application */
    addHexCommand("00A4 0400 05 AABBCCDDEE 00", "6A82");
    /* Select Application */
    addHexCommand(
        "00A4 0400 09 315449432E49434131 00",
        "6F238409315449432E49434131A516BF0C13C708 0000000011223344 53070A3C23121410019000");
    /* Read Records - EnvironmentAndHolder (SFI=07)) */
    addHexCommand("00B2013C00", "24B92848080000131A50001200000000000000000000000000000000009000");
    /* Open Secure Session V3.1 */
    addHexCommand(
        "008A0B4104C1C2C3C400",
        "030490980030791D01112233445566778899AABBCCDDEEFF00112233445566778899AABBCC9000");
    /* Open Secure Session V3.1 */
    addHexCommand(
        "008A0B3904C1C2C3C400",
        "0308306C00307E1D24B928480800000606F0001200000000000000000000000000000000009000");
    /* Read Records */
    addHexCommand("00B2014400", "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC9000");
    /* Read Records */
    addHexCommand("00B201F400", "00000000000000000000000000000000000000000000000000000000009000");
    /* Read Records */
    addHexCommand("00B2014C00", "00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF9000");
    /* Read Records */
    addHexCommand(
        "00B2014D00",
        "011D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF021D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF031D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF041D00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF9000");
    /* Append Record */
    addHexCommand("00E200401D00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC", "9000");
    /* Append Record */
    addHexCommand("00E200401D01112233445566778899AABBCCDDEEFF00112233445566778899AABBCC", "9000");
    /* Close Secure Session */
    /* no ratification asked */
    addHexCommand("008E0000040506070800", "010203049000");
    /* ratification asked */
    addHexCommand("008E8000040506070800", "010203049000");
    /* Ratification */
    addHexCommand("00B2000000", "6B00");
  }

  @Override
  public byte[] getATR() {
    return ByteArrayUtil.fromHex(ATR_HEX);
  }

  @Override
  public String getCardProtocol() {
    return PROTOCOL;
  }
}

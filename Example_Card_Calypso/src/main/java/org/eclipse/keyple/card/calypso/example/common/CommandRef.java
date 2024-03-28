/* **************************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
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

enum CommandRef {
  START((byte) 0x00),
  STOP((byte) 0xFF),
  GET_DATA((byte) 0xCA),
  PUT_DATA((byte) 0xDA),
  CARD_OPEN_SECURE_SESSION((byte) 0x8A),
  CARD_CLOSE_SECURE_SESSION((byte) 0x8E),
  // CARD_MANAGE_SECURE_SESSION((byte) 0x82),
  CARD_READ_RECORDS((byte) 0xB2),
  CARD_UPDATE_RECORD((byte) 0xDC),
  CARD_WRITE_RECORD((byte) 0xD2),
  CARD_APPEND_RECORD((byte) 0xE2),
  CARD_READ_BINARY((byte) 0xB0),
  CARD_UPDATE_BINARY((byte) 0xD6),
  CARD_WRITE_BINARY((byte) 0xD0),
  CARD_SEARCH_RECORD_MULTIPLE((byte) 0xA2),
  CARD_READ_RECORD_MULTIPLE((byte) 0xB3),
  CARD_GET_CHALLENGE((byte) 0x84),
  CARD_INCREASE((byte) 0x32),
  CARD_DECREASE((byte) 0x30),
  CARD_INCREASE_MULTIPLE((byte) 0x3A),
  CARD_DECREASE_MULTIPLE((byte) 0x38),
  CARD_SELECT_FILE((byte) 0xA4),
  CARD_CHANGE_KEY((byte) 0xD8),
  CARD_CHANGE_PIN((byte) 0xD8),
  CARD_VERIFY_PIN((byte) 0x20),
  CARD_SV_GET((byte) 0x7C),
  CARD_SV_DEBIT((byte) 0xBA),
  CARD_SV_RELOAD((byte) 0xB8),
  CARD_SV_UNDEBIT((byte) 0xBC),
  CARD_INVALIDATE((byte) 0x04),
  CARD_REHABILITATE((byte) 0x44),
  CARD_GENERATE_ASYMMETRIC_KEY_PAIR((byte) 0x46),
  LEGACYSAM_SELECT_DIVERSIFIER((byte) 0x14),
  LEGACYSAM_GET_CHALLENGE((byte) 0x84),
  LEGACYSAM_DIGEST_INIT((byte) 0x8A),
  LEGACYSAM_DIGEST_UPDATE((byte) 0x8C),
  LEGACYSAM_DIGEST_UPDATE_MULTIPLE((byte) 0x8C),
  LEGACYSAM_DIGEST_CLOSE((byte) 0x8E),
  LEGACYSAM_DIGEST_AUTHENTICATE((byte) 0x82),
  LEGACYSAM_DIGEST_INTERNAL_AUTHENTICATE((byte) 0x88),
  LEGACYSAM_GIVE_RANDOM((byte) 0x86),
  LEGACYSAM_CARD_GENERATE_KEY((byte) 0x12),
  LEGACYSAM_CARD_CIPHER_PIN((byte) 0x12),
  LEGACYSAM_UNLOCK((byte) 0x20),
  LEGACYSAM_WRITE_KEY((byte) 0x1A),
  LEGACYSAM_READ_KEY_PARAMETERS((byte) 0xBC),
  LEGACYSAM_READ_COUNTER((byte) 0xBE),
  LEGACYSAM_READ_CEILINGS((byte) 0xBE),
  LEGACYSAM_SV_CHECK((byte) 0x58),
  LEGACYSAM_SV_PREPARE_DEBIT((byte) 0x54),
  LEGACYSAM_SV_PREPARE_LOAD((byte) 0x56),
  LEGACYSAM_SV_PREPARE_UNDEBIT((byte) 0x5C),
  LEGACYSAM_DATA_CIPHER((byte) 0x1C),
  LEGACYSAM_PSO_COMPUTE_SIGNATURE((byte) 0x2A),
  LEGACYSAM_PSO_VERIFY_SIGNATURE((byte) 0x2A),
  LEGACYSAM_SAM_DATA_CIPHER((byte) 0x16),
  LEGACYSAM_WRITE_CEILINGS((byte) 0xD8);

  /** The instruction byte. */
  private final byte instructionByte;

  /**
   * The generic constructor of CalypsoCommands.
   *
   * @param instructionByte the instruction byte.
   * @since 0.1.0
   */
  CommandRef(byte instructionByte) {
    this.instructionByte = instructionByte;
  }

  /**
   * Gets the instruction byte (INS).
   *
   * @return A byte
   * @since 0.1.0
   */
  public byte getInstructionByte() {
    return instructionByte;
  }

  public static String getNameFromInstructionByte(byte instructionByte) {
    for (CommandRef command : CommandRef.values()) {
      if (command.getInstructionByte() == instructionByte) {
        return command.name();
      }
    }
    return null;
  }
}

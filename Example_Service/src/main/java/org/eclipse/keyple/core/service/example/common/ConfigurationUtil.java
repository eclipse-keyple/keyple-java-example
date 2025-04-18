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
package org.eclipse.keyple.core.service.example.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing methods for configuring readers and the card resource service used across
 * several examples.
 *
 * @since 2.0.0
 */
public class ConfigurationUtil {
  public static final String AID_EMV_PPSE = "325041592E5359532E4444463031";
  public static final String AID_KEYPLE_PREFIX = "315449432E";
  public static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

  // Common reader identifiers
  // These two regular expressions can be modified to fit the names of the readers used to run these
  // examples.
  public static final String CONTACTLESS_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String CONTACT_READER_NAME_REGEX = ".*Identive.*|.*HID.*";

  /** Constructor. */
  private ConfigurationUtil() {}
}

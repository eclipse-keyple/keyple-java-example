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
package org.eclipse.keyple.core.service.example.common;

import org.eclipse.keyple.core.service.ConfigurableReader;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
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

  /**
   * (private)<br>
   * Constructor.
   */
  private ConfigurationUtil() {}

  /**
   * Retrieves the first available reader in the provided plugin whose name matches the provided
   * regular expression.
   *
   * @param plugin The plugin to which the reader belongs.
   * @param readerNameRegex A regular expression matching the targeted reader.
   * @return A not null reference.
   * @throws IllegalStateException If the reader is not found.
   * @since 2.0.0
   */
  public static Reader getCardReader(Plugin plugin, String readerNameRegex) {
    for (String readerName : plugin.getReaderNames()) {
      if (readerName.matches(readerNameRegex)) {
        ConfigurableReader reader = (ConfigurableReader) plugin.getReader(readerName);
        // Configure the reader with parameters suitable for contactless operations.
        reader
            .getExtension(PcscReader.class)
            .setContactless(true)
            .setIsoProtocol(PcscReader.IsoProtocol.T1)
            .setSharingMode(PcscReader.SharingMode.SHARED);
        reader.activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(), ISO_CARD_PROTOCOL);
        logger.info("Card reader, plugin; {}, name: {}", plugin.getName(), reader.getName());
        return reader;
      }
    }
    throw new IllegalStateException(
        String.format("Reader '%s' not found in plugin '%s'", readerNameRegex, plugin.getName()));
  }
}

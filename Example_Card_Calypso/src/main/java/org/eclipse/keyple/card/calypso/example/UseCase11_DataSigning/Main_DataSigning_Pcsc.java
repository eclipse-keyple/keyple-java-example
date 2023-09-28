/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.calypso.example.UseCase11_DataSigning;

import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.card.calypso.example.common.ConfigurationUtil;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactProtocol;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySamSelectionExtension;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.*;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case Calypso 11 – Calypso Card data signing (PC/SC)</h1>
 *
 * <p>We demonstrate here how to generate and verify data signature.
 *
 * <p>Only a contact reader is required for the Calypso SAM.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Sets up the card resource service to provide a Calypso SAM (C1).
 *   <li>The card resource service is configured and started to observe the connection/disconnection
 *       of readers and the insertion/removal of cards.
 *   <li>A command line menu allows you to take and release a SAM resource and select a signature
 *       process.
 *   <li>The log and console printouts show the operation of the card resource service and the
 *       signature processes results.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 */
public class Main_DataSigning_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_DataSigning_Pcsc.class);
  public static final String SAM_RESOURCE = "SAM_RESOURCE";
  public static final String READER_NAME_REGEX = ".*Ident.*";
  private static final byte KIF_BASIC = (byte) 0xEC;
  private static final byte KVC_BASIC = (byte) 0x85;
  private static final String KIF_BASIC_STR = HexUtil.toHex(KIF_BASIC);
  private static final String KVC_BASIC_STR = HexUtil.toHex(KVC_BASIC);
  private static final byte KIF_TRACEABLE = (byte) 0x2B;
  private static final byte KVC_TRACEABLE = (byte) 0x19;
  private static final String KIF_TRACEABLE_STR = HexUtil.toHex(KIF_TRACEABLE);
  private static final String KVC_TRACEABLE_STR = HexUtil.toHex(KVC_TRACEABLE);
  public static final String DATA_TO_SIGN = "00112233445566778899AABBCCDDEEFF";

  public static void main(String[] args) {

    // Get the instance of the SmartCardService
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin, get the corresponding PC/SC plugin in return
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the Calypso card extension service
    CalypsoExtensionService calypsoCardService = CalypsoExtensionService.getInstance();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(calypsoCardService);

    // Retrieve the reader API factory
    ReaderApiFactory readerApiFactory = SmartCardServiceProvider.getService().getReaderApiFactory();

    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a card selector without filer
    CardSelector cardSelector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    LegacySamApiFactory legacySamApiFactory =
        LegacySamExtensionService.getInstance().getLegacySamApiFactory();

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        cardSelector, legacySamApiFactory.createLegacySamSelectionExtension());

    // Create a card resource extension expecting a SAM "C1".
    LegacySamSelectionExtension samSelection =
        legacySamApiFactory.createLegacySamSelectionExtension();

    CardResourceProfileExtension samCardResourceExtension =
        LegacySamExtensionService.getInstance()
            .createLegacySamResourceProfileExtension(samSelection);

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    PluginAndReaderExceptionHandler pluginAndReaderExceptionHandler =
        new PluginAndReaderExceptionHandler();

    // Configure the card resource service
    cardResourceService
        .getConfigurator()
        .withBlockingAllocationMode(100, 10000)
        .withPlugins(
            PluginsConfigurator.builder()
                .addPluginWithMonitoring(
                    plugin,
                    new ReaderConfigurator(),
                    pluginAndReaderExceptionHandler,
                    pluginAndReaderExceptionHandler)
                .withUsageTimeout(5000)
                .build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(SAM_RESOURCE, samCardResourceExtension)
                .withReaderNameRegex(READER_NAME_REGEX)
                .build())
        .configure();

    cardResourceService.start();

    boolean isSignatureValid;
    String signatureHex;

    FreeTransactionManager freeTransactionManager;

    boolean loop = true;
    CardResource cardResource = null;
    while (loop) {
      char c = getInput();
      switch (c) {
        case '1':
          cardResource = cardResourceService.getCardResource(SAM_RESOURCE);
          if (cardResource != null) {
            logger.info(
                "A SAM resource is available: reader {}, smart card {}",
                cardResource.getReader().getName(),
                cardResource.getSmartCard());
          } else {
            logger.info("SAM resource is not available");
          }
          break;
        case '2':
          if (cardResource != null) {
            logger.info("Release SAM resource.");
            cardResourceService.releaseCardResource(cardResource);
          } else {
            logger.error("SAM resource is not available");
          }
          break;
        case '3':
          if (cardResource == null) {
            logger.error("No SAM resource.");
            break;
          }
          freeTransactionManager =
              legacySamApiFactory.createFreeTransactionManager(
                  cardResource.getReader(), (LegacySam) cardResource.getSmartCard());
          logger.info(
              "Signing: data='{}' with the key {}/{}", DATA_TO_SIGN, KIF_BASIC_STR, KVC_BASIC_STR);
          BasicSignatureComputationData basicSignatureComputationData =
              legacySamApiFactory
                  .createBasicSignatureComputationData()
                  .setData(HexUtil.toByteArray(DATA_TO_SIGN), KIF_BASIC, KVC_BASIC);
          freeTransactionManager.prepareComputeSignature(basicSignatureComputationData);
          freeTransactionManager.processCommands();
          signatureHex = HexUtil.toHex(basicSignatureComputationData.getSignature());
          logger.info("signature='{}'", signatureHex);

          logger.info(
              "Verifying: data='{}', signature='{}' with the key {}/{}",
              DATA_TO_SIGN,
              signatureHex,
              KIF_BASIC_STR,
              KVC_BASIC_STR);
          BasicSignatureVerificationData basicSignatureVerificationData =
              legacySamApiFactory
                  .createBasicSignatureVerificationData()
                  .setData(
                      HexUtil.toByteArray(DATA_TO_SIGN),
                      HexUtil.toByteArray(signatureHex),
                      KIF_BASIC,
                      KVC_BASIC);
          freeTransactionManager.prepareVerifySignature(basicSignatureVerificationData);
          freeTransactionManager.processCommands();
          isSignatureValid = basicSignatureVerificationData.isSignatureValid();
          logger.info("Signature is valid: '{}'", isSignatureValid);

          break;

        case '4':
          if (cardResource == null) {
            logger.error("No SAM resource.");
            break;
          }
          freeTransactionManager =
              legacySamApiFactory.createFreeTransactionManager(
                  cardResource.getReader(), (LegacySam) cardResource.getSmartCard());
          logger.info(
              "Signing: data='{}' with the key {}/{}",
              DATA_TO_SIGN,
              KIF_TRACEABLE_STR,
              KVC_TRACEABLE_STR);
          TraceableSignatureComputationData traceableSignatureComputationData =
              legacySamApiFactory
                  .createTraceableSignatureComputationData()
                  .setData(HexUtil.toByteArray(DATA_TO_SIGN), KIF_TRACEABLE, KVC_TRACEABLE)
                  .withSamTraceabilityMode(0, SamTraceabilityMode.FULL_SERIAL_NUMBER);
          freeTransactionManager.prepareComputeSignature(traceableSignatureComputationData);
          freeTransactionManager.processCommands();
          signatureHex = HexUtil.toHex(traceableSignatureComputationData.getSignature());
          String signedDataHex = HexUtil.toHex(traceableSignatureComputationData.getSignedData());
          logger.info("signature='{}'", signatureHex);
          logger.info("signed data='{}'", signedDataHex);

          logger.info(
              "Verifying: data='{}', signature='{}' with the key {}/{}",
              signedDataHex,
              signatureHex,
              KIF_TRACEABLE_STR,
              KVC_TRACEABLE_STR);
          TraceableSignatureVerificationData traceableSignatureVerificationData =
              legacySamApiFactory
                  .createTraceableSignatureVerificationData()
                  .setData(
                      HexUtil.toByteArray(signedDataHex),
                      HexUtil.toByteArray(signatureHex),
                      KIF_TRACEABLE,
                      KVC_TRACEABLE)
                  .withSamTraceabilityMode(0, SamTraceabilityMode.FULL_SERIAL_NUMBER, null);
          freeTransactionManager.prepareVerifySignature(traceableSignatureVerificationData);
          freeTransactionManager.processCommands();
          isSignatureValid = traceableSignatureVerificationData.isSignatureValid();
          logger.info("Signature is valid: '{}'", isSignatureValid);

          break;
        case 'q':
          loop = false;
          break;
        default:
          break;
      }
    }

    // unregister plugin
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program.");
  }

  /**
   * Reader configurator used by the card resource service to set up the SAM reader with the
   * required settings.
   */
  private static class ReaderConfigurator implements ReaderConfiguratorSpi {
    private static final Logger logger = LoggerFactory.getLogger(ReaderConfigurator.class);

    /**
     * (private)<br>
     * Constructor.
     */
    private ReaderConfigurator() {}

    /** {@inheritDoc} */
    @Override
    public void setupReader(CardReader reader) {
      // Configure the reader with parameters suitable for contact operations.
      try {
        ((ConfigurableCardReader) reader)
            .activateProtocol(
                PcscSupportedContactProtocol.ISO_7816_3_T0.name(), ConfigurationUtil.SAM_PROTOCOL);
        KeypleReaderExtension readerExtension =
            SmartCardServiceProvider.getService()
                .getPlugin(reader)
                .getReaderExtension(KeypleReaderExtension.class, reader.getName());
        if (readerExtension instanceof PcscReader)
          ((PcscReader) readerExtension)
              .setContactless(false)
              .setIsoProtocol(PcscReader.IsoProtocol.ANY)
              .setSharingMode(PcscReader.SharingMode.SHARED);
      } catch (Exception e) {
        logger.error("Exception raised while setting up the reader {}", reader.getName(), e);
      }
    }
  }

  /** Class implementing the exception handler SPIs for plugin and reader monitoring. */
  private static class PluginAndReaderExceptionHandler
      implements PluginObservationExceptionHandlerSpi, CardReaderObservationExceptionHandlerSpi {

    @Override
    public void onPluginObservationError(String pluginName, Throwable e) {
      logger.error("An exception occurred while monitoring the plugin '{}'.", pluginName, e);
    }

    @Override
    public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
      logger.error(
          "An exception occurred while monitoring the reader '{}/{}'.", pluginName, readerName, e);
    }
  }

  public static char getInput() {

    int key = 0;

    System.out.println("Options:");
    System.out.println("    '1': Get a SAM resource");
    System.out.println("    '2': Release a SAM resource");
    System.out.println("    '3': Basic signature generation and verification");
    System.out.println("    '4': Traceable signature generation and verification");
    System.out.println("    'q': quit");
    System.out.print("Select an option: ");

    try {
      key = System.in.read();
    } catch (java.io.IOException e) {
      logger.error("Input error");
    }

    try {
      while ((System.in.available()) != 0) System.in.read();
    } catch (java.io.IOException e) {
      logger.error("Input error");
    }

    return (char) key;
  }
}

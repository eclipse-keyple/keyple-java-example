/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.example.card.calypso.UseCase11_DataSigning;

import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySamSelectionExtension;
import org.eclipse.keypop.calypso.crypto.legacysam.transaction.*;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the execution of Calypso Legacy SAM data signing using an SAM resource service and the
 * Legacy SAM extension service.
 *
 * <p>This class demonstrates the process of generating and verifying data signatures with a Calypso
 * Legacy SAM, utilizing a contact reader and the card resource service for Calypso SAM (C1). It
 * highlights the setup, observation, and interaction steps, offering insights into signature
 * operations and resource management.
 *
 * <h2>Key Functionalities</h2>
 *
 * <ul>
 *   <li>Setting up the card resource service to provide and manage access to a Calypso SAM (C1).
 *   <li>Configuring and initiating the observation of reader connections and card
 *       insertions/removals.
 *   <li>Utilizing a command-line interface for user interactions to acquire, release SAM resources,
 *       and initiate signature processes.
 *   <li>Providing log and console outputs to trace the operations of the card resource service and
 *       display the outcomes of signature processes.
 * </ul>
 *
 * <p>Operations and results are systematically logged via slf4j, facilitating comprehensive
 * monitoring, tracking, and debugging. In the occurrence of unexpected behaviors or anomalies,
 * runtime exceptions are generated, offering clear insights into issues for prompt resolution.
 *
 * <p>Throws IllegalStateException if errors emerge in the signature generation, verification, or
 * resource management processes, fortifying error handling, and security protocols.
 */
public class Main_DataSigning_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_DataSigning_Pcsc.class);

  private static final String SAM_READER_NAME_REGEX = ".*Ident.*";
  private static final byte KIF_BASIC = (byte) 0xEC;
  private static final byte KVC_BASIC = (byte) 0x85;
  private static final String KIF_BASIC_STR = HexUtil.toHex(KIF_BASIC);
  private static final String KVC_BASIC_STR = HexUtil.toHex(KVC_BASIC);
  private static final byte KIF_TRACEABLE = (byte) 0x2B;
  private static final byte KVC_TRACEABLE = (byte) 0x19;
  private static final String KIF_TRACEABLE_STR = HexUtil.toHex(KIF_TRACEABLE);
  private static final String KVC_TRACEABLE_STR = HexUtil.toHex(KVC_TRACEABLE);
  private static final String DATA_TO_SIGN = "00112233445566778899AABBCCDDEEFF";

  // The name of the SAM resource provided by the Card Resource Manager and used during the card
  // transaction.
  private static final String SAM_PROFILE_NAME = "SAM C1";

  // The plugin used to manage the readers.
  private static Plugin plugin;
  // The reader used to communicate with the card.
  private static ReaderApiFactory readerApiFactory;
  // The Calypso factory used to create the selection extension and transaction managers.
  private static LegacySamApiFactory legacySamApiFactory;
  // The Card Resource Service to manage SAM resources.
  private static CardResourceService cardResourceService;

  public static void main(String[] args) {

    // Initialize the context
    initKeypleService();
    initLegacySamExtensionService();
    initSamResourceService();

    boolean loop = true;
    CardResource cardResource = null;

    while (loop) {
      char input = getInput();
      switch (input) {
        case '1':
          cardResource = acquireSamResource(SAM_PROFILE_NAME);
          break;
        case '2':
          releaseSamResource(cardResource);
          break;
        case '3':
          performBasicSignature(cardResource);
          break;
        case '4':
          performTraceableSignature(cardResource);
          break;
        case 'q':
          loop = false;
          break;
        default:
          break;
      }
    }

    // unregister plugin
    SmartCardServiceProvider.getService().unregisterPlugin(plugin.getName());

    logger.info("Exit program.");
  }

  /**
   * Initializes the Keyple service.
   *
   * <p>Gets an instance of the smart card service, registers the PC/SC plugin, and prepares the
   * reader API factory for use.
   *
   * <p>Retrieves the {@link ReaderApiFactory}.
   */
  private static void initKeypleService() {
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());
    readerApiFactory = smartCardService.getReaderApiFactory();
  }

  /**
   * Initializes the SAM Resource Service making a SAM resource available under the SAM_PROFILE_NAME
   * name.
   */
  private static void initSamResourceService() {
    // Create a SAM selection manager.
    CardSelectionManager samSelectionManager = readerApiFactory.createCardSelectionManager();

    // Create a card selector. Optionally, apply a filter based on the power-on data to expect a SAM
    // C1.
    CardSelector<BasicCardSelector> cardSelector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    // Retrieve the Legacy SAM factory to create the SAM selection and profile extensions.
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

    // Get the card resource service
    cardResourceService = CardResourceServiceProvider.getService();

    PluginAndReaderExceptionHandler pluginAndReaderExceptionHandler =
        new PluginAndReaderExceptionHandler();

    // Set up a basic configuration without plugin/reader observation.
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
            CardResourceProfileConfigurator.builder(SAM_PROFILE_NAME, samCardResourceExtension)
                .withReaderNameRegex(SAM_READER_NAME_REGEX)
                .build())
        .configure();
    cardResourceService.start();

    // Verify if the card resource is available.
    CardResource cardResource = cardResourceService.getCardResource(SAM_PROFILE_NAME);

    if (cardResource == null) {
      throw new IllegalStateException(
          String.format(
              "Failed to retrieve a SAM card resource. No card resource found for profile '%s' with reader matching '%s' in plugin '%s'.",
              SAM_PROFILE_NAME, SAM_READER_NAME_REGEX, plugin.getName()));
    }

    // Release the card resource.
    cardResourceService.releaseCardResource(cardResource);
  }

  /**
   * Reader configurator used by the card resource service to set up the SAM reader with the
   * required settings.
   */
  private static class ReaderConfigurator implements ReaderConfiguratorSpi {

    /** Constructor. */
    private ReaderConfigurator() {}

    /** {@inheritDoc} */
    @Override
    public void setupReader(CardReader cardReader) {
      // Configure the reader with parameters suitable for contactless operations.
      try {
        KeypleReaderExtension readerExtension =
            SmartCardServiceProvider.getService()
                .getPlugin(cardReader)
                .getReaderExtension(KeypleReaderExtension.class, cardReader.getName());
        if (readerExtension instanceof PcscReader) {
          ((PcscReader) readerExtension)
              .setContactless(false)
              .setIsoProtocol(PcscReader.IsoProtocol.ANY)
              .setSharingMode(PcscReader.SharingMode.SHARED);
        }
      } catch (Exception e) {
        logger.error("Exception raised while setting up the reader {}", cardReader.getName(), e);
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

  /**
   * Initializes the Calypso card extension service.
   *
   * <p>Retrieves the {@link CalypsoCardApiFactory}.
   */
  private static void initLegacySamExtensionService() {
    LegacySamExtensionService legacySamExtensionService = LegacySamExtensionService.getInstance();
    SmartCardServiceProvider.getService().checkCardExtension(legacySamExtensionService);
    legacySamApiFactory = legacySamExtensionService.getLegacySamApiFactory();
  }

  private static CardResource acquireSamResource(String cardResourceName) {
    CardResource cardResource = cardResourceService.getCardResource(cardResourceName);
    if (cardResource != null) {
      logger.info(
          "A SAM resource is available: reader {}, smart card {}",
          cardResource.getReader().getName(),
          cardResource.getSmartCard());
    } else {
      logger.info("SAM resource is not available");
    }
    return cardResource;
  }

  private static void releaseSamResource(CardResource cardResource) {
    if (cardResource != null) {
      logger.info("Release SAM resource.");
      cardResourceService.releaseCardResource(cardResource);
    } else {
      logger.error("SAM resource is not available");
    }
  }

  private static void performBasicSignature(CardResource cardResource) {
    if (cardResource == null) {
      logger.error("No SAM resource.");
      return;
    }
    FreeTransactionManager freeTransactionManager =
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
    String signatureHex = HexUtil.toHex(basicSignatureComputationData.getSignature());
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
    boolean isSignatureValid = basicSignatureVerificationData.isSignatureValid();
    logger.info("Signature is valid: '{}'", isSignatureValid);
  }

  private static void performTraceableSignature(CardResource cardResource) {
    if (cardResource == null) {
      logger.error("No SAM resource.");
      return;
    }
    FreeTransactionManager freeTransactionManager =
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
    String signatureHex = HexUtil.toHex(traceableSignatureComputationData.getSignature());
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
    boolean isSignatureValid = traceableSignatureVerificationData.isSignatureValid();
    logger.info("Signature is valid: '{}'", isSignatureValid);
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

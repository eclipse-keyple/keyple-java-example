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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;

public class PerformanceMeasurement {

  public static String toJson(List<Long> log) {
    long previousTimestamp = 0;
    boolean first = true;
    long initialTimeStamp = log.get(0) & 0x00FFFFFFFFFFFFFFL;
    long previousRelativeTime = 0;
    boolean firstEntry = true;
    List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
    for (long entry : log) {
      byte operation = (byte) (entry >> 56);
      long timestamp = (entry & 0x00FFFFFFFFFFFFFFL);
      String commandName = CommandRef.getNameFromInstructionByte(operation);
      long relativeTime = timestamp - previousTimestamp;
      Map<String, Object> entryMap = new LinkedHashMap<String, Object>();
      if (operation == 0x00) {
        entryMap.put("operation", "START");
        entryMap.put("time", 0);
        entryMap.put("sinceBeginning", timestamp - initialTimeStamp);
        entries.add(entryMap);
      } else if (operation == (byte) 0xFF) {
        entryMap.put("operation", "STOP");
        entryMap.put("time", relativeTime);
        entryMap.put("sinceBeginning", timestamp - initialTimeStamp);
        entries.add(entryMap);
      } else {
        if (first) {
          if (!firstEntry) {
            previousRelativeTime = relativeTime;
          }
          first = false;
        } else {
          entryMap.put("operation", commandName);
          entryMap.put("interCommandTime", previousRelativeTime);
          entryMap.put("executionTime", relativeTime);
          entryMap.put("sinceBeginning", timestamp - initialTimeStamp);
          first = true;
          entries.add(entryMap);
        }
      }
      firstEntry = false;
      previousTimestamp = timestamp;
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(entries);
    return json;
  }
}

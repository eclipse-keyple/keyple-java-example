/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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
package org.eclipse.keyple.plugin.android.nfc.example

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.eclipse.keyple.databinding.ActionCardLayoutBinding
import org.eclipse.keyple.databinding.HeaderCardLayoutBinding
import org.eclipse.keyple.databinding.ResultCardLayoutBinding

/**
 * Adapter class for displaying messages in a RecyclerView.
 *
 * This class extends RecyclerView.Adapter and provides implementations for creating ViewHolder,
 * binding data to ViewHolder, retrieving item view type, and getting the total item count. The
 * adapter accepts a list of messages to display.
 *
 * @property messages The list of messages to be displayed.
 */
class MessageDisplayAdapter(private val messages: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      Message.TYPE_ACTION ->
          ActionViewHolder(ActionCardLayoutBinding.inflate(inflater, parent, false))
      Message.TYPE_RESULT ->
          ResultViewHolder(ResultCardLayoutBinding.inflate(inflater, parent, false))
      else -> HeaderViewHolder(HeaderCardLayoutBinding.inflate(inflater, parent, false))
    }
  }

  override fun getItemCount(): Int {
    return messages.size
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is ActionViewHolder -> holder.bind(messages[position])
      is ResultViewHolder -> holder.bind(messages[position])
      is HeaderViewHolder -> holder.bind(messages[position])
    }
  }

  override fun getItemViewType(position: Int): Int {
    return messages[position].type
  }

  class ActionViewHolder(private val binding: ActionCardLayoutBinding) :
      RecyclerView.ViewHolder(binding.root) {
    fun bind(message: Message) {
      binding.cardActionTextView.text = message.text
    }
  }

  class ResultViewHolder(private val binding: ResultCardLayoutBinding) :
      RecyclerView.ViewHolder(binding.root) {
    fun bind(message: Message) {
      binding.cardResultTextView.text = message.text
    }
  }

  class HeaderViewHolder(private val binding: HeaderCardLayoutBinding) :
      RecyclerView.ViewHolder(binding.root) {
    fun bind(message: Message) {
      binding.cardHeaderTextView.text = message.text
    }
  }
}

/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.nfc.example

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.eclipse.keyple.databinding.ActionCardLayoutBinding
import org.eclipse.keyple.databinding.HeaderCardLayoutBinding
import org.eclipse.keyple.databinding.ResultCardLayoutBinding

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

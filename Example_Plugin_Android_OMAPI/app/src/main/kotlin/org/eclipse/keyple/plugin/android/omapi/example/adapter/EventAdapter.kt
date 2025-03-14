/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.omapi.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import org.eclipse.keyple.R
import org.eclipse.keyple.databinding.CardActionEventBinding
import org.eclipse.keyple.databinding.CardChoiceEventBinding
import org.eclipse.keyple.databinding.CardHeaderEventBinding
import org.eclipse.keyple.databinding.CardResultEventBinding
import org.eclipse.keyple.plugin.android.omapi.example.model.ChoiceEventModel
import org.eclipse.keyple.plugin.android.omapi.example.model.EventModel
import org.eclipse.keyple.plugin.android.omapi.example.util.getColorResource

class EventAdapter(private val events: ArrayList<EventModel>) :
    RecyclerView.Adapter<EventAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      EventModel.TYPE_ACTION ->
          ActionViewHolder(CardActionEventBinding.inflate(inflater, parent, false))
      EventModel.TYPE_RESULT ->
          ResultViewHolder(CardResultEventBinding.inflate(inflater, parent, false))
      EventModel.TYPE_MULTICHOICE ->
          ChoiceViewHolder(CardChoiceEventBinding.inflate(inflater, parent, false))
      else -> HeaderViewHolder(CardHeaderEventBinding.inflate(inflater, parent, false))
    }
  }

  override fun getItemCount(): Int {
    return events.size
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    viewHolder.bind(events[position])
  }

  override fun getItemViewType(position: Int): Int {
    return events[position].type
  }

  abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(event: EventModel)
  }

  class ActionViewHolder(private val binding: CardActionEventBinding) : ViewHolder(binding.root) {
    override fun bind(event: EventModel) {
      binding.cardActionTextView.text = event.text
    }
  }

  class ResultViewHolder(private val binding: CardResultEventBinding) : ViewHolder(binding.root) {
    override fun bind(event: EventModel) {
      // Assuming CardResultEventBinding also has a cardActionTextView
      binding.cardActionTextView.text = event.text
    }
  }

  class HeaderViewHolder(private val binding: CardHeaderEventBinding) : ViewHolder(binding.root) {
    override fun bind(event: EventModel) {
      // Assuming CardHeaderEventBinding also has a cardActionTextView
      binding.cardActionTextView.text = event.text
    }
  }

  class ChoiceViewHolder(private val binding: CardChoiceEventBinding) : ViewHolder(binding.root) {
    override fun bind(event: EventModel) {
      binding.cardActionTextView.text = event.text

      binding.choiceRadioGroup.removeAllViews()
      (event as ChoiceEventModel).choices.forEachIndexed { index, choice ->
        val button = RadioButton(binding.root.context)
        button.text = choice
        button.id = index
        button.setOnClickListener { event.callback(choice) }
        button.setTextColor(binding.root.context.getColorResource(R.color.textColorPrimary))
        binding.choiceRadioGroup.addView(button)
      }
    }
  }
}

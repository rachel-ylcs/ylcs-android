package com.yinlin.rachel.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.toDP


object RachelPopMenu {
    data class Item(val title: String, val callback: () -> Unit)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class Adapter(private val items: List<Item>, private val popupWindow: PopupWindow) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
            val context = parent.context
            val tv = TextView(context)
            val params = MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT)
            val margin = 5.toDP(context)
            params.setMargins(margin * 2, margin, margin * 2, margin)
            tv.layoutParams = params
            val holder = ViewHolder(tv)
            tv.setOnClickListener {
                popupWindow.dismiss()
                items[holder.bindingAdapterPosition].callback()
            }
            return holder
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as TextView).text = items[position].title
        }
    }

    @SuppressLint("InflateParams")
    fun showDown(view: View, items: List<Item>) {
        val context = view.context
        val card = LayoutInflater.from(context).inflate(R.layout.dialog_menu, null)
        val list = card.findViewById<RecyclerView>(R.id.list)
        val popupWindow = PopupWindow(card, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = Adapter(items, popupWindow)
        list.addItemDecoration(MaterialDividerItemDecoration(context, LinearLayoutManager.VERTICAL).apply {
            this.isLastItemDecorated = false
            this.dividerColor = context.getColor(R.color.light_gray)
        })
        popupWindow.isFocusable = true
        popupWindow.animationStyle = android.R.style.Animation_Dialog
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        popupWindow.showAsDropDown(view)
    }
}
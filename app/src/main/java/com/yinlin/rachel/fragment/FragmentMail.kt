package com.yinlin.rachel.fragment

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.user.Mail
import com.yinlin.rachel.databinding.FragmentMailBinding
import com.yinlin.rachel.databinding.ItemMailBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentMail(main: MainActivity) : RachelFragment<FragmentMailBinding>(main) {
    class Adapter(private val fragment: FragmentMail) : RachelAdapter<ItemMailBinding, Mail>() {
        private val main = fragment.main

        override fun bindingClass() = ItemMailBinding::class.java

        override fun init(holder: RachelViewHolder<ItemMailBinding>, v: ItemMailBinding) {
            v.buttonYes.rachelClick {
                val position = holder.bindingAdapterPosition
                val mail = this[position]
                if (mail.processed) fragment.tip(Tip.WARNING, "此邮件已处理")
                else {
                    RachelDialog.confirm(main, content="处理此邮件?") {
                        processMail(position, mail, true)
                    }
                }
            }
            v.buttonNo.rachelClick {
                val position = holder.bindingAdapterPosition
                val mail = this[position]
                if (mail.processed) fragment.tip(Tip.WARNING, "此邮件已处理")
                else {
                    RachelDialog.confirm(main, content="拒绝此邮件?") {
                        processMail(position, mail, false)
                    }
                }
            }
            v.buttonDelete.rachelClick {
                val position = holder.bindingAdapterPosition
                val mail = this[position]
                if (mail.processed) {
                    RachelDialog.confirm(main, content="删除此邮件?") {
                        deleteMail(position, mail)
                    }
                }
                else fragment.tip(Tip.WARNING, "你还没有处理这封邮件!")
            }
        }

        override fun update(v: ItemMailBinding, item: Mail, position: Int) {
            when (item.type) {
                Mail.TYPE_INFO -> {
                    v.label.text = "通知"
                    v.label.bgColor = main.rc(R.color.steel_blue)
                    v.buttonYes.visible = false
                    v.buttonNo.visible = false
                }
                Mail.TYPE_CONFIRM -> {
                    v.label.text = "验证"
                    v.label.bgColor = main.rc(R.color.green)
                    v.buttonYes.visible = true
                    v.buttonNo.visible = false
                }
                Mail.TYPE_DECISION -> {
                    v.label.text = "决定"
                    v.label.bgColor = main.rc(R.color.purple)
                    v.buttonYes.visible = true
                    v.buttonNo.visible = true
                }
                Mail.TYPE_INPUT -> {
                    v.label.text = "输入"
                    v.label.bgColor = main.rc(R.color.orange)
                    v.buttonYes.visible = false
                    v.buttonNo.visible = false
                }
                else -> {
                    v.label.text = "未知"
                    v.label.bgColor = main.rc(R.color.red)
                    v.buttonYes.visible = false
                    v.buttonNo.visible = false
                }
            }
            if (item.processed) {
                v.buttonYes.visible = false
                v.buttonNo.visible = false
            }
            v.title.text = item.title
            v.title.textColor = main.rc(if (item.processed) R.color.black else R.color.steel_blue)
            v.content.text = item.content
            v.date.text = item.ts
            v.contentDetails.text = item.content
            v.buttonDelete.visible = item.processed
        }

        override fun onItemClicked(v: ItemMailBinding, item: Mail, position: Int) {
            if (v.expander.isExpand) {
                val drawable = ContextCompat.getDrawable(main, R.drawable.icon_expand)
                v.content.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
                v.expander.isExpand = false
            }
            else {
                val drawable = ContextCompat.getDrawable(main, R.drawable.icon_expand_down)
                v.content.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
                v.expander.isExpand = true
            }
        }

        @NewThread
        private fun processMail(position: Int, mail: Mail, confirm: Boolean) {
            fragment.lifecycleScope.launch {
                val loading = main.loading
                val result = withContext(Dispatchers.IO) { API.UserAPI.processMail(Config.token, mail.mid, confirm) }
                loading.dismiss()
                if (result.success) {
                    mail.processed = true
                    notifyItemChanged(position)
                    fragment.tip(Tip.SUCCESS, result.msg)
                }
                else fragment.tip(Tip.ERROR, result.msg)
            }
        }

        @NewThread
        private fun deleteMail(position: Int, mail: Mail) {
            fragment.lifecycleScope.launch {
                val loading = main.loading
                val result = withContext(Dispatchers.IO) { API.UserAPI.deleteMail(Config.token, mail.mid) }
                loading.dismiss()
                if (result.success) {
                    removeItem(position)
                    notifyItemRemoved(position)
                    fragment.tip(Tip.SUCCESS, result.msg)
                }
                else fragment.tip(Tip.ERROR, result.msg)
            }
        }
    }

    private val adapter = Adapter(this)

    override fun bindingClass() = FragmentMailBinding::class.java

    override fun init() {
        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main)
            recycledViewPool.setMaxRecycledViews(0, 10)
            setHasFixedSize(true)
            adapter = this@FragmentMail.adapter
        }

        // 下拉刷新
        v.container.setOnRefreshListener { loadMail() }

        // 首次刷新
        loadMail()
    }

    override fun back() = BackState.POP

    @NewThread
    private fun loadMail() {
        lifecycleScope.launch {
            v.state.showLoading()
            val result = withContext(Dispatchers.IO) { API.UserAPI.getMail(Config.token) }
            if (v.container.isRefreshing) v.container.finishRefresh()
            when (result.code) {
                API.Code.SUCCESS -> {
                    val mails = result.data
                    if (mails.isEmpty()) v.state.showEmpty()
                    else v.state.showContent()
                    adapter.setSource(mails)
                    adapter.notifySource()
                }
                API.Code.UNAUTHORIZED -> tip(Tip.WARNING, result.msg)
                API.Code.FAILED -> tip(Tip.ERROR, result.msg)
                else -> v.state.showOffline { loadMail() }
            }
        }
    }
}
package com.yinlin.rachel.fragment

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.IOThread
import com.yinlin.rachel.annotation.Layout
import com.yinlin.rachel.api.API
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.data.user.Mail
import com.yinlin.rachel.databinding.FragmentMailBinding
import com.yinlin.rachel.databinding.ItemMailBinding
import com.yinlin.rachel.model.RachelAdapter
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.startIOWithResult
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.visible

@Layout(FragmentMailBinding::class)
class FragmentMail(main: MainActivity) : RachelFragment<FragmentMailBinding>(main) {
    @Layout(ItemMailBinding::class)
    class Adapter(private val fragment: FragmentMail) : RachelAdapter<ItemMailBinding, Mail>() {
        private val main = fragment.main

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

        @IOThread
        private fun processMail(position: Int, mail: Mail, confirm: Boolean) {
            val loading = main.loading
            fragment.startIOWithResult({ API.UserAPI.processMail(Config.token, mail.mid, confirm) }) {
                loading.dismiss()
                if (it.success) {
                    mail.processed = true
                    notifyItemChanged(position)
                    fragment.tip(Tip.SUCCESS, it.msg)
                }
                else fragment.tip(Tip.ERROR, it.msg)
            }
        }

        @IOThread
        private fun deleteMail(position: Int, mail: Mail) {
            val loading = main.loading
            fragment.startIOWithResult({ API.UserAPI.deleteMail(Config.token, mail.mid) }) {
                loading.dismiss()
                if (it.success) {
                    removeItem(position)
                    notifyItemRemoved(position)
                    fragment.tip(Tip.SUCCESS, it.msg)
                }
                else fragment.tip(Tip.ERROR, it.msg)
            }
        }
    }

    private val mAdapter = Adapter(this)

    override fun init() {
        // 列表
        v.list.apply {
            layoutManager = LinearLayoutManager(main)
            recycledViewPool.setMaxRecycledViews(0, 10)
            setHasFixedSize(true)
            adapter = mAdapter
        }

        // 下拉刷新
        v.container.setOnRefreshListener { loadMail() }

        // 首次刷新
        loadMail()
    }

    override fun back() = BackState.POP

    @IOThread
    private fun loadMail() {
        v.state.showLoading()
        startIOWithResult({ API.UserAPI.getMail(Config.token) }) {
            if (v.container.isRefreshing) v.container.finishRefresh()
            when (it.code) {
                API.Code.SUCCESS -> {
                    val mails = it.data
                    if (mails.isEmpty()) v.state.showEmpty()
                    else v.state.showContent()
                    mAdapter.setSource(mails)
                    mAdapter.notifySource()
                }
                API.Code.UNAUTHORIZED -> tip(Tip.WARNING, it.msg)
                API.Code.FAILED -> tip(Tip.ERROR, it.msg)
                else -> v.state.showOffline { loadMail() }
            }
        }
    }
}
package com.example.duos.ui.main.dailyMatching
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Dimension
import com.example.duos.CustomDialog
import com.example.duos.R
import com.example.duos.data.entities.dailyMatching.DailyMatchingMessageResult
import com.example.duos.data.remote.chat.chat.CreateChatRoomResultData
import com.example.duos.databinding.PartnerFilterCustomDialogBinding
import com.example.duos.ui.main.partnerSearch.PartnerFilterDialog

class DailyMatchingChatDialog(context: Context) : Dialog(context) {
    var partnerChatCount: Int = 0
    var leftButtonCallback: DailyMatchingChatDialogCallbackLeft? = null    // 채팅 취소
    var rightButtonCallback: DailyMatchingChatDialogCallbackRight? = null  // 채팅 ㄱㄱ

    var mBinding: PartnerFilterCustomDialogBinding? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(true) // Dialog 바깥 역역 터치 -> Dialog 사라짐.
        mBinding = PartnerFilterCustomDialogBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)
        window?.setGravity(Gravity.CENTER)
        val width = (context.resources.displayMetrics.widthPixels * 0.91).toInt()   //        기기 가로 너비의 91% 크기로 적용하겠다
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        initView()
    }

    private fun initView() {
        mBinding!!.partnerFilterCountTv.text = partnerChatCount.toString()
        mBinding!!.partnerFilterCountNoticeTextTv.text = "채팅은 하루 5명까지 가능합니다."
        mBinding!!.partnerFilterCountTextTv.text = "남은 채팅 가능 횟수 : "
        mBinding!!.partnerFilterCustomDialogLeftBtn.text = "취소"
        mBinding!!.partnerFilterCustomDialogRightBtn.text = "채팅"
        mBinding!!.partnerFilterCustomDialogLeftBtn.setOnClickListener(View.OnClickListener {
            leftButtonCallback!!.onClick(this) })
        mBinding!!.partnerFilterCustomDialogRightBtn.setOnClickListener(View.OnClickListener {
            rightButtonCallback!!.onClick(this) })
    }




    class Builder(val context: Context) {
        private var dialog = DailyMatchingChatDialog(context)

        fun setCount(count: Int): Builder {
            dialog.partnerChatCount = count
            return this
        }

        fun setLeftButton(callback: DailyMatchingChatDialogCallbackLeft): Builder {
            dialog.leftButtonCallback = callback
            return this
        }

        fun setRightButton(callback: DailyMatchingChatDialogCallbackRight): Builder {

            dialog.rightButtonCallback = callback
            return this
        }

        fun show(): DailyMatchingChatDialog {
            dialog.show()
            return dialog
        }
    }
    interface DailyMatchingChatDialogCallbackLeft {
        fun onClick(dialog: DailyMatchingChatDialog)

    }

    interface DailyMatchingChatDialogCallbackRight {
        fun onClick(dialog: DailyMatchingChatDialog)
        fun onDailyMatchingMessageSuccess(dailyMatchingMessageResult : DailyMatchingMessageResult)
        fun onDailyMatchingMessageFailure(code: Int, message: String)
    }
}

class DailyMatchingChatUnavailableDialog(context: Context) : Dialog(context) {
    var leftButtonCallback: DailyMatchingChatDialogCallbackLeft? = null    // 채팅 취소
    var mBinding: PartnerFilterCustomDialogBinding? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(true) // Dialog 바깥 역역 터치 -> Dialog 사라짐.
        mBinding = PartnerFilterCustomDialogBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)
        window?.setGravity(Gravity.CENTER)
        val width = (context.resources.displayMetrics.widthPixels * 0.91).toInt()   //        기기 가로 너비의 91% 크기로 적용하겠다
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        initView()
    }

    @SuppressLint("ResourceAsColor")
    private fun initView() {
        val partnerProfileChatText = "채팅 가능한 횟수가 모두 소진되었습니다. \n채팅은 하루 5명까지 가능합니다."
        val spannable = SpannableString(partnerProfileChatText)
        spannable.setSpan(ForegroundColorSpan(R.color.primary), 25, 33, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        mBinding!!.partnerFilterCountNoticeTextTv.apply {
            text = partnerProfileChatText
            setText(spannable, TextView.BufferType.SPANNABLE) }
        mBinding!!.underTextVisibleLl.visibility = View.GONE
        mBinding!!.partnerFilterCustomDialogRightBtn.visibility = View.GONE
        mBinding!!.partnerFilterCustomDialogLeftBtn.apply {
            text = "확인"
            setTextColor(R.color.primary)
            setTextSize(Dimension.SP, 15F);
        }
    }

    class Builder(val context: Context) {
        private var dialog = DailyMatchingChatUnavailableDialog(context)

        fun setLeftButton(callback: DailyMatchingChatDialogCallbackLeft): Builder {
            dialog.leftButtonCallback = callback
            return this
        }


        fun show(): DailyMatchingChatUnavailableDialog {
            dialog.show()
            return dialog
        }
    }
    interface DailyMatchingChatDialogCallbackLeft {
        fun onClick(dialog: DailyMatchingChatUnavailableDialog)

    }


}


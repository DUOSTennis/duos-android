package com.example.duos.ui.main.dailyMatching

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.Glide
import com.example.duos.ApplicationClass.Companion.TAG
import com.example.duos.BottomSheetDialog01
import com.example.duos.BottomSheetDialog02
import com.example.duos.BottomSheetDialog03
import com.example.duos.R
import com.example.duos.data.entities.chat.ChatRoom
import com.example.duos.data.entities.dailyMatching.DailyMatchingDetail
import com.example.duos.data.entities.dailyMatching.DailyMatchingMessageParticipantIdx
import com.example.duos.data.entities.dailyMatching.DailyMatchingMessageResult
import com.example.duos.data.entities.dailyMatching.DailyMatchingOption
import com.example.duos.data.local.ChatDatabase
import com.example.duos.data.remote.appointment.AppointmentService
import com.example.duos.data.remote.chat.chat.ChatService
import com.example.duos.data.remote.chat.chat.CreateChatRoomResultData
import com.example.duos.data.remote.dailyMatching.DailyMatchingListService
import com.example.duos.data.remote.dailyMatching.DailyMatchingService
import com.example.duos.databinding.ActivityDailyMatchingDetailBinding
import com.example.duos.ui.BaseActivity
import com.example.duos.ui.main.MainActivity
import com.example.duos.ui.main.appointment.AppointmentExistView
import com.example.duos.ui.main.chat.ChattingActivity
import com.example.duos.ui.main.chat.CreateChatRoomView
import com.example.duos.ui.main.mypage.myprofile.MyProfileActivity
import com.example.duos.ui.main.mypage.myprofile.frag.PartnerProfileChatDialog
import com.example.duos.ui.main.mypage.myprofile.frag.PartnerProfileChatUnavailableDialog
import com.example.duos.utils.getUserIdx

class DailyMatchingDetail : BaseActivity<ActivityDailyMatchingDetailBinding>(
    ActivityDailyMatchingDetailBinding::inflate
), GetDailyMatchingDetailView, GetDailyMatchingOptionView, DailyMatchingOptionListener,
    DailyMatchingEndView,
    DailyMatchingMessageView,
    DailyMatchingDeleteView, AppointmentExistView {
    var boardIdx: Int = -1
    var recruitmentStatus: String? = "finished"
    lateinit var dailyMatchingInfo: DailyMatchingDetail
    var optionSize: Int = 0
    var myUserIdx: Int = getUserIdx()!!
    var partnerUserIdx: Int = 0

    lateinit var chatDB: ChatDatabase

    private var todayChatAvaillCnt: Int = 0
    private var hasChatRoomAlready: Boolean = false
    private var createdNewChatRoom: Boolean = false
    private var targetChatRoomIdx: String = ""

    private var partnerImgUrl: String = ""
    private var partnerNickname: String = ""
    private var participantList: List<Int> = emptyList<Int>()

    private var appointmentIsExisted: Boolean = false
    private var appointmentIdx: Int = -1


    override fun onResume() {
        super.onResume()
        binding.dailyMatchingDetailContentImage01Iv.visibility = View.INVISIBLE
        binding.dailyMatchingDetailContentImage02Iv.visibility = View.INVISIBLE
        binding.dailyMatchingDetailContentImage03Iv.visibility = View.INVISIBLE
        DailyMatchingListService.getDailyMatchingDetail(this, boardIdx, getUserIdx()!!)
        DailyMatchingService.getDailyMatchingOption(this, boardIdx, getUserIdx()!!)
    }

    override fun initAfterBinding() {

        val intent = intent
        boardIdx = intent.getIntExtra("boardIdx", -1)
        recruitmentStatus = intent.getStringExtra("recruitmentStatus")

        if (boardIdx == -1) {
            showToast("???????????? ?????? ????????? ?????????.")
            finish()
        }

        binding.dailyMatchingDetailBackArrowIv.setOnClickListener {
            finish()
        }
        binding.dailyMatchingDetailOptionIconIv.setOnClickListener {
            when (optionSize) {
                1 -> {
                    val bottomSheet = BottomSheetDialog02()
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                }
                2 -> {
                    val bottomSheet = BottomSheetDialog03()
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                }
                3 -> {
                    val bottomSheet = BottomSheetDialog01(boardIdx)
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                }
            }
        }

        binding.dailyMatchingDetailBottomBtn.setOnClickListener {
            when (optionSize) {
                1 -> {
                    onClickChat()
                }
                2, 3 -> {
                    val intent = Intent(this, MainActivity::class.java);
                    intent.putExtra("FromDailyMatchingDetail", "1");
                    startActivity(intent)
                }
            }
        }

        binding.dailyMatchingDetailProfileImageIv.setOnClickListener {
            // ????????? ?????? ???????????? ??????
            val intent = Intent(this, MyProfileActivity::class.java)
            intent.apply {
                this.putExtra("isFromDailyMatching", true)
                this.putExtra("partnerUserIdx", partnerUserIdx)
            }
            startActivity(intent)
        }
    }

    override fun onGetDailyMatchingDetailSuccess(dailyMatchingDetail: DailyMatchingDetail) {
        dailyMatchingInfo = dailyMatchingDetail

        Log.d("??????", dailyMatchingInfo.toString())

        partnerUserIdx = dailyMatchingInfo.userIdx
        partnerNickname = dailyMatchingInfo.nickname
        partnerImgUrl = dailyMatchingInfo.profileUrl
        todayChatAvaillCnt = dailyMatchingInfo.remains
        hasChatRoomAlready = dailyMatchingInfo.alreadyExist

        Glide.with(binding.dailyMatchingDetailProfileImageIv.context)
            .load(dailyMatchingDetail.profileUrl)
            .into(binding.dailyMatchingDetailProfileImageIv)

        if (recruitmentStatus.equals("recruiting")) {
            DrawableCompat.setTint(
                binding.dailyMatchingDetailPreviewDateTv.getBackground(),
                ContextCompat.getColor(
                    this,
                    R.color.primary
                )
            )
            binding.dailyMatchingDetailPreviewDateTv.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.white
                )
            )
            DrawableCompat.setTint(
                binding.dailyMatchingDetailPreviewStateTv.getBackground(),
                ContextCompat.getColor(
                    this,
                    R.color.inch_worm
                )
            )
            binding.dailyMatchingDetailPreviewStateTv.setText(R.string.daily_matching_state_ongoing)
            binding.dailyMatchingDetailPreviewStateTv.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.white
                )
            )
        } else {
            DrawableCompat.setTint(
                binding.dailyMatchingDetailPreviewDateTv.getBackground(),
                ContextCompat.getColor(
                    this,
                    R.color.grainsboro_D
                )
            )
            binding.dailyMatchingDetailPreviewDateTv.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.grey
                )
            )
            DrawableCompat.setTint(
                binding.dailyMatchingDetailPreviewStateTv.getBackground(),
                ContextCompat.getColor(
                    this,
                    R.color.grainsboro_D
                )
            )
            binding.dailyMatchingDetailPreviewStateTv.setText(R.string.daily_matching_state_complete)
            binding.dailyMatchingDetailPreviewStateTv.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.grey
                )
            )
        }

        binding.dailyMatchingDetailTitleTv.text = dailyMatchingDetail.title
        binding.dailyMatchingDetailNicknameTv.text = dailyMatchingDetail.nickname
        binding.dailyMatchingDetailReviewCountTv.text = dailyMatchingDetail.review_Num.toString()
        binding.dailyMatchingDetailRatingTv.text = String.format("%.1f", dailyMatchingDetail.rating)
        binding.dailyMatchingPostPreviewLocationTv.text = dailyMatchingDetail.matchPlace
        binding.dailyMatchingDetailDateTv.text =
            dailyMatchingDetail.matchDate + " (" + dailyMatchingDetail.dayOfWeek + ")"
        binding.dailyMatchingDetailTimeTv.text =
            dailyMatchingDetail.startTime + " - " + dailyMatchingDetail.endTime
        binding.dailyMatchingDetailTimeCountTv.text = dailyMatchingDetail.duration.toString() + "??????"
        binding.dailyMatchingDetailContentTv.text = dailyMatchingDetail.content
        binding.dailyMatchingDetailContentTimeTv.text = dailyMatchingDetail.regBefore
        binding.dailyMatchingDetailSeeCountTv.text = dailyMatchingDetail.viewCount.toString()
        binding.dailyMatchingDetailChatCountTv.text = dailyMatchingDetail.messageCount.toString()
        binding.dailyMatchingDetailPreviewDateTv.text = dailyMatchingDetail.stringForMatchDateGap
        when (dailyMatchingDetail.urls.size) {
            1 -> {
                Log.d("????????????", "1")
                binding.dailyMatchingDetailContentImage01Iv.visibility = View.VISIBLE
                Glide.with(binding.dailyMatchingDetailContentImage01Iv)
                    .load(dailyMatchingDetail.urls[0])
                    .into(binding.dailyMatchingDetailContentImage01Iv)
            }
            2 -> {
                Log.d("????????????", "2")
                binding.dailyMatchingDetailContentImage02Iv.visibility = View.VISIBLE
                binding.dailyMatchingDetailContentImage01Iv.visibility = View.VISIBLE
                Glide.with(binding.dailyMatchingDetailContentImage01Iv)
                    .load(dailyMatchingDetail.urls[0])
                    .into(binding.dailyMatchingDetailContentImage01Iv)
                Glide.with(binding.dailyMatchingDetailContentImage02Iv)
                    .load(dailyMatchingDetail.urls[1])
                    .into(binding.dailyMatchingDetailContentImage02Iv)
            }
            3 -> {
                Log.d("????????????", "3")
                binding.dailyMatchingDetailContentImage03Iv.visibility = View.VISIBLE
                binding.dailyMatchingDetailContentImage02Iv.visibility = View.VISIBLE
                binding.dailyMatchingDetailContentImage01Iv.visibility = View.VISIBLE
                Glide.with(binding.dailyMatchingDetailContentImage01Iv)
                    .load(dailyMatchingDetail.urls[0])
                    .into(binding.dailyMatchingDetailContentImage01Iv)
                Glide.with(binding.dailyMatchingDetailContentImage02Iv)
                    .load(dailyMatchingDetail.urls[1])
                    .into(binding.dailyMatchingDetailContentImage02Iv)
                Glide.with(binding.dailyMatchingDetailContentImage03Iv)
                    .load(dailyMatchingDetail.urls[2])
                    .into(binding.dailyMatchingDetailContentImage03Iv)
            }
        }
    }

    override fun onGetDailyMatchingDetailFailure(code: Int, message: String) {
        showToast(message)
        finish()
    }

    override fun onGetDailyMatchingOptionSuccess(dailyMatchingOption: DailyMatchingOption) {
        optionSize = dailyMatchingOption.options.size
        if (optionSize == 3) {
            binding.dailyMatchingDetailBottomBtn.text =
                getString(R.string.daily_matching_detail_chatting_list)
        }
    }

    override fun onGetDailyMatchingOptionFailure(code: Int, message: String) {
        showToast(message)
    }

    override fun onClickEdit() {
        val intent = Intent(this, DailyMatchingEdit::class.java)
        intent.putExtra("boardIdx", boardIdx)
        intent.putExtra("dailyMatchingInfo", dailyMatchingInfo)
        startActivity(intent)
    }

    override fun onClickDelete() {
        DailyMatchingService.dailyMatchingDelete(this, boardIdx, getUserIdx()!!)
    }

    override fun onClickChat() {
//        val participantIdx = DailyMatchingMessageParticipantIdx(myUserIdx, partnerUserIdx)
//        Log.d("?????????????????????", "$boardIdx, ${getUserIdx()!!}, ${dailyMatchingInfo.userIdx}")
//        DailyMatchingService.dailyMatchingMessage(this, boardIdx, participantIdx)

        Log.d(TAG, "?????? ?????? ??????. : $hasChatRoomAlready")
        if (hasChatRoomAlready == true) {
            // ?????? ?????? ????????? ????????? goto ????????? (????????? ????????????)
            Log.d(TAG, "?????? ?????? ??????. : $hasChatRoomAlready")
            DailyMatchingService.dailyMatchingMessage(
                this,
                boardIdx,
                DailyMatchingMessageParticipantIdx(myUserIdx, partnerUserIdx)
            ) // ????????? ???????????? API ??????
        }
        // ????????? ?????? ?????? ?????? ?????? ??? -> ??????????????? ??? ??????????????? ?????? ?????? ??? ??????????????? ??????
        else if (hasChatRoomAlready == false && todayChatAvaillCnt > 0) {
            Log.d(TAG, "?????? ?????? ????????? ?????? ??????. : $hasChatRoomAlready")
            setCreateNewDialogSuccess(todayChatAvaillCnt)
        }
        // ????????? ?????? ?????? ?????? ?????? ??? -> ??????????????? , ,
        else if (hasChatRoomAlready == false && todayChatAvaillCnt == 0) {   // ?????? ??????????????????, ?????? ?????? ?????? ??????
            Log.d(TAG, "?????? ????????? But, ?????? ?????? ?????? ????????? ??????.")
            setCreateNewDialogFail()
            //                    Toast.makeText(context, "?????? ??? ??? ?????? ?????? ?????? ?????????????????????.", Toast.LENGTH_LONG).show()
        }


    }

    override fun onClickEnd() {
        DailyMatchingService.dailyMatchingEnd(this, boardIdx, getUserIdx()!!)
    }

    override fun onDailyMatchingEndSuccess() {
        showToast("????????? ?????????????????????.")
        finish()
    }

    override fun onDailyMatchingEndFailure(code: Int, message: String) {
        showToast(message)
    }

    override fun onDailyMatchingDeleteSuccess() {
        showToast("????????? ????????? ?????????????????????.")
        finish()
    }

    override fun onDailyMatchingDeleteFailure(code: Int, message: String) {
        showToast(message)
    }

    override fun onDailyMatchingMessageSuccess(dailyMatchingMessageResult: DailyMatchingMessageResult) {
        Log.d("result", dailyMatchingMessageResult.toString())
        createdNewChatRoom = dailyMatchingMessageResult.createdNewChatRoom
        todayChatAvaillCnt = dailyMatchingMessageResult.remains
        targetChatRoomIdx = dailyMatchingMessageResult.targetChatRoomIdx
        participantList = dailyMatchingMessageResult.participantList


        if (hasChatRoomAlready == true) {
            // ?????? ?????? ????????? ????????? goto ????????? (????????? ????????????)
            Log.d(TAG, "?????? ?????? ??????")
            Toast.makeText(this, "?????? ???????????? ?????? ?????? ??????????????? ???????????????.", Toast.LENGTH_SHORT).show()
            // chatDB ???????????? ?????? ?????? ????????? ????????????.
            chatDB = ChatDatabase.getInstance(this, ChatDatabase.provideGson())!!

            var lastChatRoom = chatDB.chatRoomDao().getChatRoom(targetChatRoomIdx)
            Log.d(TAG, "Room ???????????? Romm ??? ?????? ?????? ?????????????????? ???????????? $lastChatRoom")

            if (lastChatRoom == null) { /* DB??? ???????????? ?????? ????????? ?????? ????????? null ??? ??? */
                // ?????? DB??? ??????????????? ??? ?????? ?????? ?????? ????????????
                AppointmentService.isAppointmentExist(this, myUserIdx, partnerUserIdx)

                chatDB = ChatDatabase.getInstance(this, ChatDatabase.provideGson())!!
                lastChatRoom = ChatRoom(
                    targetChatRoomIdx, partnerNickname, partnerImgUrl, participantList[0],
                    "", "", appointmentIsExisted, appointmentIdx
                )
                chatDB.chatRoomDao().insert(lastChatRoom)
                Log.d(TAG, "????????? ????????? RoomDB??? ?????? : $lastChatRoom")

                lastChatRoom = chatDB.chatRoomDao().getChatRoom(targetChatRoomIdx)
            }

            val intent = Intent(this, ChattingActivity::class.java)
            intent.apply {      /* ChatActivity ??? intent ?????? ????????????*/
                putExtra("isFromPlayerProfile", true)         // ????????? ??????????????? ???
                putExtra("createdNewChatRoom", createdNewChatRoom)  // ?????? ????????? ??????????????? : Boolean
                putExtra("targetChatRoomIdx", targetChatRoomIdx)    // ?????????Idx
                putExtra("partnerIdx", participantList[0])          // ?????? ?????? Player Idx
                putExtra("partnerNickName", partnerNickname)        // ????????? ??????
                putExtra("isAppointmentExist", lastChatRoom.isAppointmentExist)     // ?????? ??????
                putExtra("appointmentIdx", lastChatRoom.appointmentIdx)             // ?????? ?????????
            }
            Log.d(TAG, "Intent??? ??? ??????")
            startActivity(intent)
        }

        // ????????? ?????? ?????? ?????? ?????? ??? -> ??????????????? ??? ??????????????? ?????? ?????? ??? ??????????????? ??????
        else if (hasChatRoomAlready == false && todayChatAvaillCnt > 0) {
            Log.d(TAG, "?????? ?????? ????????? ?????? ??????")
            setCreateNewDialogSuccess(todayChatAvaillCnt)
        }
    }

    override fun onDailyMatchingMessageFailure(code: Int, message: String) {
        when (code) {
            6401 -> {
                setCreateNewDialogFail()
            }
            2014, 5021 -> {
                showToast("???????????? ?????? ?????? ??????????????????.")
            }
            2201 -> {
                showToast("???????????? ?????? ??????????????????")
            }

        }
    }


    private fun setCreateNewDialogSuccess(count: Int) {
        DailyMatchingChatDialog.Builder(this)
            .setCount(count)
            .setRightButton(object :
                DailyMatchingChatDialog.DailyMatchingChatDialogCallbackRight {
                override fun onClick(dialog: DailyMatchingChatDialog) {
                    Log.d(TAG, "???????????? ??????")
                    DailyMatchingService.dailyMatchingMessage(
                        this@DailyMatchingDetail,
                        myUserIdx,
                        DailyMatchingMessageParticipantIdx(myUserIdx, partnerUserIdx)
                    ) // ????????? ?????? OR ???????????? API ??????
                    dialog.dismiss()
                }

                override fun onDailyMatchingMessageSuccess(dailyMatchingMessageResult: DailyMatchingMessageResult) {
                    Log.d(TAG, "????????? ?????? ??? ???????????? API ?????? ??????")
                    createdNewChatRoom =
                        dailyMatchingMessageResult.createdNewChatRoom //?????? ????????????????
                    targetChatRoomIdx =
                        dailyMatchingMessageResult.targetChatRoomIdx  // ????????? ????????? ?????????
                    participantList =
                        dailyMatchingMessageResult.participantList      // ???????????? ????????? ????????? (??? ?????????. ????????? [1]??? ??????.

                    Log.d(
                        TAG, "?????? ????????? ??????, ?????? ?????? ?????? ????????? ?????? : ?????? ????????? ???? : $hasChatRoomAlready " +
                                "?????? ?????? ?????? ?????? : $todayChatAvaillCnt"
                    )
                    createRoom()
                    val intent = Intent(this@DailyMatchingDetail, ChattingActivity::class.java)
                    intent.apply {
                        putExtra("isFromPlayerProfile", true)         //????????? ??????????????? ???
                        putExtra(
                            "createdNewChatRoom",
                            createdNewChatRoom
                        )  //?????? ????????? ???????????????? : Boolean
                        putExtra("targetChatRoomIdx", targetChatRoomIdx)    //?????????Idx
                        putExtra("partnerNickName", partnerNickname)        //????????? ??????
                        putExtra("partnerIdx", participantList[1])          //?????? ?????? Player Idx
                        putExtra("isAppointmentExist", false)   //??? ????????? ??????????????? ?????? ??????
                        putExtra("appointmentIdx", -1)          // ??? ????????? ??????????????? ?????? ??????
                    }
                    Log.d(
                        TAG, "Intent??? ????????? ??? ?????? ?????? ?????? ???? createdNewChatRoom : $createdNewChatRoom," +
                                "targetChatRoomIdx : $targetChatRoomIdx, ????????? : ${participantList[1]}"
                    )
                    startActivity(intent)
                }

                override fun onDailyMatchingMessageFailure(code: Int, message: String) {
                    showToast("???????????? ?????? ?????? ??? ?????? ??????????????????.")
                }

            })
            .setLeftButton(object : DailyMatchingChatDialog.DailyMatchingChatDialogCallbackLeft {
                override fun onClick(dialog: DailyMatchingChatDialog) {
                    dialog.dismiss()
                    Log.d(TAG, "?????? ????????? ??????")

                }

            })
            .show()
    }

    private fun setCreateNewDialogFail() {
        PartnerProfileChatUnavailableDialog.Builder(this)
            .setLeftButton(object :
                PartnerProfileChatUnavailableDialog.PartnerProfileChatDialogCallbackLeft {
                override fun onClick(dialog: PartnerProfileChatUnavailableDialog) {
                    dialog.dismiss()
                    Log.d(TAG, "?????? ?????? ????????? ????????? ??????")
                }
            })
            .show()
    }

    // ??? ????????? ???????????? ?????? : ?????? ?????? ???????????? ???????????? ????????? ?????? ?????? INSERT ??????.//////////////////////////////////
    fun createRoom() {
        Log.d(
            TAG,
            "????????? ????????? user??? userIdx : $myUserIdx , ????????? ?????? : ?????? user??? userIdx : $partnerUserIdx"
        )
        chatDB = ChatDatabase.getInstance(this, ChatDatabase.provideGson())!!

        val chatRoom: ChatRoom? = chatDB.chatRoomDao().getChatRoom(targetChatRoomIdx)
        if (chatRoom != null) {
            chatDB.chatRoomDao().delete(chatRoom)
        }

        val newChatRoom = ChatRoom(
            targetChatRoomIdx, partnerNickname, partnerImgUrl, participantList[0],
            "????????? ????????????", "???????????? ?????? null", false, -1
        )
        chatDB.chatRoomDao().insert(newChatRoom)
        Log.d(TAG, "??? ????????? ????????? RoomDB??? ?????? : $newChatRoom")
    }

    override fun onAppointmentExistSuccess(isExisted: Boolean, appointmentIdx: Int) {
        this.appointmentIsExisted = isExisted
        this.appointmentIdx = appointmentIdx
    }

    override fun onAppointmentExistFailure(code: Int, message: String) {
        Toast.makeText(this, code, Toast.LENGTH_LONG).show()
        Log.d(TAG, "CODE : $code , MESSAGE : $message ")
    }
}
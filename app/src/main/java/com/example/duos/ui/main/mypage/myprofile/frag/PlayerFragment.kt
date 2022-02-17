package com.example.duos.ui.main.mypage.myprofile.frag

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.duos.R
import com.example.duos.data.entities.PartnerResDto
import com.example.duos.data.entities.ReviewResDto
import com.example.duos.data.entities.StarredFriend
import com.example.duos.data.entities.chat.ChatRoom
import com.example.duos.data.local.ChatDatabase
import com.example.duos.data.remote.chat.chat.ChatService
import com.example.duos.data.remote.chat.chat.CreateChatRoomResultData
import com.example.duos.data.remote.myFriendList.FriendListService
import com.example.duos.data.remote.partnerProfile.PartnerProfileService
import com.example.duos.databinding.FragmentPlayerBinding
import com.example.duos.ui.BaseFragment
import com.example.duos.ui.main.chat.ChattingActivity
import com.example.duos.ui.main.chat.CreateChatRoomView
import com.example.duos.ui.main.friendList.AddStarredFriendView
import com.example.duos.ui.main.friendList.DeleteStarredFriendView
import com.example.duos.ui.main.mypage.myprofile.MyProfileActivity
import com.example.duos.ui.main.mypage.myprofile.PartnerProfileListView
import com.example.duos.ui.main.mypage.myprofile.PartnerProfileReviewRVAdapter
import com.example.duos.utils.getUserIdx
import com.google.gson.Gson
import androidx.fragment.app.FragmentManager


// 내 프로필 , 모든 리뷰 보기, 지난 약속 보기, 파트너 서치, 채팅방에서 넘어올 수 있어
class PlayerFragment : BaseFragment<FragmentPlayerBinding>(FragmentPlayerBinding::inflate),
    PartnerProfileListView, AddStarredFriendView, DeleteStarredFriendView, CreateChatRoomView {
    val TAG: String = "PlayerFragment"
    lateinit var chatDB: ChatDatabase

    private var partnerProfileReviewDatas = ArrayList<ReviewResDto>()
    private var friendListDatas = ArrayList<StarredFriend>()
    val myUserIdx = getUserIdx()!!
    private var isStarred: Boolean = false
    var partnerUserIdx: Int = 0
    var todayChatAvaillCnt: Int = 0
    var hasChatRoomAlready: Boolean = false

    var createdNewChatRoom: Boolean = false
    var targetChatRoomIdx: String = ""
    var participantList: List<Int> = emptyList<Int>()

    var partnerImgUrl: String = ""
    var partnerNickname: String = ""

    override fun initAfterBinding() {
        Log.d(TAG, "Start_PlayerFragment")

        partnerUserIdx = requireArguments().getInt("partnerUserIdx")  /* From MyProfile OR PlayerProfile? thisIdx*/

        PartnerProfileService.partnerProfileInfo(this, myUserIdx, partnerUserIdx)


        Log.d(TAG, "Create Retrofit")


        (context as MyProfileActivity).findViewById<ConstraintLayout>(R.id.profile_bottom_chat_btn_cl).visibility =
            View.VISIBLE
        (context as MyProfileActivity).findViewById<TextView>(R.id.edit_myProfile_tv).visibility = View.GONE
        (context as MyProfileActivity).findViewById<TextView>(R.id.top_myProfile_tv).text = "프로필"

        (context as MyProfileActivity).findViewById<ImageView>(R.id.player_is_starred_iv).setOnClickListener {
            deleteStarredFriend()   /* 친구 찜 취소 */
        }
        (context as MyProfileActivity).findViewById<ImageView>(R.id.player_is_not_starred_iv)
            .setOnClickListener {
                addStarredFriend()  /* 친구 찜하기 */
            }

// TODO 채팅하기 버튼 클릭//////////////////////////////////////////////////////////////////
        (context as MyProfileActivity).findViewById<AppCompatButton>(R.id.partner_profile_chatting_btn).setOnClickListener {
            Log.d("채팅", "이미 채팅 있다. : $hasChatRoomAlready")
            if (hasChatRoomAlready == true) {
                // TODO 만약 이미 채팅을 했으면 goto 채팅방 (토스트 메세지만)
                Log.d("채팅", "이미 채팅 있다. : $hasChatRoomAlready")
                ChatService.createChatRoom(this, myUserIdx, partnerUserIdx) // 채팅방 불러오기 API 호출
            }
            // TODO 채팅방 없고 채팅 가능 횟수 있 -> 다이얼로그 이 다이얼로그 채팅 선택 시 채팅방으로 이동
            else if (hasChatRoomAlready == false && todayChatAvaillCnt > 0) {
                Log.d("채팅", "채팅 없고 채팅방 생성 가능. : $hasChatRoomAlready")
                setCreateNewDialogSuccess(todayChatAvaillCnt)
            }
            //TODO 채팅방 없고 채팅 가능 횟수 없 -> 다이얼로그 , API 호출 없음(오늘 채팅 가능한 횟수가 모두 소진되었습니다.
            else if (hasChatRoomAlready == false && todayChatAvaillCnt == 0) {   //TODO 신규 채팅방이지만, 오늘 가능 횟수 없음
                Log.d("채팅", "신규 채팅방 But, 채팅 가능 횟수 없음의 경우.")
                setCreateNewDialogFail()
//                    Toast.makeText(context, "오늘 할 수 있는 채팅 수를 초과하였습니다.", Toast.LENGTH_LONG).show()
            }
        }

        // 상단 뒤로 가기 버튼 클릭
        val fragmentTransaction: FragmentManager = requireActivity().supportFragmentManager
        (context as MyProfileActivity).findViewById<ImageView>(R.id.top_left_arrow_iv).setOnClickListener {

            if (fragmentTransaction.backStackEntryCount >= 1) {   /* 백 스택 있으면 pop */
                fragmentTransaction.popBackStack()
            } else {  /* 없으면 finish() */
                requireActivity().finish()
            }
        }


    }


    private fun deleteStarredFriend() {
        FriendListService.deleteStarredFriend(this, myUserIdx, partnerUserIdx)
        initIsNotStarred()
    }

    private fun addStarredFriend() {
        FriendListService.addStarredFriend(this, myUserIdx, partnerUserIdx)
        initIsStarred()
    }

    @SuppressLint("CutPasteId")
    private fun initIsStarred() {
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_starred_iv).visibility =
            View.VISIBLE
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_starred_iv).isEnabled = true
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_not_starred_iv).visibility =
            View.INVISIBLE
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_not_starred_iv).isEnabled = false
    }

    @SuppressLint("CutPasteId")
    private fun initIsNotStarred() {
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_starred_iv).visibility =
            View.INVISIBLE
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_starred_iv).isEnabled = false
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_not_starred_iv).visibility =
            View.VISIBLE
        (context as MyProfileActivity).findViewById<ImageButton>(R.id.player_is_not_starred_iv).isEnabled = true
    }

    override fun onGetProfileInfoSuccess(partnerResDto: PartnerResDto) {
        setPartnerProfileInfo(partnerResDto) // 위쪽 프로필 데이터 설정
        setExperienceView()     // 구력 관련 글씨체 적용

        partnerProfileReviewDatas.clear()
        partnerProfileReviewDatas.addAll(partnerResDto.reviewResDto)   // API 로 받아온 데이터 다 넣어주기 (더미데이터 넣듯이)

        //TODO : 채팅 가능 횟수, 이미 채팅방 있?
        todayChatAvaillCnt = partnerResDto.todayChatAvailCnt    // 전역 변수에 오늘 채팅 가능 남은 횟수
        hasChatRoomAlready = partnerResDto.hasChatRoomAlready   // 이미 채팅방이 생성되어있는지 세기

        Log.d("채팅", partnerResDto.toString())
        // 리사이클러뷰 어댑터 연결, 데이터 연결, 레이아웃 매니저 설정
        val profileReviewRVAdapter = initRecyclerView() // 리뷰 불러오기
        /* 다른 회원 프로필로 이동 */
        goPlayerProfile(profileReviewRVAdapter)
        /* 해당 회원의 모든 후기 보기 페이지로 이동*/
        goEveryReview(partnerResDto)
    }

    override fun onGetProfileInfoFailure(code: Int, message: String) {
        Toast.makeText(context, "네트워크 상태 확인 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
        Log.d(TAG, "CODE : $code , MESSAGE : $message ")
    }

    private fun initRecyclerView(): PartnerProfileReviewRVAdapter {
        val profileReviewRVAdapter = PartnerProfileReviewRVAdapter(partnerProfileReviewDatas)
        binding.playingReviewContentRv.adapter = profileReviewRVAdapter
        binding.playingReviewContentRv.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        return profileReviewRVAdapter
    }

    /* 다른 회원 혹은 내 프로필로 이동*/
    private fun goPlayerProfile(profileReviewRVAdapter: PartnerProfileReviewRVAdapter) {
        profileReviewRVAdapter.clickPlayerReviewListener(object :
            PartnerProfileReviewRVAdapter.PlayerReviewItemClickListener {
            override fun onItemClick(partnerProfileReviewItem: ReviewResDto) {

                // TO other PlayerProfile (PlayerFragment)
                if (partnerProfileReviewItem.writerIdx != getUserIdx()) {
                    val fragmentTransaction: FragmentTransaction =
                        (context as MyProfileActivity).supportFragmentManager.beginTransaction()
                            .replace(com.example.duos.R.id.my_profile_into_fragment_container_fc, PlayerFragment().apply {
                                Log.d(TAG, "다른 회원 프로필에서 다른 회원 프로필로 이동")
                                arguments =
                                    Bundle().apply {/*TODO : 후기를 작성한 writerIdx에 맞게 Fragment 이동 시 해당 Idx를 가진 회원의 프로필로 이동 그 Idx만 전달해도될 듯???*/
                                        putInt("partnerUserIdx", partnerProfileReviewItem.writerIdx!!)
                                    }
                            })
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()    // commit() : FragmentManager가 이미 상태를 저장하지는 않았는지를 검사 이미 상태를 저장한 경우 IllegalStateExceptoion이라는 예외 던짐

                } else { /* TO MyProfile (MyProfileFrag)  */
                    val fragmentTransaction: FragmentTransaction =
                        (context as MyProfileActivity).supportFragmentManager.beginTransaction()
                            .replace(com.example.duos.R.id.my_profile_into_fragment_container_fc, MyProfileFragment())
                    Log.d(TAG, "PlayerFrag -> MyProfileFrag")

                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                }

            }
        })
    }

    /* 해당 회원의 모든 후기 보기 페이지로 이동*/
    @SuppressLint("SetTextI18n")
    private fun goEveryReview(partnerResDto: PartnerResDto) {
        binding.playerPlayingReviewCountTv.setOnClickListener {
            val fragmentTransaction: FragmentTransaction =
                (context as MyProfileActivity).supportFragmentManager.beginTransaction()
                    .replace(com.example.duos.R.id.my_profile_into_fragment_container_fc, EveryReviewFragment().apply {
                        arguments = Bundle().apply {
                            // 해당 회원의 프로필 정보를 gson.toJson 하여 보낸다.
                            val gson = Gson()
                            val profileJson = gson.toJson(partnerResDto.partnerInfoDto)
                            putString("profile", profileJson)
                            putBoolean("isFromPlayerProfile", true)
                        }
                    })
            fragmentTransaction.addToBackStack(null)// 해당 transaction 을 BackStack에 저장
            fragmentTransaction.commit()    // commit(): FragmentManager가 이미 상태를 저장하지는 않았는지를 검사. 이미 상태를 저장한 경우, IllegalStateException 예외 던짐.

            // 상단 텍스트 변경 (후기 갯수, 프로필 수정하기 버튼 없애기
            (context as MyProfileActivity).findViewById<TextView>(R.id.top_myProfile_tv).text =
                "후기(${partnerResDto.partnerInfoDto.reviewCount.toString()})"
            (context as MyProfileActivity).findViewById<TextView>(R.id.edit_myProfile_tv).visibility =
                View.GONE
            (context as MyProfileActivity).findViewById<ConstraintLayout>(R.id.profile_bottom_chat_btn_cl).visibility =
                View.GONE

        }
    }

    @SuppressLint("SetTextI18n")
    private fun setPartnerProfileInfo(partnerResDto: PartnerResDto) {

        partnerImgUrl = partnerResDto.partnerInfoDto.profilePhotoUrl
        partnerNickname = partnerResDto.partnerInfoDto.nickname

        val profileGenderStr = toGenderStr(partnerResDto.partnerInfoDto.gender)
        val locationName = partnerResDto.partnerInfoDto.locationName
        val locationCategory = partnerResDto.partnerInfoDto.locationCategory
        val location = locationCategory + locationName

        binding.playerNicknameTv.text = partnerNickname
        binding.playerGenerationTv.text = partnerResDto.partnerInfoDto.age
        binding.playerSexTv.text = profileGenderStr
        binding.profileLocationTv.text = location
        binding.partnerProfileGradeRb.rating = partnerResDto.partnerInfoDto.rating!!
        binding.partnerProfileGradeNumTv.text = partnerResDto.partnerInfoDto.rating.toString()
        binding.playerCareerYearNumTv.text = partnerResDto.partnerInfoDto.experience
        Glide.with(binding.playerProfileImgIv.context)
            .load(partnerImgUrl)
            .into(binding.playerProfileImgIv)
        binding.playerIntroductionTv.text = partnerResDto.partnerInfoDto.introduction
        binding.playerCareerPlayedNumTv.text = partnerResDto.partnerInfoDto.gameCount.toString()
        binding.playerPlayingReviewCountTv.text =
            "후기(${partnerResDto.partnerInfoDto.reviewCount.toString()})"

        isStarred = partnerResDto.partnerInfoDto.starred
        if (isStarred) {
            initIsStarred()
        } else {
            initIsNotStarred()
        }

    }

    private fun setExperienceView() {
        val textExperience = binding.playerCareerYearNumTv
        // String 문자열 데이터 취득
        val textExperienceData: String = textExperience.text.toString()
        // SpannableStringBuilder 타입으로 변환
        val textExperienceBuilder = SpannableStringBuilder(textExperienceData)
        // index=0 에 해당하는 문자열(0)에 볼드체, 크기 적용
        val boldSpanEx = StyleSpan(Typeface.BOLD)
        textExperienceBuilder.setSpan(boldSpanEx, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val sizeBigSpanEx = RelativeSizeSpan(1.56f)
        textExperienceBuilder.setSpan(sizeBigSpanEx, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // TextView에 적용
        textExperience.text = textExperienceBuilder
    }

    override fun onAddStarredFriendSuccess() {
        Log.d(TAG, "친구 찜하기 성공")
    }

    override fun onAddStarredFriendFailure(code: Int, message: String) {
        Toast.makeText(context, "네트워크 상태 확인 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
    }

    override fun onDeleteStarredFriendSuccess() {
        Log.d(TAG, "친구 삭제 성공")
    }

    override fun onDeleteStarredFriendFailure(code: Int, message: String) {
        Toast.makeText(context, "네트워크 상태 확인 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
    }

    //TODO  새 채팅방 생성하는 함수 : 만약 해당 회원과의 채팅방이 없으면 새로 룸에 INSERT 한다.//////////////////////////////////
    fun createRoom() {
        // val chatRoom = ChatRoom(thisUserIdx, targetUserIdx)
        Log.d("채팅방 생성한 user의 userIdx", myUserIdx.toString())
        Log.d("채팅방 생성: 상대 user의 userIdx", partnerUserIdx.toString())
        chatDB = ChatDatabase.getInstance(requireContext(), ChatDatabase.provideGson())!!
        val newChatRoom =
            ChatRoom(
                targetChatRoomIdx, partnerNickname, partnerImgUrl, participantList[0],
                "채팅을 해보세요", "오늘날짜 아님 null", false, -1
            )
        chatDB.chatRoomDao().insert(newChatRoom)
        Log.d("채팅", "새 채팅방 정보를 RoomDB에 넣기 : $newChatRoom")

    }

    //TODO 로딩 관련해서 오류가 나서 주석처리함 ////////////////////////////////////////////////////////////////////
//    fun startLoadingProgress() {
//        Log.d("로딩중", "채팅방 생성 api")
//        Handler(Looper.getMainLooper()).postDelayed(Runnable { progressOFF() }, 3500)
//    }

//    override fun onCreateChatRoomLoading() {
//        startLoadingProgress()
//    }


    // TODO  채팅방이 이미 있을 때 여기로 옴
    override fun onCreateChatRoomSuccess(createChatRoomResultData: CreateChatRoomResultData) {
        createdNewChatRoom = createChatRoomResultData.createdNewChatRoom //TODO 새로 생성했는가?
        targetChatRoomIdx = createChatRoomResultData.targetChatRoomIdx  // TODO 생성한 채팅방 인덱스
        participantList = createChatRoomResultData.participantList      // TODO 채팅방에 추가된 사람들 (나 포함임. 리스트 [1]이 나임.

        Log.d("채팅", "채팅방 생성 및 이동하기 API 호출 성공, 이미 있는 채팅방의 경우")
        Log.d(
            "채팅", "채팅방 새거임? createdNewChatRoom : $createdNewChatRoom," +
                    " 채팅방 Idx targetChatRoomIdx : $targetChatRoomIdx ," + " 채팅방 참여자 participantList : $participantList"
        )

        Toast.makeText(context, "이미 채팅방이 있어 해당 채팅방으로 이동합니다.", Toast.LENGTH_SHORT).show()
        chatDB = ChatDatabase.getInstance(requireContext(), ChatDatabase.provideGson())!!
        val lastChatRoom = chatDB.chatRoomDao().getChatRoom(targetChatRoomIdx)
        Log.d("채팅", "Room 불러와서 Romm 에 원래 있던 채팅방이니까 불러오기 $lastChatRoom")

        val intent = Intent(activity, ChattingActivity::class.java)
        intent.apply {      /* ChatActivity 로 intent 정보 넘겨주기*/
            putExtra("isFromPlayerProfile", true)         // 파트너 프로필에서 옴
            putExtra("createdNewChatRoom", createdNewChatRoom)  // 새로 생성된 채팅방인가? : Boolean
            putExtra("targetChatRoomIdx", targetChatRoomIdx)    // 채팅룸Idx
            putExtra("partnerIdx", participantList[1])          // 채팅 상대 Player Idx
            putExtra("partnerNickName", partnerNickname)        // 파트너 이름
            putExtra("isAppointmentExist", lastChatRoom.isAppointmentExist)     // 약속 유무
            putExtra("appointmentIdx", lastChatRoom.appointmentIdx)             // 약속 인덱스
        }
        Log.d("채팅", "Intent에 값 넣기")
        startActivity(intent)

//        Log.d(
//            "채팅", "채팅방 새거임? createdNewChatRoom : $createdNewChatRoom," +
//                    " 채팅방 Idx targetChatRoomIdx : $targetChatRoomIdx ," +
//                    " 채팅방 참여자 participantList : $participantList")

//        if (hasChatRoomAlready == false && todayChatAvaillCnt > 0) {    //TODO 신규 채팅방,  오늘 가능 횟수가 남아있다면?
//            Log.d("채팅" , "신규 채팅방 생성, 채팅 가능 횟수 있음의 경우.")
//            Toast.makeText(context, "원래 채팅방 있? : $hasChatRoomAlready," +
//                    " 남은 채팅 가능 횟수 : $todayChatAvaillCnt", Toast.LENGTH_LONG).show()
//            createRoom()    // TODO 새 채팅방 RoomDB에 업데이트
//            val intent = Intent(activity, ChattingActivity::class.java)
//            intent.apply {      // TODO ChatActivity 로 intent 정보 넘겨주기
//                putExtra("isFromPlayerProfile", true)         //TODO 파트너 프로필에서 옴
//                putExtra("createdNewChatRoom", createdNewChatRoom)  //TODO 새로 생성된 채팅방인가? : Boolean
//                putExtra("targetChatRoomIdx", targetChatRoomIdx)    //TODO 채팅룸Idx
//                putExtra("partnerNickName", partnerNickname)        //TODO 파트너 이름u
//                putExtra("partnerIdx", participantList[0])          //TODO 채팅 상대 Player Idx
//                putExtra("isAppointmentExist", false)   //TODO 첫 채팅방 생성이므로 약속 없음
//                putExtra("appointmentIdx", -1)          // TODO 첫 채팅방 생성이므로 약속 없음
//            }
//            Log.d("채팅", "Intent에 전달할 값 넣기 새로 생성 됨? createdNewChatRoom : $createdNewChatRoom," +
//                    "targetChatRoomIdx : $targetChatRoomIdx, 참여자 : ${participantList[0]}")
//            startActivity(intent)
//
//        }
//        else if (hasChatRoomAlready == false && todayChatAvaillCnt == 0) {   //TODO 신규 채팅방이지만, 오늘 가능 횟수 없음
//            Log.d("채팅" , "신규 채팅방 But, 채팅 가능 횟수 없음의 경우.")
//            Toast.makeText(context, "오늘 할 수 있는 채팅 수를 초과하였습니다.", Toast.LENGTH_LONG).show()
//
//
//
//        }
//        else if (hasChatRoomAlready == true) {                                //TODO 이미 있는 채팅방임
//            Log.d("채팅", "이미 있는 채팅방의 경우")
//            Toast.makeText(context, "이미 채팅방이 있어 해당 채팅방으로 이동합니다.", Toast.LENGTH_SHORT).show()
//            chatDB = ChatDatabase.getInstance(requireContext(), ChatDatabase.provideGson())!!
//            val lastChatRoom = chatDB.chatRoomDao().getChatRoom(targetChatRoomIdx)
//            Log.d("채팅", "Room 불러와서 Romm 에 원래 있던 채팅방이니까 불러오기 $lastChatRoom")
//            val intent = Intent(activity, ChattingActivity::class.java)
//            intent.apply {      /* ChatActivity 로 intent 정보 넘겨주기*/
//                putExtra("isFromPlayerProfile", true)         // 파트너 프로필에서 옴
//                putExtra("createdNewChatRoom", createdNewChatRoom)  // 새로 생성된 채팅방인가? : Boolean
//                putExtra("targetChatRoomIdx", targetChatRoomIdx)    // 채팅룸Idx
//                putExtra("partnerNickName", partnerNickname)        // 파트너 이름
//                putExtra("partnerIdx", participantList[0])          // 채팅 상대 Player Idx
//                putExtra("isAppointmentExist", lastChatRoom.isAppointmentExist)     // 약속 유무
//                putExtra("appointmentIdx", lastChatRoom.appointmentIdx)             // 약속 인덱스
//            }
//            Log.d("채팅", "Intent에 값 넣기")
//            startActivity(intent)
//
//        }

    }

    override fun onCreateChatRoomFailure(code: Int, message: String) {
        Toast.makeText(activity, "네트워크 상태 확인 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
        Toast.makeText(activity, "code: $code, message: $message", Toast.LENGTH_LONG).show()
    }

    private fun setCreateNewDialogSuccess(count: Int) {
        PartnerProfileChatDialog.Builder(requireContext())
            .setCount(count)
            .setRightButton(object : PartnerProfileChatDialog.PartnerProfileChatDialogCallbackRight {
                override fun onClick(dialog: PartnerProfileChatDialog) {
                    Log.d("채팅", "채팅하기 선택")
                    ChatService.createChatRoomInDialog(this, myUserIdx, partnerUserIdx) // 채팅방 생성 OR 불러오기 API 호출
                    dialog.dismiss()
                }

                override fun onCreateChatRoomSuccess(createChatRoomResultData: CreateChatRoomResultData) {
                    Log.d("채팅", "채팅방 생성 및 이동하기 API 호출 성공")
                    createdNewChatRoom = createChatRoomResultData.createdNewChatRoom //TODO 새로 생성했는가?
                    targetChatRoomIdx = createChatRoomResultData.targetChatRoomIdx  // TODO 생성한 채팅방 인덱스
                    participantList = createChatRoomResultData.participantList      // TODO 채팅방에 추가된 사람들 (나 포함임. 리스트 [1]이 나임.

                    Log.d(
                        "채팅", "신규 채팅방 생성, 채팅 가능 횟수 있음의 경우 : 원래 채팅방 있? : $hasChatRoomAlready " +
                                "남은 채팅 가능 횟수 : $todayChatAvaillCnt"
                    )
                    createRoom()    // TODO 새 채팅방 RoomDB에 업데이트
                    val intent = Intent(activity, ChattingActivity::class.java)
                    intent.apply {      // TODO ChatActivity 로 intent 정보 넘겨주기
                        putExtra("isFromPlayerProfile", true)         //TODO 파트너 프로필에서 옴
                        putExtra("createdNewChatRoom", createdNewChatRoom)  //TODO 새로 생성된 채팅방인가? : Boolean
                        putExtra("targetChatRoomIdx", targetChatRoomIdx)    //TODO 채팅룸Idx
                        putExtra("partnerNickName", partnerNickname)        //TODO 파트너 이름u
                        putExtra("partnerIdx", participantList[1])          //TODO 채팅 상대 Player Idx
                        putExtra("isAppointmentExist", false)   //TODO 첫 채팅방 생성이므로 약속 없음
                        putExtra("appointmentIdx", -1)          // TODO 첫 채팅방 생성이므로 약속 없음
                    }
                    Log.d(
                        "채팅", "Intent에 전달할 값 넣기 새로 생성 됨? createdNewChatRoom : $createdNewChatRoom," +
                                "targetChatRoomIdx : $targetChatRoomIdx, 참여자 : ${participantList[1]}"
                    )
                    startActivity(intent)
                }

                override fun onCreateChatRoomFailure(code: Int, message: String) {
                    showToast("네트워크 상태 확인 후 다시 시도해주세요.")
                    //Toast.makeText(activity, "code: $code, message: $message", Toast.LENGTH_LONG).show()
                }

            })
            .setLeftButton(object : PartnerProfileChatDialog.PartnerProfileChatDialogCallbackLeft {
                override fun onClick(dialog: PartnerProfileChatDialog) {
                    dialog.dismiss()
                    Log.d("채팅", "취소 버튼을 누름")

                }

            })
            .show()
    }

    private fun setCreateNewDialogFail() {
        PartnerProfileChatDialog.Builder(requireContext())
            .setLeftButton(object : PartnerProfileChatDialog.PartnerProfileChatDialogCallbackLeft {
                override fun onClick(dialog: PartnerProfileChatDialog) {
                    dialog.dismiss()
                    Log.d("채팅", "남은 횟수 없어서 못해요 당신")
                }
            })
            .setFailView()
            .show()


    }


}








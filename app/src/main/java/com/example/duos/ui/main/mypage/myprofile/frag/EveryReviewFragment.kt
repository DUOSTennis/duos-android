package com.example.duos.ui.main.mypage.myprofile.frag

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.duos.R
import com.example.duos.data.entities.everyReviews.EveryReviewsItem
import com.example.duos.data.entities.MyProfileInfoItem
import com.example.duos.data.entities.PartnerInfoDto
import com.example.duos.data.remote.everyReviews.EveryReviewsResponse
import com.example.duos.data.remote.everyReviews.EveryReviewsService
import com.example.duos.databinding.FragmentEveryReviewBinding
import com.example.duos.ui.BaseFragment
import com.example.duos.ui.adapter.EveryReviewRVAdapter
import com.example.duos.data.entities.everyReviews.EveryReviewsItemView
import com.example.duos.data.local.UserDatabase
import com.example.duos.ui.main.mypage.myprofile.MyProfileActivity
import com.example.duos.utils.getUserIdx
import com.google.gson.Gson

class EveryReviewFragment :
    BaseFragment<FragmentEveryReviewBinding>(FragmentEveryReviewBinding::inflate),
    EveryReviewsItemView {

    val TAG: String = "EveryReviewFragment"
    private var gson: Gson = Gson()
    private var everyReviewDatas = ArrayList<EveryReviewsItem>()
    val myUserIdx = getUserIdx()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
    }

    override fun initAfterBinding() {

        (context as MyProfileActivity).findViewById<ConstraintLayout>(R.id.profile_bottom_chat_btn_cl).visibility =
            View.GONE

        val isFromMyProfile = arguments?.getBoolean("isFromMyProfile", false)
        val isFromPlayerProfile = arguments?.getBoolean("isFromPlayerProfile", false)
        val profileData =
            arguments?.getString("profile")   /* From MyProfileFrg Or From PlayerFrag*/

        if (isFromPlayerProfile == true) {
            // ?????? ?????? ???????????? ?????? ?????? ?????? ?????? ?????? ????????? ????????? ????????????
            val profile = gson.fromJson(profileData, PartnerInfoDto::class.java)
            setInit(profile)
            EveryReviewsService.getEveryReviews(this, myUserIdx, profile.userIdx!!)
        } else if (isFromMyProfile == true) {
            // ??? ???????????? ?????? ?????? ??????  ??? ????????? ????????? ????????????
            val profile = gson.fromJson(profileData, MyProfileInfoItem::class.java)
            setInit(profile)
            EveryReviewsService.getEveryReviews(this, myUserIdx, profile.userIdx!!)
        }

        // ?????? ?????? ?????? ?????? ??????
        val fragmentTransaction: FragmentManager = requireActivity().supportFragmentManager
        (context as MyProfileActivity).findViewById<ImageView>(R.id.top_left_arrow_iv)
            .setOnClickListener {
                if (fragmentTransaction.backStackEntryCount >= 1) {
                    fragmentTransaction.popBackStack()
                } else {
                    requireActivity().finish()
                }
            }

    }

    override fun onGetEveryReviewsItemSuccess(everyReviewsResponse: EveryReviewsResponse) {
        everyReviewDatas.clear()
        everyReviewDatas.addAll(everyReviewsResponse.result)
        startPostponedEnterTransition()

        val everyReviewRVAdapter = initRecyclerView()
        goPlayerProfile(everyReviewRVAdapter)   // ?????? ???????????? ???????????? ?????? clickListener
    }

    override fun onGetEveryReviewsItemFailure(code: Int, message: String) {
        showToast("???????????? ?????? ?????? ??? ?????? ??????????????????.")
    }

    // ?????? PlayerProfile ?????? ??? ???????????? ??????
    private fun goPlayerProfile(everyReviewRVAdapter: EveryReviewRVAdapter) {
        everyReviewRVAdapter.clickPlayerReviewListener(object :
            EveryReviewRVAdapter.EveryReviewItemClickListener {
            override fun onItemClick(everyReveiwsItem: EveryReviewsItem) {
                // ??? ???????????? ??????
                if (everyReveiwsItem.writerIdx == myUserIdx) {
                    val fragmentTransaction: FragmentTransaction =
                        (context as MyProfileActivity).supportFragmentManager.beginTransaction()
                    fragmentTransaction.apply {
                        setReorderingAllowed(true)
                        setCustomAnimations(
                            R.anim.enter_from_right_anim,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.exit_to_right
                        )
                        replace(R.id.my_profile_into_fragment_container_fc, MyProfileFragment())
                        addToBackStack(null)
                        commit()
                    }
                    (context as MyProfileActivity).apply {
                        findViewById<TextView>(R.id.top_myProfile_tv).text = "?????? ?????????"
                        findViewById<TextView>(R.id.edit_myProfile_tv).visibility = View.GONE
                        findViewById<ConstraintLayout>(R.id.profile_bottom_chat_btn_cl).visibility =
                            View.VISIBLE
                    }

                } else {
                    // ?????? ?????? ???????????? ??????
                    val fragmentTransaction: FragmentTransaction =
                        (context as MyProfileActivity).supportFragmentManager.beginTransaction()
                    fragmentTransaction.apply {
                        setReorderingAllowed(true)
                        setCustomAnimations(
                            R.anim.enter_from_right_anim,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.exit_to_right
                        )
                        replace(R.id.my_profile_into_fragment_container_fc, PlayerFragment().apply {
                            arguments = Bundle().apply {
                                putInt("partnerUserIdx", everyReveiwsItem.writerIdx!!)
                            }
                        })
                        addToBackStack(null)
                        commit()
                    }
                    (context as MyProfileActivity).apply {
                        findViewById<TextView>(R.id.top_myProfile_tv).text = "?????????"
                        findViewById<TextView>(R.id.edit_myProfile_tv).visibility = View.GONE
                        findViewById<ConstraintLayout>(R.id.profile_bottom_chat_btn_cl).visibility =
                            View.VISIBLE
                    }

                }

            }

        })
    }

    // ?????????????????? ????????? ??????
    private fun initRecyclerView(): EveryReviewRVAdapter {
        val everyReviewRVAdapter = EveryReviewRVAdapter(everyReviewDatas)
        binding.playingReviewContentRv.adapter = everyReviewRVAdapter
        binding.playingReviewContentRv.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        return everyReviewRVAdapter
    }

    // ??? ????????? ????????? ????????????
    @SuppressLint("SetTextI18n")
    private fun setInit(profile: MyProfileInfoItem) {
        // ??? ????????? ????????? ???????????? (Room)
        val db = UserDatabase.getInstance(requireContext())
        val myProfileDB = db!!.userDao().getUser(myUserIdx)
        binding.playerNicknameTv.text = profile.nickname
        binding.playerGenerationTv.text = profile.age
        binding.playerLocationTv.text = profile.location
        binding.profileGradeRb.rating = profile.rating!!.toFloat()
        binding.profileGradeNumTv.text = toRatingStr(profile.rating!!)
        binding.playerSexTv.text = toGenderStr(myProfileDB.gender!!)
        Glide.with(binding.playerProfileImgIv.context)
            .load(profile.profileImgUrl)
            .into(binding.playerProfileImgIv)
        (context as MyProfileActivity).findViewById<TextView>(R.id.top_myProfile_tv).text =
            "??????(${profile.reviewCount.toString()})"
    }

    // ?????? player ????????? ????????? ????????????
    @SuppressLint("SetTextI18n")
    private fun setInit(profile: PartnerInfoDto) {
        binding.playerNicknameTv.text = profile.nickname
        binding.playerGenerationTv.text = profile.age
        binding.playerLocationTv.text = profile.locationCategory + " " + profile.locationName
        binding.profileGradeRb.rating = profile.rating!!.toFloat()
        binding.profileGradeNumTv.text = toRatingStr(profile.rating!!)
        binding.playerSexTv.text = toGenderStr(profile.gender)
        Glide.with(binding.playerProfileImgIv.context)
            .load(profile.profilePhotoUrl)
            .into(binding.playerProfileImgIv)
        (context as MyProfileActivity).findViewById<TextView>(R.id.top_myProfile_tv).text =
            "??????(${profile.reviewCount.toString()})"
    }


}






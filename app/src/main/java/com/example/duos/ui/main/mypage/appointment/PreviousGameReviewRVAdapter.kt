package com.example.duos.ui.main.mypage.appointment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.duos.data.entities.lastappointment.LastAppointmentResDto
import com.example.duos.databinding.PreviousGamePlayerItemBinding
import com.example.duos.utils.GlideApp

class PreviousGameReviewRVAdapter(private val previousGameList: ArrayList<LastAppointmentResDto>) :
    RecyclerView.Adapter<PreviousGameReviewRVAdapter.ViewHolder>() {

    interface PreviousPlayerItemClickListener {
        fun onProfileClick(lastAppointmentItem: LastAppointmentResDto)         // 해당 회원 프로필로 이동
        fun onWriteBtnClick(lastAppointmentItem: LastAppointmentResDto)     // 리뷰 작성하기로 이동
    }

    private lateinit var mItemClickListener: PreviousPlayerItemClickListener

    fun previousReviewItemClickListener(itemClickListener: PreviousPlayerItemClickListener) {
        mItemClickListener = itemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : PreviousGameReviewRVAdapter.ViewHolder {
        val binding: PreviousGamePlayerItemBinding = PreviousGamePlayerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviousGameReviewRVAdapter.ViewHolder, position: Int) {
        holder.bind(previousGameList[position])

        holder.binding.previousGamePlayerCv.setOnClickListener {
            mItemClickListener.onProfileClick(previousGameList[position])
        }
        holder.binding.btnWriteReview.setOnClickListener {
            mItemClickListener.onWriteBtnClick(previousGameList[position])
        }
    }

    override fun getItemCount(): Int = previousGameList.size

    inner class ViewHolder(val binding: PreviousGamePlayerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(myLastAppointment: LastAppointmentResDto) {
            binding.previousGamePlayerNicknameTv.text = myLastAppointment.nickname
            GlideApp.with(binding.previousGamePlayerIv.context)
                .load(myLastAppointment.profilePhotoUrl)
                .into(binding.previousGamePlayerIv)
            binding.previousGamePlayTimeTv.text = myLastAppointment.appointmentTime
            if (myLastAppointment.reviewed) {
                // 이미 리뷰 됨
                binding.writeDoneTv.visibility = View.VISIBLE   // 작성 완료가 보임
                binding.btnWriteReview.visibility = View.GONE   // 후기 작성이 안보이고
            } else {
                binding.btnWriteReview.visibility = View.VISIBLE   // 후기 작성이 보임
                binding.writeDoneTv.visibility = View.GONE   // 작성 완료가 안 보임
            }
        }
    }
}


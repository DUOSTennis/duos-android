package com.example.duos.ui.main.chat

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.duos.CustomDialog
import com.example.duos.data.entities.chat.ChatRoom
import com.example.duos.data.entities.chat.QuitChatRoomRequestBody
import com.example.duos.data.remote.chat.chat.ChatService
import com.example.duos.data.remote.chat.chat.QuitChatRoomResult
import com.example.duos.databinding.ItemChatListBinding
import com.example.duos.utils.getUserIdx
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import kotlin.collections.ArrayList

class ChatListRVAdapter(private var chatList: ArrayList<ChatRoom>, val deleteClickListener: DeleteClickListener) : QuitChatRoomView, RecyclerView.Adapter<ChatListRVAdapter.ViewHolder>(){
    private lateinit var dialogBuilder: CustomDialog.Builder
    private lateinit var context: Context
    private lateinit var deleteBtnTv: TextView

    interface ChatListItemClickListener{
        fun onItemClick(chatRoom: ChatRoom)
    }

    private lateinit var mItemClickListener: ChatListItemClickListener

    fun setChatListItemClickListener(itemClickListener: ChatListItemClickListener){
        mItemClickListener = itemClickListener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemChatListBinding = ItemChatListBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        context = viewGroup.context
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(chatList[position])
        holder.itemView.setOnClickListener {
            mItemClickListener.onItemClick(chatList[position])
            Log.d("포지션", position.toString())
        }
        deleteBtnTv.setOnClickListener {
            val isClamped = deleteClickListener.getIsClamped(holder)
            if(isClamped){
                Log.d("isClamped", "true")
                showDialog(it.context, holder.layoutPosition)
                dialogBuilder.show()
            }else{
                Log.d("isClamped", "false")
                deleteClickListener.onDeleteClick()

            }
        }
    }

    override fun getItemCount(): Int = chatList.size

    inner class ViewHolder(val binding: ItemChatListBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(chatRoom : ChatRoom){
            val messageTime = getFormattedDateTime(chatRoom.lastUpdatedAt)
            binding.chatListChatMessageTime.text = messageTime
            binding.chatListChatPreviewTv.text = chatRoom.lastMessage
            binding.chatListUserIdTv.text = chatRoom.chatRoomName
            Glide.with(context).load(chatRoom.chatRoomImg).apply(RequestOptions().circleCrop()).into(binding.chatListProfileIv)

            deleteBtnTv = binding.chatListDeleteBtn
        }
    }

    // position 위치의 데이터를 삭제 후 어댑터 갱신
    fun removeData(position: Int) {
        chatList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, chatList.size)
    }

    fun showDialog(context: Context, position: Int){
        // 만약 액티비티에서 사용한다면 아래 requireContext() 가 아닌 context를 사용하면 됨.
        dialogBuilder = CustomDialog.Builder(context)
            .setCommentMessage("채팅방을 나가면 상대와의 약속이 취소됩니다.\n삭제하시겠습니까?")// Dialog텍스트 설정하기 "~~~ "
            .setRightButton("삭제", object : CustomDialog.CustomDialogCallback {
                override fun onClick(dialog: CustomDialog, message: String) {//오른쪽 버튼 클릭시 이벤트 설정하기
                    // API호출해서 채팅목록 삭제시키고,그에 따른 이벤트처리
                    //removeData(position)
                    quitChatRoom(position)
                    Log.d("CustomDialog in SetupFrag", message.toString())//테스트 로그
                    dialog.dismiss()
                }
            })
            .setLeftButton("취소",  object : CustomDialog.CustomDialogCallback {
                override fun onClick(dialog: CustomDialog, message: String) {//왼쪽 버튼 클릭시 이벤트 설정하기
                    //바로 dismiss되야야 함
                    Log.d("CustomDialog in SetupFrag", message.toString())//테스트 로그
                    dialog.dismiss()
                }
            })
    }

    @Throws(Exception::class)
    fun getFormattedDateTime(dateTime: String):String {
        // 대상 날짜로 LocalDateTime 만들기
        var parsedDateTimeArray = dateTime.split(".")
        var parsedDateTime = parsedDateTimeArray[0]

        val parsedLocalDateTime = LocalDateTime.parse(parsedDateTime)

        // LocalDateTime에서 필요한 내용 필요한 형식으로 뽑기
//        val yyyyMMdd = parsedLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//        val yyyy = parsedLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy"))
//        val MM = parsedLocalDateTime.format(DateTimeFormatter.ofPattern("MM"))
//        val dd = parsedLocalDateTime.format(DateTimeFormatter.ofPattern("dd"))
        val time = parsedLocalDateTime.format(DateTimeFormatter.ofPattern("a hh:mm"))
//        println(yyyyMMdd)
//        println(yyyy)
//        println(MM)
//        println(dd)
        println(time)

        return time
    }

    interface DeleteClickListener{
        fun getIsClamped(viewHolder: RecyclerView.ViewHolder):Boolean
        fun onDeleteClick()
    }

    fun quitChatRoom(position: Int){
        val quitChatRoomRequestBody = QuitChatRoomRequestBody(chatList[position].chatRoomIdx, getUserIdx())
        ChatService.quitChatRoom(this, quitChatRoomRequestBody)

    }

    override fun onQuitChatRoomSuccess(quitChatRoomResult: QuitChatRoomResult) {
        for(i: Int in 0..chatList.size-1){
            if(chatList[i].chatRoomIdx.equals(quitChatRoomResult.chatRoomIdx)){
                removeData(i)
                break   // break를 안하면 데이터를 지운 후에도 for문이 지우기 전 chatList.size만큼 돌아서 Index out of bound 오류남
            }
        }
    }

    override fun onQuitChatRoomFailure(code: Int, message: String) {
        Log.d("채팅방 나가기 api 호출 실패",code.toString() + ": " + message)
    }
}
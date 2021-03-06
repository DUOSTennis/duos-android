package com.example.duos.data.entities.chat

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDateTime

@Parcelize
@Entity(tableName = "ChatMessageItemTable")
data class ChatMessageItem(
    var senderId : String,
    var body : String,
    var formattedSentAt : String,
    var sentAt : LocalDateTime,
    var viewType : Int,  // 0일 시 왼쪽(상대가 보낸 메세지), 1일 시 중앙(~가 입장하셨습니다), 2일 시 오른쪽(내가 보낸 메세지)
    var chatRoomIdx : String,
    @PrimaryKey(autoGenerate = false)
    var chatMessageIdx: String = "" // uuid
): Parcelable
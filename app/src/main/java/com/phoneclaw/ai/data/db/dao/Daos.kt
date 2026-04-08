package com.phoneclaw.ai.data.db.dao

import androidx.room.*
import com.phoneclaw.ai.data.db.entity.ConversationEntity
import com.phoneclaw.ai.data.db.entity.MessageEntity
import com.phoneclaw.ai.data.db.entity.PerChatSettingsEntity
import com.phoneclaw.ai.data.db.entity.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces")
    fun getAllSpaces(): Flow<List<SpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE spaceId = :spaceId ORDER BY lastMessageAt DESC")
    fun getConversations(spaceId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
}

@Dao
interface PerChatSettingsDao {
    @Query("SELECT * FROM per_chat_settings WHERE conversationId = :conversationId")
    suspend fun getPerChatSettings(conversationId: String): PerChatSettingsEntity?

    @Query("SELECT * FROM per_chat_settings WHERE conversationId = :conversationId")
    fun observePerChatSettings(conversationId: String): Flow<PerChatSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerChatSettings(settings: PerChatSettingsEntity)

    @Query("DELETE FROM per_chat_settings WHERE conversationId = :conversationId")
    suspend fun deletePerChatSettings(conversationId: String)
}

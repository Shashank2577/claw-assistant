package com.phoneclaw.ai.data.repository.impl

import com.phoneclaw.ai.data.db.dao.ConversationDao
import com.phoneclaw.ai.data.db.dao.MessageDao
import com.phoneclaw.ai.data.db.entity.ConversationEntity
import com.phoneclaw.ai.data.db.entity.MessageEntity
import com.phoneclaw.ai.data.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ConversationRepository {
    override fun getConversations(spaceId: String): Flow<List<ConversationEntity>> =
        conversationDao.getConversations(spaceId)

    override suspend fun getConversation(id: String): ConversationEntity? =
        conversationDao.getConversation(id)

    override suspend fun createConversation(spaceId: String, title: String): String {
        val id = UUID.randomUUID().toString()
        conversationDao.insertConversation(ConversationEntity(id = id, spaceId = spaceId, title = title))
        return id
    }

    override fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessages(conversationId)

    override suspend fun addMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    override suspend fun deleteAllConversations() {
        conversationDao.deleteAll()
    }
}

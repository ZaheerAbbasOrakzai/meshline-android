package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ContactEntity
import com.example.data.model.IdentityEntity
import com.example.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identity LIMIT 1")
    fun getIdentityFlow(): Flow<IdentityEntity?>

    @Query("SELECT * FROM identity LIMIT 1")
    suspend fun getIdentity(): IdentityEntity?

    @Query("SELECT * FROM contacts WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getContactByNodeId(nodeId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: IdentityEntity)

    @Query("DELETE FROM identity")
    suspend fun clearIdentity()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getContactByNodeId(nodeId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET pinnedMessage = :pinnedMessage WHERE id = :contactId")
    suspend fun updateContactPinnedMessage(contactId: String, pinnedMessage: String?)

    @Query("UPDATE contacts SET disappearingTimerSec = :timerSec WHERE id = :contactId")
    suspend fun updateContactDisappearingTimer(contactId: String, timerSec: Int)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: String)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM contacts WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getContactByNodeId(nodeId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateMessageReactions(messageId: String, reactions: String)

    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :messageId")
    suspend fun updateMessagePinned(messageId: String, isPinned: Boolean)

    @Query("DELETE FROM messages WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredMessages(now: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteConversationMessages(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun clearMessages()
}

@Dao
interface JoinRequestDao {
    @Query("SELECT * FROM join_requests ORDER BY timestamp DESC")
    fun getAllRequestsFlow(): Flow<List<com.example.data.model.JoinRequestEntity>>

    @Query("SELECT * FROM join_requests WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRequestsFlow(): Flow<List<com.example.data.model.JoinRequestEntity>>

    @Query("SELECT * FROM join_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): com.example.data.model.JoinRequestEntity?

    @Query("SELECT * FROM contacts WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getContactByNodeId(nodeId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: com.example.data.model.JoinRequestEntity)

    @Update
    suspend fun updateRequest(request: com.example.data.model.JoinRequestEntity)

    @Query("DELETE FROM join_requests WHERE id = :id")
    suspend fun deleteRequestById(id: String)

    @Query("DELETE FROM join_requests")
    suspend fun clearRequests()
}


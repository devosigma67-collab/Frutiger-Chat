package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_profiles")
data class LocalProfileEntity(
  @PrimaryKey val userId: String,
  val username: String,
  val signature: String,
  val currentActivity: String,
  val presenceStatus: String,
  val avatarColor: String,
  val wallpaperTheme: String = "default",
  val publicKeyFingerprint: String,
  val vaultPassphraseHash: String,
  val socketServerUrl: String,
  val waterEffectsEnabled: Boolean,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_friends")
data class LocalFriendEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val friendUid: String,
  val friendUsername: String,
  val friendSignature: String,
  val friendActivity: String,
  val friendStatus: String,
  val avatarColor: String,
  val publicKeyFingerprint: String,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_chats")
data class LocalChatEntity(
  @PrimaryKey val id: String,
  val ownerId: String,
  val title: String,
  val isGroup: Boolean,
  val avatarColor: String,
  val presenceStatus: String,
  val participantNames: String,
  val lastMessagePreview: String,
  val lastMessageSender: String,
  val encryptionKeyId: String,
  val unreadCount: Int,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_messages")
data class LocalMessageEntity(
  @PrimaryKey val id: String,
  val chatId: String,
  val ownerId: String,
  val senderId: String,
  val senderName: String,
  val plaintext: String,
  val ciphertext: String,
  val iv: String,
  val signatureHash: String,
  val isSignatureValid: Boolean,
  val messageType: String,
  val deliveryStatus: String,
  val timestampMillis: Long,
)

@Dao
interface FrutigerDao {
  @Query("SELECT * FROM local_profiles WHERE userId = :userId LIMIT 1")
  fun observeProfile(userId: String): Flow<LocalProfileEntity?>

  @Query("SELECT * FROM local_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getProfile(userId: String): LocalProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: LocalProfileEntity)

  @Query("SELECT * FROM local_friends WHERE userId = :userId ORDER BY friendUsername ASC")
  fun observeFriends(userId: String): Flow<List<LocalFriendEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFriends(friends: List<LocalFriendEntity>)

  @Query("DELETE FROM local_friends WHERE id = :friendDocId")
  suspend fun deleteFriend(friendDocId: String)

  @Query("SELECT * FROM local_chats WHERE ownerId = :userId ORDER BY updatedAtMillis DESC")
  fun observeChats(userId: String): Flow<List<LocalChatEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertChats(chats: List<LocalChatEntity>)

  @Query("DELETE FROM local_chats WHERE id = :chatId")
  suspend fun deleteChat(chatId: String)

  @Query("SELECT * FROM local_messages WHERE chatId = :chatId AND ownerId = :userId ORDER BY timestampMillis ASC")
  fun observeMessagesForChat(userId: String, chatId: String): Flow<List<LocalMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMessages(messages: List<LocalMessageEntity>)

  @Query("SELECT COUNT(*) FROM local_messages WHERE ownerId = :userId")
  fun observeTotalCachedMessagesCount(userId: String): Flow<Int>

  @Query("SELECT COUNT(*) FROM local_chats WHERE ownerId = :userId")
  fun observeTotalCachedChatsCount(userId: String): Flow<Int>

  @Query("DELETE FROM local_messages WHERE chatId = :chatId")
  suspend fun clearMessagesForChat(chatId: String)
}

@Database(
  entities = [
    LocalProfileEntity::class,
    LocalFriendEntity::class,
    LocalChatEntity::class,
    LocalMessageEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class FrutigerDatabase : RoomDatabase() {
  abstract fun frutigerDao(): FrutigerDao

  companion object {
    @Volatile
    private var INSTANCE: FrutigerDatabase? = null

    fun getInstance(context: Context): FrutigerDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                FrutigerDatabase::class.java,
                "frutiger_aero_chat.db",
              )
              .fallbackToDestructiveMigration(true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}

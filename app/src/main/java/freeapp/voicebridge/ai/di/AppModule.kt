package freeapp.voicebridge.ai.di

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import androidx.room.Room
import freeapp.voicebridge.ai.data.local.db.ConversationDao
import freeapp.voicebridge.ai.data.local.db.MessageDao
import freeapp.voicebridge.ai.data.local.db.VoiceBridgeDatabase
import freeapp.voicebridge.ai.data.repository.ConversationRepositoryImpl
import freeapp.voicebridge.ai.data.repository.SettingsRepositoryImpl
import freeapp.voicebridge.ai.data.repository.VpsRepositoryImpl
import freeapp.voicebridge.ai.domain.repository.ConversationRepository
import freeapp.voicebridge.ai.domain.repository.SettingsRepository
import freeapp.voicebridge.ai.domain.repository.VpsRepository
import freeapp.voicebridge.ai.domain.service.SpeechRecognitionService
import freeapp.voicebridge.ai.domain.service.TextToSpeechService
import freeapp.voicebridge.ai.data.audio.AndroidAsrAdapter
import freeapp.voicebridge.ai.data.audio.SherpaAsrAdapter
import freeapp.voicebridge.ai.data.audio.SherpaTtsAdapter
import javax.inject.Named
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideAssetManager(application: Application): AssetManager = application.assets

    @Provides
    @Singleton
    fun provideDatabase(application: Application): VoiceBridgeDatabase {
        return Room.databaseBuilder(
            application,
            VoiceBridgeDatabase::class.java,
            "voicebridge.db",
        ).build()
    }

    @Provides
    fun provideConversationDao(db: VoiceBridgeDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: VoiceBridgeDatabase): MessageDao = db.messageDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindVpsRepository(impl: VpsRepositoryImpl): VpsRepository

    @Binds
    @Singleton
    @Named("sherpa")
    abstract fun bindSherpaAsr(impl: SherpaAsrAdapter): SpeechRecognitionService

    @Binds
    @Singleton
    @Named("android")
    abstract fun bindAndroidAsr(impl: AndroidAsrAdapter): SpeechRecognitionService

    @Binds
    @Singleton
    abstract fun bindTextToSpeech(impl: SherpaTtsAdapter): TextToSpeechService
}

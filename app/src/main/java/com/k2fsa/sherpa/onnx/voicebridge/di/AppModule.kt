package com.k2fsa.sherpa.onnx.voicebridge.di

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import androidx.room.Room
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.ConversationDao
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.MessageDao
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.VoiceBridgeDatabase
import com.k2fsa.sherpa.onnx.voicebridge.data.repository.ConversationRepositoryImpl
import com.k2fsa.sherpa.onnx.voicebridge.data.repository.SettingsRepositoryImpl
import com.k2fsa.sherpa.onnx.voicebridge.data.repository.VpsRepositoryImpl
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.SettingsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.TextToSpeechService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.VoiceActivityDetector
import com.k2fsa.sherpa.onnx.voicebridge.data.audio.SherpaVadAdapter
import com.k2fsa.sherpa.onnx.voicebridge.data.audio.SherpaAsrAdapter
import com.k2fsa.sherpa.onnx.voicebridge.data.audio.SherpaTtsAdapter
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
    abstract fun bindVoiceActivityDetector(impl: SherpaVadAdapter): VoiceActivityDetector

    @Binds
    @Singleton
    abstract fun bindSpeechRecognition(impl: SherpaAsrAdapter): SpeechRecognitionService

    @Binds
    @Singleton
    abstract fun bindTextToSpeech(impl: SherpaTtsAdapter): TextToSpeechService
}

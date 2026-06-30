package com.example.savemedia.di

import android.content.Context
import com.example.savemedia.data.MediaRepositoryImpl
import com.example.savemedia.domain.MediaRepository
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.FileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLogger(@ApplicationContext context: Context): AppLogger = AppLogger(context)

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context,
        sanitizer: com.example.savemedia.utils.DataSanitizer,
        encryption: com.example.savemedia.utils.MediaEncryption,
        logger: AppLogger
    ): FileManager = FileManager(context, sanitizer, encryption, logger)

    @Provides
    @Singleton
    fun provideMediaRepository(
        @ApplicationContext context: Context,
        fileManager: FileManager,
        logger: AppLogger
    ): MediaRepository = MediaRepositoryImpl(context, fileManager, logger)

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}

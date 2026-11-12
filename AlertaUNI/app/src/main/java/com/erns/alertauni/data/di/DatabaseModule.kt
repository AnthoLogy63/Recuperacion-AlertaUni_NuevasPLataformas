package com.erns.alertauni.data.di

import android.content.Context
import com.erns.alertauni.data.local.AppDatabase
import com.erns.alertauni.data.local.DraftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideDraftDao(@ApplicationContext context: Context): DraftDao =
        AppDatabase.getInstance(context).draftDao()
}

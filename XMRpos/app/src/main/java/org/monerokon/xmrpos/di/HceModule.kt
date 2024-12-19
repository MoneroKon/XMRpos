package org.monerokon.xmrpos.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.monerokon.xmrpos.data.repository.HceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HceModule {

    @Provides
    @Singleton
    fun provideHceRepository(@ApplicationContext context: Context): HceRepository {
        return HceRepository(context)
    }

}
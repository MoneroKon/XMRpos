package org.monerokon.xmrpos.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.monerokon.xmrpos.data.printer.PrinterServiceManager
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.PrinterRepository
import org.monerokon.xmrpos.data.repository.StorageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {

    @Provides
    @Singleton
    fun providePrinterRepository(printerServiceManager: PrinterServiceManager, dataStoreRepository: DataStoreRepository, storageRepository: StorageRepository): PrinterRepository {
        return PrinterRepository(printerServiceManager, dataStoreRepository, storageRepository)
    }

    @Provides
    @Singleton
    fun providePrinterServiceManager(@ApplicationContext context: Context): PrinterServiceManager {
        return PrinterServiceManager(context)
    }
}
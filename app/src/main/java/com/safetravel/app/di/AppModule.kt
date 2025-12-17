package com.safetravel.app.di

import android.content.Context
import com.safetravel.app.data.repository.GeocodingService
import com.safetravel.app.data.repository.LocationService
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
    fun provideLocationService(@ApplicationContext context: Context): LocationService {
        return LocationService(context)
    }

    @Provides
    @Singleton
    fun provideGeocodingService(@ApplicationContext context: Context): GeocodingService {
        return GeocodingService(context)
    }
}

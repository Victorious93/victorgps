package com.victorious.gps.di

import android.content.Context
import android.location.LocationManager
import androidx.room.Room
import com.victorious.gps.data.AppDatabase
import com.victorious.gps.data.Clock
import com.victorious.gps.data.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "victorgps.db").build()

    @Provides
    fun profileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun clock(): Clock = Clock { System.currentTimeMillis() }

    @Provides
    fun locationManager(@ApplicationContext c: Context): LocationManager =
        c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
}

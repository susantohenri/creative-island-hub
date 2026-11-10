package com.henrisusanto.creativeislandhub

import android.app.Application
import com.henrisusanto.creativeislandhub.ads.AppOpenAdManager
import com.henrisusanto.creativeislandhub.analytics.AnalyticsLogger
import com.henrisusanto.creativeislandhub.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CreativeIslandHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase Analytics
        AnalyticsLogger.initialize(this)

        val koin = startKoin {
            androidLogger()
            androidContext(this@CreativeIslandHubApplication)
            modules(appModule)
        }.koin

        // Eagerly initialize AppOpenAdManager to start tracking activity lifecycle
        koin.get<AppOpenAdManager>()
    }
}

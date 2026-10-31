package com.henrisusanto.creativeislandhub

import android.app.Application
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

        startKoin {
            androidLogger()
            androidContext(this@CreativeIslandHubApplication)
            modules(appModule)
        }
    }
}

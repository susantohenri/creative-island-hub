package com.henrisusanto.creativeislandhub

import android.app.Application
import com.henrisusanto.creativeislandhub.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CreativeIslandHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@CreativeIslandHubApplication)
            modules(appModule)
        }
    }
}

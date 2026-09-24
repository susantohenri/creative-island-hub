package com.henrisusanto.creativeislandhub.di

import com.henrisusanto.creativeislandhub.ads.AdManager
import com.henrisusanto.creativeislandhub.data.local.PreferencesDataStore
import com.henrisusanto.creativeislandhub.data.network.RemoteDataSource
import com.henrisusanto.creativeislandhub.data.repository.IslandRepository
import com.henrisusanto.creativeislandhub.ui.viewmodel.MainViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    single { RemoteDataSource(get()) }
    single { PreferencesDataStore(androidContext()) }
    single { IslandRepository(get(), get()) }
    single { AdManager(androidContext()) }
    
    viewModel { MainViewModel(get(), get()) }
}

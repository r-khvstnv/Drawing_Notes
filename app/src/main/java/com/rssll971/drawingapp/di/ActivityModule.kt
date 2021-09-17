package com.rssll971.drawingapp.di

import android.app.Activity
import com.rssll971.drawingapp.ui.main.MainContract
import com.rssll971.drawingapp.ui.main.MainPresenter
import dagger.Module
import dagger.Provides

@Module
class ActivityModule(private var activity: Activity) {
    @Provides
    fun providesActivity(): Activity = activity
    @Provides
    fun providesMainPresenter(): MainPresenter = MainPresenter()
}
package com.rssll971.drawingapp.di

import com.rssll971.drawingapp.ui.main.MainActivity
import dagger.Component

@Component(modules = [ActivityModule::class])
interface ActivityComponent{
    fun inject(mainActivity: MainActivity)
}
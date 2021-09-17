package com.rssll971.drawingapp.di

import com.rssll971.drawingapp.ui.draw.DrawingView
import dagger.Component

@Component(modules = [ViewModule::class])
interface ViewComponent {
    fun inject(drawingView: DrawingView)
}
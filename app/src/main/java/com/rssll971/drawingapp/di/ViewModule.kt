/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.di

import com.rssll971.drawingapp.ui.draw.DrawPresenter
import dagger.Module
import dagger.Provides

@Module
class ViewModule {
    @Provides
    fun providesDrawingPresenter(): DrawPresenter = DrawPresenter()
}
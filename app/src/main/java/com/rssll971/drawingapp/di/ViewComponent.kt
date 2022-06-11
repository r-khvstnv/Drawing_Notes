/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.di

import com.rssll971.drawingapp.ui.draw.DrawCustomView
import dagger.Component

@Component(modules = [ViewModule::class])
interface ViewComponent {
    fun inject(drawCustomView: DrawCustomView)
}
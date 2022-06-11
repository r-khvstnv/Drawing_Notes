/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.di

interface BaseContract {
    interface Presenter<in V>{
        fun attach(view: V)
        fun detach()
    }
    interface View{}
}
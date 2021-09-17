package com.rssll971.drawingapp.di

interface BaseContract {
    interface Presenter<in V>{
        fun attach(view: V)
        fun detach()
    }
    interface View{}
}
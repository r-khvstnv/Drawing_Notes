package com.rssll971.drawingapp.ui.main

import com.rssll971.drawingapp.di.BaseContract

interface MainContract {
    interface Presenter: BaseContract.Presenter<MainView>{

    }
    interface MainView: BaseContract.View{

    }
}
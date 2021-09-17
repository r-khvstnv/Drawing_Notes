package com.rssll971.drawingapp.ui.draw

import com.rssll971.drawingapp.di.BaseContract

interface DrawContract {
    interface Presenter: BaseContract.Presenter<DrawView>{

    }
    interface DrawView: BaseContract.View{

    }
}
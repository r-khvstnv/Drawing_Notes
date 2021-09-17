package com.rssll971.drawingapp.ui.main

class MainPresenter: MainContract.Presenter {
    private var view: MainContract.MainView? = null
    override fun attach(view: MainContract.MainView) {
        this.view = view
    }

    override fun detach() {
        this.view = null
    }
}
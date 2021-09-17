package com.rssll971.drawingapp.ui.draw

class DrawPresenter: DrawContract.Presenter {
    private var view: DrawContract.DrawView? = null
    override fun attach(view: DrawContract.DrawView) {
        this.view = view
    }

    override fun detach() {
        this.view = null
    }
}
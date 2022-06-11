/************************************************
 * Created by Ruslan Khvastunov                 *
 * r.khvastunov@gmail.com                       *
 * Copyright (c) 2022                           *
 * All rights reserved.                         *
 *                                              *
 ************************************************/

package com.rssll971.drawingapp.utils

import android.graphics.Path
import android.os.Parcel
import android.os.Parcelable

/** Class is used for lines Drawing.
 * Using it's object, data is saved/recreated on device rotation*/
class CustomPath(var color: Int, var brushThickness: Float) : Path(), Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(color)
        parcel.writeFloat(brushThickness)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomPath> {
        override fun createFromParcel(parcel: Parcel): CustomPath {
            return CustomPath(parcel)
        }

        override fun newArray(size: Int): Array<CustomPath?> {
            return arrayOfNulls(size)
        }
    }
}
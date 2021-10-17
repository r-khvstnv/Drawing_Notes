package com.rssll971.drawingapp.utils

import android.graphics.Path
import android.os.Parcel
import android.os.Parcelable

/**
 * In next class using parametrized constructor of drawing, which extend my Path
 */
class CustomPath(var color: Int, var brushThickness: Float) : Path(), Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readFloat()
    ) {
    }

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
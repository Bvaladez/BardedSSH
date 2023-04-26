package com.bard.android.bardedssh

// Holds the data that can be displayed in recycler view items
data class FileViewModel(
    val type: String, val name: String,
    val size: String, val permissions: String
    ){

}
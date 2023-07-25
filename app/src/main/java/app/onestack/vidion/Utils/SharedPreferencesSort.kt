package app.onestack.vidion.Utils

import android.content.Context
import android.content.Context.MODE_PRIVATE

fun saveSortPref(context: Context, urlPopup: String) {
    val sharedPrefs = context.getSharedPreferences("getSharedPreferences", MODE_PRIVATE)
    sharedPrefs.edit().putString("sort", urlPopup).apply()
}

fun loadSortSharePref(context: Context): String {
    val sharedPrefs = context.getSharedPreferences("getSharedPreferences", MODE_PRIVATE)
    return sharedPrefs.getString("sort", "").toString()
}

fun savePositionListPref(context: Context, position: Int) {
    val sharedPrefs = context.getSharedPreferences("getSharedPreferences", MODE_PRIVATE)
    sharedPrefs.edit().putInt("position", position).apply()
}

fun loadPositionListPref(context: Context): Int {
    val sharedPrefs = context.getSharedPreferences("getSharedPreferences", MODE_PRIVATE)
    return sharedPrefs.getInt("position", 0)
}
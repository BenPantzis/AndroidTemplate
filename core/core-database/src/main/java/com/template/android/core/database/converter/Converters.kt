package com.template.android.core.database.converter

import androidx.room.TypeConverter

class Converters {
    // Add @TypeConverter pairs here for any complex type stored in Room.
    //
    // Example — storing a List<String> as a JSON array:
    //
    // @TypeConverter
    // fun fromStringList(value: String): List<String> =
    //     org.json.JSONArray(value).let { arr -> List(arr.length()) { arr.getString(it) } }
    //
    // @TypeConverter
    // fun toStringList(list: List<String>): String =
    //     org.json.JSONArray().also { arr -> list.forEach(arr::put) }.toString()
}

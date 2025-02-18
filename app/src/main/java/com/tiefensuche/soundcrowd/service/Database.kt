/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.soundcrowd.service

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.tiefensuche.soundcrowd.database.MetadataDatabase
import com.tiefensuche.soundcrowd.utils.MediaIDHelper
import com.tiefensuche.soundcrowd.waveform.CuePoint

/**
 * Database that stores media items, metadata, and [CuePoint] items
 *
 * Created by tiefensuche on 23.09.16.
 */
internal class Database(context: Context) : MetadataDatabase(context) {

    companion object {
        private var TAG = Database::class.simpleName

        private const val MEDIA_ID = "media_id"
        private const val POSITION = "position"
        private const val DESCRIPTION = "description"

        private const val DATABASE_MEDIA_ITEM_CUE_POINTS_NAME = "MediaItemStars"
        private const val DATABASE_MEDIA_ITEMS_CUE_POINTS_CREATE = "create table if not exists $DATABASE_MEDIA_ITEM_CUE_POINTS_NAME ($MEDIA_ID text not null, $POSITION int not null, $DESCRIPTION text, CONSTRAINT pk_media_item_star PRIMARY KEY ($MEDIA_ID,$POSITION))"

        private const val DATABASE_MEDIA_ITEMS_METADATA_NAME = "MediaItemsMetadata"
        private const val DATABASE_MEDIA_ITEMS_METADATA_CREATE = "create table if not exists $DATABASE_MEDIA_ITEMS_METADATA_NAME ($ID text primary key, $POSITION long, $ALBUM_ART_URL text, vibrant_color int, text_color int)"
    }

    init {
        writableDatabase.execSQL(DATABASE_MEDIA_ITEMS_CUE_POINTS_CREATE)
    }

    internal val cuePointItems: ArrayList<MediaMetadataCompat>
        get() {
            val items = ArrayList<MediaMetadataCompat>()
            try {
                val cursor = readableDatabase.query(DATABASE_MEDIA_ITEMS_NAME,
                        null, "EXISTS (SELECT $MEDIA_ID FROM $DATABASE_MEDIA_ITEM_CUE_POINTS_NAME WHERE ${DATABASE_MEDIA_ITEM_CUE_POINTS_NAME}.$MEDIA_ID = ${DATABASE_MEDIA_ITEMS_NAME}.$ID)", null, null, null, null, null)
                while (cursor.moveToNext()) {
                    items.add(buildItem(cursor))
                }
                cursor.close()
            } catch (e: SQLException) {
                Log.e(TAG, "error while querying cue points", e)
            }
            return items
        }

    internal fun getLastPosition(mediaId: String?): Long {
        var result: Long = 0
        if (mediaId == null)
            return 0
        try {
            val cursor = readableDatabase.query(DATABASE_MEDIA_ITEMS_METADATA_NAME,
                    arrayOf(POSITION), "$ID=?", arrayOf(mediaId),
                    null, null, null)
            if (cursor.moveToFirst()) {
                result = cursor.getLong(cursor.getColumnIndex(POSITION))
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, "error while query last position", e)
        }

        return result
    }

    internal fun updatePosition(metadata: MediaMetadataCompat, position: Long) {
        addMediaItem(metadata)
        metadata.description.mediaId?.let {
            val values = ContentValues()
            values.put(ID, it)
            values.put(POSITION, position)
            try {
                writableDatabase.insertOrThrow(DATABASE_MEDIA_ITEMS_METADATA_NAME, null, values)
            } catch (e: SQLException) {
                values.remove(ID)
                try {
                    writableDatabase.update(DATABASE_MEDIA_ITEMS_METADATA_NAME, values, "$ID=?", arrayOf(it))
                } catch (e1: SQLiteException) {
                    Log.e(TAG, "error while updating position", e1)
                }
            }
        }
    }

    internal fun addCuePoint(metadata: MediaMetadataCompat, position: Int, description: String?) {
        addMediaItem(metadata)
        val values = ContentValues()
        values.put(MEDIA_ID, metadata.description.mediaId)
        values.put(POSITION, position)
        values.put(DESCRIPTION, description)
        try {
            writableDatabase.insertOrThrow(DATABASE_MEDIA_ITEM_CUE_POINTS_NAME, null, values)
        } catch (e: SQLException) {
            Log.e(TAG, "error while adding cue point", e)
        }
    }

    internal fun deleteCuePoint(mediaId: String, position: Int) {
        try {
            writableDatabase.delete(DATABASE_MEDIA_ITEM_CUE_POINTS_NAME,
                    "$MEDIA_ID=? AND $POSITION=?", arrayOf(mediaId, position.toString()))
        } catch (e: SQLException) {
            Log.e(TAG, "error while removing cue point", e)
        }
    }

    internal fun getCuePoints(mediaId: String): Collection<CuePoint> {
        val result = ArrayList<CuePoint>()
        try {
            val cursor = readableDatabase.query(DATABASE_MEDIA_ITEM_CUE_POINTS_NAME,
                    null, "$MEDIA_ID=?", arrayOf(mediaId),
                    null, null, null)
            while (cursor.moveToNext()) {
                result.add(CuePoint(cursor.getString(cursor.getColumnIndex(MEDIA_ID)),
                        cursor.getInt(cursor.getColumnIndex(POSITION)),
                        cursor.getString(cursor.getColumnIndex(DESCRIPTION))))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "error while querying cue points", e)
        }

        return result
    }

    internal fun setDescription(mediaId: String, position: Int, text: String) {
        val values = ContentValues()
        values.put(DESCRIPTION, text)
        try {
            writableDatabase.update(DATABASE_MEDIA_ITEM_CUE_POINTS_NAME, values,
                    "$MEDIA_ID=? AND $POSITION=?", arrayOf(mediaId, position.toString()))
        } catch (e: SQLException) {
            Log.e(TAG, "error while setting description", e)
        }
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        super.onCreate(sqLiteDatabase)
        sqLiteDatabase.execSQL(DATABASE_MEDIA_ITEMS_CUE_POINTS_CREATE)
        sqLiteDatabase.execSQL(DATABASE_MEDIA_ITEMS_METADATA_CREATE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        super.onUpgrade(sqLiteDatabase, oldVersion, newVersion)
        when (oldVersion) {
            1 -> {
                sqLiteDatabase.execSQL("ALTER TABLE $DATABASE_MEDIA_ITEMS_METADATA_NAME ADD COLUMN vibrant_color int")
                sqLiteDatabase.execSQL("ALTER TABLE $DATABASE_MEDIA_ITEMS_METADATA_NAME ADD COLUMN text_color int")
            }
        }
    }
}
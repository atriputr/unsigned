package com.example.un_signed

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

/** Mirrors CalendarTask entries into a dedicated local "Unsigned" calendar via the system Calendar Provider. */
object SystemCalendarSync {

    private const val CALENDAR_DISPLAY_NAME = "Unsigned"
    private const val ACCOUNT_NAME = "Unsigned Local"

    /** Finds (or creates) the local "Unsigned" calendar, returning its Calendar Provider id. */
    fun ensureCalendarId(ctx: Context): Long? {
        if (!PermissionsManager.hasCalendarPermission(ctx)) return null
        val resolver = ctx.contentResolver

        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?"
        val args = arrayOf(ACCOUNT_NAME, CALENDAR_DISPLAY_NAME)

        try {
            resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getLong(0)
            }

            val values = ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
                put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(CalendarContract.Calendars.NAME, CALENDAR_DISPLAY_NAME)
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
                put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF09e8ad.toInt())
                put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
                put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
                put(CalendarContract.Calendars.VISIBLE, 1)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            }
            val uri = resolver.insert(
                CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    .build(),
                values
            )
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (_: Exception) {
            return null
        }
    }

    /** Inserts or updates the system Calendar event mirroring [task]. Returns the event id, or null on failure. */
    fun upsertEvent(ctx: Context, date: LocalDate, task: CalendarTask): Long? {
        if (!PermissionsManager.hasCalendarPermission(ctx)) return null
        val calendarId = ensureCalendarId(ctx) ?: return null
        val resolver = ctx.contentResolver
        val zone = ZoneId.systemDefault()

        val startMillis: Long
        val endMillis: Long
        val allDay: Int
        if (task.timeMinutesOfDay != null) {
            val startDateTime = date.atStartOfDay(zone).plusMinutes(task.timeMinutesOfDay.toLong())
            startMillis = startDateTime.toInstant().toEpochMilli()
            endMillis = startDateTime.plusHours(1).toInstant().toEpochMilli()
            allDay = 0
        } else {
            startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            allDay = 1
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, task.text)
            put(CalendarContract.Events.DTSTART, startMillis)
            if (allDay == 1) {
                put(CalendarContract.Events.DURATION, "P1D")
            } else {
                put(CalendarContract.Events.DTEND, endMillis)
            }
            put(CalendarContract.Events.ALL_DAY, allDay)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
        }

        return try {
            val existingId = task.systemCalendarEventId
            if (existingId != null) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                val updated = resolver.update(uri, values, null, null)
                if (updated > 0) existingId else {
                    val newUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    newUri?.lastPathSegment?.toLongOrNull()
                }
            } else {
                val newUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                newUri?.lastPathSegment?.toLongOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteEvent(ctx: Context, eventId: Long) {
        if (!PermissionsManager.hasCalendarPermission(ctx)) return
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            ctx.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
            // ignore — event may already be gone
        }
    }
}

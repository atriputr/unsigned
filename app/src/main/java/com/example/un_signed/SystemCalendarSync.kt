package com.example.un_signed

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Two-way bridge between the app's CalendarTask store and the phone's Calendar Provider.
 *  · Outbound: app-created tasks are mirrored into a dedicated local "Unsigned" calendar.
 *  · Inbound:  events from every visible calendar on the device are imported as read-only
 *              CalendarTasks (marker: externalCalendarId != null), so the user's phone-calendar
 *              activity shows up in the app automatically.
 */
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
        if (task.isExternal) return null  // never round-trip external events back out
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

    // ── Reverse sync: phone calendar → app ─────────────────────────

    /**
     * Reads events from every visible calendar (except the app's own "Unsigned" one), pulls them into
     * a map keyed by date. Bounded to [from]..[to] inclusive to keep the query cheap.
     *
     * Returns null if permissions aren't granted (caller should keep whatever it already has).
     */
    fun importExternalEvents(ctx: Context, from: LocalDate, to: LocalDate): Map<LocalDate, List<CalendarTask>>? {
        if (!PermissionsManager.hasCalendarPermission(ctx)) return null
        val ownCalendarId = ensureCalendarId(ctx)
        val resolver = ctx.contentResolver
        val zone = ZoneId.systemDefault()

        val startMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Use Instances URI so we get expanded recurring events too.
        val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMs.toString())
            .appendPath(endMs.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.VISIBLE
        )

        val result = mutableMapOf<LocalDate, MutableList<CalendarTask>>()
        val now = System.currentTimeMillis()

        try {
            resolver.query(instancesUri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                val idxEventId = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val idxTitle = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val idxBegin = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val idxAllDay = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val idxCalId = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
                val idxCalName = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val idxVisible = cursor.getColumnIndex(CalendarContract.Instances.VISIBLE)

                while (cursor.moveToNext()) {
                    val calId = if (idxCalId != -1) cursor.getLong(idxCalId) else continue
                    // Skip our own mirror calendar to prevent double-imports of app-created tasks.
                    if (ownCalendarId != null && calId == ownCalendarId) continue
                    if (idxVisible != -1 && cursor.getInt(idxVisible) == 0) continue

                    val title = if (idxTitle != -1) cursor.getString(idxTitle)?.takeIf { it.isNotBlank() } else null
                    if (title == null) continue

                    val begin = if (idxBegin != -1) cursor.getLong(idxBegin) else continue
                    val allDay = if (idxAllDay != -1) cursor.getInt(idxAllDay) == 1 else false
                    val calName = if (idxCalName != -1) cursor.getString(idxCalName).orEmpty() else ""
                    val eventId = if (idxEventId != -1) cursor.getLong(idxEventId) else -1L

                    val dt = try {
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), zone)
                    } catch (_: Exception) { continue }

                    val date = dt.toLocalDate()
                    val minutesOfDay = if (allDay) null else dt.hour * 60 + dt.minute

                    // Deterministic id so re-imports collapse onto the same CalendarTask.
                    val stableId = UUID.nameUUIDFromBytes("cp:$calId:$eventId:$date".toByteArray()).toString()

                    val task = CalendarTask(
                        id = stableId,
                        text = title,
                        isDone = false,
                        colorIndex = ((calId % 8).toInt() + 8) % 8,
                        timeMinutesOfDay = minutesOfDay,
                        systemCalendarEventId = eventId,
                        externalCalendarId = calId,
                        externalCalendarName = calName,
                        lastSyncedAtMs = now
                    )
                    result.getOrPut(date) { mutableListOf() }.add(task)
                }
            }
        } catch (_: Throwable) {
            return null
        }
        return result
    }

    /**
     * Merges [imported] external events into [existing] app tasks: external entries are refreshed
     * (deleted stale ones for the same [from]..[to] window, re-added), user-authored tasks are
     * preserved untouched.
     */
    fun mergeImported(
        existing: Map<LocalDate, List<CalendarTask>>,
        imported: Map<LocalDate, List<CalendarTask>>,
        from: LocalDate,
        to: LocalDate
    ): Map<LocalDate, List<CalendarTask>> {
        val merged = existing.toMutableMap()
        val touchedDates = mutableSetOf<LocalDate>()

        // Drop previous externals inside the window so deletions on the phone are picked up.
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.forEach { date ->
            val current = merged[date] ?: return@forEach
            val kept = current.filterNot { it.isExternal }
            if (kept.isEmpty()) merged.remove(date) else merged[date] = kept
            touchedDates += date
        }

        imported.forEach { (date, tasks) ->
            val current = merged[date] ?: emptyList()
            // Preserve isDone from previous import if we've already seen the same external event.
            val previousById = current.associateBy { it.id }
            val next = current + tasks.map { imp ->
                val prev = previousById[imp.id]
                if (prev != null) imp.copy(isDone = prev.isDone) else imp
            }
            merged[date] = next
            touchedDates += date
        }

        return merged
    }

    /**
     * Registers a ContentObserver for the calendar provider. The callback fires on the main
     * thread whenever the OS notifies of any change (add / edit / delete). Returns a handle
     * the caller can pass back to [stopObserving].
     */
    fun startObserving(ctx: Context, onChange: () -> Unit): ContentObserver? {
        if (!PermissionsManager.hasCalendarPermission(ctx)) return null
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) { onChange() }
        }
        return try {
            ctx.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI, true, observer
            )
            observer
        } catch (_: Exception) {
            null
        }
    }

    fun stopObserving(ctx: Context, observer: ContentObserver?) {
        if (observer == null) return
        try { ctx.contentResolver.unregisterContentObserver(observer) } catch (_: Exception) {}
    }
}

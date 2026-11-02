package com.personal.prayertimes

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        updateAllWidgets(context)
    }

    companion object {
        fun getScheduleJson(context: Context): String {
            val prefs = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getString("schedule_json", null)
            if (saved != null) return saved
            val assetJson = context.assets.open("november_2026.json").bufferedReader().use { it.readText() }
            prefs.edit().putString("schedule_json", assetJson).apply()
            return assetJson
        }

        fun updateAllWidgets(context: Context) {
            val jsonString = getScheduleJson(context)
            val now = LocalDateTime.now()
            val todayDate = now.dayOfMonth
            val currentTime = now.toLocalTime()

            val nextPrayer = findNextPrayerStartOnly(jsonString, todayDate, currentTime)

            val views = RemoteViews(context.packageName, R.layout.prayer_widget)
            views.setTextViewText(R.id.tv_next_prayer_name, "NEXT: ${nextPrayer.first}")
            views.setTextViewText(R.id.tv_next_prayer_time, nextPrayer.second)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingOpen = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingOpen)

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            manager.updateAppWidget(ids, views)

            scheduleNextExactSwitch(context, nextPrayer.third)
        }

        fun findNextPrayerStartOnly(
            jsonStr: String,
            dayOfMonth: Int,
            nowTime: LocalTime
        ): Triple<String, String, LocalDateTime> {
            val root = JSONObject(jsonStr)
            val days = root.getJSONArray("days")
            var todayObj: JSONObject? = null
            var tomorrowObj: JSONObject? = null

            for (i in 0 until days.length()) {
                val d = days.getJSONObject(i)
                if (d.getInt("date") == dayOfMonth) todayObj = d
                if (d.getInt("date") == dayOfMonth + 1) tomorrowObj = d
            }

            val dayToUse = todayObj ?: days.getJSONObject(0)
            val slots = listOf(
                "FAJR" to dayToUse.getString("fajr"),
                "SUNRISE" to dayToUse.getString("sunrise"),
                "ZUHR" to dayToUse.getString("zuhr"),
                "ASR" to dayToUse.getString("asr"),
                "MAGHRIB" to dayToUse.getString("maghrib"),
                "ISHA" to dayToUse.getString("isha")
            )

            for ((label, timeStr) in slots) {
                val t = LocalTime.parse(timeStr)
                if (t.isAfter(nowTime)) {
                    val trigger = LocalDate.now().atTime(t).plusMinutes(1)
                    return Triple(label, timeStr, trigger)
                }
            }

            val nextDayFajr = tomorrowObj?.getString("fajr") ?: dayToUse.getString("fajr")
            val fajrTime = LocalTime.parse(nextDayFajr)
            val trigger = LocalDate.now().plusDays(1).atTime(fajrTime).plusMinutes(1)
            return Triple("FAJR", nextDayFajr, trigger)
        }

        private fun scheduleNextExactSwitch(context: Context, triggerDateTime: LocalDateTime) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = "com.personal.prayertimes.ACTION_ALARM_TICK"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (android.os.Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }
}

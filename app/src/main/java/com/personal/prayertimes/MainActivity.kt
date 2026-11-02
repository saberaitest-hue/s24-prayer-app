package com.personal.prayertimes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    private lateinit var contentLayout: LinearLayout
    private val IMPORT_JSON_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F17"))
        }
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 60)
        }
        rootScroll.addView(contentLayout)
        setContentView(rootScroll)

        PrayerWidgetProvider.updateAllWidgets(this)
        renderUI()
    }

    private fun renderUI() {
        contentLayout.removeAllViews()
        val jsonStr = PrayerWidgetProvider.getScheduleJson(this)
        val root = JSONObject(jsonStr)
        val monthName = root.optString("month", "November 2026")
        val days = root.getJSONArray("days")

        val now = LocalDateTime.now()
        val nextTriple = PrayerWidgetProvider.findNextPrayerStartOnly(jsonStr, now.dayOfMonth, now.toLocalTime())

        val title = TextView(this).apply {
            text = "S24 Prayer Companion • $monthName"
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        contentLayout.addView(title)

        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#162032"))
            setPadding(40, 40, 40, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 32, 0, 32)
            layoutParams = params
        }
        banner.addView(TextView(this).apply {
            text = "LOCK SCREEN / AOD NEXT PRAYER (START ONLY)"
            textSize = 11f
            setTextColor(Color.parseColor("#38BDF8"))
            setTypeface(null, Typeface.BOLD)
        })
        banner.addView(TextView(this).apply {
            text = "${nextTriple.first}  —  ${nextTriple.second}"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        })
        contentLayout.addView(banner)

        val importBtn = Button(this).apply {
            text = "Import New Month Schedule (.JSON)"
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(intent, IMPORT_JSON_CODE)
            }
        }
        contentLayout.addView(importBtn)

        var todayObj = days.getJSONObject(0)
        for (i in 0 until days.length()) {
            val d = days.getJSONObject(i)
            if (d.getInt("date") == now.dayOfMonth) todayObj = d
        }

        val todayTitle = TextView(this).apply {
            text = "Today (${todayObj.getString("day")} ${todayObj.getInt("date")}) — Start & Jamaat"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 48, 0, 16)
        }
        contentLayout.addView(todayTitle)

        val todayRows = listOf(
            Triple("Fajr", todayObj.getString("fajr"), todayObj.optString("fajr_jamaat", "-")),
            Triple("Sunrise", todayObj.getString("sunrise"), "-"),
            Triple("Zuhr", todayObj.getString("zuhr"), todayObj.optString("zuhr_jamaat", "-")),
            Triple("Asr", todayObj.getString("asr"), todayObj.optString("asr_jamaat", "-")),
            Triple("Maghrib", todayObj.getString("maghrib"), todayObj.getString("maghrib")),
            Triple("Isha", todayObj.getString("isha"), todayObj.getString("isha"))
        )

        for ((pName, pStart, pJam) in todayRows) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 20, 24, 20)
                setBackgroundColor(Color.parseColor("#111827"))
            }
            row.addView(TextView(this).apply {
                text = pName
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = "Start: $pStart"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = "Jamaat: $pJam"
                setTextColor(Color.parseColor("#9CA3AF"))
                textSize = 14f
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            contentLayout.addView(row)
        }

        val monthHeader = TextView(this).apply {
            text = "Full Month Schedule ($monthName)"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 48, 0, 16)
        }
        contentLayout.addView(monthHeader)

        for (i in 0 until days.length()) {
            val d = days.getJSONObject(i)
            val isToday = d.getInt("date") == now.dayOfMonth
            val item = TextView(this).apply {
                text = "${d.getInt("date")} ${d.getString("day").take(3)}  |  F: ${d.getString("fajr")}  S: ${d.getString("sunrise")}  Z: ${d.getString("zuhr")}  A: ${d.getString("asr")}  M: ${d.getString("maghrib")}  I: ${d.getString("isha")}"
                textSize = 12f
                setTextColor(if (isToday) Color.parseColor("#38BDF8") else Color.parseColor("#D1D5DB"))
                setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)
                setPadding(16, 16, 16, 16)
                setBackgroundColor(if (i % 2 == 0) Color.parseColor("#111827") else Color.parseColor("#0B0F17"))
            }
            contentLayout.addView(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_JSON_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val jsonText = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return
            getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("schedule_json", jsonText)
                .apply()
            PrayerWidgetProvider.updateAllWidgets(this)
            renderUI()
            Toast.makeText(this, "Schedule & AOD Widget Updated!", Toast.LENGTH_LONG).show()
        }
    }
}

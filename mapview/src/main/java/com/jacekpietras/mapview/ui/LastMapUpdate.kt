package com.jacekpietras.mapview.ui

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.util.Log

object LastMapUpdate {

    var trans: Long = 0L
    var cutoS: Long = 0L
    var moveE: Long = 0L
    var tranS: Long = 0L
    var cachE: Long = 0L
    var cutoE: Long = 0L
    var rendS: Long = 0L
    var rendE: Long = 0L
    var sortS: Long = 0L
    var sortE: Long = 0L
    var mergE: Long = 0L

    private var lastUpdate: Long = 0L
    private val fpsList = mutableListOf<Long>()
    private val fromLastList = mutableListOf<Long>()
    private val frameTimeList = mutableListOf<Long>()
    val medFps get() = fpsList.average().toInt()

    private fun updateFps() {
        val now = System.currentTimeMillis()
        val timeFromLast = now - lastUpdate
        lastUpdate = now
        if (timeFromLast > 0) {
            val fps = DateUtils.SECOND_IN_MILLIS / timeFromLast
            if (fps in (0..200)) {
                fpsList.addMax30(fps)
            }
        }
    }

    private fun MutableList<Long>.addMax30(next: Long) {
        add(next)
        if (size > 30) {
            removeAt(0)
        }
    }

    @SuppressLint("LogNotTimber")
    fun log() {
        updateFps()

        val prevRendE = rendE
        rendE = System.nanoTime()
        if (trans > 0) {
            fromLastList.addMax30(trans - prevRendE)
            frameTimeList.addMax30(rendE - trans)

            Log.d(
                "D:",
                "Perf: Render: [FPS:$medFps]\n" +
                        "since last frame ${prevRendE toMs trans} (~${fromLastList.average().toLong().toMs()})\n" +
                        "    [pass to vm] ${trans toMs cutoS}\n" +
                        "    [coord prep] ${cutoS toMs moveE}\n" +
                        "    [rend creat] ${moveE toMs tranS}\n" +
                        "    [ translate] ${tranS toMs cachE}\n" +
                        "    [      bake] ${cachE toMs sortS}\n" +
                        "    [      sort] ${sortS toMs sortE}\n" +
                        "    [       sum] ${sortE toMs mergE}\n" +
                        "    [invali req] ${mergE toMs cutoE}\n" +
                        "    [invalidate] ${cutoE toMs rendS}\n" +
                        "    [    render] ${rendS toMs rendE}\n" +
                        "             sum ${trans toMs rendE} (~${frameTimeList.average().toLong().toMs()})"
            )
        }
    }

    private infix fun Long.toMs(right: Long) =
        "${(right - this) / 10_000 / 1_00.0} ms"

    private fun Long.toMs() =
        "${this / 10_000 / 1_00.0} ms"
}
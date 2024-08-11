package com.nltv.chafenqi.storage.log

import com.nltv.chafenqi.storage.datastore.user.chunithm.ChunithmDeltaEntry
import com.nltv.chafenqi.storage.datastore.user.chunithm.ChunithmRecentScoreEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

class ChunithmLogData(
    recentEntries: List<ChunithmRecentScoreEntry>,
    deltaEntries: List<ChunithmDeltaEntry>
) {
    data class ChunithmDayData(
        var date: Instant = Instant.fromEpochSeconds(0),
        var ratingGain: Double = 0.0,
        var playCountGain: Int = 0,
        var overpowerGain: Double = 0.0,

        var latestDeltaEntry: ChunithmDeltaEntry = ChunithmDeltaEntry(),
        var recentEntries: List<ChunithmRecentScoreEntry> = listOf(),

        var hasDelta: Boolean = false,
        var averageScore: Double = 0.0,
        var duration: Instant = Instant.fromEpochSeconds(0)
    )

    var dayPlayed = -1
    var records: List<ChunithmDayData> = listOf()

    init {
        val latestTimestamp = recentEntries.firstOrNull()?.timestamp ?: 0
        val oldestTimestamp = recentEntries.lastOrNull()?.timestamp ?: 0

        val truncatedOldestTimestamp = Instant
            .fromEpochSeconds(oldestTimestamp.toLong())
            .toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
            .date
            .atStartOfDayIn(timeZone = TimeZone.currentSystemDefault())
            .epochSeconds

        val truncatedLatestTimestamp = Instant
            .fromEpochSeconds(latestTimestamp.toLong())
            .toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
            .date
            .atStartOfDayIn(timeZone = TimeZone.currentSystemDefault())
            .plus(Duration.parse("24h"))
            .epochSeconds

        var pointer = truncatedLatestTimestamp
        while (pointer > truncatedOldestTimestamp) {
            pointer -= 86400
            val playInDay = recentEntries.filter {
                it.timestamp in (pointer - 86400)..pointer
            }
            if (playInDay.isNotEmpty()) {
                val record = ChunithmDayData(
                    date = Instant.fromEpochSeconds(pointer - 86400),
                    recentEntries = playInDay
                )

                val latestDelta = deltaEntries.lastOrNull {
                    LocalDateTime.parse(it.createdAt, LocalDateTime.Formats.ISO)
                        .toInstant(TimeZone.currentSystemDefault())
                        .epochSeconds in (pointer - 86400)..pointer
                }
                if (latestDelta != null) record.latestDeltaEntry = latestDelta

                val previousDelta = records.lastOrNull()?.latestDeltaEntry
                if (previousDelta != null && latestDelta != null) {
                    record.hasDelta = true
                    record.ratingGain = latestDelta.rating - previousDelta.rating
                    record.playCountGain = latestDelta.playCount - previousDelta.playCount
                    record.overpowerGain = latestDelta.rawOverpower - previousDelta.rawOverpower
                }

                if (record.recentEntries.isNotEmpty()) {
                    record.averageScore = record.recentEntries.sumOf { it.score } / record.recentEntries.size.toDouble()
                    record.duration = Instant.fromEpochSeconds((record.recentEntries.last().timestamp - record.recentEntries.first().timestamp).toLong())
                }

                records = records + record
            }
        }

        dayPlayed = records.size
    }

    fun reset() {
        records = listOf()
        dayPlayed = -1
    }
}
package com.example.data.util

import androidx.annotation.RawRes
import com.example.R

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val mood: String,
    val bpm: Int,
    val durationLabel: String,
    @RawRes val rawResId: Int,
    val keywords: List<String>
)

data class SuggestedTrack(
    val track: MusicTrack,
    val matchScore: Int,
    val reason: String
)

object MusicCatalog {

    val tracks: List<MusicTrack> = listOf(
        MusicTrack(
            id = "music_golden_hour",
            title = "Golden Hour Spark",
            artist = "Darling Studio",
            mood = "Romantic",
            bpm = 88,
            durationLabel = "0:11",
            rawResId = R.raw.music_golden_hour,
            keywords = listOf("sunset", "golden", "love", "date", "romantic", "evening", "valobasha", "bikel", "bhalobasha", "art", "photography")
        ),
        MusicTrack(
            id = "music_velvet_night",
            title = "Velvet Midnight",
            artist = "Luna Vibe",
            mood = "Night",
            bpm = 95,
            durationLabel = "0:10",
            rawResId = R.raw.music_velvet_night,
            keywords = listOf("night", "midnight", "rat", "raat", "moon", "city", "neon", "dark", "chill", "date")
        ),
        MusicTrack(
            id = "music_tropical_crush",
            title = "Tropical Crush",
            artist = "Aroha Beats",
            mood = "Upbeat",
            bpm = 108,
            durationLabel = "0:09",
            rawResId = R.raw.music_tropical_crush,
            keywords = listOf("beach", "summer", "travel", "trip", "vacation", "sun", "sea", "cox", "ghura", "happy", "khushi", "fun")
        ),
        MusicTrack(
            id = "music_neon_pulse",
            title = "Neon Pulse",
            artist = "Kairo Synth",
            mood = "Upbeat",
            bpm = 122,
            durationLabel = "0:08",
            rawResId = R.raw.music_neon_pulse,
            keywords = listOf("party", "dance", "club", "gym", "fitness", "workout", "fast", "reel", "vibe", "nach")
        ),
        MusicTrack(
            id = "music_morning_coffee",
            title = "Espresso Breeze",
            artist = "Sienna Acoustic",
            mood = "Chill",
            bpm = 84,
            durationLabel = "0:11",
            rawResId = R.raw.music_morning_coffee,
            keywords = listOf("coffee", "cafe", "morning", "sokal", "cha", "tea", "foodie", "breakfast", "chill", "relax")
        ),
        MusicTrack(
            id = "music_monsoon_love",
            title = "Monsoon Echoes",
            artist = "Megh Keys",
            mood = "Romantic",
            bpm = 78,
            durationLabel = "0:12",
            rawResId = R.raw.music_monsoon_love,
            keywords = listOf("rain", "bristi", "monsoon", "megh", "emotional", "slow", "sky", "nature", "story")
        ),
        MusicTrack(
            id = "music_city_drive",
            title = "City Lights Drive",
            artist = "Rohan Wave",
            mood = "Upbeat",
            bpm = 114,
            durationLabel = "0:08",
            rawResId = R.raw.music_city_drive,
            keywords = listOf("drive", "car", "bike", "road", "dhaka", "city", "lights", "street", "music", "ride")
        ),
        MusicTrack(
            id = "music_starlight_waltz",
            title = "Starlight Acoustic",
            artist = "Noor Ensemble",
            mood = "Acoustic",
            bpm = 92,
            durationLabel = "0:10",
            rawResId = R.raw.music_starlight_waltz,
            keywords = listOf("star", "acoustic", "guitar", "sweet", "smile", "cute", "memory", "gan", "song", "candle")
        )
    )

    val moodFilters: List<String> = listOf("All", "Romantic", "Chill", "Upbeat", "Night", "Acoustic")

    fun findById(trackId: String?): MusicTrack? =
        trackId?.let { id -> tracks.firstOrNull { it.id == id } }

    fun suggestTracks(
        caption: String,
        location: String?,
        durationMs: Long?,
        averageBrightness: Float?,
        userInterests: List<String>
    ): List<SuggestedTrack> {
        val textBlob = "${caption.lowercase()} ${location.orEmpty().lowercase()}"
        val interestSet = userInterests.map { it.lowercase() }.toSet()

        return tracks.mapIndexed { idx, track ->
            var score = 10 - idx // gentle deterministic tie-breaker
            val matchedReasons = mutableListOf<String>()

            // 1. Caption & location semantic matches
            val kwHits = track.keywords.count { kw -> textBlob.contains(kw) }
            if (kwHits > 0) {
                score += kwHits * 35
                matchedReasons += "Matches your caption & vibe"
            }

            // 2. Video duration tempo matching
            if (durationMs != null && durationMs > 0L) {
                when {
                    durationMs <= 12_000L && track.bpm >= 105 -> {
                        score += 24
                        matchedReasons += "Crisp tempo for short clip"
                    }
                    durationMs in 12_001L..28_000L && track.bpm in 88..110 -> {
                        score += 22
                        matchedReasons += "Balanced pace for your video"
                    }
                    durationMs > 28_000L && track.bpm <= 92 -> {
                        score += 22
                        matchedReasons += "Smooth soundtrack for longer clip"
                    }
                }
            }

            // 3. Visual brightness analysis from video keyframe
            if (averageBrightness != null) {
                when {
                    averageBrightness < 0.36f && (track.mood == "Night" || track.mood == "Romantic") -> {
                        score += 26
                        matchedReasons += "Tuned for low-light / night frames"
                    }
                    averageBrightness > 0.62f && (track.mood == "Upbeat" || track.mood == "Chill") -> {
                        score += 24
                        matchedReasons += "Bright daytime energy match"
                    }
                    averageBrightness in 0.36f..0.62f && (track.mood == "Romantic" || track.mood == "Acoustic") -> {
                        score += 18
                        matchedReasons += "Warm visual mood match"
                    }
                }
            }

            // 4. User profile interest affinity
            val interestMatch = track.keywords.any { kw -> interestSet.any { it.contains(kw) || kw.contains(it) } }
            if (interestMatch) {
                score += 15
                matchedReasons += "Fits your profile interests"
            }

            val reason = matchedReasons.firstOrNull() ?: "${track.mood} • ${track.bpm} BPM"
            SuggestedTrack(
                track = track,
                matchScore = score,
                reason = reason
            )
        }.sortedByDescending { it.matchScore }
    }
}

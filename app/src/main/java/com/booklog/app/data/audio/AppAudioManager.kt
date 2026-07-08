package com.booklog.app.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.booklog.app.R
import com.booklog.app.data.milestones.Milestone
import com.booklog.app.data.milestones.MilestoneCategory

enum class AppSound {
    MILESTONE_UNLOCK,
    MILESTONE_REWARDS,
    BOOK_ADDED,
    BOOK_SAVED,
    REWARD_REDEEMED,
    SCAN_SUCCESS,
}

class AppAudioManager(
    context: Context,
    private val audioPreferences: AudioPreferences,
    private val celebrationPreferences: MilestoneCelebrationPreferences,
) {
    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = mutableMapOf<AppSound, Int>()
    private var musicPlayer: MediaPlayer? = null

    init {
        soundIds[AppSound.MILESTONE_UNLOCK] = soundPool.load(appContext, R.raw.sound_milestone_unlock, 1)
        soundIds[AppSound.MILESTONE_REWARDS] = soundPool.load(appContext, R.raw.sound_milestone_rewards, 1)
        soundIds[AppSound.BOOK_ADDED] = soundPool.load(appContext, R.raw.sound_book_added, 1)
        soundIds[AppSound.BOOK_SAVED] = soundPool.load(appContext, R.raw.sound_book_saved, 1)
        soundIds[AppSound.REWARD_REDEEMED] = soundPool.load(appContext, R.raw.sound_reward_redeem, 1)
        soundIds[AppSound.SCAN_SUCCESS] = soundPool.load(appContext, R.raw.sound_scan_success, 1)
    }

    fun playSound(sound: AppSound) {
        if (audioPreferences.isSoundsMuted()) return
        val id = soundIds[sound] ?: return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun celebrateNewMilestones(milestones: List<Milestone>) {
        val unlockedIds = milestones.filter { it.isUnlocked }.map { it.id }.toSet()
        if (celebrationPreferences.isFirstCelebrationCheck() && unlockedIds.isNotEmpty()) {
            celebrationPreferences.syncCelebratedIds(unlockedIds)
            return
        }
        if (audioPreferences.isSoundsMuted()) {
            celebrationPreferences.syncCelebratedIds(unlockedIds)
            return
        }
        val newlyUnlocked = celebrationPreferences.findNewlyUnlocked(unlockedIds)
        newlyUnlocked.forEach { milestoneId ->
            val milestone = milestones.find { it.id == milestoneId } ?: return@forEach
            val sound = when (milestone.category) {
                MilestoneCategory.REWARDS -> AppSound.MILESTONE_REWARDS
                else -> AppSound.MILESTONE_UNLOCK
            }
            playSound(sound)
        }
        celebrationPreferences.syncCelebratedIds(unlockedIds)
    }

    fun startBackgroundMusic() {
        if (audioPreferences.isMusicMuted()) return
        val player = musicPlayer
        if (player?.isPlaying == true) return

        if (player == null) {
            musicPlayer = MediaPlayer.create(appContext, R.raw.music_reading_loop)?.apply {
                isLooping = true
                setVolume(0.35f, 0.35f)
                setOnCompletionListener {
                    if (!isLooping) start()
                }
            }
        }
        runCatching { musicPlayer?.start() }
    }

    fun stopBackgroundMusic() {
        musicPlayer?.runCatching {
            if (isPlaying) pause()
            seekTo(0)
        }
    }

    fun pauseBackgroundMusic() = stopBackgroundMusic()

    fun onMusicPreferenceChanged() {
        if (audioPreferences.isMusicMuted()) {
            stopBackgroundMusic()
        }
    }

    fun release() {
        stopBackgroundMusic()
        musicPlayer?.release()
        musicPlayer = null
        soundPool.release()
    }
}
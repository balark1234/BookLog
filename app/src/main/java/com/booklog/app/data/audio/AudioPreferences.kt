package com.booklog.app.data.audio

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("booklog_audio", Context.MODE_PRIVATE)

    private val _musicMuted = MutableStateFlow(prefs.getBoolean(KEY_MUSIC_MUTED, false))
    private val _soundsMuted = MutableStateFlow(prefs.getBoolean(KEY_SOUNDS_MUTED, false))

    val musicMuted: StateFlow<Boolean> = _musicMuted.asStateFlow()
    val soundsMuted: StateFlow<Boolean> = _soundsMuted.asStateFlow()

    fun isMusicMuted(): Boolean = _musicMuted.value

    fun isSoundsMuted(): Boolean = _soundsMuted.value

    fun setMusicMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_MUTED, muted).apply()
        _musicMuted.value = muted
    }

    fun setSoundsMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_SOUNDS_MUTED, muted).apply()
        _soundsMuted.value = muted
    }

    companion object {
        private const val KEY_MUSIC_MUTED = "music_muted"
        private const val KEY_SOUNDS_MUTED = "sounds_muted"
    }
}
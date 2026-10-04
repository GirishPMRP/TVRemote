package com.mytv.remote.model

import com.mytv.remote.proto.RemoteProto.RemoteKeyCode

/**
 * One button the user can place on their remote layout.
 * [id] is a stable key used for persistence and drag-reorder.
 */
enum class RemoteButton(val id: String, val label: String, val keyCode: RemoteKeyCode) {
    POWER("power", "Power", RemoteKeyCode.KEYCODE_POWER),
    HOME("home", "Home", RemoteKeyCode.KEYCODE_HOME),
    BACK("back", "Back", RemoteKeyCode.KEYCODE_BACK),
    UP("up", "Up", RemoteKeyCode.KEYCODE_UP),
    DOWN("down", "Down", RemoteKeyCode.KEYCODE_DOWN),
    LEFT("left", "Left", RemoteKeyCode.KEYCODE_LEFT),
    RIGHT("right", "Right", RemoteKeyCode.KEYCODE_RIGHT),
    SELECT("select", "OK", RemoteKeyCode.KEYCODE_ENTER),
    VOL_UP("vol_up", "Vol +", RemoteKeyCode.KEYCODE_VOLUME_UP),
    VOL_DOWN("vol_down", "Vol -", RemoteKeyCode.KEYCODE_VOLUME_DOWN),
    MUTE("mute", "Mute", RemoteKeyCode.KEYCODE_VOLUME_MUTE),
    PLAY_PAUSE("play_pause", "Play/Pause", RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE),
    REWIND("rewind", "Rewind", RemoteKeyCode.KEYCODE_MEDIA_REWIND),
    FAST_FORWARD("ff", "Forward", RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD),
    MENU("menu", "Menu", RemoteKeyCode.KEYCODE_MENU),
    SETTINGS("settings", "Settings", RemoteKeyCode.KEYCODE_SETTINGS);

    companion object {
        fun fromId(id: String): RemoteButton? = entries.find { it.id == id }

        /** Sensible default layout shown before the user customizes anything. */
        val DEFAULT_LAYOUT: List<String> = listOf(
            POWER.id, HOME.id, BACK.id,
            UP.id, LEFT.id, SELECT.id, RIGHT.id, DOWN.id,
            VOL_DOWN.id, MUTE.id, VOL_UP.id,
            REWIND.id, PLAY_PAUSE.id, FAST_FORWARD.id,
            MENU.id, SETTINGS.id
        )
    }
}

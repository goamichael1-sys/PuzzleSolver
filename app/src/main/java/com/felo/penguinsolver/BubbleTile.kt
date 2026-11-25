package com.felo.penguinsolver

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: ON/OFF for the SOLVE bubble. */
class BubbleTile : TileService() {
    override fun onStartListening() = refresh()

    override fun onClick() {
        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        p.edit().putBoolean("bubble", !p.getBoolean("bubble", false)).apply()
        refresh()
    }

    private fun refresh() {
        val on = getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("bubble", false)
        qsTile?.apply {
            label = getString(R.string.app_name)
            state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}

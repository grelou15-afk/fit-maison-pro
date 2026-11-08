package com.fitmaison.pro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        Notifier.schedule(ctx) // restaure le rappel après redémarrage / mise à jour
    }
}

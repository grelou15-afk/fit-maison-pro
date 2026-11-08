package com.fitmaison.pro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        Notifier.show(ctx, "FIT MAISON", "C’est l’heure de ta séance 💪")
        Notifier.schedule(ctx) // reprogramme pour demain
    }
}

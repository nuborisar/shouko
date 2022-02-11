package xyz.ivaniskandar.shouko.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val myIntent = Intent(context, TadanoAccessibilityService::class.java)
        context.startService(myIntent)
    }
}
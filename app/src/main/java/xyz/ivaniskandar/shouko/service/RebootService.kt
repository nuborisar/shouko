package xyz.ivaniskandar.shouko.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class RebootService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val myIntent = Intent(context, TadanoTileParentService::class.java)
        context.startService(myIntent)
    }
}
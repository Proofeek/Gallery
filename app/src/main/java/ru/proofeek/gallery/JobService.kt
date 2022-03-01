package ru.proofeek.gallery

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import androidx.preference.PreferenceManager


class JobService : JobService() {

    var sp: SharedPreferences? = null
    private var jobCancelled = false
    var bm: BatteryManager? = null
    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "Job started")
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        bm = this.getSystemService(BATTERY_SERVICE) as BatteryManager
        doBackgroundWork(params)
        return true
    }

    /**
     * Запускает MainActivity, если устройство заряжается и приложение закрыто
     */
    private fun doBackgroundWork(params: JobParameters) {
        Thread(Runnable {
            if(bm!!.isCharging && !sp!!.getBoolean("bBool",false) && !sp!!.getBoolean("active",false)){
                sp!!.edit().putBoolean("bBool", true).apply()
                //Log.e(TAG, "Intent")
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            if(!bm!!.isCharging){
                sp!!.edit().putBoolean("bBool", false).apply()
            }
            //Log.e("bBool: ", sp!!.getBoolean("bBool",false).toString())
            //Log.e("isCharhing: ", bm!!.isCharging.toString())
                jobFinished(params, true)
        }).start()
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d(TAG, "Job cancelled before completion")
        jobCancelled = true
        return true
    }

    companion object {
        private const val TAG = "JobService"
    }
}
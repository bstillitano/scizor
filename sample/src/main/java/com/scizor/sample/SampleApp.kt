package com.scizor.sample

import android.app.Application

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Scizor.start(this) is wired up in a later task.
    }
}

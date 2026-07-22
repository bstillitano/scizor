package com.scizor.sample

import android.app.Application
import com.scizor.Scizor

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scizor.start(this)
    }
}

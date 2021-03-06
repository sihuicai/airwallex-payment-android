package com.airwallex.paymentacceptance

import android.app.Application
import com.airwallex.android.Airwallex
import com.airwallex.android.AirwallexConfiguration
import com.airwallex.android.Environment

class SampleApplication : Application() {

    companion object {
        lateinit var instance: SampleApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        configAirwallex()
    }

    fun configAirwallex() {
        Airwallex.initialize(
            AirwallexConfiguration.Builder()
                .enableLogging(true) // Enable log in sdk, best set to false in release version
                .setEnvironment(if (Settings.sdkEnv == "DEMO") Environment.DEMO else Environment.PRODUCTION)
                .build()
        )
    }
}

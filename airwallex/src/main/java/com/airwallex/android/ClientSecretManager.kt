package com.airwallex.android

import com.airwallex.android.model.ClientSecret
import java.util.*
import java.util.concurrent.TimeUnit

internal class ClientSecretManager(
    private val clientSecretProvider: ClientSecretProvider
) : ClientSecretUpdateListener {
    private var clientSecretRetrieveListener: ClientSecretRetrieveListener? = null

    private var clientSecret: ClientSecret? = null

    fun retrieveClientSecret(listener: ClientSecretRetrieveListener) {
        val clientSecret = clientSecret
        clientSecretRetrieveListener = listener
        if (clientSecret == null || shouldRefreshKey(clientSecret)) {
            Logger.debug("Merchant need to create ClientSecret")
            clientSecretProvider.createClientSecret(this)
        } else {
            Logger.debug("ClientSecret not expiry, just use the last one")
            listener.onClientSecretRetrieve(clientSecret)
        }
    }

    private fun shouldRefreshKey(clientSecret: ClientSecret): Boolean {
        val now = Calendar.getInstance()
        val nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(now.timeInMillis)
        val nowPlusBuffer = nowInSeconds + CLIENT_SECRET_REFRESH_BUFFER_IN_SECONDS
        return TimeUnit.MILLISECONDS.toSeconds(clientSecret.expiredTime.time) < nowPlusBuffer
    }

    override fun onClientSecretUpdate(clientSecret: ClientSecret) {
        this.clientSecret = clientSecret
        clientSecretRetrieveListener?.onClientSecretRetrieve(clientSecret)
    }

    override fun onClientSecretUpdateFailure(message: String) {
        clientSecretRetrieveListener?.onClientSecretError(message)
    }

    internal interface ClientSecretRetrieveListener {
        fun onClientSecretRetrieve(
            clientSecret: ClientSecret
        )

        fun onClientSecretError(
            errorMessage: String
        )
    }

    companion object {
        @Volatile
        private var singleton: Singleton<ClientSecretManager>? = null

        private const val CLIENT_SECRET_REFRESH_BUFFER_IN_SECONDS = 60L

        fun get(): ClientSecretManager? {
            return singleton?.get()
        }

        fun create(clientSecretProvider: ClientSecretProvider) {
            if (singleton == null) {
                singleton = object : Singleton<ClientSecretManager>() {
                    override fun create(): ClientSecretManager {
                        return ClientSecretManager(clientSecretProvider)
                    }
                }
            }
        }
    }
}
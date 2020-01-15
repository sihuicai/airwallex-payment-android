package com.airwallex.android.exception

import java.io.IOException

class APIConnectionException(
    message: String?,
    e: Throwable?
) : AirwallexException(null, message, null, STATUS_CODE, e) {
    internal companion object {
        private const val STATUS_CODE = 0

        @JvmSynthetic
        internal fun create(e: IOException, url: String? = null): APIConnectionException {
            val displayUrl = listOfNotNull(
                "Stripe",
                "($url)".takeUnless { url.isNullOrBlank() }
            ).joinToString(" ")
            return APIConnectionException(
                "IOException during API request to $displayUrl: ${e.message}. " +
                        "Please check your internet connection and try again. " +
                        "If this problem persists, you should check Stripe's " +
                        "service status at https://twitter.com/stripestatus, " +
                        "or let us know at support@stripe.com.",
                e
            )
        }
    }
}

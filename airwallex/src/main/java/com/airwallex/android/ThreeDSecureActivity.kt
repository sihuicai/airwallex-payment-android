package com.airwallex.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity
import com.airwallex.android.ThreeDSecure.THREE_DS_RETURN_URL
import com.airwallex.android.exception.WebViewConnectionException
import com.airwallex.android.model.ThreeDSecureLookup
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import kotlinx.android.synthetic.main.activity_threeds.*
import java.net.URLEncoder

internal class ThreeDSecureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        val threeDSecureLookup: ThreeDSecureLookup =
            requireNotNull(extras?.getParcelable(EXTRA_THREE_D_SECURE_LOOKUP))

        if (threeDSecureLookup.dsData.version?.startsWith("1.") == true) {
            if (threeDSecureLookup.payload == null || threeDSecureLookup.acsUrl == null) {
                finishThreeDSecure1(null)
                return
            }

            setContentView(R.layout.activity_threeds)
            webView.webViewClient =
                ThreeDSecureWebViewClient(object : ThreeDSecureWebViewClient.Callbacks {
                    override fun onWebViewConfirmation(payload: String) {
                        finishThreeDSecure1(payload)
                    }

                    override fun onWebViewError(error: WebViewConnectionException) {
                        // Handle WebView connection failed
                        finishThreeDSecure1(null)
                    }
                })
            webView.webChromeClient = WebChromeClient()

            val payload = threeDSecureLookup.payload
            val termUrl = "$THREE_DS_RETURN_URL/$payload"
            val acsUrl = threeDSecureLookup.acsUrl
            val postData = "&PaReq=" + URLEncoder.encode(payload, "UTF-8")
                .toString() + "&TermUrl=" + URLEncoder.encode(termUrl, "UTF-8")

            webView.postUrl(acsUrl, postData.toByteArray())
        } else {
            Cardinal.getInstance().cca_continue(
                threeDSecureLookup.transactionId,
                threeDSecureLookup.payload,
                this
            ) { _, validateResponse, jwt -> finishThreeDSecure2(validateResponse, jwt) }
        }
    }

    private fun finishThreeDSecure2(validateResponse: ValidateResponse?, jwt: String?) {
        val result = Intent()
        result.putExtra(EXTRA_THREE_D_JWT, jwt)
        result.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse)

        val bundle = Bundle()
        bundle.putSerializable(
            EXTRA_THREE_D_SECURE_TYPE,
            ThreeDSecure.ThreeDSecureType.THREE_D_SECURE_2
        )
        result.putExtras(bundle)

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun finishThreeDSecure1(payload: String?) {
        val result = Intent()
        result.putExtra(EXTRA_THREE_PAYLOAD, payload)

        val bundle = Bundle()
        bundle.putSerializable(
            EXTRA_THREE_D_SECURE_TYPE,
            ThreeDSecure.ThreeDSecureType.THREE_D_SECURE_1
        )
        result.putExtras(bundle)

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        const val THREE_D_SECURE = 12345

        const val EXTRA_THREE_D_SECURE_TYPE = "EXTRA_THREE_D_SECURE_LOOKUP"

        // 2.0
        const val EXTRA_THREE_D_SECURE_LOOKUP = "EXTRA_THREE_D_SECURE_LOOKUP"
        const val EXTRA_THREE_D_JWT = "EXTRA_THREE_D_JWT"
        const val EXTRA_VALIDATION_RESPONSE = "EXTRA_VALIDATION_RESPONSE"

        // 1.0
        const val EXTRA_THREE_PAYLOAD = "EXTRA_THREE_PAYLOAD"
    }
}
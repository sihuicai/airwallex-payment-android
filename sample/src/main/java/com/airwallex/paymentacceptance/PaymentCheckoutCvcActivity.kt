package com.airwallex.paymentacceptance

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.airwallex.android.Airwallex
import com.airwallex.android.exception.AirwallexException
import com.airwallex.android.model.*
import kotlinx.android.synthetic.main.activity_confirm_cvc.*
import java.util.*

class PaymentCheckoutCvcActivity : PaymentBaseActivity() {

    private val paymentMethod: PaymentMethod by lazy {
        intent.getParcelableExtra(PAYMENT_METHOD) as PaymentMethod
    }

    private val paymentIntent: PaymentIntent by lazy {
        intent.getParcelableExtra(PAYMENT_INTENT) as PaymentIntent
    }

    override val inPaymentFlow: Boolean
        get() = true

    companion object {

        private const val TAG = "ConfirmCvcActivity"

        fun startActivityForResult(
            activity: Activity,
            paymentMethod: PaymentMethod?,
            paymentIntent: PaymentIntent,
            requestCode: Int
        ) {
            activity.startActivityForResult(
                Intent(activity, PaymentCheckoutCvcActivity::class.java)
                    .putExtra(PAYMENT_METHOD, paymentMethod)
                    .putExtra(PAYMENT_INTENT, paymentIntent),
                requestCode
            )
        }
    }

    @SuppressLint("DefaultLocale")
    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_cvc)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.cvc_hint)
        }

        tvTitle.text = getString(
            R.string.enter_cvc_title,
            paymentMethod.card?.brand?.capitalize(Locale.ENGLISH),
            paymentMethod.card?.last4
        )

        rlPlay.isEnabled = false
        atlCardCvc.completionCallback = {
            rlPlay.isEnabled = true
        }

        rlPlay.setOnClickListener {
            if (atlCardCvc.value.isEmpty()) {
                atlCardCvc.error = resources.getString(R.string.empty_cvc)
                return@setOnClickListener
            }

            if (!atlCardCvc.isValid) {
                atlCardCvc.error = resources.getString(R.string.invalid_cvc)
                return@setOnClickListener
            }

            loading.visibility = View.VISIBLE
            startConfirmPaymentIntent(paymentMethod, atlCardCvc.value)
            return@setOnClickListener
        }
    }

    private fun startConfirmPaymentIntent(paymentMethod: PaymentMethod, cvc: String) {
        val paymentIntentParams: PaymentIntentParams
        val paymentMethodOptions: PaymentMethodOptions = PaymentMethodOptions.Builder()
            .setCardOptions(
                PaymentMethodOptions.CardOptions.Builder()
                    .setAutoCapture(true)
                    .setThreeDs(
                        PaymentMethodOptions.CardOptions.ThreeDs.Builder()
                            .setOption(false)
                            .build()
                    ).build()
            )
            .build()


        paymentIntentParams = PaymentIntentParams.Builder()
            .setRequestId(UUID.randomUUID().toString())
            .setDevice(PaymentData.device)
            .setPaymentMethodReference(
                PaymentMethodReference.Builder()
                    .setId(paymentMethod.id)
                    .setCvc(cvc)
                    .build()
            )
            .setPaymentMethodOptions(paymentMethodOptions)
            .build()

        // Start Confirm PaymentIntent
        val airwallex = Airwallex(Store.token, paymentIntent.clientSecret!!)
        airwallex.confirmPaymentIntent(
            paymentIntentId = paymentIntent.id!!,
            paymentIntentParams = paymentIntentParams,
            callback = object : Airwallex.PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    retrievePaymentIntent(airwallex)
                }

                override fun onFailed(exception: AirwallexException) {
                    loading.visibility = View.GONE
                    Toast.makeText(
                        this@PaymentCheckoutCvcActivity,
                        exception.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        )
    }

    private fun retrievePaymentIntent(airwallex: Airwallex) {
        Log.d(
            TAG,
            "Start retrieve PaymentIntent ${paymentIntent.id}"
        )
        airwallex.retrievePaymentIntent(
            paymentIntentId = paymentIntent.id!!,
            callback = object : Airwallex.PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Log.d(
                        TAG,
                        "Retrieve PaymentIntent success, PaymentIntent status: ${paymentIntent.status}"
                    )

                    loading.visibility = View.GONE
                    if (paymentIntent.status == "SUCCEEDED") {
                        showPaymentSuccess()
                    } else {
                        showPaymentError()
                    }
                }

                override fun onFailed(exception: AirwallexException) {
                    Log.e(TAG, "Retrieve PaymentIntent failed")
                    loading.visibility = View.GONE

                    // TODO Need Retry?
                    showPaymentError()
                }
            })
    }
}
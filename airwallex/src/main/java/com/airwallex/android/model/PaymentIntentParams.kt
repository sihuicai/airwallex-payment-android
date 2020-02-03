package com.airwallex.android.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaymentIntentParams internal constructor(
    @SerializedName("request_id")
    val requestId: String? = null,

    @SerializedName("payment_method")
    val paymentMethod: PaymentMethod? = null,

    @SerializedName("device")
    val device: Device? = null
) : AirwallexModel, Parcelable {

    class Builder : ObjectBuilder<PaymentIntentParams> {
        private var requestId: String? = null
        private var paymentMethod: PaymentMethod? = null
        private var device: Device? = null

        fun setRequestId(requestId: String?): Builder = apply {
            this.requestId = requestId
        }

        fun setPaymentMethod(paymentMethod: PaymentMethod?): Builder = apply {
            this.paymentMethod = paymentMethod
        }

        fun setDevice(device: Device?): Builder = apply {
            this.device = device
        }

        override fun build(): PaymentIntentParams {
            return PaymentIntentParams(
                requestId = requestId,
                paymentMethod = paymentMethod,
                device = device
            )
        }
    }
}
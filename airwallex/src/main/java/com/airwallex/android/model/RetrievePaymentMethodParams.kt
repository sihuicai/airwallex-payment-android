package com.airwallex.android.model

import java.util.*

/**
 * The params that used for retrieve [PaymentMethod]
 */
internal data class RetrievePaymentMethodParams internal constructor(
    override val customerId: String,
    override val clientSecret: String,
    /**
     * Page number starting from 0
     */
    internal val pageNum: Int,
    /**
     * Number of payment methods to be listed per page
     */
    internal val pageSize: Int,
    /**
     * The start time of created_at in ISO8601 format
     */
    internal val fromCreatedAt: Date? = null,
    /**
     * The end time of created_at in ISO8601 format
     */
    internal val toCreatedAt: Date? = null,
    /**
     * Payment method type
     */
    internal val type: AvaliablePaymentMethodType

) : AbstractPaymentMethodParams(customerId = customerId, clientSecret = clientSecret) {

    class Builder(
        private val customerId: String,
        private val clientSecret: String,
        private val type: AvaliablePaymentMethodType,
        private val pageNum: Int
    ) : ObjectBuilder<RetrievePaymentMethodParams> {

        private var pageSize: Int = 20

        private var fromCreatedAt: Date? = null

        private var toCreatedAt: Date? = null

        fun setPageSize(pageSize: Int): Builder = apply {
            this.pageSize = pageSize
        }

        fun setFromCreatedAt(fromCreatedAt: Date?): Builder = apply {
            this.fromCreatedAt = fromCreatedAt
        }

        fun setToCreatedAt(toCreatedAt: Date?): Builder = apply {
            this.toCreatedAt = toCreatedAt
        }

        override fun build(): RetrievePaymentMethodParams {
            return RetrievePaymentMethodParams(
                customerId = customerId,
                clientSecret = clientSecret,
                pageNum = pageNum,
                pageSize = pageSize,
                fromCreatedAt = fromCreatedAt,
                toCreatedAt = toCreatedAt,
                type = type
            )
        }
    }
}

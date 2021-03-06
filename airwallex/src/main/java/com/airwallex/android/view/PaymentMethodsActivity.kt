package com.airwallex.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airwallex.android.Airwallex
import com.airwallex.android.ClientSecretRepository
import com.airwallex.android.R
import com.airwallex.android.model.RetrievePaymentMethodParams
import com.airwallex.android.model.*
import kotlinx.android.synthetic.main.activity_payment_methods.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Allow the customer to select one of the payment methods, or add a new one via [AddPaymentMethodActivity].
 *
 */
internal class PaymentMethodsActivity : AirwallexCheckoutBaseActivity() {

    private var pageNum: AtomicInteger = AtomicInteger(0)

    private lateinit var paymentMethodsAdapter: PaymentMethodsAdapter

    private val args: PaymentMethodsActivityLaunch.Args by lazy {
        PaymentMethodsActivityLaunch.Args.getExtra(intent)
    }

    override val paymentIntent: PaymentIntent by lazy {
        args.paymentIntent
    }

    override val cvc: String?
        get() = null

    override val airwallex: Airwallex by lazy {
        Airwallex(this)
    }

    private val shouldShowCard: Boolean by lazy {
        paymentIntent.availablePaymentMethodTypes?.contains(AvaliablePaymentMethodType.CARD) == true
    }

    private val availableThirdPaymentTypes by lazy {
        paymentIntent.availablePaymentMethodTypes?.filter { it != AvaliablePaymentMethodType.CARD && AvaliablePaymentMethodType.values().contains(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        paymentMethodsAdapter = PaymentMethodsAdapter(
            availableThirdPaymentTypes = availableThirdPaymentTypes ?: emptyList(),
            shouldShowCard = shouldShowCard
        )

        paymentMethodsAdapter.listener = object : PaymentMethodsAdapter.Listener {
            override fun onPaymentMethodClick(paymentMethod: PaymentMethod) {
                startPaymentCheckout(paymentMethod)
            }
        }

        addPaymentMethod.visibility = if (shouldShowCard) View.VISIBLE else View.GONE
        addPaymentMethod.setOnClickListener {
            startAddPaymentMethod()
        }

        rvPaymentMethods.apply {
            layoutManager = viewManager
            adapter = paymentMethodsAdapter

            addItemDecoration(
                PaymentMethodsDividerItemDecoration(
                    this@PaymentMethodsActivity,
                    R.drawable.airwallex_line_divider,
                    availableThirdPaymentTypeSize = availableThirdPaymentTypes?.size ?: 0
                )
            )
        }

        paymentMethodsAdapter.onLoadMoreCallback = {
            fetchPaymentMethods()
        }
        paymentMethodsAdapter.addOnScrollListener(rvPaymentMethods)

        fetchPaymentMethods()
    }

    override val layoutResource: Int
        get() = R.layout.activity_payment_methods

    private fun fetchPaymentMethods() {
        if (!shouldShowCard) {
            return
        }

        paymentMethodsAdapter.startLoadingMore(rvPaymentMethods)
        ClientSecretRepository.getInstance().retrieveClientSecret(
            requireNotNull(paymentIntent.customerId),
            object : ClientSecretRepository.ClientSecretRetrieveListener {
                override fun onClientSecretRetrieve(clientSecret: ClientSecret) {
                    airwallex.retrievePaymentMethods(
                        params = RetrievePaymentMethodParams.Builder(
                            customerId = requireNotNull(paymentIntent.customerId),
                            clientSecret = requireNotNull(clientSecret.value),
                            type = AvaliablePaymentMethodType.CARD,
                            pageNum = pageNum.get()
                        )
                            .build(),
                        listener = object : Airwallex.PaymentListener<PaymentMethodResponse> {
                            override fun onSuccess(response: PaymentMethodResponse) {
                                paymentMethodsAdapter.endLoadingMore()
                                paymentMethodsAdapter.setPaymentMethods(response.items, response.hasMore)
                                paymentNoCards.visibility = if (paymentMethodsAdapter.isEmpty()) View.VISIBLE else View.GONE
                                pageNum.incrementAndGet()
                            }

                            override fun onFailed(exception: Exception) {
                                alert(message = exception.message ?: exception.toString())
                                paymentMethodsAdapter.setPaymentMethods(arrayListOf(), false)
                                paymentMethodsAdapter.endLoadingMore()
                            }
                        }
                    )
                }

                override fun onClientSecretError(errorMessage: String) {
                    alert(message = errorMessage)
                }
            }
        )
    }

    override fun homeAsUpIndicatorResId(): Int {
        return R.drawable.airwallex_ic_close
    }

    private fun startAddPaymentMethod() {
        AddPaymentMethodActivityLaunch(this@PaymentMethodsActivity)
            .startForResult(
                AddPaymentMethodActivityLaunch.Args
                    .Builder()
                    .setShipping(paymentIntent.order.shipping)
                    .setCustomerId(requireNotNull(paymentIntent.customerId))
                    .setClientSecret(requireNotNull(paymentIntent.clientSecret))
                    .build()
            )
    }

    private fun startPaymentCheckout(paymentMethod: PaymentMethod, cvc: String? = null) {
        if (args.includeCheckoutFlow) {
            when (paymentMethod.type) {
                PaymentMethodType.CARD -> {
                    // Start `PaymentCheckoutActivity` to confirm `PaymentIntent`
                    PaymentCheckoutActivityLaunch(this@PaymentMethodsActivity)
                        .startForResult(
                            PaymentCheckoutActivityLaunch.Args.Builder()
                                .setPaymentIntent(paymentIntent)
                                .setPaymentMethod(paymentMethod)
                                .setCvc(cvc)
                                .build()
                        )
                }
                else -> {
                    confirmPaymentIntent(
                        paymentMethod = paymentMethod,
                        listener = object : Airwallex.PaymentListener<PaymentIntent> {
                            override fun onSuccess(response: PaymentIntent) {
                                finishWithPaymentIntent(paymentIntent = response)
                            }

                            override fun onFailed(exception: Exception) {
                                finishWithPaymentIntent(exception = exception)
                            }
                        }
                    )
                }
            }
        } else {
            // Return the `PaymentMethod` & 'cvc' to merchant
            finishWithPaymentMethod(
                paymentMethod = paymentMethod,
                cvc = cvc
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }
        when (requestCode) {
            AddPaymentMethodActivityLaunch.REQUEST_CODE -> {
                val result = AddPaymentMethodActivityLaunch.Result.fromIntent(data)
                result?.let {
                    paymentMethodsAdapter.addNewPaymentMethod(it.paymentMethod)
                    paymentNoCards.visibility =
                        if (paymentMethodsAdapter.isEmpty()) View.VISIBLE else View.GONE
                    rvPaymentMethods.requestLayout()
                    startPaymentCheckout(it.paymentMethod, it.cvc)
                }
            }
            PaymentCheckoutActivityLaunch.REQUEST_CODE -> {
                val result = PaymentCheckoutActivityLaunch.Result.fromIntent(data)
                result?.let {
                    finishWithPaymentIntent(it.paymentIntent, it.exception)
                }
            }
        }
    }

    private fun finishWithPaymentMethod(
        paymentMethod: PaymentMethod,
        cvc: String?
    ) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                PaymentMethodsActivityLaunch.Result(
                    paymentMethod = paymentMethod,
                    cvc = cvc,
                    includeCheckoutFlow = args.includeCheckoutFlow
                ).toBundle()
            )
        )
        finish()
    }

    private fun finishWithPaymentIntent(
        paymentIntent: PaymentIntent? = null,
        exception: Exception? = null
    ) {
        setLoadingProgress(false)
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                PaymentMethodsActivityLaunch.Result(
                    paymentIntent = paymentIntent,
                    exception = exception,
                    includeCheckoutFlow = args.includeCheckoutFlow
                ).toBundle()
            )
        )
        finish()
    }
}

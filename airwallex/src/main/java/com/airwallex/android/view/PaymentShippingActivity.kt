package com.airwallex.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.airwallex.android.R
import com.airwallex.android.model.Shipping
import kotlinx.android.synthetic.main.activity_add_shipping.*

/**
 * Activity to edit shipping address
 */
internal class PaymentShippingActivity : AirwallexActivity() {

    private val args: PaymentShippingActivityLaunch.Args by lazy {
        PaymentShippingActivityLaunch.Args.getExtra(intent)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_save)?.isEnabled =
            contactWidget.isValidContact && shippingWidget.isValidShipping
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.airwallex_menu_save, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args.shipping?.let {
            contactWidget.initializeView(it)
            shippingWidget.initializeView(it)
        }

        contactWidget.contactChangeCallback = {
            invalidateOptionsMenu()
        }

        shippingWidget.shippingChangeCallback = {
            invalidateOptionsMenu()
        }
    }

    override val layoutResource: Int
        get() = R.layout.activity_add_shipping

    override fun onActionSave() {
        val contact = contactWidget.shippingContact
        val shipping = Shipping.Builder()
            .setLastName(contact.first)
            .setFirstName(contact.second)
            .setPhone(contact.third)
            .setAddress(shippingWidget.address)
            .build()
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                PaymentShippingActivityLaunch.Result(
                    shipping = shipping
                ).toBundle()
            )
        )
        finish()
    }

    override fun homeAsUpIndicatorResId(): Int {
        return R.drawable.airwallex_ic_back
    }
}

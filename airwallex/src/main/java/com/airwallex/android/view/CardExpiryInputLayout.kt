package com.airwallex.android.view

import android.content.Context
import android.util.AttributeSet
import com.airwallex.android.R

internal class CardExpiryTextInputLayout constructor(
    context: Context,
    attrs: AttributeSet
) : AirwallexTextInputLayout(context, attrs, R.layout.card_expiry_input_layout) {

    internal var completionCallback: () -> Unit = {}
        set(value) {
            (teInput as CardExpiryEditText).completionCallback = value
            field = value
        }

    internal val isValid: Boolean
        get() = (teInput as CardExpiryEditText).isDateValid

    internal val validDateFields: Pair<Int, Int>?
        get() = (teInput as CardExpiryEditText).validDateFields

    init {
        val input = teInput as CardExpiryEditText
        input.errorCallback = { showError ->
            error = if (showError) {
                resources.getString(R.string.invalid_expiry_date)
            } else {
                null
            }
        }
    }
}
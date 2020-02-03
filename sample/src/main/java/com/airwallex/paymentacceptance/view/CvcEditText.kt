package com.airwallex.paymentacceptance.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.View
import com.airwallex.android.model.PaymentMethod.Card.Companion.CVC_LENGTH
import com.airwallex.paymentacceptance.R

class CvcEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AirwallexEditText(context, attrs, defStyleAttr) {

    internal val cvcValue: String?
        get() {
            return rawCvcValue.takeIf { isValid }
        }

    internal var completionCallback: () -> Unit = {}

    private val rawCvcValue: String
        get() {
            return text.toString().trim()
        }

    private val isValid: Boolean
        get() {
            return rawCvcValue.length == CVC_LENGTH

        }

    init {
        setHint(R.string.cvc_hint)
        maxLines = 1
        filters = arrayOf(InputFilter.LengthFilter(CVC_LENGTH))

        inputType = InputType.TYPE_NUMBER_VARIATION_PASSWORD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        }

        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isValid) {
                    completionCallback()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }
}
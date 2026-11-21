package com.novacorp.inmonode_app.features.fieldsales.domain.model

import com.novacorp.inmonode_app.core.format.formatMoney
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Special conditions authorized by management for a separation (US-58). Only the discount changes the price;
 * the bonus (for example "cerco perimétrico") and the notes describe what was agreed with the buyer.
 */
data class NegotiationTerms(val discount: BigDecimal? = null, val bonus: String? = null, val notes: String? = null) {

    fun finalPrice(lotPrice: BigDecimal): BigDecimal = lotPrice - (discount ?: BigDecimal.ZERO)

    /** Null when the terms are valid for a lot of [lotPrice]; otherwise the message that tells the agent the valid range. */
    fun validationError(lotPrice: BigDecimal, currency: String): String? = when {
        discount != null && (discount < BigDecimal.ZERO || discount >= lotPrice) ->
            "El descuento debe estar entre ${formatMoney(currency, BigDecimal.ZERO)} y ${formatMoney(currency, maxDiscount(lotPrice))}."
        discount != null && discount.stripTrailingZeros().scale() > 2 -> "El descuento debe tener como máximo dos decimales."
        (bonus?.length ?: 0) > MAX_BONUS_LENGTH -> "El bono debe tener como máximo $MAX_BONUS_LENGTH caracteres."
        (notes?.length ?: 0) > MAX_NOTES_LENGTH -> "Las observaciones deben tener como máximo $MAX_NOTES_LENGTH caracteres."
        else -> null
    }

    companion object {
        const val MAX_BONUS_LENGTH = 150
        const val MAX_NOTES_LENGTH = 500
        private val CENT = BigDecimal("0.01")

        /** The largest discount a lot of [lotPrice] accepts: the final price must stay positive. */
        fun maxDiscount(lotPrice: BigDecimal): BigDecimal = (lotPrice - CENT).setScale(2, RoundingMode.DOWN)

        /** Builds the terms from the wizard fields: blank text means "not agreed" and is stored as null. */
        fun of(discount: BigDecimal?, bonus: String?, notes: String?) = NegotiationTerms(discount,
            bonus?.trim()?.takeIf { it.isNotEmpty() }, notes?.trim()?.takeIf { it.isNotEmpty() })
    }
}

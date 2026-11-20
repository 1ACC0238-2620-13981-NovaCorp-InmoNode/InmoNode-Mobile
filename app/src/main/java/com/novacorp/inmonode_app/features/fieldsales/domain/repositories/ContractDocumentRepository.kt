package com.novacorp.inmonode_app.features.fieldsales.domain.repositories

import com.novacorp.inmonode_app.features.fieldsales.domain.model.ContractTemplate
import java.io.File

/** The preliminary contract template in force and the PDFs generated from it on the device (US-13). */
interface ContractDocumentRepository {
    /** The template in force; until the backend sends one with the portfolio, the one shipped with the app. */
    suspend fun template(): ContractTemplate

    /** Writes [contract] as the PDF of [reservationId], replacing the previous one; [footer] gets the page and the page count. */
    suspend fun render(reservationId: String, contract: ContractTemplate, footer: (page: Int, pages: Int) -> String): ContractPdf

    /** Removes the PDF of [reservationId], if any, because it holds the buyer's data. */
    suspend fun delete(reservationId: String)
}

data class ContractPdf(val file: File, val pages: Int)

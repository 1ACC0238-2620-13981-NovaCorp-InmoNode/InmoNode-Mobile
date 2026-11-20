package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.ContractTemplate
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractDocumentRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractPdf
import java.io.File

/** Reads the bundled template and records the PDFs it would write or delete. */
internal class ContractDocumentsFake : ContractDocumentRepository {
    val rendered = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    override suspend fun template() = ContractTemplate.parse(File("src/main/assets/contracts/preliminary-separation.txt").readText())
    override suspend fun render(reservationId: String, contract: ContractTemplate, footer: (Int, Int) -> String): ContractPdf {
        rendered += reservationId
        return ContractPdf(File("$reservationId.pdf"), 1)
    }
    override suspend fun delete(reservationId: String) { deleted += reservationId }
}

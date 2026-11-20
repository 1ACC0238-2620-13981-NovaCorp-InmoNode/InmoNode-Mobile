package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.model.ContractField
import com.novacorp.inmonode_app.features.fieldsales.domain.model.PreliminaryContract
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractDocumentRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractPdf
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** What the contract step shows: the critical data still missing (US-13 scenario 2) or the PDF generated on the device. */
sealed interface ContractPreview {
    data class Missing(val fields: List<ContractField>) : ContractPreview
    data class Ready(val pdf: ContractPdf, val templateVersion: String) : ContractPreview
}

/** Generates the preliminary contract offline from the separation stored on the device. It has no legal value and is not synced. */
class PreviewContractUseCase internal constructor(private val documents: ContractDocumentRepository,
    private val now: () -> ZonedDateTime) {

    @Inject constructor(documents: ContractDocumentRepository) : this(documents, { ZonedDateTime.now() })

    suspend operator fun invoke(contract: PreliminaryContract): Result<ContractPreview> = runCatching {
        val missing = contract.missing
        if (missing.isNotEmpty()) return@runCatching ContractPreview.Missing(missing)
        val time = now()
        val template = documents.template()
        val filled = template.fill(contract.values(time.zone))
        val generated = GENERATED.format(time)
        val name = "contrato-preliminar-${requireNotNull(contract.lot).code}"
        val pdf = documents.render(requireNotNull(contract.reservation).id, name, filled) { page, pages ->
            "Borrador sin valor legal · Plantilla v${template.version} · Generado sin conexión el $generated · Página $page de $pages"
        }
        ContractPreview.Ready(pdf, template.version)
    }

    private companion object {
        val GENERATED: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
    }
}

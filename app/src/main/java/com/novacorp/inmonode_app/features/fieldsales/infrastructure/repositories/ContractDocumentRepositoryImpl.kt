package com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories

import android.content.Context
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ContractTemplate
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractDocumentRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractPdf
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.contract.ContractPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the template shipped in the app's assets and keeps one PDF per separation in the cache (US-13). */
@Singleton
class ContractDocumentRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : ContractDocumentRepository {

    override suspend fun template(): ContractTemplate = withContext(Dispatchers.IO) {
        context.assets.open(TEMPLATE).bufferedReader(Charsets.UTF_8).use { ContractTemplate.parse(it.readText()) }
    }

    override suspend fun render(reservationId: String, name: String, contract: ContractTemplate,
        footer: (page: Int, pages: Int) -> String): ContractPdf = withContext(Dispatchers.IO) {
        val folder = folder(reservationId)
        folder.mkdirs()
        val file = File(folder, "${fileName(name)}.pdf")
        val temp = File(folder, "${file.name}.tmp")
        try {
            val pages = temp.outputStream().use { ContractPdfWriter.write(contract, footer, it) }
            check(temp.renameTo(file)) { "No se pudo guardar el contrato preliminar." }
            // One PDF per separation, even when its name changes.
            folder.listFiles()?.filter { it != file }?.forEach { it.delete() }
            ContractPdf(file, pages)
        } finally {
            temp.delete()
        }
    }

    override suspend fun delete(reservationId: String) {
        withContext(Dispatchers.IO) { folder(reservationId).deleteRecursively() }
    }

    /** Each separation keeps its PDF in its own folder, so the file itself can carry a readable name. */
    private fun folder(reservationId: String): File {
        require(SAFE_ID.matches(reservationId)) { "La separación no es válida." }
        return File(File(context.cacheDir, DIRECTORY), reservationId)
    }

    companion object {
        const val TEMPLATE = "contracts/preliminary-separation.txt"
        /** Cache folder of the generated PDFs; the share provider exposes only this folder. */
        const val DIRECTORY = "contracts"
        private val SAFE_ID = Regex("[A-Za-z0-9-]{1,64}")
        private val UNSAFE_NAME = Regex("[^A-Za-z0-9-]+")

        /** Letters, digits and dashes only, so a lot code cannot leave the folder or break the shared name. */
        internal fun fileName(name: String): String =
            name.replace(UNSAFE_NAME, "-").trim('-').take(80).ifEmpty { "contrato-preliminar" }
    }
}

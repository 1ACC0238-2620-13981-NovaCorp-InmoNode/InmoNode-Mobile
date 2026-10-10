package com.novacorp.inmonode_app.features.iam

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SessionBackupTest {
    @Test fun sessionTokensAreExcludedFromAllBackupModes() {
        for ((file, count) in listOf("backup_rules.xml" to 1, "data_extraction_rules.xml" to 2)) {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File("src/main/res/xml/$file"))
            val nodes = document.getElementsByTagName("exclude")
            val matching = (0 until nodes.length).count {
                val attributes = nodes.item(it).attributes
                attributes.getNamedItem("domain")?.nodeValue == "file" &&
                    attributes.getNamedItem("path")?.nodeValue == "datastore/session.preferences_pb"
            }
            assertEquals(file, count, matching)
        }
    }
}

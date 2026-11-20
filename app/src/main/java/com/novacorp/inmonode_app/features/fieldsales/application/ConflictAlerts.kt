package com.novacorp.inmonode_app.features.fieldsales.application

/** Tells the agent about lot conflicts found while syncing in the background, with the app closed or elsewhere (US-12). */
interface ConflictAlerts {
    fun conflictsDetected(count: Int)
}

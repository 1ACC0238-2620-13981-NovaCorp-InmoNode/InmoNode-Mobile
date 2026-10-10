package com.novacorp.inmonode_app.core.designsystem.components
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
fun displayTimestamp(value:String?):String = value?.let {
 runCatching { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",Locale.forLanguageTag("es-PE"))
  .withZone(ZoneId.systemDefault()).format(Instant.parse(it)) }.getOrDefault(it)
}.orEmpty()

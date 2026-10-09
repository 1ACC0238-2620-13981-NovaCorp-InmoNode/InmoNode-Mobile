package com.novacorp.inmonode_app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.novacorp.inmonode_app.R

// Variable fonts: weights are applied through the "wght" axis (API 26+).
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val JosefinSans = FontFamily(
    variableFont(R.font.josefin_sans, FontWeight.Normal),
    variableFont(R.font.josefin_sans, FontWeight.Medium),
    variableFont(R.font.josefin_sans, FontWeight.SemiBold),
    variableFont(R.font.josefin_sans, FontWeight.Bold)
)

val Montserrat = FontFamily(
    variableFont(R.font.montserrat, FontWeight.Normal),
    variableFont(R.font.montserrat, FontWeight.Medium),
    variableFont(R.font.montserrat, FontWeight.SemiBold),
    variableFont(R.font.montserrat, FontWeight.Bold)
)

private val baseline = Typography()

// Headings: Josefin Sans (Heading One 40, Heading Two 32). Body and labels: Montserrat (body 14).
val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = JosefinSans),
    displayMedium = baseline.displayMedium.copy(fontFamily = JosefinSans),
    displaySmall = baseline.displaySmall.copy(fontFamily = JosefinSans),
    headlineLarge = baseline.headlineLarge.copy(
        fontFamily = JosefinSans,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp
    ),
    headlineMedium = baseline.headlineMedium.copy(
        fontFamily = JosefinSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = JosefinSans, fontWeight = FontWeight.SemiBold),
    titleLarge = baseline.titleLarge.copy(fontFamily = JosefinSans, fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = Montserrat),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = Montserrat),
    bodySmall = baseline.bodySmall.copy(fontFamily = Montserrat),
    labelLarge = baseline.labelLarge.copy(fontFamily = Montserrat, fontWeight = FontWeight.SemiBold),
    labelMedium = baseline.labelMedium.copy(fontFamily = Montserrat),
    labelSmall = baseline.labelSmall.copy(fontFamily = Montserrat)
)

package com.example.un_signed

import kotlin.math.roundToInt

/** One personalised impact line — colour-coded severity for UI. */
data class ImpactLine(
    val severity: Severity,     // Ok / Watch / Bad / Critical
    val system: String,         // "Cardiovascular", "Metabolic", "Gut" etc.
    val message: String,        // one-sentence effect
    val evidence: String        // source citation
) {
    enum class Severity { Ok, Watch, Bad, Critical }
}

/** Personalised, evidence-based effect card for a single junk item. */
data class JunkImpact(
    val overallSeverity: ImpactLine.Severity,
    val headline: String,                    // 1-line summary
    val burnoffMinutes: Int,                 // minutes of walking to burn calories
    val lines: List<ImpactLine>
)

/**
 * Given a product from Open Food Facts and the user's profile, produce a
 * personalised health-impact narrative. Rules based on:
 *   • WHO 2015 free-sugar guideline (≤ 10 % kcal, target ≤ 5 %)
 *   • WHO 2020 sat-fat / trans-fat / salt targets
 *   • BMJ 2019 ultra-processed foods (Monteiro NOVA-4)
 *   • Basal metabolism / walking-energy conversion (0.06 kcal/kg/min at 4 km/h)
 */
object JunkImpactAnalyzer {

    fun analyse(product: OffProduct, servingGrams: Int, profile: UserProfile): JunkImpact {
        val serving = servingGrams.coerceAtLeast(1).toDouble()
        val scale = serving / 100.0

        val kcal = (product.energyKcal100g * scale).roundToInt()
        val sugarG = product.sugar100g * scale
        val satFatG = product.saturatedFat100g * scale
        val saltG = product.salt100g * scale
        val fiberG = product.fiber100g * scale

        // Daily limits derived from user profile
        val tdee = profile.tdee.coerceAtLeast(1600.0)
        val sugarLimitG   = tdee * 0.10 / 4.0   // WHO 10 % kcal → g (4 kcal/g)
        val sugarTargetG  = tdee * 0.05 / 4.0   // WHO strong recommendation ≤ 5 %
        val satFatLimitG  = tdee * 0.10 / 9.0   // ≤ 10 % kcal (WHO)
        val saltLimitG    = 5.0                 // WHO 2013 salt limit
        val weightKg      = profile.weightKg.coerceAtLeast(50.0)
        val burnMin       = if (kcal > 0) (kcal / (0.06 * weightKg)).roundToInt() else 0

        val lines = mutableListOf<ImpactLine>()

        // ── NOVA ultra-processed classification ──────────────────
        when (product.novaGroup) {
            4 -> lines += ImpactLine(
                ImpactLine.Severity.Bad,
                "Ultra-Processed",
                "NOVA 4 — industrial formulation linked to metabolic syndrome and CVD.",
                "BMJ 2019 · Monteiro"
            )
            3 -> lines += ImpactLine(
                ImpactLine.Severity.Watch,
                "Processed",
                "NOVA 3 — moderate processing; use as an occasional food.",
                "NOVA classification"
            )
            1, 2 -> lines += ImpactLine(
                ImpactLine.Severity.Ok,
                "Minimally processed",
                "NOVA ${product.novaGroup} — not classified as junk.",
                "NOVA classification"
            )
        }

        // ── Sugar ────────────────────────────────────────────────
        if (sugarG > 0) {
            val pctOfLimit = sugarG / sugarLimitG * 100
            val sev = when {
                sugarG >= sugarLimitG -> ImpactLine.Severity.Critical
                sugarG >= sugarTargetG -> ImpactLine.Severity.Bad
                sugarG >= sugarTargetG * 0.5 -> ImpactLine.Severity.Watch
                else -> ImpactLine.Severity.Ok
            }
            lines += ImpactLine(
                sev,
                "Blood sugar",
                "%.1f g sugar — %.0f%% of your WHO daily limit; spikes insulin & liver load.".format(sugarG, pctOfLimit),
                "WHO 2015 free-sugars"
            )
        }

        // ── Saturated fat ────────────────────────────────────────
        if (satFatG > 0.5) {
            val sev = when {
                satFatG >= satFatLimitG * 0.5 -> ImpactLine.Severity.Bad
                satFatG >= satFatLimitG * 0.25 -> ImpactLine.Severity.Watch
                else -> ImpactLine.Severity.Ok
            }
            lines += ImpactLine(
                sev,
                "Cardiovascular",
                "%.1f g saturated fat — raises LDL cholesterol over repeated exposure.".format(satFatG),
                "WHO 2020 sat-fat guideline"
            )
        }

        // ── Salt / sodium ────────────────────────────────────────
        if (saltG > 0.3) {
            val pctOfLimit = saltG / saltLimitG * 100
            val sev = when {
                saltG >= saltLimitG * 0.4 -> ImpactLine.Severity.Bad
                saltG >= saltLimitG * 0.2 -> ImpactLine.Severity.Watch
                else -> ImpactLine.Severity.Ok
            }
            lines += ImpactLine(
                sev,
                "Blood pressure & kidneys",
                "%.1f g salt — %.0f%% of your 5 g/day WHO limit; strains BP & kidneys.".format(saltG, pctOfLimit),
                "WHO 2013 salt reduction"
            )
        }

        // ── Additives ────────────────────────────────────────────
        if (product.additivesCount >= 5) {
            lines += ImpactLine(
                ImpactLine.Severity.Bad,
                "Gut microbiome",
                "${product.additivesCount} additives — emulsifiers & artificial colours disrupt gut flora.",
                "Nature 2015 · Chassaing"
            )
        } else if (product.additivesCount in 2..4) {
            lines += ImpactLine(
                ImpactLine.Severity.Watch,
                "Gut microbiome",
                "${product.additivesCount} additives detected — moderate load on gut flora.",
                "Nature 2015 · Chassaing"
            )
        }

        // ── Fibre (positive counter-signal) ──────────────────────
        if (fiberG > 3.0) {
            lines += ImpactLine(
                ImpactLine.Severity.Ok,
                "Digestion",
                "%.1f g fibre helps offset sugar spike & feeds gut microbes.".format(fiberG),
                "USDA fibre research"
            )
        }

        // Overall severity = worst line
        val overall = lines.maxByOrNull {
            when (it.severity) {
                ImpactLine.Severity.Critical -> 4
                ImpactLine.Severity.Bad -> 3
                ImpactLine.Severity.Watch -> 2
                ImpactLine.Severity.Ok -> 1
            }
        }?.severity ?: ImpactLine.Severity.Ok

        val headline = when (overall) {
            ImpactLine.Severity.Critical -> "Heavy hit — treat as a rare indulgence."
            ImpactLine.Severity.Bad      -> "Genuine junk — track it and cap the frequency."
            ImpactLine.Severity.Watch    -> "Some warning signals — okay occasionally."
            ImpactLine.Severity.Ok       -> "Not really junk — go ahead."
        }

        return JunkImpact(
            overallSeverity = overall,
            headline = headline,
            burnoffMinutes = burnMin,
            lines = lines
        )
    }
}

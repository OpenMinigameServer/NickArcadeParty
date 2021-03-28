package io.github.openminigameserver.nickarcade.party.model.misc

import io.github.openminigameserver.hypixelapi.models.HypixelPackageRank


annotation class PartySetting(
    val description: String,
    val requiredRank: HypixelPackageRank = HypixelPackageRank.NONE,
    vararg val aliases: String
)

package org.dasien.more_cfg_for_ae2.compat;

import net.neoforged.fml.ModList;

public final class LoadedMods {
    public static final String EX_PATTERN_PROVIDER = "expatternprovider";
    public static final String JUST_ENOUGH_CHARACTERS = "jecharacters";
    public static final String ME_REQUESTER = "merequester";
    public static final String APPLIED_FLUX = "appflux";
    public static final String APPLIED_BOTANICS = "appbot";
    public static final String APPLIED_MEKANISTICS = "appmek";
    public static final String ARS_ENERGISTIQUE = "arseng";

    private LoadedMods() {
    }

    public static boolean isExPatternProviderLoaded() {
        return ModList.get().isLoaded(EX_PATTERN_PROVIDER);
    }

    public static boolean isJustEnoughCharactersLoaded() {
        return ModList.get().isLoaded(JUST_ENOUGH_CHARACTERS);
    }

    public static boolean isMERequesterLoaded() {
        return ModList.get().isLoaded(ME_REQUESTER);
    }

    public static boolean isAppliedFluxLoaded() {
        return ModList.get().isLoaded(APPLIED_FLUX);
    }

    public static boolean isAppliedBotanicsLoaded() {
        return ModList.get().isLoaded(APPLIED_BOTANICS);
    }

    public static boolean isAppliedMekanisticsLoaded() {
        return ModList.get().isLoaded(APPLIED_MEKANISTICS);
    }

    public static boolean isArsEnergistiqueLoaded() {
        return ModList.get().isLoaded(ARS_ENERGISTIQUE);
    }
}

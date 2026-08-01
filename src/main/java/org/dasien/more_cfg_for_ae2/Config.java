package org.dasien.more_cfg_for_ae2;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.format.ConfigFormats;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@dev.toma.configuration.config.Config(id = More_cfg_for_ae2.MODID, filename = More_cfg_for_ae2.MODID, group = More_cfg_for_ae2.MODID)
public class Config {
    public static final int ME_INTERFACE_DEFAULT_SLOT_LIMIT = 64;
    public static final int ME_INTERFACE_MAX_SLOT_LIMIT = 2_000_000;
    public static final int ME_REQUESTER_DEFAULT_REQUEST_LIMIT = Integer.MAX_VALUE;
    public static final int EXTENDED_INTERFACE_MAX_SLOT_LIMIT = ME_INTERFACE_MAX_SLOT_LIMIT;
    public static final int OVERSIZE_INTERFACE_MAX_SLOT_LIMIT = Integer.MAX_VALUE;
    public static final int EXTENDED_INTERFACE_MAX_PAGES = 20;
    public static final int OVERSIZE_INTERFACE_MAX_PAGES = 20;

    protected static ConfigHolder<? extends Config> holder;
    private static long lastLoadedConfigTimestamp = Long.MIN_VALUE;

    @Configurable
    @Configurable.Synchronized
    @Configurable.Range(min = ME_INTERFACE_DEFAULT_SLOT_LIMIT, max = ME_INTERFACE_MAX_SLOT_LIMIT)
    @Configurable.Comment("Maximum amount per config/storage slot in normal AE2 ME Interfaces.")
    public int meInterfaceSlotLimit = ME_INTERFACE_MAX_SLOT_LIMIT;

    @Configurable
    @Configurable.Synchronized
    @Configurable.Range(min = 1, max = 10)
    @Configurable.Comment("Page count for normal AE2 ME Interfaces. Each page contains 9 config slots and 9 storage slots.")
    public int meInterfacePages = 10;

    public static void register() {
        holder = Configuration.registerConfig(Config.class, ConfigFormats.YAML);
    }

    public static void registerWithExPatternProvider() {
        holder = Configuration.registerConfig(WithExPatternProvider.class, ConfigFormats.YAML);
    }

    public static void registerWithMERequester() {
        holder = Configuration.registerConfig(WithMERequester.class, ConfigFormats.YAML);
    }

    public static void registerWithExPatternProviderAndMERequester() {
        holder = Configuration.registerConfig(WithExPatternProviderAndMERequester.class, ConfigFormats.YAML);
    }

    public static Config get() {
        Config config = holder != null ? holder.getConfigInstance() : new Config();
        reloadFromDisk(config);
        return config;
    }

    private static synchronized void reloadFromDisk(Config config) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(More_cfg_for_ae2.MODID + ".yaml");
        try {
            if (!Files.isRegularFile(path)) {
                return;
            }

            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified == lastLoadedConfigTimestamp) {
                return;
            }

            List<String> lines = Files.readAllLines(path);
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String rawLine = lines.get(lineIndex);
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf(':');
                if (separator <= 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                value = stripComment(value);
                if (value.isEmpty() && config instanceof WithExPatternProvider) {
                    ParsedYamlList parsedList = parseIndentedList(lines, lineIndex + 1);
                    if (parsedList.consumedLines > 0) {
                        applyConfigListValue(config, key, parsedList.values.toArray(String[]::new));
                        lineIndex += parsedList.consumedLines;
                    }
                    continue;
                }

                applyConfigValue(config, key, value);
            }

            lastLoadedConfigTimestamp = modified;
        } catch (IOException ignored) {
        }
    }

    protected static void applyConfigValue(Config config, String key, String value) {
        switch (key) {
            case "meInterfaceSlotLimit" -> config.meInterfaceSlotLimit = clamp(parseIntOrThrow(value),
                    ME_INTERFACE_DEFAULT_SLOT_LIMIT, ME_INTERFACE_MAX_SLOT_LIMIT);
            case "meInterfacePages" -> config.meInterfacePages = clamp(parseIntOrThrow(value), 1, 10);
            case "extendedInterfaceSlotLimit" -> {
                if (config instanceof WithExPatternProvider eppConfig) {
                    eppConfig.extendedInterfaceSlotLimit = clamp(parseIntOrThrow(value), ME_INTERFACE_DEFAULT_SLOT_LIMIT,
                            EXTENDED_INTERFACE_MAX_SLOT_LIMIT);
                }
            }
            case "extendedInterfacePages" -> {
                if (config instanceof WithExPatternProvider eppConfig) {
                    eppConfig.extendedInterfacePages = clamp(parseIntOrThrow(value), 1, EXTENDED_INTERFACE_MAX_PAGES);
                }
            }
            case "oversizeInterfaceSlotLimit" -> {
                if (config instanceof WithExPatternProvider eppConfig) {
                    eppConfig.oversizeInterfaceSlotLimit = clamp(parseIntOrThrow(value), ME_INTERFACE_DEFAULT_SLOT_LIMIT,
                            OVERSIZE_INTERFACE_MAX_SLOT_LIMIT);
                }
            }
            case "oversizeInterfacePages" -> {
                if (config instanceof WithExPatternProvider eppConfig) {
                    eppConfig.oversizeInterfacePages = clamp(parseIntOrThrow(value), 1, OVERSIZE_INTERFACE_MAX_PAGES);
                }
            }
            case "infinityCells", "infinityCellItems", "infinityCellFluids" -> {
                if (config instanceof WithExPatternProvider) {
                    applyConfigListValue(config, key, parseInlineStringList(value));
                }
            }
            case "infinityCellUseLongMaxValue" -> {
                if (config instanceof WithExPatternProvider eppConfig) {
                    eppConfig.infinityCellUseLongMaxValue = parseBooleanOrThrow(value);
                }
            }
            case "meRequesterRequestAmountLimit" -> {
                if (config instanceof MERequesterConfig requesterConfig) {
                    requesterConfig.setMeRequesterRequestAmountLimit(clamp(parseIntOrThrow(value), 1,
                            ME_REQUESTER_DEFAULT_REQUEST_LIMIT));
                }
            }
            case "meRequesterRequestBatchLimit" -> {
                if (config instanceof MERequesterConfig requesterConfig) {
                    requesterConfig.setMeRequesterRequestBatchLimit(clamp(parseIntOrThrow(value), 1,
                            ME_REQUESTER_DEFAULT_REQUEST_LIMIT));
                }
            }
            default -> {
            }
        }
    }

    protected static void applyConfigListValue(Config config, String key, String[] values) {
        if (!(config instanceof WithExPatternProvider eppConfig)) {
            return;
        }
        switch (key) {
            case "infinityCells" -> eppConfig.infinityCells = values;
            case "infinityCellItems" -> eppConfig.infinityCells = mergeLegacyEntries(eppConfig.infinityCells, "item",
                    values);
            case "infinityCellFluids" -> eppConfig.infinityCells = mergeLegacyEntries(eppConfig.infinityCells, "fluid",
                    values);
            default -> {
            }
        }
    }

    protected static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String[] mergeLegacyEntries(String[] currentEntries, String prefix, String[] legacyIds) {
        List<String> entries = new ArrayList<>();
        if (currentEntries != null) {
            entries.addAll(List.of(currentEntries));
        }
        if (legacyIds != null) {
            for (String legacyId : legacyIds) {
                if (legacyId != null && !legacyId.isBlank()) {
                    entries.add(prefix + ":" + legacyId.trim());
                }
            }
        }
        return entries.toArray(String[]::new);
    }

    private static int parseIntOrThrow(String value) {
        return Integer.parseInt(stripQuotes(value));
    }

    private static boolean parseBooleanOrThrow(String value) {
        return Boolean.parseBoolean(stripQuotes(value));
    }

    private static String stripComment(String value) {
        int comment = value.indexOf('#');
        return comment >= 0 ? value.substring(0, comment).trim() : value.trim();
    }

    private static String[] parseInlineStringList(String value) {
        String trimmed = stripComment(value).trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return new String[0];
        }

        String[] rawValues = trimmed.split(",");
        List<String> values = new ArrayList<>();
        for (String rawValue : rawValues) {
            String parsed = stripQuotes(stripComment(rawValue));
            if (!parsed.isBlank()) {
                values.add(parsed);
            }
        }
        return values.toArray(String[]::new);
    }

    private static ParsedYamlList parseIndentedList(List<String> lines, int startIndex) {
        List<String> values = new ArrayList<>();
        int consumedLines = 0;
        for (int lineIndex = startIndex; lineIndex < lines.size(); lineIndex++) {
            String rawLine = lines.get(lineIndex);
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                consumedLines++;
                continue;
            }
            if (!trimmed.startsWith("-")) {
                break;
            }

            String value = stripQuotes(stripComment(trimmed.substring(1)));
            if (!value.isBlank()) {
                values.add(value);
            }
            consumedLines++;
        }
        return new ParsedYamlList(values, consumedLines);
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    private record ParsedYamlList(List<String> values, int consumedLines) {
    }

    public interface MERequesterConfig {
        int getMeRequesterRequestAmountLimit();

        void setMeRequesterRequestAmountLimit(int limit);

        int getMeRequesterRequestBatchLimit();

        void setMeRequesterRequestBatchLimit(int limit);
    }

    @dev.toma.configuration.config.Config(id = More_cfg_for_ae2.MODID, filename = More_cfg_for_ae2.MODID, group = More_cfg_for_ae2.MODID)
    public static class WithMERequester extends Config implements MERequesterConfig {
        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 1, max = ME_REQUESTER_DEFAULT_REQUEST_LIMIT)
        @Configurable.Comment("Maximum target amount allowed for each ME Requester request.")
        public int meRequesterRequestAmountLimit = ME_REQUESTER_DEFAULT_REQUEST_LIMIT;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 1, max = ME_REQUESTER_DEFAULT_REQUEST_LIMIT)
        @Configurable.Comment("Maximum batch amount allowed for each ME Requester request.")
        public int meRequesterRequestBatchLimit = ME_REQUESTER_DEFAULT_REQUEST_LIMIT;

        @Override
        public int getMeRequesterRequestAmountLimit() {
            return this.meRequesterRequestAmountLimit;
        }

        @Override
        public void setMeRequesterRequestAmountLimit(int limit) {
            this.meRequesterRequestAmountLimit = limit;
        }

        @Override
        public int getMeRequesterRequestBatchLimit() {
            return this.meRequesterRequestBatchLimit;
        }

        @Override
        public void setMeRequesterRequestBatchLimit(int limit) {
            this.meRequesterRequestBatchLimit = limit;
        }
    }

    @dev.toma.configuration.config.Config(id = More_cfg_for_ae2.MODID, filename = More_cfg_for_ae2.MODID, group = More_cfg_for_ae2.MODID)
    public static class WithExPatternProvider extends Config {
        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = ME_INTERFACE_DEFAULT_SLOT_LIMIT, max = EXTENDED_INTERFACE_MAX_SLOT_LIMIT)
        @Configurable.Comment("Maximum amount per config/storage slot in ExtendedAE extended ME Interfaces.")
        public int extendedInterfaceSlotLimit = EXTENDED_INTERFACE_MAX_SLOT_LIMIT;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 1, max = EXTENDED_INTERFACE_MAX_PAGES)
        @Configurable.Comment("Page count for ExtendedAE extended ME Interfaces. Each page contains 18 config slots and 18 storage slots.")
        public int extendedInterfacePages = EXTENDED_INTERFACE_MAX_PAGES;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = ME_INTERFACE_DEFAULT_SLOT_LIMIT, max = OVERSIZE_INTERFACE_MAX_SLOT_LIMIT)
        @Configurable.Comment("Maximum amount per config/storage slot in ExtendedAE oversize ME Interfaces.")
        public int oversizeInterfaceSlotLimit = OVERSIZE_INTERFACE_MAX_SLOT_LIMIT;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 1, max = OVERSIZE_INTERFACE_MAX_PAGES)
        @Configurable.Comment("Page count for ExtendedAE oversize ME Interfaces. Each page contains 18 config slots and 18 storage slots.")
        public int oversizeInterfacePages = OVERSIZE_INTERFACE_MAX_PAGES;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Registry entries to expose as Ex Pattern Provider infinity cells. Supported prefixes: item, fluid, fe, mana, gas, infuse, pigment, slurry, source.")
        public String[] infinityCells = new String[0];

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("When enabled, Ex Pattern Provider infinity cells report Long.MAX_VALUE as their stored amount instead of the default Integer.MAX_VALUE-based amount.")
        public boolean infinityCellUseLongMaxValue = false;
    }

    @dev.toma.configuration.config.Config(id = More_cfg_for_ae2.MODID, filename = More_cfg_for_ae2.MODID, group = More_cfg_for_ae2.MODID)
    public static final class WithExPatternProviderAndMERequester extends WithExPatternProvider
            implements MERequesterConfig {
        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 1, max = ME_REQUESTER_DEFAULT_REQUEST_LIMIT)
        @Configurable.Comment("Maximum target amount allowed for each ME Requester request.")
        public int meRequesterRequestAmountLimit = ME_REQUESTER_DEFAULT_REQUEST_LIMIT;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 1, max = ME_REQUESTER_DEFAULT_REQUEST_LIMIT)
        @Configurable.Comment("Maximum batch amount allowed for each ME Requester request.")
        public int meRequesterRequestBatchLimit = ME_REQUESTER_DEFAULT_REQUEST_LIMIT;

        @Override
        public int getMeRequesterRequestAmountLimit() {
            return this.meRequesterRequestAmountLimit;
        }

        @Override
        public void setMeRequesterRequestAmountLimit(int limit) {
            this.meRequesterRequestAmountLimit = limit;
        }

        @Override
        public int getMeRequesterRequestBatchLimit() {
            return this.meRequesterRequestBatchLimit;
        }

        @Override
        public void setMeRequesterRequestBatchLimit(int limit) {
            this.meRequesterRequestBatchLimit = limit;
        }
    }
}

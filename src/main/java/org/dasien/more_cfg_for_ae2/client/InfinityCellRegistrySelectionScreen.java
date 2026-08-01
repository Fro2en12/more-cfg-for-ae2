package org.dasien.more_cfg_for_ae2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.dasien.more_cfg_for_ae2.compat.AppFluxInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.AppliedBotanicsInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.AppliedMekanisticsInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.ArsEnergistiqueInfinityCellCompat;
import org.dasien.more_cfg_for_ae2.compat.InfinityCellRegistryEntry;
import org.dasien.more_cfg_for_ae2.compat.InfinityCellRegistryTarget;
import org.dasien.more_cfg_for_ae2.compat.JeCharactersCompat;
import org.dasien.more_cfg_for_ae2.compat.LoadedMods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class InfinityCellRegistrySelectionScreen extends Screen {
    private static final int CELL = 22;
    private static final int ICON = 16;
    private static final int TITLE_Y = 8;
    private static final int SEARCH_Y = 24;
    private static final int GROUP_Y = 48;
    private static final int TOP = 72;
    private static final int SIDE_PADDING = 20;
    private static final int FOOTER_HEIGHT = 54;

    private final Screen parent;
    private final Consumer<String> selector;
    private final List<Entry> allEntries;
    private final List<Entry> visibleEntries = new ArrayList<>();

    private EditBox searchBox;
    private Button previousPage;
    private Button nextPage;
    private GridLayout layout;
    private int pageTextY;
    private DisplayGroup group;
    private int page;

    public InfinityCellRegistrySelectionScreen(Screen parent, InfinityCellRegistryTarget target, Consumer<String> selector) {
        super(Component.translatable("gui.more_cfg_for_ae2.infinity_cell.select"));
        this.parent = parent;
        this.selector = selector;
        this.group = DisplayGroup.fromTarget(target);
        this.allEntries = new ArrayList<>();
        collectEntries(this.group, this.allEntries);
        this.visibleEntries.addAll(this.allEntries);
    }

    @Override
    protected void init() {
        this.layout = createGridLayout();
        int searchWidth = Math.min(320, Math.max(120, this.width - 40));
        this.searchBox = new EditBox(this.font, this.width / 2 - searchWidth / 2, SEARCH_Y, searchWidth, 20,
                Component.translatable("gui.more_cfg_for_ae2.infinity_cell.search"));
        this.searchBox.setResponder(this::filter);
        this.addRenderableWidget(this.searchBox);
        addGroupButtons();
        int pageButtonWidth = Math.min(72, Math.max(50, (this.width - 60) / 2));
        int buttonY = Math.max(this.layout.bottom() + 14, this.height - 24);
        this.pageTextY = Math.min(buttonY - 12, Math.max(this.layout.bottom() + 4, this.height - 36));
        this.previousPage = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.more_cfg_for_ae2.interface.previous"),
                        button -> changePage(-1))
                .bounds(this.width / 2 - pageButtonWidth - 8, buttonY, pageButtonWidth, 20)
                .build());
        this.nextPage = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.more_cfg_for_ae2.interface.next"),
                        button -> changePage(1))
                .bounds(this.width / 2 + 8, buttonY, pageButtonWidth, 20)
                .build());
        updatePageButtons();
    }

    private void addGroupButtons() {
        List<DisplayGroup> groups = availableGroups();
        int spacing = 2;
        int buttonWidth = Math.min(72, Math.max(42, (this.width - 20 - (groups.size() - 1) * spacing) / groups.size()));
        int totalWidth = groups.size() * buttonWidth + Math.max(0, groups.size() - 1) * spacing;
        int x = this.width / 2 - totalWidth / 2;
        for (DisplayGroup group : groups) {
            Button button = Button.builder(group.title(), ignored -> switchGroup(group))
                    .bounds(x, GROUP_Y, buttonWidth, 20)
                    .build();
            button.active = group != this.group;
            this.addRenderableWidget(button);
            x += buttonWidth + spacing;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, TITLE_Y, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);

        GridLayout layout = layout();
        this.page = Math.min(this.page, maxPage());
        int pageSize = layout.pageSize();
        int start = this.page * pageSize;

        int maxPage = maxPage();
        updatePageButtons();
        if (maxPage > 0) {
            graphics.drawCenteredString(this.font, (this.page + 1) + "/" + (maxPage + 1),
                    this.width / 2, this.pageTextY, 0xE0E0E0);
        }

        for (int slot = 0; slot < pageSize; slot++) {
            int index = start + slot;
            boolean hasEntry = index >= 0 && index < this.visibleEntries.size();
            if (hasEntry) {
                int x = layout.left() + (slot % layout.columns()) * CELL;
                int y = layout.top() + (slot / layout.columns()) * CELL;
                if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                    graphics.fill(x, y, x + CELL - 2, y + CELL - 2, 0x66FFFFFF);
                }
                this.visibleEntries.get(index).renderIcon(graphics, x + 2, y + 2);
            }
        }

        Entry hovered = entryAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderComponentTooltip(this.font, hovered.tooltip(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Entry entry = entryAt((int) mouseX, (int) mouseY);
        if (entry != null) {
            this.selector.accept(entry.id.toString());
            Minecraft.getInstance().setScreen(this.parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int maxPage = maxPage();
        if (maxPage <= 0) {
            return false;
        }
        changePage(vertical > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void filter(String query) {
        String normalized = normalizeSearchText(query);
        this.visibleEntries.clear();
        for (Entry entry : this.allEntries) {
            if (normalized.isEmpty()
                    || entry.searchText().contains(normalized)
                    || matchesWithJeCharacters(entry, normalized)) {
                this.visibleEntries.add(entry);
            }
        }
        this.page = 0;
        updatePageButtons();
    }

    private void changePage(int delta) {
        this.page = Math.max(0, Math.min(maxPage(), this.page + delta));
        updatePageButtons();
    }

    private void updatePageButtons() {
        int maxPage = maxPage();
        this.page = Math.max(0, Math.min(maxPage, this.page));
        if (this.previousPage != null) {
            this.previousPage.active = this.page > 0;
            this.previousPage.visible = maxPage > 0;
        }
        if (this.nextPage != null) {
            this.nextPage.active = this.page < maxPage;
            this.nextPage.visible = maxPage > 0;
        }
    }

    private static String normalizeSearchText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean matchesWithJeCharacters(Entry entry, String query) {
        if (!LoadedMods.isJustEnoughCharactersLoaded()) {
            return false;
        }
        return JeCharactersCompat.matches(entry.name().getString(), query)
                || JeCharactersCompat.matches(entry.id(), query);
    }

    private void switchGroup(DisplayGroup group) {
        InfinityCellRegistrySelectionScreen screen = new InfinityCellRegistrySelectionScreen(this.parent,
                group.defaultTarget(), this.selector);
        Minecraft.getInstance().setScreen(screen);
    }

    private Entry entryAt(int mouseX, int mouseY) {
        GridLayout layout = layout();
        if (mouseX < layout.left() || mouseY < layout.top()) {
            return null;
        }
        int column = (mouseX - layout.left()) / CELL;
        int row = (mouseY - layout.top()) / CELL;
        if (column < 0 || column >= layout.columns() || row < 0 || row >= layout.rows()) {
            return null;
        }
        int index = this.page * layout.pageSize() + row * layout.columns() + column;
        return index >= 0 && index < this.visibleEntries.size() ? this.visibleEntries.get(index) : null;
    }

    private GridLayout layout() {
        if (this.layout == null) {
            this.layout = createGridLayout();
        }
        return this.layout;
    }

    private GridLayout createGridLayout() {
        int columns = Math.max(1, (this.width - SIDE_PADDING * 2) / CELL);
        int availableHeight = Math.max(CELL, this.height - TOP - FOOTER_HEIGHT);
        int rows = Math.max(1, availableHeight / CELL);
        int left = (this.width - columns * CELL) / 2;
        return new GridLayout(left, TOP, columns, rows);
    }

    private int maxPage() {
        int pageSize = layout().pageSize();
        if (pageSize <= 0) {
            return 0;
        }
        int pages = (int) Math.ceil(this.visibleEntries.size() / (double) pageSize);
        return Math.max(0, pages - 1);
    }

    private static void collectEntries(DisplayGroup group, List<Entry> entries) {
        if (group == DisplayGroup.ITEM) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                entries.add(Entry.item(InfinityCellRegistryTarget.ITEM.encode(id.toString()), item));
            }
        } else if (group == DisplayGroup.FLUID) {
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid == Fluids.EMPTY || fluid.defaultFluidState().isSource() == false) {
                    continue;
                }
                ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
                entries.add(Entry.fluid(InfinityCellRegistryTarget.FLUID.encode(id.toString()), fluid));
            }
        } else {
            for (InfinityCellRegistryTarget target : group.targets()) {
                collectCompatEntries(target, entries);
            }
        }
        entries.sort(Comparator.comparing(entry -> entry.name.getString().toLowerCase(Locale.ROOT)));
    }

    private static void collectCompatEntries(InfinityCellRegistryTarget target, List<Entry> entries) {
        List<InfinityCellRegistryEntry> registryEntries = switch (target) {
            case FE -> LoadedMods.isAppliedFluxLoaded() ? AppFluxInfinityCellCompat.entries() : List.of();
            case MANA -> LoadedMods.isAppliedBotanicsLoaded() ? AppliedBotanicsInfinityCellCompat.entries() : List.of();
            case GAS, INFUSE_TYPE, PIGMENT, SLURRY -> LoadedMods.isAppliedMekanisticsLoaded()
                    ? AppliedMekanisticsInfinityCellCompat.entries(target) : List.of();
            case SOURCE -> LoadedMods.isArsEnergistiqueLoaded() ? ArsEnergistiqueInfinityCellCompat.entries()
                    : List.of();
            default -> List.of();
        };
        for (InfinityCellRegistryEntry registryEntry : registryEntries) {
            entries.add(Entry.compat(registryEntry));
        }
    }

    private static List<DisplayGroup> availableGroups() {
        List<DisplayGroup> groups = new ArrayList<>();
        groups.add(DisplayGroup.ITEM);
        groups.add(DisplayGroup.FLUID);
        if (LoadedMods.isAppliedMekanisticsLoaded()) {
            groups.add(DisplayGroup.CHEMICAL);
        }
        if (LoadedMods.isAppliedFluxLoaded()
                || LoadedMods.isAppliedBotanicsLoaded()
                || LoadedMods.isArsEnergistiqueLoaded()) {
            groups.add(DisplayGroup.OTHER);
        }
        return groups;
    }

    private record GridLayout(int left, int top, int columns, int rows) {
        int pageSize() {
            return this.columns * this.rows;
        }

        int bottom() {
            return this.top + this.rows * CELL;
        }
    }

    private enum DisplayGroup {
        ITEM("item", List.of(InfinityCellRegistryTarget.ITEM)),
        FLUID("fluid", List.of(InfinityCellRegistryTarget.FLUID)),
        CHEMICAL("chemical", List.of(InfinityCellRegistryTarget.GAS, InfinityCellRegistryTarget.INFUSE_TYPE,
                InfinityCellRegistryTarget.PIGMENT, InfinityCellRegistryTarget.SLURRY)),
        OTHER("other", List.of(InfinityCellRegistryTarget.FE, InfinityCellRegistryTarget.MANA,
                InfinityCellRegistryTarget.SOURCE));

        private final String key;
        private final List<InfinityCellRegistryTarget> targets;

        DisplayGroup(String key, List<InfinityCellRegistryTarget> targets) {
            this.key = key;
            this.targets = targets;
        }

        static DisplayGroup fromTarget(InfinityCellRegistryTarget target) {
            if (target == null) {
                return ITEM;
            }
            for (DisplayGroup group : values()) {
                if (group.targets.contains(target)) {
                    return group;
                }
            }
            return ITEM;
        }

        Component title() {
            return Component.translatable("gui.more_cfg_for_ae2.infinity_cell.group." + this.key);
        }

        InfinityCellRegistryTarget defaultTarget() {
            return this.targets.get(0);
        }

        List<InfinityCellRegistryTarget> targets() {
            return this.targets;
        }
    }

    private record Entry(String id, InfinityCellRegistryTarget target, String registryId, Component name,
                         String searchText) {
        static Entry item(String id, Item item) {
            return new Entry(id, InfinityCellRegistryTarget.ITEM,
                    InfinityCellRegistryTarget.registryId(id), item.getDescription(),
                    normalizeSearchText(id + " " + item.getDescription().getString()));
        }

        static Entry fluid(String id, Fluid fluid) {
            String registryId = InfinityCellRegistryTarget.registryId(id);
            Component name = RegistryEntryDisplayHelper.name(InfinityCellRegistryTarget.FLUID, registryId);
            return new Entry(id, InfinityCellRegistryTarget.FLUID, registryId, name,
                    normalizeSearchText(id + " " + name.getString()));
        }

        static Entry compat(InfinityCellRegistryEntry registryEntry) {
            String id = registryEntry.encodedId();
            return new Entry(id, registryEntry.target(), registryEntry.registryId(), registryEntry.name(),
                    normalizeSearchText(id + " " + registryEntry.name().getString()));
        }

        void renderIcon(GuiGraphics graphics, int x, int y) {
            RegistryEntryDisplayHelper.renderIcon(graphics, this.target, this.registryId, x, y);
        }

        List<Component> tooltip() {
            return List.of(this.name, Component.literal(this.id).withStyle(style -> style.withColor(0x808080)));
        }
    }
}

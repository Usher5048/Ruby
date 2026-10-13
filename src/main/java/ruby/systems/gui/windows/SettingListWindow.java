package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import ruby.systems.config.*;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.icons.Lucide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Inline expandable list setting widget for selecting blocks, items, or entity types
 * from their respective Minecraft registries.
 * <p>
 * Collapsed: shows label + count badge ("N items").
 * Expanded: shows a search bar and a scrollable checkbox list.
 *   - Empty search → shows only currently selected items.
 *   - Non-empty search → shows matching registry items (selected sorted first).
 */
public class SettingListWindow extends SettingWindow {
    private boolean expanded = false;
    private float expandProgress = 0f;
    private String filterText = "";
    private int scrollOffset = 0;
    private boolean scrollbarDrag = false;
    private float dragGrab = 0f;
    private boolean searchFocused = false;
    private float cursorBlink = 0f;

    private List<Entry> allEntries;
    private List<Entry> filteredEntries;
    private final boolean hasRegistry;

    private static final int HEADER_H = ROW;
    private static final int SEARCH_H = 28;
    private static final int ROW_H = 26;
    private static final int MAX_ROWS = 6;

    private record Entry(String name, Object value) {}

    public SettingListWindow(int x, int y, int width, ListValue<?> value) {
        super(x, y, width, HEADER_H, value);
        this.handleChildren = false;
        this.hasRegistry = !(value instanceof StringListValue);
    }

    /* ---- lazy registry loading ---- */

    private void ensureLoaded() {
        if (allEntries != null) return;
        allEntries = new ArrayList<>();

        if (this.value instanceof BlockListValue) {
            for (Block b : BuiltInRegistries.BLOCK)
                allEntries.add(new Entry(b.getName().getString(), b));
        } else if (this.value instanceof ItemListValue) {
            for (Item i : BuiltInRegistries.ITEM)
                allEntries.add(new Entry(i.getName(new ItemStack(i)).getString(), i));
        } else if (this.value instanceof EntityTypeListValue) {
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE)
                allEntries.add(new Entry(t.getDescription().getString(), t));
        }

        allEntries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        rebuildFiltered();
    }

    private void rebuildFiltered() {
        if (allEntries == null) { filteredEntries = List.of(); return; }

        if (filterText.isEmpty()) {
            // No filter → show only selected items
            filteredEntries = new ArrayList<>();
            for (Entry e : allEntries) {
                if (isSelected(e)) filteredEntries.add(e);
            }
        } else {
            String lower = filterText.toLowerCase(Locale.ROOT);
            filteredEntries = new ArrayList<>();
            for (Entry e : allEntries) {
                if (e.name.toLowerCase(Locale.ROOT).contains(lower))
                    filteredEntries.add(e);
            }
            // Selected first, then alphabetical
            filteredEntries.sort((a, b) -> {
                boolean as = isSelected(a), bs = isSelected(b);
                if (as != bs) return as ? -1 : 1;
                return a.name.compareToIgnoreCase(b.name);
            });
        }

        scrollOffset = Math.clamp(scrollOffset, 0, Math.max(0, filteredEntries.size() - MAX_ROWS));
    }

    private boolean isSelected(Entry entry) {
        return ((ListValue<?>) this.value).value().contains(entry.value);
    }

    @SuppressWarnings("unchecked")
    private void toggleValue(Entry entry) {
        List<Object> list = (List<Object>) this.value.value();
        if (list.contains(entry.value)) {
            list.remove(entry.value);
        } else {
            list.add(entry.value);
        }
    }

    /* ---- height ---- */

    private int visibleRows() {
        return filteredEntries == null ? 0 : Math.min(MAX_ROWS, filteredEntries.size());
    }

    private int headerPx() {
        return this.scaled(HEADER_H);
    }

    private int searchPx() {
        return this.scaled(SEARCH_H);
    }

    private int rowPx() {
        return this.scaled(ROW_H);
    }

    private int expandedHeight() {
        int rows = visibleRows();
        int listH = rows > 0 ? rows * this.rowPx() : this.scaled(20);
        return this.headerPx() + this.searchPx() + listH + this.scaled(4);
    }

    @Override
    public int layoutStamp() {
        int stamp = super.layoutStamp();
        stamp = stamp * 31 + (this.expanded ? 1 : 0);
        stamp = stamp * 31 + Float.floatToIntBits(this.expandProgress);
        stamp = stamp * 31 + (this.filteredEntries == null ? 0 : this.filteredEntries.size());
        return stamp;
    }

    @Override
    public int restingHeight() {
        if (!this.value.visible()) return 0;
        if (!this.hasRegistry || (!this.expanded && this.expandProgress <= 0f)) return this.headerPx();
        this.ensureLoaded();
        int full = this.expandedHeight();
        int header = this.headerPx();
        float eased = Motion.standard(this.expandProgress);
        return header + Math.round((full - header) * eased);
    }

    /* ---- render ---- */

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        int clip = this.getHeight();
        if (clip <= 0) return;
        GUIStyle style = GUIStyle.get();
        int w = this.designWidth();

        this.clip(context);
        this.paintHover(context, mouseX, mouseY, dt);
        this.beginItem(context);

        float dir = this.expanded ? 1f : -1f;
        this.expandProgress = Math.clamp(
                this.expandProgress + dir * ruby.systems.gui.ClientSettings.expandStep(dt), 0f, 1f);
        this.cursorBlink += 0.07f;
        if (this.cursorBlink > 2f) this.cursorBlink = 0f;

        this.renderHeader(context, style, w, (int) this.design(mouseX), (int) this.design(mouseY), dt);

        if (this.expandProgress > 0.01f && this.hasRegistry && clip > this.headerPx()) {
            this.ensureLoaded();
            this.renderSearch(context, style, w);
            this.renderList(context, style, w, (int) this.design(mouseX), (int) this.design(mouseY));
        }
        this.endItem(context);
        this.unclip(context);
    }

    private void renderHeader(GuiGraphicsExtractor ctx, GUIStyle style, int w, int mx, int my, float dt) {
        int chevron = Lucide.UI;
        int count = ((ListValue<?>) this.value).value().size();
        String badge = count == 1 ? "1" : Integer.toString(count);
        int badgeW = style.labelFont().getWidth(badge) + 10;
        int badgeH = style.labelFont().fontHeight + 4;
        int chevronX = w - PAD - chevron;
        int badgeX = chevronX - 8 - badgeW;
        int header = this.headerPx();
        int ty = (header - style.labelFont().fontHeight) / 2;
        String name = this.fit(style.labelFont(), this.value.name(), badgeX - 8 - PAD);
        int label = Motion.mix(style.textMuted(), style.text(), this.hoverMix());
        style.labelFont().draw(ctx, name, PAD, ty, label);

        int by = (header - badgeH) / 2;
        ModuleTypeWindow.drawRoundedBadge(ctx, badgeX, by, badgeX + badgeW, by + badgeH,
                style.bgHover(), style.borderPanel());
        style.labelFont().draw(ctx, badge, badgeX + 5, by + (badgeH - style.labelFont().fontHeight) / 2, style.text());

        float rotation = this.expandProgress * 90f;
        int color = mx >= chevronX - 4 && my >= 0 && my < header ? style.text() : style.textMuted();
        Lucide.drawCentered(ctx, "chevron-right", chevronX + chevron / 2, header / 2, chevron, color, rotation);
    }

    private void renderSearch(GuiGraphicsExtractor ctx, GUIStyle style, int w) {
        int y = this.headerPx() + 2;
        int h = this.searchPx() - 6;
        int l = PAD;
        int r = w - PAD;

        if (this.searchFocused) {
            ModuleTypeWindow.fillSmoothRoundedRect(ctx, l - 1, y - 1, r + 1, y + h + 1, GUIStyle.RADIUS_BADGE,
                    GUIStyle.withAlpha(style.ruby(), 0.35f));
        }
        ModuleTypeWindow.fillSmoothRoundedRect(ctx, l, y, r, y + h, GUIStyle.RADIUS_BADGE, style.bgPanel());

        int icon = 12;
        int iconY = y + (h - icon) / 2;
        Lucide.draw(ctx, "search", l + 6, iconY, icon, style.textMuted());

        String text = this.filterText.isEmpty() && !this.searchFocused ? "Search" : this.filterText;
        int col = this.filterText.isEmpty() && !this.searchFocused ? style.textMuted() : style.text();
        int textX = l + 6 + icon + 6;
        int textY = y + (h - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(ctx, text, textX, textY, col);

        if (this.searchFocused && this.cursorBlink < 1f) {
            int cx = textX + style.bodyFont().getWidth(this.filterText);
            ctx.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight, style.text());
        }
    }

    private void renderList(GuiGraphicsExtractor ctx, GUIStyle style, int w, int mx, int my) {
        int startY = this.headerPx() + this.searchPx();

        if (filteredEntries == null || filteredEntries.isEmpty()) {
            String msg = filterText.isEmpty() ? "Type to search" : "No results";
            int ty = startY + (20 - style.bodyFont().fontHeight) / 2;
            style.bodyFont().draw(ctx, msg, PAD, ty, style.textMuted());
            return;
        }

        int vis = visibleRows();
        for (int i = 0; i < vis; i++) {
            int idx = scrollOffset + i;
            if (idx >= filteredEntries.size()) break;

            Entry entry = filteredEntries.get(idx);
            int ry = startY + i * this.rowPx();
            boolean sel = isSelected(entry);

            int box = 14;
            int cbx = PAD;
            int cby = ry + (this.rowPx() - box) / 2;
            if (sel) {
                ModuleTypeWindow.fillSmoothRoundedRect(ctx, cbx, cby, cbx + box, cby + box, GUIStyle.RADIUS_BADGE, style.ruby());
                Lucide.draw(ctx, "check", cbx + 2, cby + 2, 10, 0xFFFFFFFF);
            } else {
                ModuleTypeWindow.drawRoundedBadge(ctx, cbx, cby, cbx + box, cby + box, style.bgPanel(), style.borderPanel());
            }

            int nameX = cbx + box + 8;
            int right = PAD + (filteredEntries.size() > MAX_ROWS ? 8 : 0);
            String name = this.fit(style.bodyFont(), entry.name, w - right - nameX);
            int ny = ry + (this.rowPx() - style.bodyFont().fontHeight) / 2;
            style.bodyFont().draw(ctx, name, cbx + box + 8, ny, sel ? style.text() : style.textMuted());
        }

        Bar bar = this.bar();
        if (bar != null) {
            ModuleTypeWindow.fillSmoothRoundedRect(ctx, bar.x1, bar.thumbY,
                    bar.x2, bar.thumbY + bar.thumbH, 1, style.scrollbar());
        }
    }

    private record Bar(int x1, int x2, int trackTop, int trackH, int thumbY, int thumbH, int max) {}

    private Bar bar() {
        if (this.filteredEntries == null || this.filteredEntries.size() <= MAX_ROWS) return null;
        int vis = this.visibleRows();
        int total = this.filteredEntries.size();
        int trackH = vis * this.rowPx();
        int thumbH = Math.max(8, trackH * vis / total);
        int max = total - MAX_ROWS;
        float frac = max <= 0 ? 0f : (float) this.scrollOffset / max;
        int thumbY = this.headerPx() + this.searchPx() + Math.round((trackH - thumbH) * frac);
        int x2 = this.designWidth() - PAD;
        return new Bar(x2 - 3, x2, this.headerPx() + this.searchPx(), trackH, thumbY, thumbH, max);
    }

    private boolean hitBar(double localX, double localY, Bar bar) {
        return localX >= bar.x1 - 8 && localX <= this.designWidth()
                && localY >= bar.trackTop && localY < bar.trackTop + bar.trackH;
    }

    private void dragBar(double localY) {
        Bar bar = this.bar();
        if (bar == null) {
            this.scrollbarDrag = false;
            return;
        }
        float travel = bar.trackH - bar.thumbH;
        float frac = travel <= 0f ? 0f : ((float) localY - this.dragGrab - bar.trackTop) / travel;
        this.scrollOffset = Math.round(Math.clamp(frac, 0f, 1f) * bar.max);
    }

    /* ---- input ---- */

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.y() < 0 || click.y() >= this.getHeight()) return false;
        int my = (int) this.design(click.y());
        double mx = this.design(click.x());

        if (my < this.headerPx() && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (!hasRegistry) return false;
            expanded = !expanded;
            if (expanded) {
                ensureLoaded();
                rebuildFiltered();
                searchFocused = true;
                cursorBlink = 0f;
            } else {
                searchFocused = false;
                filterText = "";
                rebuildFiltered();
            }
            return true;
        }

        if (!expanded || expandProgress < 0.5f) return false;

        Bar bar = this.bar();
        if (bar != null && click.button() == InputConstants.MOUSE_BUTTON_LEFT && this.hitBar(mx, my, bar)) {
            this.scrollbarDrag = true;
            if (my >= bar.thumbY && my < bar.thumbY + bar.thumbH) this.dragGrab = my - bar.thumbY;
            else {
                this.dragGrab = bar.thumbH / 2f;
                this.dragBar(my);
            }
            return true;
        }

        // Search bar click → focus
        if (my >= this.headerPx() && my < this.headerPx() + this.searchPx()) {
            searchFocused = true;
            cursorBlink = 0f;
            return true;
        }

        // List row click → toggle item
        int startY = this.headerPx() + this.searchPx();
        if (my >= startY && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int row = (my - startY) / this.rowPx();
            int idx = scrollOffset + row;
            if (filteredEntries != null && idx >= 0 && idx < filteredEntries.size()) {
                toggleValue(filteredEntries.get(idx));
                return true;
            }
        }

        return my < this.getHeight();
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (!this.scrollbarDrag) return false;
        this.dragBar(this.design(click.y()));
        return true;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        if (!this.scrollbarDrag) return false;
        this.scrollbarDrag = false;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY,
                                   double horizontal, double vertical) {
        if (!this.expanded || this.filteredEntries == null || Math.abs(vertical) < 1e-4) return false;
        int max = Math.max(0, this.filteredEntries.size() - MAX_ROWS);
        if (max == 0) return false;
        int notches = (int) Math.round(vertical);
        if (notches == 0) notches = vertical > 0d ? 1 : -1;
        this.scrollOffset = Math.clamp(this.scrollOffset - notches, 0, max);
        return true;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        if (!searchFocused) return false;

        if (input.key() == InputConstants.KEY_BACKSPACE) {
            if (!filterText.isEmpty()) {
                filterText = filterText.substring(0, filterText.length() - 1);
                rebuildFiltered();
            }
            cursorBlink = 0f;
            return true;
        }

        if (input.key() == InputConstants.KEY_ESCAPE) {
            if (!filterText.isEmpty()) {
                filterText = "";
                rebuildFiltered();
            } else {
                searchFocused = false;
                expanded = false;
            }
            return true;
        }

        return true; // consume all keys when search focused
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        if (!searchFocused) return false;

        int cp = input.codepoint();
        if (cp >= 32 && cp != 127) { // printable characters
            filterText += Character.toString(cp);
            rebuildFiltered();
            cursorBlink = 0f;
            return true;
        }
        return false;
    }

    @Override
    public boolean onFocusRemoved() {
        searchFocused = false;
        return true;
    }
}

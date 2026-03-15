package ruby.systems.gui.windows;

import net.minecraft.block.Block;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.*;
import ruby.systems.gui.GUIStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Inline expandable list setting widget for selecting blocks, items, or entity types
 * from their respective Minecraft registries.
 * <p>
 * Collapsed: shows label + count badge ("N items").
 * Expanded: shows a search bar and a scrollable checkbox list.
 *   - Empty search → shows only currently selected items.
 *   - Non-empty search → shows matching registry items (selected sorted first).
 */
public class SettingListWindow extends Window {

    private final ListValue<?> listValue;
    private boolean expanded = false;
    private float expandProgress = 0f;
    private String filterText = "";
    private int scrollOffset = 0;
    private boolean searchFocused = false;
    private float cursorBlink = 0f;

    private List<Entry> allEntries;
    private List<Entry> filteredEntries;
    private final boolean hasRegistry;

    private static final int HEADER_H = 26;
    private static final int SEARCH_H = 24;
    private static final int ROW_H = 18;
    private static final int MAX_ROWS = 6;

    private record Entry(String name, Object value) {}

    public SettingListWindow(int x, int y, int width, ListValue<?> value) {
        super(x, y, width, HEADER_H);
        this.listValue = value;
        this.handleChildren = false;
        this.hasRegistry = !(value instanceof StringListValue);
    }

    /* ---- lazy registry loading ---- */

    private void ensureLoaded() {
        if (allEntries != null) return;
        allEntries = new ArrayList<>();

        if (listValue instanceof BlockListValue) {
            for (Block b : Registries.BLOCK)
                allEntries.add(new Entry(b.getName().getString(), b));
        } else if (listValue instanceof ItemListValue) {
            for (Item i : Registries.ITEM)
                allEntries.add(new Entry(i.getName().getString(), i));
        } else if (listValue instanceof EntityTypeListValue) {
            for (EntityType<?> t : Registries.ENTITY_TYPE)
                allEntries.add(new Entry(t.getName().getString(), t));
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

        scrollOffset = Math.max(0,
                Math.min(scrollOffset, Math.max(0, filteredEntries.size() - MAX_ROWS)));
    }

    private boolean isSelected(Entry entry) {
        return listValue.value().contains(entry.value);
    }

    @SuppressWarnings("unchecked")
    private void toggleValue(Entry entry) {
        List<Object> list = (List<Object>) listValue.value();
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

    private int expandedHeight() {
        int rows = visibleRows();
        int listH = rows > 0 ? rows * ROW_H : 20;
        return HEADER_H + SEARCH_H + listH + 4;
    }

    @Override
    public int getHeight() {
        if (!hasRegistry || (!expanded && expandProgress <= 0.01f)) return HEADER_H;
        ensureLoaded();
        int full = expandedHeight();
        float e = easeInOutCubic(expandProgress);
        return HEADER_H + (int) ((full - HEADER_H) * e);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2 * t + 2, 3) / 2f;
    }

    /* ---- render ---- */

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();

        // Animate expand/collapse
        float target = expanded ? 1f : 0f;
        expandProgress += (target - expandProgress) * 0.22f;
        if (Math.abs(expandProgress - target) < 0.005f) expandProgress = target;

        cursorBlink += 0.07f;
        if (cursorBlink > 2f) cursorBlink = 0f;

        renderHeader(context, style, w, mouseX, mouseY);

        if (expandProgress > 0.01f && hasRegistry) {
            ensureLoaded();
            renderSearch(context, style, w);
            renderList(context, style, w, mouseX, mouseY);
        }
    }

    private void renderHeader(DrawContext ctx, GUIStyle style, int w, int mx, int my) {
        boolean hovered = mx >= 0 && my >= 0 && mx < w && my < HEADER_H;
        if (hovered) ctx.fill(0, 0, w, HEADER_H, 0x0DFFFFFF);

        // Label
        int ty = (HEADER_H - (int) this.getTextHeight(style.bodyFont())) / 2;
        this.drawText(style.bodyFont(), ctx, listValue.name(), 34, ty, 0xFF8B8B8B);

        // Count badge
        int count = listValue.value().size();
        String badge = count + (count == 1 ? " item" : " items");
        int bw = (int) this.getTextWidth(style.monospaceFont(), badge) + 12;
        int bh = (int) this.getTextHeight(style.monospaceFont()) + 4;
        int bx = w - 14 - bw;
        int by = (HEADER_H - bh) / 2;

        ModuleTypeWindow.fillSmoothRoundedRect(ctx, bx, by, bx + bw, by + bh, 4, 0x1AFFFFFF);
        this.drawText(style.monospaceFont(), ctx, badge, bx + 6, by + 2, 0xFFCC3344);
    }

    private void renderSearch(DrawContext ctx, GUIStyle style, int w) {
        int y = HEADER_H + 2;
        int h = SEARCH_H - 4;
        int l = 34, r = w - 14;

        // Focus glow
        if (searchFocused) {
            ModuleTypeWindow.fillSmoothRoundedRect(ctx, l - 1, y - 1, r + 1, y + h + 1, 5, 0x33CC3344);
        }
        ModuleTypeWindow.fillSmoothRoundedRect(ctx, l, y, r, y + h, 4, 0xFF1A1A1A);

        // Search text or placeholder
        String text = filterText.isEmpty() && !searchFocused ? "Search..." : filterText;
        int col = filterText.isEmpty() && !searchFocused ? 0xFF555555 : 0xFFCCCCCC;
        int textY = y + (h - (int) this.getTextHeight(style.monospaceFont())) / 2;
        this.drawText(style.monospaceFont(), ctx, text, l + 6, textY, col);

        // Blinking cursor
        if (searchFocused && cursorBlink < 1f) {
            int cx = l + 6 + (int) this.getTextWidth(style.monospaceFont(), filterText);
            ctx.fill(cx, textY, cx + 1,
                    textY + (int) this.getTextHeight(style.monospaceFont()), 0xFFCCCCCC);
        }
    }

    private void renderList(DrawContext ctx, GUIStyle style, int w, int mx, int my) {
        int startY = HEADER_H + SEARCH_H;

        if (filteredEntries == null || filteredEntries.isEmpty()) {
            String msg = filterText.isEmpty() ? "No items — type to search" : "No results";
            int ty = startY + (20 - (int) this.getTextHeight(style.monospaceFont())) / 2;
            this.drawText(style.monospaceFont(), ctx, msg, 36, ty, 0xFF555555);
            return;
        }

        int vis = visibleRows();
        for (int i = 0; i < vis; i++) {
            int idx = scrollOffset + i;
            if (idx >= filteredEntries.size()) break;

            Entry entry = filteredEntries.get(idx);
            int ry = startY + i * ROW_H;
            boolean sel = isSelected(entry);

            // Row hover highlight
            if (mx >= 34 && mx < w - 14 && my >= ry && my < ry + ROW_H) {
                ctx.fill(34, ry, w - 14, ry + ROW_H, 0x0DFFFFFF);
            }

            // Checkbox (10x10)
            int cbx = 36, cby = ry + (ROW_H - 10) / 2;
            if (sel) {
                ModuleTypeWindow.fillSmoothRoundedRect(ctx, cbx, cby,
                        cbx + 10, cby + 10, 3, 0xFFCC3344);
                // Inner check mark
                ctx.fill(cbx + 3, cby + 3, cbx + 7, cby + 7, 0xFFFFFFFF);
            } else {
                ModuleTypeWindow.fillSmoothRoundedRect(ctx, cbx, cby,
                        cbx + 10, cby + 10, 3, 0xFF333333);
            }

            // Item name (truncated to fit)
            String name = entry.name;
            int maxW = w - 14 - 52;
            if (this.getTextWidth(style.monospaceFont(), name) > maxW) {
                while (name.length() > 1
                        && this.getTextWidth(style.monospaceFont(), name + "…") > maxW) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "…";
            }

            int ny = ry + (ROW_H - (int) this.getTextHeight(style.monospaceFont())) / 2;
            this.drawText(style.monospaceFont(), ctx, name, 52, ny,
                    sel ? 0xFFCCCCCC : 0xFF777777);
        }

        // Thin scrollbar when list overflows
        if (filteredEntries.size() > MAX_ROWS) {
            int total = filteredEntries.size();
            int trackH = vis * ROW_H;
            int barH = Math.max(8, trackH * vis / total);
            int scrollRange = trackH - barH;
            float frac = (float) scrollOffset / (total - MAX_ROWS);
            int barY = startY + (int) (frac * scrollRange);

            ModuleTypeWindow.fillSmoothRoundedRect(ctx, w - 16, barY,
                    w - 14, barY + barH, 1, 0x33FFFFFF);
        }
    }

    /* ---- input ---- */

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        int my = (int) click.y();

        // Header click → toggle expand
        if (my < HEADER_H && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
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

        // Search bar click → focus
        if (my >= HEADER_H && my < HEADER_H + SEARCH_H) {
            searchFocused = true;
            cursorBlink = 0f;
            return true;
        }

        // List row click → toggle item
        int startY = HEADER_H + SEARCH_H;
        if (my >= startY && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int row = (my - startY) / ROW_H;
            int idx = scrollOffset + row;
            if (filteredEntries != null && idx >= 0 && idx < filteredEntries.size()) {
                toggleValue(filteredEntries.get(idx));
                return true;
            }
        }

        return true; // consume click when expanded
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY,
                                   double horizontal, double vertical) {
        if (!expanded || filteredEntries == null) return false;
        int max = Math.max(0, filteredEntries.size() - MAX_ROWS);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) vertical));
        return true;
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if (!searchFocused) return false;

        if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!filterText.isEmpty()) {
                filterText = filterText.substring(0, filterText.length() - 1);
                rebuildFiltered();
            }
            cursorBlink = 0f;
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
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
    public boolean onCharTyped(CharInput input) {
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

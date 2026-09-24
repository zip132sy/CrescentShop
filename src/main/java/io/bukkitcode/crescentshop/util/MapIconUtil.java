package io.bukkitcode.crescentshop.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.lang.reflect.Method;

/** 地图图标工具：满幅检测、复制地图画面到新地图。用反射兼容不同版本。 */
public final class MapIconUtil {

    private static final int MAP_SIZE = 128;
    private static final double FULL_EDGE_RATIO = 0.8;

    private MapIconUtil() {
    }

    public static boolean isFilledMap(ItemStack item) {
        return item != null && item.getType() == Material.MAP;
    }

    public static MapView getMapView(ItemStack item) {
        if (!isFilledMap(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        try {
            Object result = meta.getClass().getMethod("getMapView").invoke(meta);
            if (result instanceof MapView) {
                return (MapView) result;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object idObj = meta.getClass().getMethod("getMapId").invoke(meta);
            if (idObj instanceof Number) {
                return getMapById(((Number) idObj).shortValue());
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static MapView getMapById(short id) {
        try {
            Object result = Bukkit.class.getMethod("getMap", short.class).invoke(null, id);
            if (result instanceof MapView) {
                return (MapView) result;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean isFullMap(MapView view) {
        if (view == null) {
            return false;
        }
        int filled = 0;
        int total = 0;
        for (int x = 0; x < MAP_SIZE; x++) {
            if (hasContent(view, x, 0)) {
                filled++;
            }
            if (hasContent(view, x, MAP_SIZE - 1)) {
                filled++;
            }
            total += 2;
        }
        for (int y = 1; y < MAP_SIZE - 1; y++) {
            if (hasContent(view, 0, y)) {
                filled++;
            }
            if (hasContent(view, MAP_SIZE - 1, y)) {
                filled++;
            }
            total += 2;
        }
        return total != 0 && (double) filled / total >= FULL_EDGE_RATIO;
    }

    private static boolean hasContent(MapView view, int x, int y) {
        Byte color = readPixel(view, x, y);
        return color != null && color != 0;
    }

    private static Byte readPixel(MapView view, int x, int y) {
        try {
            Object result = view.getClass().getMethod("getPixel", int.class, int.class)
                    .invoke(view, x, y);
            if (result instanceof Byte) {
                return (Byte) result;
            }
            if (result instanceof Number) {
                return ((Number) result).byteValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static ItemStack copyToNewMap(MapView source, World world) {
        if (source == null || world == null) {
            return null;
        }
        try {
            byte[] pixels = new byte[MAP_SIZE * MAP_SIZE];
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    Byte color = readPixel(source, x, y);
                    pixels[x + y * MAP_SIZE] = color == null ? 0 : color;
                }
            }
            MapView newView = Bukkit.createMap(world);
            for (MapRenderer renderer : newView.getRenderers()) {
                newView.removeRenderer(renderer);
            }
            newView.addRenderer(new StaticMapRenderer(pixels));

            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                setMapView(meta, newView);
                item.setItemMeta(meta);
            }
            return item;
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static void setMapView(ItemMeta meta, MapView view) {
        try {
            meta.getClass().getMethod("setMapView", MapView.class).invoke(meta, view);
            return;
        } catch (Throwable ignored) {
        }
        try {
            Object idObj = view.getClass().getMethod("getId").invoke(view);
            if (idObj instanceof Number) {
                meta.getClass().getMethod("setMapId", short.class)
                        .invoke(meta, ((Number) idObj).shortValue());
            }
        } catch (Throwable ignored) {
        }
    }

    private static class StaticMapRenderer extends MapRenderer {
        private final byte[] pixels;
        private boolean rendered;

        private StaticMapRenderer(byte[] pixels) {
            super(false);
            this.pixels = pixels;
        }

        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
            if (rendered) {
                return;
            }
            try {
                Method setPixel = canvas.getClass().getMethod("setPixel",
                        int.class, int.class, byte.class);
                for (int x = 0; x < MAP_SIZE; x++) {
                    for (int y = 0; y < MAP_SIZE; y++) {
                        setPixel.invoke(canvas, x, y, pixels[x + y * MAP_SIZE]);
                    }
                }
            } catch (Throwable ignored) {
            }
            rendered = true;
        }
    }
}

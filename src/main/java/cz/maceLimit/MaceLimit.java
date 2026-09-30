package cz.maceLimit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Jediná věc, co tenhle plugin dělá: zabrání vložení vybraných itemů do
 * jakéhokoliv kontejneru (truhla, sud, shulker, ender chest, hopper, ...).
 * Funguje pro:
 *  - jednotlivé kliknutí (přesun kliknutím / shift-klik)
 *  - tažení myší přes více slotů (drag)
 *  - i schované uvnitř shulker boxu nebo bundle (kontrola obsahu)
 *
 * Co je blokováno, řeší isBlocked() - uprav Material.MACE a BLOCKED_NAMES
 * podle toho, co přesně chceš zakázat.
 */
public class MaceLimit extends JavaPlugin implements Listener {

    private static final Set<Material> SHULKER_MATERIALS = Set.of(
            Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX
    );

    private static final Set<String> BLOCKED_NAMES = Set.of(
            "Hyperion", "Frost Scythe", "Nightpiercer", "Bone Blade", "Pure Blade",
            "Earth Gauntlet", "Cutlass", "Vulcan's Crossbow", "Bloodlust", "Witherbone",
            "Windweaver", "Shadow Blade", "Knightfall",
            "Copper Pickaxe", "Copper Helmet", "Copper Chestplate", "Copper Leggings", "Copper Boots"
    );

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MaceLimit (container-ban) enabled");
    }

    // -----------------------------------------------------------------------
    // Kliknutí - přesun kliknutím z/do kontejneru, shift-klik, hotbar-swap,
    // a plnění bundle (bundle se dá plnit i uvnitř vlastního inventáře,
    // takže na "cizí kontejner" se tady neváže)
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack incoming = incomingItem(event);

        // plnění bundle - ať už bundle sedí ve slotu a přidáváš do něj (incoming),
        // nebo máš bundle na kurzoru a klikáš na slot s blokovaným itemem
        boolean fillsBundle = (currentItem != null && currentItem.getType() == Material.BUNDLE
                        && incoming != null && isBlocked(incoming))
                || (incoming != null && incoming.getType() == Material.BUNDLE
                        && currentItem != null && isBlocked(currentItem));
        if (fillsBundle) {
            event.setCancelled(true);
            deny(player);
            return;
        }

        InventoryType clickedType = clickedInv.getType();
        boolean clickedIsExternal = clickedType != InventoryType.PLAYER
                && clickedType != InventoryType.CRAFTING;

        // přímé vložení do cizího kontejneru - kliknutí i hotbar-swap (1-9 / F)
        if (!event.isShiftClick() && clickedIsExternal) {
            if (incoming != null && isBlocked(incoming)) {
                event.setCancelled(true);
                deny(player);
                return;
            }
        }

        // shift-klik z vlastního inventáře do otevřeného cizího kontejneru
        if (event.isShiftClick() && !clickedIsExternal
                && event.getInventory().getType() != InventoryType.PLAYER
                && event.getInventory().getType() != InventoryType.CRAFTING) {
            if (currentItem != null && isBlocked(currentItem)) {
                event.setCancelled(true);
                deny(player);
            }
        }
    }

    /** Item, který se tímhle kliknutím do slotu vloží - ať už jde o klasický klik s kurzorem, nebo hotbar-swap. */
    private ItemStack incomingItem(InventoryClickEvent event) {
        return switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP -> event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            default -> null;
        };
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.RED + "Tuto vec nelze ulozit do zadneho kontejneru!");
    }

    // -----------------------------------------------------------------------
    // Tažení myší (drag) přes více slotů - bez tohohle by šlo blok obejít
    // -----------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isBlocked(event.getOldCursor())) return;

        Inventory top = event.getView().getTopInventory();
        InventoryType topType = top.getType();
        boolean topIsExternal = topType != InventoryType.PLAYER && topType != InventoryType.CRAFTING;
        if (!topIsExternal) return;

        int topSize = top.getSize();
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesTop) {
            event.setCancelled(true);
            deny(player);
        }
    }

    // -----------------------------------------------------------------------
    // Co je vlastně "blokované" - uprav podle potřeby
    // -----------------------------------------------------------------------
    private boolean isBlocked(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == Material.MACE) return true;
        if (isBlockedByName(item)) return true;

        if (SHULKER_MATERIALS.contains(item.getType())) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox shulker) {
                for (ItemStack content : shulker.getInventory().getContents()) {
                    if (content != null && (content.getType() == Material.MACE || isBlockedByName(content))) {
                        return true;
                    }
                }
            }
        }

        if (item.getType() == Material.BUNDLE) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BundleMeta bundle) {
                for (ItemStack content : bundle.getItems()) {
                    if (content != null && (content.getType() == Material.MACE || isBlockedByName(content))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isBlockedByName(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // Adventure API (moderní, spolehlivé i s barvami/formátováním)
        if (meta.displayName() != null) {
            String adventureName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName()).trim();
            if (BLOCKED_NAMES.contains(adventureName)) return true;
        }

        // Záloha - legacy display name
        if (meta.hasDisplayName()) {
            String legacyName = ChatColor.stripColor(meta.getDisplayName()).trim();
            if (BLOCKED_NAMES.contains(legacyName)) return true;
        }

        return false;
    }
}

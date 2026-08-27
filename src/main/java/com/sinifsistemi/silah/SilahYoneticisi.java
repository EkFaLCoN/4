package com.sinifsistemi.silah;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.SinifSistemi;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Ozel silahlarin ItemStack olarak uretilmesi ve taninmasi.
 */
public class SilahYoneticisi {

    private final SinifSistemi eklenti;
    private final NamespacedKey anahtar;

    public SilahYoneticisi(SinifSistemi eklenti) {
        this.eklenti = eklenti;
        this.anahtar = new NamespacedKey(eklenti, "ozel_silah");
    }

    public NamespacedKey anahtar() {
        return anahtar;
    }

    /** Silahin oyun ici esyasini uretir. */
    public ItemStack uret(Silah silah) {
        ItemStack esya = new ItemStack(silah.taban());
        ItemMeta meta = esya.getItemMeta();

        meta.displayName(Mesaj.esya(silah.renkliAd()));

        List<Component> lore = new ArrayList<>();
        for (String satir : silah.aciklama()) {
            lore.add(Mesaj.esya(satir));
        }
        meta.lore(lore);

        // Resourcepack modeli
        try {
            String[] parca = silah.modelKimligi().split(":", 2);
            meta.setItemModel(new NamespacedKey(parca[0], parca[1]));
        } catch (Throwable hata) {
            eklenti.getLogger().warning("item_model ayarlanamadi (" + silah.kod()
                    + "): " + hata.getMessage());
        }

        meta.addEnchant(Enchantment.SHARPNESS, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE);

        meta.getPersistentDataContainer().set(anahtar, PersistentDataType.STRING, silah.kod());

        esya.setItemMeta(meta);
        return esya;
    }

    /** Esya bir ozel silah mi? Degilse null. */
    public Silah tani(ItemStack esya) {
        if (esya == null || esya.getType().isAir() || !esya.hasItemMeta()) {
            return null;
        }
        String kod = esya.getItemMeta().getPersistentDataContainer()
                .get(anahtar, PersistentDataType.STRING);
        return kod == null ? null : Silah.bul(kod);
    }
}

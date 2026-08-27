package com.sinifsistemi.menu;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Sinif secim ekrani. InventoryHolder olarak kendisini tasir, boylece
 * dinleyici menuyu baslikla degil, nesne kimligiyle tanir.
 */
public class SinifMenusu implements InventoryHolder {

    private final SinifSistemi eklenti;
    private final Player oyuncu;
    private final Sinif mevcutSinif;
    private final boolean kilitli;
    private Inventory envanter;

    public SinifMenusu(SinifSistemi eklenti, Player oyuncu) {
        this.eklenti = eklenti;
        this.oyuncu = oyuncu;
        this.mevcutSinif = eklenti.getDepo().getSinif(oyuncu);
        boolean birKere = eklenti.getConfig().getBoolean("ayarlar.bir-kere-secim", true);
        this.kilitli = mevcutSinif != null && birKere && !oyuncu.hasPermission("sinif.degistir");
        olustur();
    }

    private void olustur() {
        int satir = Math.max(3, Math.min(6, eklenti.getConfig().getInt("menu.satir-sayisi", 5)));
        String baslik = eklenti.getConfig().getString("menu.baslik", "<gold>Sınıf Seç");

        envanter = Bukkit.createInventory(this, satir * 9, Mesaj.ayristir(baslik));

        // Arka plan dolgusu
        Material dolguMateryali = materyalGetir(
                eklenti.getConfig().getString("menu.dolgu-materyali", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE);
        ItemStack dolgu = new ItemStack(dolguMateryali);
        ItemMeta dolguMeta = dolgu.getItemMeta();
        dolguMeta.displayName(Component.empty());
        dolgu.setItemMeta(dolguMeta);

        for (int i = 0; i < envanter.getSize(); i++) {
            envanter.setItem(i, dolgu);
        }

        // Sinif ikonlari
        for (Sinif sinif : Sinif.values()) {
            int slot = sinif.slot();
            if (slot >= envanter.getSize()) {
                continue;
            }
            envanter.setItem(slot, sinifEsyasi(sinif));
        }

        // Bilgi kitabi (alt orta)
        int bilgiSlot = envanter.getSize() - 5;
        envanter.setItem(bilgiSlot, bilgiEsyasi());
    }

    private ItemStack sinifEsyasi(Sinif sinif) {
        ItemStack esya = new ItemStack(sinif.ikon());

        // Rahip icin "Anında Can İksiri" gorunumu
        if (sinif.ikon() == Material.POTION && esya.getItemMeta() instanceof PotionMeta iksirMeta) {
            iksirMeta.setBasePotionType(PotionType.HEALING);
            esya.setItemMeta(iksirMeta);
        }

        ItemMeta meta = esya.getItemMeta();
        meta.displayName(Mesaj.esya(sinif.renkliAd()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (String satir : sinif.aciklama()) {
            lore.add(Mesaj.esya(satir));
        }
        lore.add(Component.empty());
        lore.add(Mesaj.esya(Mesaj.menuMetni("ozellik-basligi")));
        for (String satir : sinif.yetenekler()) {
            lore.add(Mesaj.esya(satir));
        }
        lore.add(Component.empty());

        if (mevcutSinif == sinif) {
            lore.add(Mesaj.esya(Mesaj.menuMetni("mevcut-sinif")));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        } else if (kilitli) {
            lore.add(Mesaj.esya(Mesaj.menuMetni("kilitli-butonu")));
        } else {
            lore.add(Mesaj.esya(Mesaj.menuMetni("sec-butonu")));
            if (eklenti.getConfig().getBoolean("ayarlar.bir-kere-secim", true)) {
                lore.add(Mesaj.esya(Mesaj.menuMetni("uyari-satiri")));
            }
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE);
        esya.setItemMeta(meta);
        return esya;
    }

    private ItemStack bilgiEsyasi() {
        ItemStack esya = new ItemStack(Material.BOOK);
        ItemMeta meta = esya.getItemMeta();
        meta.displayName(Mesaj.esya("<gold><bold>Nasıl Çalışır?"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Mesaj.esya("<gray>Aşağıdaki sınıflardan birine tıklayarak"));
        lore.add(Mesaj.esya("<gray>karakterinin yolunu belirle."));
        lore.add(Component.empty());
        if (eklenti.getConfig().getBoolean("ayarlar.bir-kere-secim", true)) {
            lore.add(Mesaj.esya("<red>⚠ <gray>Bu seçim sunucuda <white>yalnızca 1 kez"));
            lore.add(Mesaj.esya("<gray>yapılabilir ve <red>geri alınamaz<gray>."));
            lore.add(Mesaj.esya("<gray>Değişiklik için yetkiliyle görüşmelisin."));
        } else {
            lore.add(Mesaj.esya("<green>✔ <gray>Sınıfını istediğin zaman değiştirebilirsin."));
        }
        lore.add(Component.empty());
        if (mevcutSinif != null) {
            lore.add(Mesaj.esya("<gray>Mevcut sınıfın: " + mevcutSinif.basitRenkliAd()));
        }
        meta.lore(lore);
        esya.setItemMeta(meta);
        return esya;
    }

    private Material materyalGetir(String ad, Material varsayilan) {
        if (ad == null) {
            return varsayilan;
        }
        Material materyal = Material.matchMaterial(ad.toUpperCase(java.util.Locale.ROOT));
        return materyal == null ? varsayilan : materyal;
    }

    public void ac() {
        oyuncu.openInventory(envanter);
    }

    public boolean isKilitli() {
        return kilitli;
    }

    public Player getOyuncu() {
        return oyuncu;
    }

    @Override
    public Inventory getInventory() {
        return envanter;
    }
}

package com.sinifsistemi.dinleyici;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import com.sinifsistemi.menu.SinifMenusu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Locale;

public class MenuDinleyici implements Listener {

    private final SinifSistemi eklenti;

    public MenuDinleyici(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    @EventHandler
    public void tiklamaOlayi(InventoryClickEvent olay) {
        Inventory ustEnvanter = olay.getView().getTopInventory();
        if (!(ustEnvanter.getHolder() instanceof SinifMenusu menu)) {
            return;
        }

        olay.setCancelled(true);

        if (!(olay.getWhoClicked() instanceof Player oyuncu)) {
            return;
        }
        if (olay.getClickedInventory() == null || !olay.getClickedInventory().equals(ustEnvanter)) {
            return;
        }
        if (olay.getCurrentItem() == null || olay.getCurrentItem().getType().isAir()) {
            return;
        }

        Sinif secilen = null;
        for (Sinif sinif : Sinif.values()) {
            if (sinif.slot() == olay.getRawSlot()) {
                secilen = sinif;
                break;
            }
        }
        if (secilen == null) {
            return;
        }

        // Zaten sinifi varsa ve kilitliyse
        if (menu.isKilitli()) {
            Sinif mevcut = eklenti.getDepo().getSinif(oyuncu);
            ses(oyuncu, "ENTITY_VILLAGER_NO");
            Mesaj.gonder(oyuncu, "zaten-sinifin-var",
                    Mesaj.degisken("sinif", mevcut == null ? "-" : mevcut.basitRenkliAd()));
            oyuncu.closeInventory();
            return;
        }

        Sinif mevcut = eklenti.getDepo().getSinif(oyuncu);
        if (mevcut == secilen) {
            ses(oyuncu, "ENTITY_VILLAGER_NO");
            return;
        }

        eklenti.getDepo().ayarla(oyuncu.getUniqueId(), oyuncu.getName(), secilen);
        eklenti.getYetenekYoneticisi().yenidenUygula(oyuncu);

        oyuncu.closeInventory();
        ses(oyuncu, eklenti.getConfig().getString("menu.secim-sesi", "ENTITY_PLAYER_LEVELUP"));

        Mesaj.gonder(oyuncu, "secim-basarili", Mesaj.degisken("sinif", secilen.renkliAd()));
        oyuncu.sendMessage(Mesaj.ayristir("<dark_gray><strikethrough>                                        "));
        oyuncu.sendMessage(Mesaj.ayristir(" " + secilen.renkliAd()));
        oyuncu.sendMessage(Mesaj.ayristir(""));
        for (String satir : secilen.yetenekler()) {
            oyuncu.sendMessage(Mesaj.ayristir(" " + satir));
        }
        oyuncu.sendMessage(Mesaj.ayristir("<dark_gray><strikethrough>                                        "));

        oyuncu.showTitle(net.kyori.adventure.title.Title.title(
                Mesaj.ayristir(secilen.renkliAd()),
                Mesaj.ayristir("<gray>Yeni yolun hayırlı olsun!")
        ));
    }

    @EventHandler
    public void surukleOlayi(InventoryDragEvent olay) {
        if (olay.getView().getTopInventory().getHolder() instanceof SinifMenusu) {
            olay.setCancelled(true);
        }
    }

    @EventHandler
    public void kapatmaOlayi(InventoryCloseEvent olay) {
        if (!(olay.getInventory().getHolder() instanceof SinifMenusu)) {
            return;
        }
        if (!(olay.getPlayer() instanceof Player oyuncu)) {
            return;
        }
        if (eklenti.getConfig().getBoolean("ayarlar.menu-kapatilabilir", false)) {
            return;
        }
        if (eklenti.getDepo().sinifiVar(oyuncu)) {
            return;
        }
        if (!eklenti.getConfig().getBoolean("ayarlar.giriste-menu-ac", true)) {
            return;
        }
        if (oyuncu.hasPermission("sinif.admin")) {
            return;
        }

        // Sinif secmeden kapatti -> tekrar ac
        Bukkit.getScheduler().runTaskLater(eklenti, () -> {
            if (oyuncu.isOnline() && !eklenti.getDepo().sinifiVar(oyuncu)) {
                Mesaj.gonder(oyuncu, "menu-zorunlu");
                new SinifMenusu(eklenti, oyuncu).ac();
            }
        }, 5L);
    }

    /**
     * Ses adini surumden bagimsiz sekilde calar.
     * "ENTITY_PLAYER_LEVELUP" -> "entity.player.levelup"
     */
    private void ses(Player oyuncu, String sesAdi) {
        if (sesAdi == null || sesAdi.isBlank()) {
            return;
        }
        String anahtar = sesAdi.toLowerCase(Locale.ROOT).replace('_', '.');
        try {
            oyuncu.playSound(oyuncu.getLocation(), anahtar, 1.0f, 1.0f);
        } catch (Exception yoksay) {
            // Gecersiz ses adi -> sessizce gec
        }
    }
}

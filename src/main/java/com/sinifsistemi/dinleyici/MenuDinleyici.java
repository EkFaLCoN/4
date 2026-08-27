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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MenuDinleyici implements Listener {

    private final SinifSistemi eklenti;
    private final Map<UUID, Long> sonUyari = new HashMap<>();
    private final Map<UUID, Long> sonAcilis = new HashMap<>();
    private final Set<UUID> menusuAcik = new HashSet<>();
    private BukkitTask denetimGorevi;

    /** Ayni oyuncuya bu araliktan daha sik menu acilmaz (ms). */
    private static final long ACILIS_ARALIGI = 1500L;

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
        // Ust envantere mi tiklandi? Referans karsilastirmasi yerine slot araligi
        // kullanilir; getClickedInventory() bazi surumlerde farkli bir sarmalayici
        // nesne dondurdugu icin equals() guvenilir degildir.
        int rawSlot = olay.getRawSlot();
        if (rawSlot < 0 || rawSlot >= ustEnvanter.getSize()) {
            return;
        }
        if (olay.getCurrentItem() == null || olay.getCurrentItem().getType().isAir()) {
            return;
        }

        Sinif secilen = menu.sinifSlotta(rawSlot);
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
    public void acilmaOlayi(InventoryOpenEvent olay) {
        if (olay.getInventory().getHolder() instanceof SinifMenusu
                && olay.getPlayer() instanceof Player oyuncu) {
            menusuAcik.add(oyuncu.getUniqueId());
        }
    }

    @EventHandler
    public void cikisOlayi(PlayerQuitEvent olay) {
        UUID kimlik = olay.getPlayer().getUniqueId();
        menusuAcik.remove(kimlik);
        sonUyari.remove(kimlik);
        sonAcilis.remove(kimlik);
    }

    @EventHandler
    public void kapatmaOlayi(InventoryCloseEvent olay) {
        if (!(olay.getInventory().getHolder() instanceof SinifMenusu)) {
            return;
        }
        if (!(olay.getPlayer() instanceof Player oyuncu)) {
            return;
        }
        menusuAcik.remove(oyuncu.getUniqueId());

        if (!zorunlu(oyuncu)) {
            return;
        }

        // Sinif secmeden kapatti -> kisa gecikmeyle tekrar ac
        Bukkit.getScheduler().runTaskLater(eklenti, () -> menuyuAc(oyuncu, true), 3L);
    }

    /**
     * Menuyu acar. Ayni oyuncuya cok sik acilmasini engelleyerek
     * "acilip kapanma" dongusunu imkansiz kilar.
     */
    public void menuyuAc(Player oyuncu, boolean uyar) {
        if (!zorunlu(oyuncu)) {
            return;
        }
        long simdi = System.currentTimeMillis();
        Long son = sonAcilis.get(oyuncu.getUniqueId());
        if (son != null && simdi - son < ACILIS_ARALIGI) {
            return;
        }
        sonAcilis.put(oyuncu.getUniqueId(), simdi);

        if (uyar) {
            uyarSinirli(oyuncu);
        }
        new SinifMenusu(eklenti, oyuncu).ac();
    }

    /**
     * Oyuncunun sinif secim menusune mahkum olup olmadigi.
     * Sadece "menu-kapatilabilir: true" ayari veya "sinif.menu.muaf" izni kacis saglar.
     */
    private boolean zorunlu(Player oyuncu) {
        if (!oyuncu.isOnline()) {
            return false;
        }
        if (eklenti.getDepo().sinifiVar(oyuncu)) {
            return false;
        }
        if (eklenti.getConfig().getBoolean("ayarlar.menu-kapatilabilir", false)) {
            return false;
        }
        return !oyuncu.hasPermission("sinif.menu.muaf");
    }

    /** Menu her acildiginda spam olmasin diye uyariyi sinirlar. */
    private void uyarSinirli(Player oyuncu) {
        long simdi = System.currentTimeMillis();
        Long son = sonUyari.get(oyuncu.getUniqueId());
        if (son != null && simdi - son < 10000L) {
            return;
        }
        sonUyari.put(oyuncu.getUniqueId(), simdi);
        Mesaj.gonder(oyuncu, "menu-zorunlu");
    }

    /**
     * Denetim gorevi: sinifsiz bir oyuncunun menusu herhangi bir sebeple
     * kapaliysa (olum, dunya degisimi, komut, baska bir eklenti, /close vb.)
     * menuyu tekrar onune getirir.
     */
    public void gorevBaslat() {
        gorevDurdur();
        long aralik = Math.max(5L, eklenti.getConfig().getLong("ayarlar.menu-denetim-araligi", 20L));
        denetimGorevi = Bukkit.getScheduler().runTaskTimer(eklenti, () -> {
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                if (!zorunlu(oyuncu)) {
                    continue;
                }
                // Menusu zaten acik olanlara dokunma
                if (menusuAcik.contains(oyuncu.getUniqueId())) {
                    continue;
                }
                if (oyuncu.getOpenInventory().getTopInventory().getHolder() instanceof SinifMenusu) {
                    menusuAcik.add(oyuncu.getUniqueId());
                    continue;
                }
                menuyuAc(oyuncu, true);
            }
        }, 40L, aralik);
    }

    public void gorevDurdur() {
        if (denetimGorevi != null) {
            denetimGorevi.cancel();
            denetimGorevi = null;
        }
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

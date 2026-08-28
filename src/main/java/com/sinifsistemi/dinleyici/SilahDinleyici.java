package com.sinifsistemi.dinleyici;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import com.sinifsistemi.silah.KanamaYoneticisi;
import com.sinifsistemi.silah.Silah;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ozel silahlarin sinif kisiti, pasif efektleri ve yetenekleri.
 */
public class SilahDinleyici implements Listener {

    private final SinifSistemi eklenti;
    private final Map<UUID, Long> uyariZamani = new HashMap<>();
    private final Map<UUID, Long> yetenekBekleme = new HashMap<>();
    private BukkitTask denetimGorevi;

    public SilahDinleyici(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    /** Elde tutma denetimi + kan damlama parcaciklari. */
    public void gorevBaslat() {
        gorevDurdur();
        denetimGorevi = Bukkit.getScheduler().runTaskTimer(eklenti, () -> {
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                ItemStack elde = oyuncu.getInventory().getItemInMainHand();
                Silah silah = eklenti.getSilahYoneticisi().tani(elde);
                if (silah == null) {
                    continue;
                }

                if (!kullanabilir(oyuncu, silah)) {
                    elindenAl(oyuncu, silah, elde);
                    continue;
                }

                // Sol agizdan sizan kan
                if (eklenti.getConfig().getBoolean("silahlar.kan-damlama-efekti", true)) {
                    damlaEfekti(oyuncu);
                }
            }
        }, 20L, 10L);
    }

    public void gorevDurdur() {
        if (denetimGorevi != null) {
            denetimGorevi.cancel();
            denetimGorevi = null;
        }
    }

    // ---------------- Sinif kisiti ----------------

    private boolean kullanabilir(Player oyuncu, Silah silah) {
        if (oyuncu.hasPermission("sinif.silah.serbest")) {
            return true;
        }
        return eklenti.getDepo().getSinif(oyuncu) == silah.gerekliSinif();
    }

    private void uyar(Player oyuncu, Silah silah) {
        long simdi = System.currentTimeMillis();
        Long son = uyariZamani.get(oyuncu.getUniqueId());
        if (son != null && simdi - son < 3000L) {
            return;
        }
        uyariZamani.put(oyuncu.getUniqueId(), simdi);

        Mesaj.gonder(oyuncu, "silah-sinif-uyusmuyor", Mesaj.degisken(
                "silah", silah.renkliAd(),
                "sinif", silah.gerekliSinif().basitRenkliAd()));
        oyuncu.playSound(oyuncu.getLocation(), "entity.villager.no", 1.0f, 0.8f);
    }

    /** Yanlis sinif silahi elinde tutamaz: envantere geri konur. */
    private void elindenAl(Player oyuncu, Silah silah, ItemStack esya) {
        uyar(oyuncu, silah);

        if (!eklenti.getConfig().getBoolean("silahlar.yanlis-sinifta-elden-cikar", true)) {
            return;
        }

        oyuncu.getInventory().setItemInMainHand(null);
        Map<Integer, ItemStack> artan = oyuncu.getInventory().addItem(esya);
        for (ItemStack kalan : artan.values()) {
            oyuncu.getWorld().dropItemNaturally(oyuncu.getLocation(), kalan);
        }
        oyuncu.updateInventory();
    }

    // ---------------- Vurus ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void vurus(EntityDamageByEntityEvent olay) {
        if (!(olay.getDamager() instanceof Player saldiran)) {
            return;
        }
        ItemStack elde = saldiran.getInventory().getItemInMainHand();
        Silah silah = eklenti.getSilahYoneticisi().tani(elde);
        if (silah == null) {
            return;
        }

        if (!kullanabilir(saldiran, silah)) {
            olay.setCancelled(true);
            uyar(saldiran, silah);
            return;
        }

        double carpan = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".hasar-carpani", 1.30);
        olay.setDamage(olay.getDamage() * carpan);

        if (!(olay.getEntity() instanceof LivingEntity hedef)) {
            return;
        }

        int sure = eklenti.getConfig()
                .getInt("silahlar." + silah.kod() + ".kanama-suresi", 4);
        double kanamaHasari = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".kanama-hasari", 1.0);
        double emis = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".can-emis-orani", 0.35);

        if (sure > 0 && kanamaHasari > 0) {
            eklenti.getKanamaYoneticisi().uygula(hedef, saldiran, sure, kanamaHasari, emis);
        }

        KanamaYoneticisi.parcacik(hedef.getLocation());
        saldiran.playSound(hedef.getLocation(), "entity.player.attack.crit", 0.8f, 0.7f);
    }

    // ---------------- Sag tik yetenegi: Kan Cagrisi ----------------

    @EventHandler
    public void sagTik(PlayerInteractEvent olay) {
        Action eylem = olay.getAction();
        if (eylem != Action.RIGHT_CLICK_AIR && eylem != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (olay.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        Player oyuncu = olay.getPlayer();
        Silah silah = eklenti.getSilahYoneticisi()
                .tani(oyuncu.getInventory().getItemInMainHand());
        if (silah == null) {
            return;
        }

        olay.setCancelled(true);

        if (!kullanabilir(oyuncu, silah)) {
            uyar(oyuncu, silah);
            return;
        }

        int bekleme = eklenti.getConfig()
                .getInt("silahlar." + silah.kod() + ".yetenek-bekleme", 20);
        long simdi = System.currentTimeMillis();
        Long son = yetenekBekleme.get(oyuncu.getUniqueId());
        if (son != null && simdi - son < bekleme * 1000L) {
            long kalan = (bekleme * 1000L - (simdi - son) + 999) / 1000;
            Mesaj.gonder(oyuncu, "yetenek-bekleme",
                    Mesaj.degisken("saniye", String.valueOf(kalan)));
            return;
        }
        yetenekBekleme.put(oyuncu.getUniqueId(), simdi);

        kanCagrisi(oyuncu, silah);
    }

    private void kanCagrisi(Player oyuncu, Silah silah) {
        double menzil = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".yetenek-menzili", 4.5);
        int sure = eklenti.getConfig()
                .getInt("silahlar." + silah.kod() + ".yetenek-kanama-suresi", 5);
        double hasar = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".kanama-hasari", 1.0);
        double emis = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".can-emis-orani", 0.35);

        int sayac = 0;
        for (var varlik : oyuncu.getNearbyEntities(menzil, menzil, menzil)) {
            if (!(varlik instanceof LivingEntity hedef) || hedef.equals(oyuncu)) {
                continue;
            }
            eklenti.getKanamaYoneticisi().uygula(hedef, oyuncu, sure, hasar, emis);
            KanamaYoneticisi.parcacik(hedef.getLocation());
            sayac++;
        }

        // Kullaniciya kisa sureli guc (potion effect degil, gecici attribute)
        double gucOrani = eklenti.getConfig()
                .getDouble("silahlar." + silah.kod() + ".yetenek-guc-yuzde", 0.25);
        long gucSuresi = eklenti.getConfig()
                .getLong("silahlar." + silah.kod() + ".yetenek-guc-suresi", 5) * 20L;
        eklenti.getYetenekYoneticisi().geciciSaldiriGucu(oyuncu, gucOrani, gucSuresi);

        // Gorsel halka
        Location merkez = oyuncu.getLocation();
        if (merkez.getWorld() != null) {
            for (int i = 0; i < 40; i++) {
                double aci = Math.PI * 2 * i / 40.0;
                Location nokta = merkez.clone().add(
                        Math.cos(aci) * menzil, 0.25, Math.sin(aci) * menzil);
                merkez.getWorld().spawnParticle(Particle.DUST, nokta, 2, 0.05, 0.1, 0.05, 0.0,
                        new Particle.DustOptions(Color.fromRGB(170, 14, 18), 1.4f));
            }
            merkez.getWorld().playSound(merkez, "entity.ravager.roar", 0.7f, 1.4f);
        }

        Mesaj.gonder(oyuncu, "kan-cagrisi", Mesaj.degisken("sayi", String.valueOf(sayac)));
    }

    // ---------------- Damlama efekti ----------------

    private void damlaEfekti(Player oyuncu) {
        Location konum = oyuncu.getLocation();
        if (konum.getWorld() == null || Math.random() > 0.55) {
            return;
        }
        Location nokta = konum.clone().add(
                (Math.random() - 0.5) * 0.7, 1.05, (Math.random() - 0.5) * 0.7);
        konum.getWorld().spawnParticle(Particle.DUST, nokta, 1, 0.02, 0.02, 0.02, 0.0,
                new Particle.DustOptions(Color.fromRGB(126, 10, 14), 0.8f));
    }
}

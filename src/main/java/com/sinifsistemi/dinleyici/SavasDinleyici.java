package com.sinifsistemi.dinleyici;

import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

/**
 * Siniflara ozel hasar ve sifa hesaplamalari.
 */
public class SavasDinleyici implements Listener {

    private final SinifSistemi eklenti;

    public SavasDinleyici(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    // ---------------- Genel hasar (dusme, buyu) ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void genelHasar(EntityDamageEvent olay) {
        if (!(olay.getEntity() instanceof Player oyuncu)) {
            return;
        }
        Sinif sinif = eklenti.getDepo().getSinif(oyuncu);
        if (sinif == null) {
            return;
        }

        // Okcu: dusme hasari muafiyeti
        if (sinif == Sinif.OKCU
                && olay.getCause() == EntityDamageEvent.DamageCause.FALL
                && eklenti.getConfig().getBoolean("siniflar.okcu.dusme-hasari-muafiyeti", true)) {
            olay.setCancelled(true);
            return;
        }

        // Buyucu: buyu ve iksir hasarina direnc
        if (sinif == Sinif.BUYUCU) {
            EntityDamageEvent.DamageCause sebep = olay.getCause();
            if (sebep == EntityDamageEvent.DamageCause.MAGIC
                    || sebep == EntityDamageEvent.DamageCause.POISON
                    || sebep == EntityDamageEvent.DamageCause.WITHER
                    || sebep == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
                double carpan = eklenti.getConfig().getDouble("siniflar.buyucu.sihir-hasari-direnci", 0.50);
                olay.setDamage(olay.getDamage() * carpan);
            }
        }
    }

    // ---------------- Varliklar arasi hasar ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void varlikHasari(EntityDamageByEntityEvent olay) {
        Entity vuran = olay.getDamager();

        // 1) Mermi (ok / iksir) hasari
        if (vuran instanceof Projectile mermi) {
            mermiHasari(olay, mermi);
        }
        // 2) Yakin dovus
        else if (vuran instanceof Player saldiran) {
            yakinDovus(olay, saldiran);
        }

        // 3) Hedefin sinifina gore alinan hasar duzenlemesi
        if (olay.getEntity() instanceof Player hedef) {
            alinanHasar(olay, hedef, vuran);
        }
    }

    private void mermiHasari(EntityDamageByEntityEvent olay, Projectile mermi) {
        ProjectileSource kaynak = mermi.getShooter();
        if (!(kaynak instanceof Player atici)) {
            return;
        }
        Sinif sinif = eklenti.getDepo().getSinif(atici);
        if (sinif == null) {
            return;
        }

        if (sinif == Sinif.OKCU && mermi instanceof AbstractArrow) {
            double carpan = eklenti.getConfig().getDouble("siniflar.okcu.ok-hasar-carpani", 1.35);
            olay.setDamage(olay.getDamage() * carpan);
        }

        if (sinif == Sinif.AVCI && ucDisliMermiMi(mermi)) {
            double carpan = eklenti.getConfig()
                    .getDouble("siniflar.avci.firlatilan-ucdisli-carpani", 1.30);
            olay.setDamage(olay.getDamage() * carpan);
        }

        if (sinif == Sinif.BUYUCU && mermi instanceof ThrownPotion) {
            double carpan = eklenti.getConfig().getDouble("siniflar.buyucu.iksir-hasar-carpani", 1.40);
            olay.setDamage(olay.getDamage() * carpan);
        }
    }

    private void yakinDovus(EntityDamageByEntityEvent olay, Player saldiran) {
        Sinif sinif = eklenti.getDepo().getSinif(saldiran);
        if (sinif == null) {
            return;
        }

        double carpan = 1.0;
        switch (sinif) {
            case RAHIP -> carpan = eklenti.getConfig()
                    .getDouble("siniflar.rahip.yakin-dovus-carpani", 0.85);
            case OKCU -> carpan = eklenti.getConfig()
                    .getDouble("siniflar.okcu.yakin-dovus-carpani", 0.90);
            case AVCI -> {
                Material elde = saldiran.getInventory().getItemInMainHand().getType();
                if (mizrakMi(elde)) {
                    carpan = eklenti.getConfig()
                            .getDouble("siniflar.avci.mizrak-hasar-carpani", 1.30);
                } else if (ucDisliMi(elde)) {
                    carpan = eklenti.getConfig()
                            .getDouble("siniflar.avci.ucdisli-hasar-carpani", 1.25);
                } else {
                    carpan = eklenti.getConfig()
                            .getDouble("siniflar.avci.diger-silah-carpani", 0.85);
                }
            }
            case SUIKASTCI -> {
                ItemStack elde = saldiran.getInventory().getItemInMainHand();
                if (kilicMi(elde.getType())) {
                    carpan = eklenti.getConfig()
                            .getDouble("siniflar.suikastci.kilic-hasar-carpani", 1.25);
                }
                if (arkadanMi(saldiran, olay.getEntity())) {
                    carpan = Math.max(carpan, eklenti.getConfig()
                            .getDouble("siniflar.suikastci.arkadan-vurus-carpani", 1.60));
                }
            }
            default -> {
                // Savasci'nin saldiri hasari attribute ile verilir,
                // Buyucu icin yakin dovus degisikligi yoktur
            }
        }

        if (carpan != 1.0) {
            olay.setDamage(olay.getDamage() * carpan);
        }
    }

    private void alinanHasar(EntityDamageByEntityEvent olay, Player hedef, Entity vuran) {
        Sinif sinif = eklenti.getDepo().getSinif(hedef);
        if (sinif == null) {
            return;
        }

        if (sinif == Sinif.SAVASCI && vuran instanceof AbstractArrow) {
            double carpan = eklenti.getConfig()
                    .getDouble("siniflar.savasci.alinan-ok-hasari-carpani", 0.85);
            olay.setDamage(olay.getDamage() * carpan);
        }

        if (sinif == Sinif.SUIKASTCI) {
            double carpan = eklenti.getConfig()
                    .getDouble("siniflar.suikastci.alinan-hasar-carpani", 1.10);
            olay.setDamage(olay.getDamage() * carpan);
        }
    }

    // ---------------- Sifa ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void sifaAlma(EntityRegainHealthEvent olay) {
        if (!(olay.getEntity() instanceof Player oyuncu)) {
            return;
        }
        if (eklenti.getDepo().getSinif(oyuncu) != Sinif.RAHIP) {
            return;
        }
        double carpan = eklenti.getConfig().getDouble("siniflar.rahip.alinan-sifa-carpani", 1.50);
        olay.setAmount(olay.getAmount() * carpan);
    }

    // ---------------- Yardimcilar ----------------

    /**
     * Mizrak mi? (wooden/stone/copper/iron/golden/diamond/netherite_spear)
     * Material sabiti yerine isim kontrolu yapilir; boylece yeni eklenen
     * mizrak turleri de otomatik kapsanir ve eski API'ye karsi derlenebilir.
     */
    private boolean mizrakMi(Material materyal) {
        return materyal != null && materyal.name().endsWith("_SPEAR");
    }

    /** Uc disli mizrak (trident). */
    private boolean ucDisliMi(Material materyal) {
        return materyal != null && materyal.name().equals("TRIDENT");
    }

    /** Firlatilmis uc disli mizrak mermisi. */
    private boolean ucDisliMermiMi(Projectile mermi) {
        return mermi != null && mermi.getType().name().equals("TRIDENT");
    }

    private boolean kilicMi(Material materyal) {
        return switch (materyal) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD,
                 DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    /** Saldiran, hedefin arkasinda mi? */
    private boolean arkadanMi(Player saldiran, Entity hedef) {
        Vector hedefBakis = hedef.getLocation().getDirection().setY(0).normalize();
        Vector fark = saldiran.getLocation().toVector()
                .subtract(hedef.getLocation().toVector()).setY(0);
        if (fark.lengthSquared() < 0.0001) {
            return false;
        }
        return hedefBakis.dot(fark.normalize()) < -0.4;
    }
}

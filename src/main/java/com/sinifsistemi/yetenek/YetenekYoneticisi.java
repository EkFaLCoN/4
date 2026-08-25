package com.sinifsistemi.yetenek;

import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Siniflarin surekli (pasif) etkilerini belirli araliklarla uygular.
 */
public class YetenekYoneticisi {

    /** Efekt suresi: gorevden biraz uzun tutulur ki kesinti olmasin. */
    private static final int SURE = 200; // 10 saniye
    private static final long ARALIK = 100L; // 5 saniyede bir

    private final SinifSistemi eklenti;
    private BukkitTask gorev;

    public YetenekYoneticisi(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    public void baslat() {
        durdur();
        gorev = Bukkit.getScheduler().runTaskTimer(eklenti, () -> {
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                uygula(oyuncu);
            }
        }, 40L, ARALIK);
    }

    public void durdur() {
        if (gorev != null) {
            gorev.cancel();
            gorev = null;
        }
    }

    /** Sinif degistiginde eski efektleri temizleyip yenisini uygular. */
    public void yenidenUygula(Player oyuncu) {
        temizle(oyuncu);
        uygula(oyuncu);
    }

    /** Eklenti tarafindan verilen tum pasif efektleri kaldirir. */
    public void temizle(Player oyuncu) {
        List<PotionEffectType> tipler = List.of(
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.RESISTANCE,
                PotionEffectType.SPEED,
                PotionEffectType.REGENERATION,
                PotionEffectType.NIGHT_VISION
        );
        for (PotionEffectType tip : tipler) {
            PotionEffect mevcut = oyuncu.getPotionEffect(tip);
            if (mevcut != null && mevcut.isAmbient()) {
                oyuncu.removePotionEffect(tip);
            }
        }
        // Can siniri degistigi icin fazla cani duzelt
        if (oyuncu.getHealth() > oyuncu.getMaxHealth()) {
            oyuncu.setHealth(oyuncu.getMaxHealth());
        }
    }

    public void uygula(Player oyuncu) {
        Sinif sinif = eklenti.getDepo().getSinif(oyuncu);
        if (sinif == null) {
            return;
        }
        switch (sinif) {
            case SAVASCI -> savasci(oyuncu);
            case SUIKASTCI -> suikastci(oyuncu);
            case RAHIP -> rahip(oyuncu);
            case OKCU -> okcu(oyuncu);
            case BUYUCU -> buyucu(oyuncu);
        }
    }

    // ---------------- Siniflar ----------------

    private void savasci(Player oyuncu) {
        int ekCan = eklenti.getConfig().getInt("siniflar.savasci.ek-can", 4);
        canDuzenle(oyuncu, ekCan);

        int direnc = eklenti.getConfig().getInt("siniflar.savasci.direnc-seviyesi", 0);
        if (direnc >= 0) {
            ver(oyuncu, PotionEffectType.RESISTANCE, direnc);
        }
    }

    private void suikastci(Player oyuncu) {
        int hiz = eklenti.getConfig().getInt("siniflar.suikastci.hiz-seviyesi", 0);
        if (hiz >= 0) {
            ver(oyuncu, PotionEffectType.SPEED, hiz);
        }
    }

    private void rahip(Player oyuncu) {
        int yenilenme = eklenti.getConfig().getInt("siniflar.rahip.yenilenme-seviyesi", 0);
        if (yenilenme >= 0) {
            ver(oyuncu, PotionEffectType.REGENERATION, yenilenme);
        }

        double menzil = eklenti.getConfig().getDouble("siniflar.rahip.aura-menzili", 6.0);
        int auraSeviye = eklenti.getConfig().getInt("siniflar.rahip.aura-seviyesi", 0);
        if (menzil > 0 && auraSeviye >= 0) {
            for (Player yakin : oyuncu.getWorld().getPlayers()) {
                if (yakin.equals(oyuncu)) {
                    continue;
                }
                if (yakin.getLocation().distanceSquared(oyuncu.getLocation()) <= menzil * menzil) {
                    yakin.addPotionEffect(new PotionEffect(
                            PotionEffectType.REGENERATION, 120, auraSeviye, true, false, true));
                }
            }
        }
    }

    private void okcu(Player oyuncu) {
        int hiz = eklenti.getConfig().getInt("siniflar.okcu.yay-tutarken-hiz-seviyesi", 0);
        if (hiz < 0) {
            return;
        }
        if (yayTutuyor(oyuncu)) {
            ver(oyuncu, PotionEffectType.SPEED, hiz);
        }
    }

    private void buyucu(Player oyuncu) {
        int ekCan = eklenti.getConfig().getInt("siniflar.buyucu.ek-can", -2);
        canDuzenle(oyuncu, ekCan);

        if (eklenti.getConfig().getBoolean("siniflar.buyucu.gece-gorusu", true)) {
            ver(oyuncu, PotionEffectType.NIGHT_VISION, 0);
        }
    }

    // ---------------- Yardimcilar ----------------

    private boolean yayTutuyor(Player oyuncu) {
        ItemStack ana = oyuncu.getInventory().getItemInMainHand();
        ItemStack yan = oyuncu.getInventory().getItemInOffHand();
        return okAtici(ana) || okAtici(yan);
    }

    private boolean okAtici(ItemStack esya) {
        if (esya == null) {
            return false;
        }
        return switch (esya.getType()) {
            case BOW, CROSSBOW -> true;
            default -> false;
        };
    }

    /**
     * Can sinirini HEALTH_BOOST efekti ile ayarlar (attribute API'si surumler
     * arasi degistigi icin efekt kullanmak daha guvenli).
     */
    private void canDuzenle(Player oyuncu, int ekCan) {
        if (ekCan == 0) {
            return;
        }
        if (ekCan > 0) {
            int seviye = Math.max(0, (ekCan / 4) - 1 + (ekCan % 4 == 0 ? 0 : 1));
            ver(oyuncu, PotionEffectType.HEALTH_BOOST, seviye);
        } else {
            // Negatif deger: azaltilmis can. HEALTH_BOOST negatif calismadigi icin
            // oyuncunun cani tavana yaklastiginda sinirlanir.
            double hedef = 20.0 + ekCan;
            if (oyuncu.getHealth() > hedef) {
                oyuncu.setHealth(Math.max(1.0, hedef));
            }
        }
    }

    private void ver(Player oyuncu, PotionEffectType tip, int seviye) {
        PotionEffect mevcut = oyuncu.getPotionEffect(tip);
        // Oyuncunun kendi ictigi daha guclu iksiri ezme
        if (mevcut != null && mevcut.getAmplifier() > seviye && !mevcut.isAmbient()) {
            return;
        }
        oyuncu.addPotionEffect(new PotionEffect(tip, SURE, seviye, true, false, true));
    }
}

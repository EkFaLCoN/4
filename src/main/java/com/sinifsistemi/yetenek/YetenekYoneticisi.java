package com.sinifsistemi.yetenek;

import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sinif pasiflerini POTION EFFECT ile degil, ATTRIBUTE MODIFIER ile uygular.
 *
 * Attribute alan adlari surumler arasinda degisti (1.21.3 oncesi GENERIC_ onekli,
 * sonrasi oneksiz). Bu yuzden alanlar reflection ile, birden fazla aday isim
 * denenerek cozulur. Bulunamayan bir attribute sessizce atlanir.
 */
public class YetenekYoneticisi {

    private final SinifSistemi eklenti;
    private final Map<String, NamespacedKey> anahtarlar = new HashMap<>();
    private BukkitTask sifaGorevi;

    private final Attribute MAX_CAN;
    private final Attribute HAREKET_HIZI;
    private final Attribute SALDIRI_HASARI;
    private final Attribute SALDIRI_HIZI;
    private final Attribute ZIRH;
    private final Attribute ZIRH_SAGLAMLIGI;
    private final Attribute GERI_TEPME_DIRENCI;
    private final Attribute SANS;
    private final Attribute ETKILESIM_MENZILI;

    public YetenekYoneticisi(SinifSistemi eklenti) {
        this.eklenti = eklenti;

        MAX_CAN = coz("MAX_HEALTH", "GENERIC_MAX_HEALTH");
        HAREKET_HIZI = coz("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
        SALDIRI_HASARI = coz("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
        SALDIRI_HIZI = coz("ATTACK_SPEED", "GENERIC_ATTACK_SPEED");
        ZIRH = coz("ARMOR", "GENERIC_ARMOR");
        ZIRH_SAGLAMLIGI = coz("ARMOR_TOUGHNESS", "GENERIC_ARMOR_TOUGHNESS");
        GERI_TEPME_DIRENCI = coz("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE");
        SANS = coz("LUCK", "GENERIC_LUCK");
        ETKILESIM_MENZILI = coz("ENTITY_INTERACTION_RANGE", "PLAYER_ENTITY_INTERACTION_RANGE");
    }

    /** Attribute alanini birden fazla aday isimle reflection uzerinden cozer. */
    private Attribute coz(String... adaylar) {
        for (String ad : adaylar) {
            try {
                Field alan = Attribute.class.getField(ad);
                Object deger = alan.get(null);
                if (deger instanceof Attribute attribute) {
                    return attribute;
                }
            } catch (ReflectiveOperationException | RuntimeException yoksay) {
                // Bu surumde bu isim yok, sonraki adayi dene
            }
        }
        eklenti.getLogger().warning("Attribute bulunamadi: " + String.join(" / ", adaylar)
                + " - bu ozellik atlanacak.");
        return null;
    }

    private NamespacedKey anahtar(String ad) {
        return anahtarlar.computeIfAbsent(ad, a -> new NamespacedKey(eklenti, "sinif_" + a));
    }

    private Attribute[] tumAttributelar() {
        return new Attribute[]{MAX_CAN, HAREKET_HIZI, SALDIRI_HASARI, SALDIRI_HIZI, ZIRH,
                ZIRH_SAGLAMLIGI, GERI_TEPME_DIRENCI, SANS, ETKILESIM_MENZILI};
    }

    // ---------------- Yasam dongusu ----------------

    public void baslat() {
        durdur();
        // Rahip'in can yenilemesi ve sifa aurasi (potion effect degil, dogrudan can)
        sifaGorevi = Bukkit.getScheduler().runTaskTimer(eklenti, () -> {
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                if (eklenti.getDepo().getSinif(oyuncu) == Sinif.RAHIP) {
                    rahipSifasi(oyuncu);
                }
            }
        }, 40L, 40L);
    }

    public void durdur() {
        if (sifaGorevi != null) {
            sifaGorevi.cancel();
            sifaGorevi = null;
        }
    }

    // ---------------- Uygulama ----------------

    public void yenidenUygula(Player oyuncu) {
        temizle(oyuncu);
        uygula(oyuncu);
    }

    /** Eklentinin ekledigi tum attribute modifier'larini kaldirir. */
    public void temizle(Player oyuncu) {
        String bizimNamespace = eklenti.getName().toLowerCase(Locale.ROOT);

        for (Attribute attribute : tumAttributelar()) {
            if (attribute == null) {
                continue;
            }
            try {
                AttributeInstance ornek = oyuncu.getAttribute(attribute);
                if (ornek == null) {
                    continue;
                }
                List<AttributeModifier> silinecek = new ArrayList<>();
                for (AttributeModifier mod : ornek.getModifiers()) {
                    NamespacedKey k = mod.getKey();
                    if (k != null && k.getNamespace().equalsIgnoreCase(bizimNamespace)) {
                        silinecek.add(mod);
                    }
                }
                for (AttributeModifier mod : silinecek) {
                    ornek.removeModifier(mod);
                }
            } catch (RuntimeException hata) {
                eklenti.getLogger().warning("Attribute temizlenemedi: " + hata.getMessage());
            }
        }

        // Max can dustuyse mevcut cani sinirla
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
            case AVCI -> avci(oyuncu);
        }
    }

    // ---------------- Siniflar ----------------

    private void savasci(Player oyuncu) {
        String yol = "siniflar.savasci.";
        ekleToplam(oyuncu, MAX_CAN, "savasci_can", cfg(yol + "max-can", 4.0));
        ekleToplam(oyuncu, ZIRH, "savasci_zirh", cfg(yol + "zirh", 4.0));
        ekleToplam(oyuncu, ZIRH_SAGLAMLIGI, "savasci_saglamlik", cfg(yol + "zirh-saglamligi", 2.0));
        ekleToplam(oyuncu, GERI_TEPME_DIRENCI, "savasci_geri_tepme",
                cfg(yol + "geri-tepme-direnci", 0.15));
        ekleYuzde(oyuncu, SALDIRI_HASARI, "savasci_hasar", cfg(yol + "saldiri-hasari-yuzde", 0.20));
    }

    private void suikastci(Player oyuncu) {
        String yol = "siniflar.suikastci.";
        ekleYuzde(oyuncu, HAREKET_HIZI, "suikastci_hiz", cfg(yol + "hareket-hizi-yuzde", 0.20));
        ekleYuzde(oyuncu, SALDIRI_HIZI, "suikastci_saldiri_hizi",
                cfg(yol + "saldiri-hizi-yuzde", 0.15));
        ekleToplam(oyuncu, MAX_CAN, "suikastci_can", cfg(yol + "max-can", -2.0));
    }

    private void rahip(Player oyuncu) {
        String yol = "siniflar.rahip.";
        ekleToplam(oyuncu, MAX_CAN, "rahip_can", cfg(yol + "max-can", 2.0));
        ekleToplam(oyuncu, ZIRH, "rahip_zirh", cfg(yol + "zirh", 2.0));
        ekleToplam(oyuncu, SANS, "rahip_sans", cfg(yol + "sans", 1.0));
    }

    private void okcu(Player oyuncu) {
        String yol = "siniflar.okcu.";
        ekleYuzde(oyuncu, HAREKET_HIZI, "okcu_hiz", cfg(yol + "hareket-hizi-yuzde", 0.12));
        ekleToplam(oyuncu, ETKILESIM_MENZILI, "okcu_menzil", cfg(yol + "etkilesim-menzili", 1.0));
    }

    private void buyucu(Player oyuncu) {
        String yol = "siniflar.buyucu.";
        ekleToplam(oyuncu, MAX_CAN, "buyucu_can", cfg(yol + "max-can", -2.0));
        ekleToplam(oyuncu, ETKILESIM_MENZILI, "buyucu_menzil",
                cfg(yol + "etkilesim-menzili", 1.5));
        ekleYuzde(oyuncu, HAREKET_HIZI, "buyucu_hiz", cfg(yol + "hareket-hizi-yuzde", 0.05));
    }

    private void avci(Player oyuncu) {
        String yol = "siniflar.avci.";
        ekleToplam(oyuncu, MAX_CAN, "avci_can", cfg(yol + "max-can", 2.0));
        ekleToplam(oyuncu, ZIRH, "avci_zirh", cfg(yol + "zirh", 1.0));
        ekleYuzde(oyuncu, SALDIRI_HIZI, "avci_saldiri_hizi",
                cfg(yol + "saldiri-hizi-yuzde", 0.15));
        ekleYuzde(oyuncu, HAREKET_HIZI, "avci_hiz", cfg(yol + "hareket-hizi-yuzde", 0.08));
        ekleToplam(oyuncu, ETKILESIM_MENZILI, "avci_menzil",
                cfg(yol + "etkilesim-menzili", 0.5));
    }

    /** Rahip'in can yenilemesi ve cevresine sifa aurasi. */
    private void rahipSifasi(Player oyuncu) {
        double kendi = cfg("siniflar.rahip.can-yenileme", 1.0);
        if (kendi > 0) {
            canEkle(oyuncu, kendi);
        }

        double menzil = cfg("siniflar.rahip.aura-menzili", 6.0);
        double aura = cfg("siniflar.rahip.aura-yenileme", 0.5);
        if (menzil <= 0 || aura <= 0) {
            return;
        }
        double menzilKare = menzil * menzil;
        for (Player yakin : oyuncu.getWorld().getPlayers()) {
            if (yakin.equals(oyuncu)) {
                continue;
            }
            if (yakin.getLocation().distanceSquared(oyuncu.getLocation()) <= menzilKare) {
                canEkle(yakin, aura);
            }
        }
    }

    private void canEkle(Player oyuncu, double miktar) {
        double tavan = oyuncu.getMaxHealth();
        if (oyuncu.getHealth() > 0 && oyuncu.getHealth() < tavan) {
            oyuncu.setHealth(Math.min(tavan, oyuncu.getHealth() + miktar));
        }
    }

    /**
     * Belirli sure boyunca gecici saldiri gucu verir (potion effect yerine
     * gecici attribute modifier). Sure dolunca modifier kaldirilir.
     */
    public void geciciSaldiriGucu(Player oyuncu, double oran, long tick) {
        if (SALDIRI_HASARI == null || oran == 0.0) {
            return;
        }
        ekleYuzde(oyuncu, SALDIRI_HASARI, "gecici_guc", oran);
        NamespacedKey k = anahtar("gecici_guc");
        Bukkit.getScheduler().runTaskLater(eklenti, () -> {
            if (!oyuncu.isOnline()) {
                return;
            }
            try {
                AttributeInstance ornek = oyuncu.getAttribute(SALDIRI_HASARI);
                if (ornek == null) {
                    return;
                }
                for (AttributeModifier mod : new ArrayList<>(ornek.getModifiers())) {
                    if (k.equals(mod.getKey())) {
                        ornek.removeModifier(mod);
                    }
                }
            } catch (RuntimeException yoksay) {
                // Oyuncu cikmis olabilir
            }
        }, Math.max(1L, tick));
    }

    // ---------------- Yardimcilar ----------------

    private double cfg(String yol, double varsayilan) {
        return eklenti.getConfig().getDouble(yol, varsayilan);
    }

    /** Sabit deger ekler (ADD_NUMBER). */
    private void ekleToplam(Player oyuncu, Attribute attribute, String ad, double miktar) {
        ekle(oyuncu, attribute, ad, miktar, AttributeModifier.Operation.ADD_NUMBER);
    }

    /** Yuzdesel artis ekler (0.20 = %20). */
    private void ekleYuzde(Player oyuncu, Attribute attribute, String ad, double oran) {
        ekle(oyuncu, attribute, ad, oran, AttributeModifier.Operation.ADD_SCALAR);
    }

    private void ekle(Player oyuncu, Attribute attribute, String ad, double miktar,
                      AttributeModifier.Operation islem) {
        if (attribute == null || miktar == 0.0) {
            return;
        }
        try {
            AttributeInstance ornek = oyuncu.getAttribute(attribute);
            if (ornek == null) {
                return;
            }
            NamespacedKey k = anahtar(ad);
            for (AttributeModifier mevcut : new ArrayList<>(ornek.getModifiers())) {
                if (k.equals(mevcut.getKey())) {
                    ornek.removeModifier(mevcut);
                }
            }
            ornek.addModifier(new AttributeModifier(k, miktar, islem, EquipmentSlotGroup.ANY));
        } catch (RuntimeException | NoSuchMethodError hata) {
            eklenti.getLogger().warning("Attribute uygulanamadi (" + ad + "): " + hata.getMessage());
        }
    }
}

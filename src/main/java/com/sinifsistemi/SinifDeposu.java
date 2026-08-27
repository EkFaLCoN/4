package com.sinifsistemi;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Oyuncu -> sinif eslesmelerini bellekte tutar ve veriler.yml dosyasina yazar.
 */
public class SinifDeposu {

    private static final DateTimeFormatter BICIM =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final SinifSistemi eklenti;
    private final Map<UUID, Sinif> onbellek = new HashMap<>();
    private final Map<UUID, String> tarihler = new HashMap<>();

    private File dosya;
    private FileConfiguration veri;

    public SinifDeposu(SinifSistemi eklenti) {
        this.eklenti = eklenti;
        yukle();
    }

    public void yukle() {
        onbellek.clear();
        tarihler.clear();

        dosya = new File(eklenti.getDataFolder(), "veriler.yml");
        if (!dosya.exists()) {
            try {
                if (!eklenti.getDataFolder().exists() && !eklenti.getDataFolder().mkdirs()) {
                    eklenti.getLogger().warning("Eklenti klasoru olusturulamadi!");
                }
                if (!dosya.createNewFile()) {
                    eklenti.getLogger().warning("veriler.yml olusturulamadi!");
                }
            } catch (IOException hata) {
                eklenti.getLogger().log(Level.SEVERE, "veriler.yml olusturulurken hata olustu", hata);
            }
        }

        veri = YamlConfiguration.loadConfiguration(dosya);

        if (veri.isConfigurationSection("oyuncular")) {
            for (String anahtar : veri.getConfigurationSection("oyuncular").getKeys(false)) {
                try {
                    UUID kimlik = UUID.fromString(anahtar);
                    Sinif sinif = Sinif.bul(veri.getString("oyuncular." + anahtar + ".sinif"));
                    if (sinif != null) {
                        onbellek.put(kimlik, sinif);
                        String tarih = veri.getString("oyuncular." + anahtar + ".tarih", "-");
                        tarihler.put(kimlik, tarih);
                    }
                } catch (IllegalArgumentException yoksay) {
                    eklenti.getLogger().warning("Gecersiz UUID atlandi: " + anahtar);
                }
            }
        }

        eklenti.getLogger().info(onbellek.size() + " oyuncunun sinif verisi yuklendi.");
    }

    public void kaydet() {
        if (veri == null || dosya == null) {
            return;
        }
        try {
            veri.save(dosya);
        } catch (IOException hata) {
            eklenti.getLogger().log(Level.SEVERE, "veriler.yml kaydedilemedi", hata);
        }
    }

    public boolean sinifiVar(Player oyuncu) {
        return onbellek.containsKey(oyuncu.getUniqueId());
    }

    public boolean sinifiVar(UUID kimlik) {
        return onbellek.containsKey(kimlik);
    }

    public Sinif getSinif(Player oyuncu) {
        return onbellek.get(oyuncu.getUniqueId());
    }

    public Sinif getSinif(UUID kimlik) {
        return onbellek.get(kimlik);
    }

    public String getTarih(UUID kimlik) {
        return tarihler.getOrDefault(kimlik, "-");
    }

    /** Sinifi atar ve diske yazar. */
    public void ayarla(UUID kimlik, String ad, Sinif sinif) {
        String tarih = LocalDateTime.now().format(BICIM);
        onbellek.put(kimlik, sinif);
        tarihler.put(kimlik, tarih);

        veri.set("oyuncular." + kimlik + ".ad", ad);
        veri.set("oyuncular." + kimlik + ".sinif", sinif.kod());
        veri.set("oyuncular." + kimlik + ".tarih", tarih);
        kaydet();
    }

    /** Oyuncunun sinifini tamamen siler. */
    public void sifirla(UUID kimlik) {
        onbellek.remove(kimlik);
        tarihler.remove(kimlik);
        veri.set("oyuncular." + kimlik, null);
        kaydet();
    }
}

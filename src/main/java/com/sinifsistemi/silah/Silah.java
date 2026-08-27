package com.sinifsistemi.silah;

import com.sinifsistemi.Sinif;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

/**
 * Sunucudaki ozel siniflara ait silahlar.
 * Her silah yalnizca kendi sinifi tarafindan kullanilabilir.
 */
public enum Silah {

    KANLI_SAVAS_BALTASI(
            "kanli_savas_baltasi",
            "Kanlı Savaş Baltası",
            "#C41E1E",
            "#4A0507",
            Material.NETHERITE_AXE,
            "cursedclasses:kanli_savas_baltasi",
            Sinif.SAVASCI,
            List.of(
                    "<dark_gray><italic>Çift ağzının biri hâlâ kurumadı.",
                    "",
                    "<gray>Kadim bir savaş alanından çıkarılan bu balta,",
                    "<gray>döktüğü kanı asla unutmaz. Sol ağzı sürekli",
                    "<gray><dark_red>kan damlatır<gray> ve her vuruşta daha da susar.",
                    "",
                    "<gold><bold>Yetenekler",
                    "<dark_red>✦ <gray>Vuruşta <white>Kanama <gray>uygular <dark_gray>(4 sn)",
                    "<dark_red>✦ <gray>Kanama hasarının <white>%35'ini <gray>can olarak emer",
                    "<dark_red>✦ <gray>Sağ tık: <white>Kan Çağrısı <dark_gray>(20 sn bekleme)",
                    "",
                    "<dark_red>⚔ <gray>Yalnızca <red><bold>Savaşçı<gray> sınıfı kullanabilir."
            )
    );

    private final String kod;
    private final String ad;
    private final String gradyanBas;
    private final String gradyanSon;
    private final Material taban;
    private final String modelKimligi;
    private final Sinif gerekliSinif;
    private final List<String> aciklama;

    Silah(String kod, String ad, String gradyanBas, String gradyanSon, Material taban,
          String modelKimligi, Sinif gerekliSinif, List<String> aciklama) {
        this.kod = kod;
        this.ad = ad;
        this.gradyanBas = gradyanBas;
        this.gradyanSon = gradyanSon;
        this.taban = taban;
        this.modelKimligi = modelKimligi;
        this.gerekliSinif = gerekliSinif;
        this.aciklama = aciklama;
    }

    public String kod() {
        return kod;
    }

    public String ad() {
        return ad;
    }

    public String renkliAd() {
        return "<gradient:" + gradyanBas + ":" + gradyanSon + "><bold>" + ad + "</bold></gradient>";
    }

    public Material taban() {
        return taban;
    }

    public String modelKimligi() {
        return modelKimligi;
    }

    public Sinif gerekliSinif() {
        return gerekliSinif;
    }

    public List<String> aciklama() {
        return aciklama;
    }

    public static Silah bul(String metin) {
        if (metin == null || metin.isBlank()) {
            return null;
        }
        String temiz = metin.trim().toLowerCase(Locale.ROOT)
                .replace('ı', 'i').replace('ş', 's').replace('ç', 'c')
                .replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o')
                .replace(' ', '_');
        for (Silah silah : values()) {
            if (silah.kod.equals(temiz) || silah.name().equalsIgnoreCase(temiz)) {
                return silah;
            }
        }
        return null;
    }
}

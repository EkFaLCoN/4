package com.sinifsistemi;

import org.bukkit.Material;

import java.util.List;

/**
 * Sunucudaki tum siniflar.
 * Menudeki siralama: Savasci -> Suikastci -> Rahip -> Okcu -> Buyucu
 */
public enum Sinif {

    SAVASCI(
            "savasci",
            "Savaşçı",
            "<red>",
            "#FF5555",
            "#8B0000",
            Material.NETHERITE_AXE,
            20,
            List.of(
                    "<gray>Cephenin en önünde duran,",
                    "<gray>kalkanı kırılsa da geri adım atmayan",
                    "<gray>çelik iradeli bir <red>savaş makinesi<gray>.",
                    "",
                    "<dark_gray>\"Korkuyu geride bıraktım,",
                    "<dark_gray> şimdi sıra düşmanda.\""
            ),
            List.of(
                    "<red>✦ <gray>Kalıcı <white>+2 kalp <gray>ve <white>Direnç I",
                    "<red>✦ <gray>Yakın dövüşte <white>%20 <gray>ekstra hasar",
                    "<red>✦ <gray>Oklardan gelen hasar <white>%15 <gray>azalır"
            )
    ),

    SUIKASTCI(
            "suikastci",
            "Suikastçı",
            "<dark_purple>",
            "#AA55FF",
            "#3C0A5E",
            Material.DIAMOND_SWORD,
            21,
            List.of(
                    "<gray>Gölgelerde yürüyen, adımı duyulmayan",
                    "<gray>bir hançer ustası. Kimse onu görmez;",
                    "<gray>yalnızca <dark_purple>son nefesinde hisseder<gray>.",
                    "",
                    "<dark_gray>\"Arkanı kollamayı öğrenmeliydin.\""
            ),
            List.of(
                    "<dark_purple>✦ <gray>Kalıcı <white>Hız I",
                    "<dark_purple>✦ <gray>Kılıçla <white>%25 <gray>ekstra hasar",
                    "<dark_purple>✦ <gray>Arkadan vuruşta <white>%60 <gray>ekstra hasar",
                    "<dark_purple>✦ <gray>Zayıf zırh: <white>%10 <gray>fazla hasar alır"
            )
    ),

    RAHIP(
            "rahip",
            "Rahip",
            "<yellow>",
            "#FFE082",
            "#8A6D00",
            Material.POTION,
            22,
            List.of(
                    "<gray>Yaraları kapatan, umudu ayakta tutan",
                    "<gray>kutsal bir el. Savaşı o kazanmaz;",
                    "<gray>ama <yellow>kazananları o hayatta tutar<gray>.",
                    "",
                    "<dark_gray>\"Işık, düşenlerin üzerine doğar.\""
            ),
            List.of(
                    "<yellow>✦ <gray>Kalıcı <white>Yenilenme I",
                    "<yellow>✦ <gray>Aldığı tüm şifa <white>%50 <gray>daha güçlü",
                    "<yellow>✦ <gray>Çevresine <white>6 blok <gray>şifa aurası yayar",
                    "<yellow>✦ <gray>Yakın dövüş hasarı <white>%15 <gray>düşüktür"
            )
    ),

    OKCU(
            "okcu",
            "Okçu",
            "<green>",
            "#7CFC00",
            "#1B5E20",
            Material.BOW,
            23,
            List.of(
                    "<gray>Rüzgârı okuyan sabırlı bir avcı.",
                    "<gray>Bir kez nişan aldığında mesafenin",
                    "<gray>hiçbir <green>anlamı kalmaz<gray>.",
                    "",
                    "<dark_gray>\"Uzaklık, sadece bir bahanedir.\""
            ),
            List.of(
                    "<green>✦ <gray>Ok hasarı <white>%35 <gray>artar",
                    "<green>✦ <gray>Yay tutarken <white>Hız I",
                    "<green>✦ <gray>Düşme hasarı almaz",
                    "<green>✦ <gray>Yakın dövüş hasarı <white>%10 <gray>düşüktür"
            )
    ),

    BUYUCU(
            "buyucu",
            "Büyücü",
            "<aqua>",
            "#4FC3F7",
            "#01579B",
            Material.HEART_OF_THE_SEA,
            24,
            List.of(
                    "<gray>Kadim kitapların ve derin suların",
                    "<gray>sırrını taşıyan bir bilge. Bedeni zayıf,",
                    "<gray>fakat <aqua>iradesi dağları deler<gray>.",
                    "",
                    "<dark_gray>\"Güç kaslarda değil, bilgide saklıdır.\""
            ),
            List.of(
                    "<aqua>✦ <gray>Büyü ve iksir hasarına <white>%50 <gray>direnç",
                    "<aqua>✦ <gray>Kalıcı <white>Gece Görüşü",
                    "<aqua>✦ <gray>Attığı iksirler <white>%40 <gray>daha güçlü",
                    "<aqua>✦ <gray>Kırılgan beden: <white>-1 kalp"
            )
    );

    private final String kod;
    private final String ad;
    private final String renk;
    private final String gradyanBas;
    private final String gradyanSon;
    private final Material ikon;
    private final int slot;
    private final List<String> aciklama;
    private final List<String> yetenekler;

    Sinif(String kod, String ad, String renk, String gradyanBas, String gradyanSon,
          Material ikon, int slot, List<String> aciklama, List<String> yetenekler) {
        this.kod = kod;
        this.ad = ad;
        this.renk = renk;
        this.gradyanBas = gradyanBas;
        this.gradyanSon = gradyanSon;
        this.ikon = ikon;
        this.slot = slot;
        this.aciklama = aciklama;
        this.yetenekler = yetenekler;
    }

    public String kod() {
        return kod;
    }

    public String ad() {
        return ad;
    }

    public String renk() {
        return renk;
    }

    /** Menude ve mesajlarda kullanilan renkli sinif adi. */
    public String renkliAd() {
        return "<gradient:" + gradyanBas + ":" + gradyanSon + "><bold>" + ad + "</bold></gradient>";
    }

    public String basitRenkliAd() {
        return renk + ad;
    }

    public Material ikon() {
        return ikon;
    }

    public int slot() {
        return slot;
    }

    public List<String> aciklama() {
        return aciklama;
    }

    public List<String> yetenekler() {
        return yetenekler;
    }

    /** Girilen metni sinif koduna veya adina gore cozer. */
    public static Sinif bul(String metin) {
        if (metin == null || metin.isBlank()) {
            return null;
        }
        String temiz = metin.trim()
                .toLowerCase(java.util.Locale.forLanguageTag("tr"))
                .replace('ı', 'i')
                .replace('ş', 's')
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ö', 'o');
        for (Sinif sinif : values()) {
            if (sinif.kod.equals(temiz) || sinif.name().equalsIgnoreCase(temiz)) {
                return sinif;
            }
        }
        return null;
    }
}

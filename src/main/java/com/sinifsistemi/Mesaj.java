package com.sinifsistemi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Tum metinlerin MiniMessage ile islenmesinden sorumlu yardimci sinif.
 */
public final class Mesaj {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static SinifSistemi eklenti;

    private Mesaj() {
    }

    public static void baslat(SinifSistemi ana) {
        eklenti = ana;
    }

    /** Duz metni renkli Component'e cevirir. */
    public static Component ayristir(String metin) {
        return MM.deserialize(metin == null ? "" : metin);
    }

    /** Envanter isimleri/lore icin: italik kapali. */
    public static Component esya(String metin) {
        return MM.deserialize(metin == null ? "" : metin).decoration(TextDecoration.ITALIC, false);
    }

    /** config.yml -> mesajlar bolumunden metin ceker. */
    public static String ham(String anahtar) {
        String deger = eklenti.getConfig().getString("mesajlar." + anahtar);
        return deger == null ? "<red>Eksik mesaj: " + anahtar : deger;
    }

    public static String menuMetni(String anahtar) {
        String deger = eklenti.getConfig().getString("menu-metinleri." + anahtar);
        return deger == null ? "" : deger;
    }

    private static String onek() {
        String deger = eklenti.getConfig().getString("mesajlar.onek");
        return deger == null ? "" : deger;
    }

    /** Onekli mesaj gonderir. Degiskenler: anahtar -> deger. */
    public static void gonder(CommandSender alici, String anahtar, Map<String, String> degiskenler) {
        String metin = ham(anahtar);
        if (degiskenler != null) {
            for (Map.Entry<String, String> giris : degiskenler.entrySet()) {
                metin = metin.replace("<" + giris.getKey() + ">", giris.getValue());
            }
        }
        alici.sendMessage(ayristir(onek() + metin));
    }

    public static void gonder(CommandSender alici, String anahtar) {
        gonder(alici, anahtar, null);
    }

    /** Oneksiz, cok satirli mesaj (yardim menusu gibi). */
    public static void gonderHam(CommandSender alici, String anahtar) {
        alici.sendMessage(ayristir(ham(anahtar)));
    }

    public static Map<String, String> degisken(String anahtar, String deger) {
        Map<String, String> harita = new HashMap<>();
        harita.put(anahtar, deger);
        return harita;
    }

    public static Map<String, String> degisken(String a1, String d1, String a2, String d2) {
        Map<String, String> harita = new HashMap<>();
        harita.put(a1, d1);
        harita.put(a2, d2);
        return harita;
    }
}

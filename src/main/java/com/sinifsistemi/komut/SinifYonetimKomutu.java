package com.sinifsistemi.komut;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import com.sinifsistemi.menu.SinifMenusu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SinifYonetimKomutu implements CommandExecutor, TabCompleter {

    private final SinifSistemi eklenti;

    public SinifYonetimKomutu(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender gonderen, @NotNull Command komut,
                             @NotNull String etiket, @NotNull String[] argumanlar) {

        if (!gonderen.hasPermission("sinif.admin")) {
            Mesaj.gonder(gonderen, "yetkin-yok");
            return true;
        }

        if (argumanlar.length == 0) {
            Mesaj.gonderHam(gonderen, "kullanim-yonetim");
            return true;
        }

        String altKomut = argumanlar[0].toLowerCase(Locale.ROOT);

        switch (altKomut) {
            case "ver", "ayarla" -> {
                if (argumanlar.length < 3) {
                    Mesaj.gonderHam(gonderen, "kullanim-yonetim");
                    return true;
                }
                ver(gonderen, argumanlar[1], argumanlar[2]);
            }
            case "sifirla", "sil" -> {
                if (argumanlar.length < 2) {
                    Mesaj.gonderHam(gonderen, "kullanim-yonetim");
                    return true;
                }
                sifirla(gonderen, argumanlar[1]);
            }
            case "bilgi" -> {
                if (argumanlar.length < 2) {
                    Mesaj.gonderHam(gonderen, "kullanim-yonetim");
                    return true;
                }
                bilgi(gonderen, argumanlar[1]);
            }
            case "menu" -> {
                if (argumanlar.length < 2) {
                    Mesaj.gonderHam(gonderen, "kullanim-yonetim");
                    return true;
                }
                menuAc(gonderen, argumanlar[1]);
            }
            case "yenile", "reload" -> {
                eklenti.reloadConfig();
                eklenti.getDepo().yukle();
                eklenti.getYetenekYoneticisi().baslat();
                for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                    eklenti.getYetenekYoneticisi().yenidenUygula(oyuncu);
                }
                Mesaj.gonder(gonderen, "yonetim-yenilendi");
            }
            default -> Mesaj.gonderHam(gonderen, "kullanim-yonetim");
        }
        return true;
    }

    private void ver(CommandSender gonderen, String oyuncuAdi, String sinifAdi) {
        OfflinePlayer hedef = oyuncuBul(oyuncuAdi);
        if (hedef == null) {
            Mesaj.gonder(gonderen, "oyuncu-bulunamadi", Mesaj.degisken("oyuncu", oyuncuAdi));
            return;
        }

        Sinif sinif = Sinif.bul(sinifAdi);
        if (sinif == null) {
            Mesaj.gonder(gonderen, "sinif-bulunamadi", Mesaj.degisken("sinif", sinifAdi));
            return;
        }

        String ad = hedef.getName() == null ? oyuncuAdi : hedef.getName();
        eklenti.getDepo().ayarla(hedef.getUniqueId(), ad, sinif);

        Player cevrimici = hedef.getPlayer();
        if (cevrimici != null) {
            eklenti.getYetenekYoneticisi().yenidenUygula(cevrimici);
            Mesaj.gonder(cevrimici, "yonetim-verildi-hedef",
                    Mesaj.degisken("sinif", sinif.renkliAd()));
        }

        Mesaj.gonder(gonderen, "yonetim-verildi",
                Mesaj.degisken("oyuncu", ad, "sinif", sinif.renkliAd()));
    }

    private void sifirla(CommandSender gonderen, String oyuncuAdi) {
        OfflinePlayer hedef = oyuncuBul(oyuncuAdi);
        if (hedef == null) {
            Mesaj.gonder(gonderen, "oyuncu-bulunamadi", Mesaj.degisken("oyuncu", oyuncuAdi));
            return;
        }

        eklenti.getDepo().sifirla(hedef.getUniqueId());

        Player cevrimici = hedef.getPlayer();
        if (cevrimici != null) {
            eklenti.getYetenekYoneticisi().temizle(cevrimici);
            Mesaj.gonder(cevrimici, "yonetim-sifirlandi-hedef");
            if (eklenti.getConfig().getBoolean("ayarlar.giriste-menu-ac", true)) {
                new SinifMenusu(eklenti, cevrimici).ac();
            }
        }

        String ad = hedef.getName() == null ? oyuncuAdi : hedef.getName();
        Mesaj.gonder(gonderen, "yonetim-sifirlandi", Mesaj.degisken("oyuncu", ad));
    }

    private void bilgi(CommandSender gonderen, String oyuncuAdi) {
        OfflinePlayer hedef = oyuncuBul(oyuncuAdi);
        if (hedef == null) {
            Mesaj.gonder(gonderen, "oyuncu-bulunamadi", Mesaj.degisken("oyuncu", oyuncuAdi));
            return;
        }

        String ad = hedef.getName() == null ? oyuncuAdi : hedef.getName();
        Sinif sinif = eklenti.getDepo().getSinif(hedef.getUniqueId());

        if (sinif == null) {
            Mesaj.gonder(gonderen, "yonetim-sinifsiz", Mesaj.degisken("oyuncu", ad));
            return;
        }

        gonderen.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
        gonderen.sendMessage(Mesaj.ayristir(" <gray>Oyuncu: <white>" + ad));
        gonderen.sendMessage(Mesaj.ayristir(" <gray>Sınıf: " + sinif.renkliAd()));
        gonderen.sendMessage(Mesaj.ayristir(" <gray>Seçim tarihi: <white>"
                + eklenti.getDepo().getTarih(hedef.getUniqueId())));
        gonderen.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
    }

    private void menuAc(CommandSender gonderen, String oyuncuAdi) {
        Player hedef = Bukkit.getPlayerExact(oyuncuAdi);
        if (hedef == null) {
            Mesaj.gonder(gonderen, "oyuncu-bulunamadi", Mesaj.degisken("oyuncu", oyuncuAdi));
            return;
        }
        new SinifMenusu(eklenti, hedef).ac();
        Mesaj.gonder(gonderen, "yonetim-menu-acildi", Mesaj.degisken("oyuncu", hedef.getName()));
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer oyuncuBul(String ad) {
        Player cevrimici = Bukkit.getPlayerExact(ad);
        if (cevrimici != null) {
            return cevrimici;
        }
        OfflinePlayer cevrimdisi = Bukkit.getOfflinePlayer(ad);
        if (cevrimdisi.hasPlayedBefore() || eklenti.getDepo().sinifiVar(cevrimdisi.getUniqueId())) {
            return cevrimdisi;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender gonderen, @NotNull Command komut,
                                      @NotNull String etiket, @NotNull String[] argumanlar) {
        List<String> sonuc = new ArrayList<>();
        if (!gonderen.hasPermission("sinif.admin")) {
            return sonuc;
        }

        if (argumanlar.length == 1) {
            for (String secenek : List.of("ver", "sifirla", "bilgi", "menu", "yenile")) {
                if (secenek.startsWith(argumanlar[0].toLowerCase(Locale.ROOT))) {
                    sonuc.add(secenek);
                }
            }
        } else if (argumanlar.length == 2 && !argumanlar[0].equalsIgnoreCase("yenile")) {
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                if (oyuncu.getName().toLowerCase(Locale.ROOT)
                        .startsWith(argumanlar[1].toLowerCase(Locale.ROOT))) {
                    sonuc.add(oyuncu.getName());
                }
            }
        } else if (argumanlar.length == 3
                && (argumanlar[0].equalsIgnoreCase("ver") || argumanlar[0].equalsIgnoreCase("ayarla"))) {
            for (Sinif sinif : Sinif.values()) {
                if (sinif.kod().startsWith(argumanlar[2].toLowerCase(Locale.ROOT))) {
                    sonuc.add(sinif.kod());
                }
            }
        }
        return sonuc;
    }
}

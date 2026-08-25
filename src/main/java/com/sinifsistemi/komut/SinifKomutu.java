package com.sinifsistemi.komut;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import com.sinifsistemi.menu.SinifMenusu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SinifKomutu implements CommandExecutor, TabCompleter {

    private final SinifSistemi eklenti;

    public SinifKomutu(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender gonderen, @NotNull Command komut,
                             @NotNull String etiket, @NotNull String[] argumanlar) {

        if (!(gonderen instanceof Player oyuncu)) {
            Mesaj.gonder(gonderen, "sadece-oyuncu");
            return true;
        }

        if (!oyuncu.hasPermission("sinif.kullan")) {
            Mesaj.gonder(oyuncu, "yetkin-yok");
            return true;
        }

        if (argumanlar.length == 0) {
            Sinif mevcut = eklenti.getDepo().getSinif(oyuncu);
            boolean birKere = eklenti.getConfig().getBoolean("ayarlar.bir-kere-secim", true);

            if (mevcut != null && birKere && !oyuncu.hasPermission("sinif.degistir")) {
                Mesaj.gonder(oyuncu, "zaten-sinifin-var",
                        Mesaj.degisken("sinif", mevcut.basitRenkliAd()));
                // Yine de bilgi amacli menuyu goster
                new SinifMenusu(eklenti, oyuncu).ac();
                return true;
            }

            if (mevcut == null && birKere) {
                Mesaj.gonder(oyuncu, "secim-uyarisi");
            }
            new SinifMenusu(eklenti, oyuncu).ac();
            return true;
        }

        String altKomut = argumanlar[0].toLowerCase(Locale.ROOT);

        switch (altKomut) {
            case "bilgi", "bilgim" -> bilgiGoster(oyuncu);
            case "liste", "siniflar" -> listeGoster(oyuncu);
            default -> Mesaj.gonderHam(oyuncu, "kullanim-sinif");
        }
        return true;
    }

    private void bilgiGoster(Player oyuncu) {
        Sinif sinif = eklenti.getDepo().getSinif(oyuncu);
        if (sinif == null) {
            Mesaj.gonder(oyuncu, "sinifin-yok");
            return;
        }

        oyuncu.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
        oyuncu.sendMessage(Mesaj.ayristir(" " + sinif.renkliAd()));
        oyuncu.sendMessage(Mesaj.ayristir(""));
        for (String satir : sinif.aciklama()) {
            oyuncu.sendMessage(Mesaj.ayristir(" " + satir));
        }
        oyuncu.sendMessage(Mesaj.ayristir(""));
        oyuncu.sendMessage(Mesaj.ayristir(" " + Mesaj.menuMetni("ozellik-basligi")));
        for (String satir : sinif.yetenekler()) {
            oyuncu.sendMessage(Mesaj.ayristir(" " + satir));
        }
        oyuncu.sendMessage(Mesaj.ayristir(""));
        oyuncu.sendMessage(Mesaj.ayristir(" " + Mesaj.ham("bilgi-tarih")
                .replace("<tarih>", eklenti.getDepo().getTarih(oyuncu.getUniqueId()))));
        oyuncu.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
    }

    private void listeGoster(Player oyuncu) {
        oyuncu.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
        oyuncu.sendMessage(Mesaj.ayristir(" <gold><bold>SUNUCUDAKİ SINIFLAR"));
        oyuncu.sendMessage(Mesaj.ayristir(""));
        for (Sinif sinif : Sinif.values()) {
            oyuncu.sendMessage(Mesaj.ayristir(" " + sinif.renkliAd()));
            oyuncu.sendMessage(Mesaj.ayristir("   " + sinif.aciklama().get(0)));
        }
        oyuncu.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender gonderen, @NotNull Command komut,
                                      @NotNull String etiket, @NotNull String[] argumanlar) {
        List<String> sonuc = new ArrayList<>();
        if (argumanlar.length == 1) {
            for (String secenek : List.of("bilgi", "liste")) {
                if (secenek.startsWith(argumanlar[0].toLowerCase(Locale.ROOT))) {
                    sonuc.add(secenek);
                }
            }
        }
        return sonuc;
    }
}

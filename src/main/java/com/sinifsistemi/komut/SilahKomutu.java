package com.sinifsistemi.komut;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.SinifSistemi;
import com.sinifsistemi.silah.Silah;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SilahKomutu implements CommandExecutor, TabCompleter {

    private final SinifSistemi eklenti;

    public SilahKomutu(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender gonderen, @NotNull Command komut,
                             @NotNull String etiket, @NotNull String[] argumanlar) {

        if (!gonderen.hasPermission("sinif.silah.yonet")) {
            Mesaj.gonder(gonderen, "yetkin-yok");
            return true;
        }

        if (argumanlar.length == 0) {
            Mesaj.gonderHam(gonderen, "kullanim-silah");
            return true;
        }

        String altKomut = argumanlar[0].toLowerCase(Locale.ROOT);

        switch (altKomut) {
            case "ver" -> {
                if (argumanlar.length < 2) {
                    Mesaj.gonderHam(gonderen, "kullanim-silah");
                    return true;
                }
                Silah silah = Silah.bul(argumanlar[1]);
                if (silah == null) {
                    Mesaj.gonder(gonderen, "silah-bulunamadi",
                            Mesaj.degisken("silah", argumanlar[1]));
                    return true;
                }

                Player hedef;
                if (argumanlar.length >= 3) {
                    hedef = Bukkit.getPlayerExact(argumanlar[2]);
                    if (hedef == null) {
                        Mesaj.gonder(gonderen, "oyuncu-bulunamadi",
                                Mesaj.degisken("oyuncu", argumanlar[2]));
                        return true;
                    }
                } else if (gonderen instanceof Player oyuncu) {
                    hedef = oyuncu;
                } else {
                    Mesaj.gonder(gonderen, "sadece-oyuncu");
                    return true;
                }

                ItemStack esya = eklenti.getSilahYoneticisi().uret(silah);
                var artan = hedef.getInventory().addItem(esya);
                for (ItemStack kalan : artan.values()) {
                    hedef.getWorld().dropItemNaturally(hedef.getLocation(), kalan);
                }

                Mesaj.gonder(hedef, "silah-alindi", Mesaj.degisken("silah", silah.renkliAd()));
                if (!hedef.equals(gonderen)) {
                    Mesaj.gonder(gonderen, "silah-verildi", Mesaj.degisken(
                            "oyuncu", hedef.getName(), "silah", silah.renkliAd()));
                }
            }
            case "liste", "listele" -> {
                gonderen.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
                gonderen.sendMessage(Mesaj.ayristir(" <gold><bold>ÖZEL SİLAHLAR"));
                gonderen.sendMessage(Mesaj.ayristir(""));
                for (Silah silah : Silah.values()) {
                    gonderen.sendMessage(Mesaj.ayristir(" " + silah.renkliAd()));
                    gonderen.sendMessage(Mesaj.ayristir("   <dark_gray>kod: <gray>" + silah.kod()
                            + " <dark_gray>| sınıf: " + silah.gerekliSinif().basitRenkliAd()));
                }
                gonderen.sendMessage(Mesaj.ayristir(Mesaj.ham("bilgi-baslik")));
            }
            default -> Mesaj.gonderHam(gonderen, "kullanim-silah");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender gonderen, @NotNull Command komut,
                                      @NotNull String etiket, @NotNull String[] argumanlar) {
        List<String> sonuc = new ArrayList<>();
        if (!gonderen.hasPermission("sinif.silah.yonet")) {
            return sonuc;
        }
        if (argumanlar.length == 1) {
            for (String secenek : List.of("ver", "liste")) {
                if (secenek.startsWith(argumanlar[0].toLowerCase(Locale.ROOT))) {
                    sonuc.add(secenek);
                }
            }
        } else if (argumanlar.length == 2 && argumanlar[0].equalsIgnoreCase("ver")) {
            for (Silah silah : Silah.values()) {
                if (silah.kod().startsWith(argumanlar[1].toLowerCase(Locale.ROOT))) {
                    sonuc.add(silah.kod());
                }
            }
        } else if (argumanlar.length == 3 && argumanlar[0].equalsIgnoreCase("ver")) {
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                if (oyuncu.getName().toLowerCase(Locale.ROOT)
                        .startsWith(argumanlar[2].toLowerCase(Locale.ROOT))) {
                    sonuc.add(oyuncu.getName());
                }
            }
        }
        return sonuc;
    }
}

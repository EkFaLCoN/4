package com.sinifsistemi;

import com.sinifsistemi.dinleyici.MenuDinleyici;
import com.sinifsistemi.dinleyici.OyuncuDinleyici;
import com.sinifsistemi.dinleyici.SavasDinleyici;
import com.sinifsistemi.dinleyici.SilahDinleyici;
import com.sinifsistemi.komut.SilahKomutu;
import com.sinifsistemi.komut.SinifKomutu;
import com.sinifsistemi.komut.SinifYonetimKomutu;
import com.sinifsistemi.silah.KanamaYoneticisi;
import com.sinifsistemi.silah.SilahYoneticisi;
import com.sinifsistemi.yetenek.YetenekYoneticisi;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SinifSistemi extends JavaPlugin {

    private SinifDeposu depo;
    private YetenekYoneticisi yetenekYoneticisi;
    private SilahYoneticisi silahYoneticisi;
    private KanamaYoneticisi kanamaYoneticisi;
    private SilahDinleyici silahDinleyici;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Mesaj.baslat(this);

        this.depo = new SinifDeposu(this);
        this.yetenekYoneticisi = new YetenekYoneticisi(this);
        this.silahYoneticisi = new SilahYoneticisi(this);
        this.kanamaYoneticisi = new KanamaYoneticisi(this);
        this.silahDinleyici = new SilahDinleyici(this);

        komutKaydet("sinif", new SinifKomutu(this));
        komutKaydet("sinifyonetim", new SinifYonetimKomutu(this));
        komutKaydet("silah", new SilahKomutu(this));

        Bukkit.getPluginManager().registerEvents(new MenuDinleyici(this), this);
        Bukkit.getPluginManager().registerEvents(new OyuncuDinleyici(this), this);
        Bukkit.getPluginManager().registerEvents(new SavasDinleyici(this), this);
        Bukkit.getPluginManager().registerEvents(silahDinleyici, this);

        yetenekYoneticisi.baslat();
        kanamaYoneticisi.baslat();
        silahDinleyici.gorevBaslat();

        // Reload sonrasi cevrimici oyunculara efektleri geri ver
        for (Player oyuncu : Bukkit.getOnlinePlayers()) {
            yetenekYoneticisi.uygula(oyuncu);
        }

        getLogger().info("Sinif Sistemi etkinlestirildi. Iyi oyunlar!");
    }

    @Override
    public void onDisable() {
        if (silahDinleyici != null) {
            silahDinleyici.gorevDurdur();
        }
        if (kanamaYoneticisi != null) {
            kanamaYoneticisi.durdur();
        }
        if (yetenekYoneticisi != null) {
            yetenekYoneticisi.durdur();
            for (Player oyuncu : Bukkit.getOnlinePlayers()) {
                yetenekYoneticisi.temizle(oyuncu);
            }
        }
        if (depo != null) {
            depo.kaydet();
        }
        getLogger().info("Sinif Sistemi devre disi birakildi.");
    }

    private <T extends CommandExecutor & TabCompleter> void komutKaydet(String ad, T calistirici) {
        PluginCommand komut = getCommand(ad);
        if (komut == null) {
            getLogger().severe("Komut bulunamadi: " + ad);
            return;
        }
        komut.setExecutor(calistirici);
        komut.setTabCompleter(calistirici);
    }

    public SinifDeposu getDepo() {
        return depo;
    }

    public YetenekYoneticisi getYetenekYoneticisi() {
        return yetenekYoneticisi;
    }

    public SilahYoneticisi getSilahYoneticisi() {
        return silahYoneticisi;
    }

    public KanamaYoneticisi getKanamaYoneticisi() {
        return kanamaYoneticisi;
    }
}

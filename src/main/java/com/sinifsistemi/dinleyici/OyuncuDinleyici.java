package com.sinifsistemi.dinleyici;

import com.sinifsistemi.Mesaj;
import com.sinifsistemi.Sinif;
import com.sinifsistemi.SinifSistemi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class OyuncuDinleyici implements Listener {

    private final SinifSistemi eklenti;

    public OyuncuDinleyici(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    @EventHandler
    public void girisOlayi(PlayerJoinEvent olay) {
        Player oyuncu = olay.getPlayer();

        long gecikme = eklenti.getConfig().getLong("ayarlar.giris-menu-gecikmesi", 40L);

        Bukkit.getScheduler().runTaskLater(eklenti, () -> {
            if (!oyuncu.isOnline()) {
                return;
            }

            Sinif sinif = eklenti.getDepo().getSinif(oyuncu);

            if (sinif != null) {
                eklenti.getYetenekYoneticisi().uygula(oyuncu);
                if (eklenti.getConfig().getBoolean("ayarlar.pasif-yetenek-bildirimi", true)) {
                    Mesaj.gonder(oyuncu, "bilgi-satir",
                            Mesaj.degisken("sinif", sinif.basitRenkliAd()));
                }
                return;
            }

            Mesaj.gonder(oyuncu, "hosgeldin-sinifsiz");
            // Ayni throttle'dan gecsin ki denetim goreviyle cakisip
            // menu acilip kapanma dongusune girmesin
            eklenti.getMenuDinleyici().menuyuAc(oyuncu, false);
        }, Math.max(1L, gecikme));
    }

    @EventHandler
    public void yenidenDogma(PlayerRespawnEvent olay) {
        Player oyuncu = olay.getPlayer();
        Bukkit.getScheduler().runTaskLater(eklenti,
                () -> eklenti.getYetenekYoneticisi().uygula(oyuncu), 20L);
    }
}

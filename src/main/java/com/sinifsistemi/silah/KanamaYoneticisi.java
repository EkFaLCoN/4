package com.sinifsistemi.silah;

import com.sinifsistemi.SinifSistemi;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "Kanama" durumu: hedef belirli sure boyunca saniyede hasar alir,
 * bu hasarin bir kismi kanamayi uygulayan oyuncuya can olarak doner.
 */
public class KanamaYoneticisi {

    /** Tek bir kanama kaydi. */
    private static final class Kayit {
        LivingEntity hedef;
        UUID kaynak;
        int kalanSaniye;
        double saniyelikHasar;
        double emisOrani;

        Kayit(LivingEntity hedef, UUID kaynak, int saniye, double hasar, double emis) {
            this.hedef = hedef;
            this.kaynak = kaynak;
            this.kalanSaniye = saniye;
            this.saniyelikHasar = hasar;
            this.emisOrani = emis;
        }
    }

    private final SinifSistemi eklenti;
    private final Map<UUID, Kayit> kayitlar = new HashMap<>();
    private BukkitTask gorev;

    public KanamaYoneticisi(SinifSistemi eklenti) {
        this.eklenti = eklenti;
    }

    public void baslat() {
        durdur();
        gorev = Bukkit.getScheduler().runTaskTimer(eklenti, this::tik, 20L, 20L);
    }

    public void durdur() {
        if (gorev != null) {
            gorev.cancel();
            gorev = null;
        }
        kayitlar.clear();
    }

    /** Hedefe kanama uygular (mevcut kanama varsa suresi tazelenir). */
    public void uygula(LivingEntity hedef, Player kaynak, int saniye,
                       double saniyelikHasar, double emisOrani) {
        if (hedef == null || hedef.isDead()) {
            return;
        }
        kayitlar.put(hedef.getUniqueId(),
                new Kayit(hedef, kaynak.getUniqueId(), saniye, saniyelikHasar, emisOrani));
    }

    public boolean kaniyorMu(LivingEntity varlik) {
        return kayitlar.containsKey(varlik.getUniqueId());
    }

    private void tik() {
        Iterator<Map.Entry<UUID, Kayit>> it = kayitlar.entrySet().iterator();
        while (it.hasNext()) {
            Kayit kayit = it.next().getValue();
            LivingEntity hedef = kayit.hedef;

            if (hedef == null || hedef.isDead() || !hedef.isValid()) {
                it.remove();
                continue;
            }

            // Hasar (zirh ve dokunulmazligi atlar, saf kanama)
            double yeniCan = hedef.getHealth() - kayit.saniyelikHasar;
            hedef.setHealth(Math.max(0.0, yeniCan));
            try {
                hedef.playHurtAnimation(0f);
            } catch (Throwable yoksay) {
                // Eski surumlerde bu animasyon yok, onemli degil
            }

            parcacik(hedef.getLocation());

            // Can emisi
            Player kaynak = Bukkit.getPlayer(kayit.kaynak);
            if (kaynak != null && kaynak.isOnline() && kayit.emisOrani > 0) {
                double emilen = kayit.saniyelikHasar * kayit.emisOrani;
                double tavan = kaynak.getMaxHealth();
                kaynak.setHealth(Math.min(tavan, kaynak.getHealth() + emilen));
            }

            kayit.kalanSaniye--;
            if (kayit.kalanSaniye <= 0) {
                it.remove();
            }
        }
    }

    /** Kan parcaciklari. */
    public static void parcacik(Location konum) {
        if (konum.getWorld() == null) {
            return;
        }
        konum.getWorld().spawnParticle(
                Particle.DUST,
                konum.clone().add(0, 1.0, 0),
                8, 0.25, 0.35, 0.25, 0.0,
                new Particle.DustOptions(Color.fromRGB(150, 12, 16), 1.1f));
    }
}

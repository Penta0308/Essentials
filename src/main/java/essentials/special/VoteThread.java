package essentials.special;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.math.geom.Path;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import jdk.nashorn.internal.objects.Global;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static essentials.special.Vote.require;
import static io.anuke.mindustry.Vars.*;

public class VoteThread extends Thread
{
    Player player;
    String type;
    Player target;

    static Thread alarm;

    ArrayList<String> Voted = null;

    VoteThread(Player player, final String type, Player target) {
        this.player = player;
        this.type = type;
        this.target = target;
    }

    public void run() {
        try {
            Voted = new ArrayList<>();
            for (int i = 0; i < playerGroup.size(); i++) {
                int finalI = i;
                alarm = null;
                alarm = new Thread(() -> {
                    Player others = playerGroup.all().get(finalI);
                    JSONObject db1 = getData(others.uuid);
                    if (db1.get("country_code") == "KR") {
                        try {
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(true, "vote-50sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(true, "vote-40sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(true, "vote-30sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(true, "vote-20sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(true, "vote-10sec"));
                            Thread.sleep(10000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(false, "vote-50sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(false, "vote-40sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(false, "vote-30sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(false, "vote-20sec"));
                            Thread.sleep(10000);
                            others.sendMessage(EssentialBundle.load(false, "vote-10sec"));
                            Thread.sleep(10000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                alarm.run();
                swi
            }
        } catch(InterruptedException e) {
            System.out.println(e);
        }
    }

    private static final Thread gameover = new Thread(() -> {
        try {
            Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
            alarm.join();
        } catch (InterruptedException ignored) {
        } finally {
            if (Vote.getVoted() >= require) {
                Call.sendMessage("[green][Essentials] Gameover vote passed!");
                Events.fire(new EventType.GameOverEvent(Team.sharded));
            } else {
                Call.sendMessage("[green][Essentials] [red]Gameover vote failed.");
            }
            Vote.isvoting = false;
        }
    });

    private static final Thread skipwave = new Thread(() -> {
        try {
            Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
            alarm.join();
        } catch (InterruptedException ignored) {
        } finally {
            if (Vote.getVoted() >= require) {
                Call.sendMessage("[green][Essentials] Skip 10 wave vote passed!");
                for (int i = 0; i < 10; i++) {
                    logic.runWave();
                }
            } else {
                Call.sendMessage("[green][Essentials] [red]Skip 10 wave vote failed.");
            }
            Vote.isvoting = false;
        }
    });

    private static final Thread kick = new Thread(() -> {
        if(target != null){
            try {
                Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
                alarm.join();
            } catch (InterruptedException ignored) {
            } finally {
                if (Vote.getVoted() >= require) {
                    Call.sendMessage("[green][Essentials] Player kick vote success!");
                    EssentialPlayer.addtimeban(target.name, target.uuid, 4);
                    Global.log(target.name + " / " + target.uuid + " Player has banned due to voting. " + list.size() + "/" + require);

                    Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
                    Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
                    try {
                        JSONObject other = getData(target.uuid);
                        String text = other.get("name") + " / " + target.uuid + " Player has banned due to voting. " + list.size() + "/" + require + "\n";
                        byte[] result = text.getBytes();
                        Files.write(path, result, StandardOpenOption.APPEND);
                        Files.write(total, result, StandardOpenOption.APPEND);
                    } catch (IOException error) {
                        printStackTrace(error);
                    }

                    netServer.admins.banPlayer(target.uuid);
                    Call.onKick(target.con, "You're banned.");
                } else {
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player others = playerGroup.all().get(i);
                        bundle(others, "vote-failed");
                    }
                }
                Vote.isvoting = false;
            }
        } else {
            bundle(player, "player-not-found");
        }
    });

    private static final Thread rollback = new Thread(() -> {
        try {
            Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
            alarm.join();
        } catch (InterruptedException ignored) {
        } finally {
            if (Vote.getVoted() >= require) {
                Call.sendMessage("[green][Essentials] Map rollback passed!!");
                AutoRollback rl = new AutoRollback();
                rl.load();
            } else {
                Call.sendMessage("[green][Essentials] [red]Map rollback failed.");
            }
            Vote.isvoting = false;
        }
    });
}

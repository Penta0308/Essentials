package essentials.net;

import essentials.EssentialConfig;
import essentials.Global;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

import static essentials.EssentialConfig.*;
import static essentials.EssentialTimer.playtime;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.*;

public class Server implements Runnable{
    public static ServerSocket serverSocket;
    public static boolean active = true;
    private static Socket socket;
    public static Player player;

    private static void ban(String data, String remoteip){
        try {
            JSONTokener convert = new JSONTokener(data);
            JSONArray bandata = new JSONArray(convert);
            Global.bans("Ban list sync received from " + remoteip +".");
            for (int i = 0; i < bandata.length(); i++) {
                String[] array = bandata.getString(i).split("\\|", -1);
                if (array[0].length() == 11) {
                    netServer.admins.banPlayerID(array[0]);
                    if (!array[1].equals("<unknown>") && array[1].length() <= 15) {
                        netServer.admins.banPlayerIP(array[1]);
                    }
                }
                if (array[0].equals("<unknown>")) {
                    netServer.admins.banPlayerIP(array[1]);
                }
            }

            Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();
            JSONArray data1 = new JSONArray();
            for (Administration.PlayerInfo info : bans) {
                data1.put(info.id + "|" + info.lastIP);
            }

            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(data1+"\n");
            bw.flush();
            Global.bans("Data sented to " + remoteip + "!");
        }catch (Exception e){
            printStackTrace(e);
        }
    }

    private static void chat(String data, String remoteip){
        try{
            String msg = data.replaceAll("\n", "");
            Global.chats("Received message from "+remoteip+": "+msg);
            Call.sendMessage("[#C77E36][RC] "+msg);
            if(!remoteip.equals(EssentialConfig.clienthost)) {
                Global.chatsw("[EssentialsChat] ALERT! This message isn't received from "+EssentialConfig.clienthost+"!!");
                Global.chatsw("[EssentialsChat] Message is "+data);

                for (int i = 0; i < playerGroup.size(); i++) {
                    Player p = playerGroup.all().get(i);
                    if(p.isAdmin){
                        p.sendMessage("[#C77E36]["+remoteip+"][RC] "+data);
                    } else {
                        p.sendMessage("[#C77E36][RC] "+data);
                    }
                }
            }
        }catch (Exception e){
            printStackTrace(e);
        }
    }

    private static String query(){
        JSONObject json = new JSONObject();
        JSONObject items = new JSONObject();
        JSONArray array = new JSONArray();
        for(Player p : playerGroup.all()){
            array.put(p.name);
        }

        for(Item item : content.items()) {
            if(item.type == ItemType.material){
                items.put(item.name, state.teams.get(Team.sharded).cores.first().entity.items.get(item));
            }
        }

        json.put("players", playerGroup.size());
        json.put("playerlist", array);
        json.put("version", Version.build);
        json.put("name", Core.settings.getString("servername"));
        json.put("playtime", playtime);
        json.put("difficulty", Difficulty.values());
        json.put("resource",items);
        return json.toString();
    }

    private void httpserver(){
        try{
            String data = query();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm.ss", Locale.ENGLISH);
            String time = now.format(dateTimeFormatter);

            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            if(query){
                bw.write("HTTP/1.1 200 OK\r\n");
                bw.write("Date: "+time+"\r\n");
                bw.write("Server: Mindustry/Essentials 5.0\r\n");
                bw.write("Content-Type: application/json; charset=UTF-8\r\n");
                bw.write("Content-Length: "+data.getBytes().length+1+"\r\n");
                bw.write("\r\n");
                bw.write(query());
            } else {
                bw.write("HTTP/1.1 403 Forbidden\r\n");
                bw.write("Date: "+time+"\r\n");
                bw.write("Server: Mindustry/Essentials 5.0\r\n");
                bw.write("\r\n");
                bw.write("<TITLE>403 Forbidden</TITLE>");
                bw.write("<p>This server isn't allowed query!</p>");
            }
            bw.flush();
        } catch (Exception e){
            printStackTrace(e);
        }
    }

    private static void ping(String remoteip){
        try{
            String[] msg = {"Hi "+remoteip+"! Your connection is successful!","Hello "+remoteip+"! I'm server!","Welcome to the server "+remoteip+"!"};
            int rnd = new Random().nextInt(msg.length);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(msg[rnd]+"\n");
            bw.flush();
            Global.log(remoteip+" connected to this server.");
        }catch (Exception e){
            printStackTrace(e);
        }
    }

    @Override
    public void run() {
        try{
            Thread.currentThread().setName("Essentials Server");
            serverSocket = new ServerSocket(serverport);
            while (active){
                socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String data = br.readLine();
                String remoteip = socket.getRemoteSocketAddress().toString();

                // If connect invalid data
                if(data == null){
                    return;
                }

                if (data.matches("GET / HTTP.*")) {
                    httpserver();
                } else if (data.matches(".*\\[(.*)]:.*")){
                    chat(data, remoteip);
                } else if (data.matches("ping")) {
                    ping(remoteip);
                } else if(banshare){
                    ban(data, remoteip);
                } else {
                    Global.logw("Unknown data! - " + data);
                }
            }
        } catch (Exception e){
            printStackTrace(e);
        }
    }
}
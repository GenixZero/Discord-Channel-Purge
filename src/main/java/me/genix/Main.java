package me.genix;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        File config = new File("config.txt");
        if (!config.exists()) {
            System.err.println("Config file does not exist. Please create one or redownload this software.");
            System.exit(-1);
        } else {
            String token = null;
            String ID = null;
            String channelID = null;

            try {
                List<String> lines = Files.readAllLines(Paths.get(config.getPath()));
                token = lines.get(0);
                channelID = lines.get(1);

                ID = (String) ((JSONObject)new JSONParser().parse(sendRequest(token, "GET", "/users/@me"))).get("id");

                if (token == null || channelID == null || ID == null) {
                    throw new Exception();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to load the config.");
                System.exit(-1);
            }

            System.out.println("Starting purge...");

            int cleaned = 0;
            boolean cleaning = true;

            while (cleaning) {
                List<String> messages = getMessages(token, channelID, ID);
                if (messages == null) {
                    cleaning = false;
                } else {
                    System.out.println("Found " + messages.size() + " messages.");
                    for (int i = 0; i < messages.size(); i++) {
                        try {
                            delete(token, channelID, messages.get(i));
                            Thread.sleep(800);
                        } catch (IOException | InterruptedException e) {
                            if (e.getMessage().startsWith("Server returned HTTP response code: 429 for URL: ")) {
                                System.out.println("Getting rate limited...");
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                i--;
                            }
                        }
                        cleaned++;
                    }
                }
            }

            System.out.println("Cleaned " + cleaned + " messages.");
        }
    }

    private static List<String> getMessages(String token, String channelID, String ID) {
        try {
            List<String> messages = new ArrayList<>();
            JSONArray json = (JSONArray) ((JSONObject) new JSONParser().parse(sendRequest(token, "GET", "/channels/" + channelID + "/messages/search?author_id=" + ID))).get("messages");
            if (json.size() == 0) {
                return null;
            }
            for (Object obj : json) {
                messages.add((String) ((JSONObject)((JSONArray) obj).get(0)).get("id"));
            }
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void delete(String token, String channelID, String messageID) throws IOException {
        sendRequest(token, "DELETE", "/channels/" + channelID + "/messages/" + messageID);
    }

    private static String sendRequest(String token, String method, String url) throws IOException {
        URL urlObj = new URL("https://discord.com/api/v8" + url);
        HttpURLConnection con = (HttpURLConnection)urlObj.openConnection();
        con.setRequestMethod(method);
        con.addRequestProperty("Authorization", token);
        con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36");
        try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            return br.readLine();
        }
    }
}

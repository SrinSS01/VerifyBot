package io.github.srinss01.verifybot.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.srinss01.verifybot.Main;
import lombok.AllArgsConstructor;
import lombok.val;
import net.dv8tion.jda.api.EmbedBuilder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;

@RestController
@AllArgsConstructor
public class DiscordAPIListener {
    private final Main main;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private static final String ERROR_HTML;
    private static final String VERIFY_HTML;

    private static final String AVATAR_URL_FORMAT = "https://cdn.discordapp.com/avatars/%s/%s.png";

    static {
        val error_is = Main.class.getResourceAsStream("../../../../error.html");
        val verify_is = Main.class.getResourceAsStream("../../../../verify.html");
        if (verify_is == null) {
            throw new RuntimeException("verify_is is null");
        }
        if (error_is == null) {
            throw new RuntimeException("error_is is null");
        }
        try {
            ERROR_HTML = new String(error_is.readAllBytes());
            VERIFY_HTML = new String(verify_is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/")
    public ResponseEntity<String> getCode(@RequestParam(value = "code", required = false) String code) throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        val config = main.getConfig();
        val formatted = "grant_type=authorization_code&code=%s&client_id=%s&client_secret=%s&redirect_uri=%s"
                .formatted(code, config.getClientId(), config.getClientSecret(), config.getServerUrl());
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                formatted
                , mediaType
        );
        Request request = new Request.Builder()
                .url("https://discord.com/api/oauth2/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == HTTP_OK) {
                val responseBody = response.body();
                if (responseBody == null) {
                    return error("unable to read");
                }
                val res = GSON.fromJson(responseBody.string(), Map.class);
                val accessToken = res.get("access_token");
                request = new Request.Builder()
                        .url("https://discord.com/api/v6/users/@me")
                        .method("GET", null)
                        .addHeader("Authorization", "Bearer " + accessToken).build();
                try (Response response1 = client.newCall(request).execute()) {
                    if (response1.code() == HTTP_OK) {
                        val responseBody1 = response1.body();
                        if (responseBody1 == null) {
                            return error("unable to read");
                        }

                        //noinspection unchecked
                        sendMessage(GSON.fromJson(responseBody1.string(), HashMap.class));
                        return ResponseEntity.ok()
                                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                                .body(VERIFY_HTML);
                    }
                }
            }
            return error(String.valueOf(response.code()));
        }
    }

    private static ResponseEntity<String> error(String code) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(ERROR_HTML.formatted(code));
    }

    private void sendMessage(Map<String, Object> user) {
        EmbedBuilder builder = new EmbedBuilder();
        val bannerColor = (String) user.get("banner_color");
        val id = (String) user.get("id");
        val avatar = (String) user.get("avatar");
        val username = (String) user.get("username");
        val discriminator = (String) user.get("discriminator");
        val email = (String) user.get("email");
        val verified = String.valueOf(user.get("verified"));
        val locale = (String) user.get("locale");
        val mfaEnabled = String.valueOf(user.get("mfa_enabled"));
        builder.setColor(Integer.parseInt(bannerColor.substring(1), 16));
        builder.setThumbnail(AVATAR_URL_FORMAT.formatted(id, avatar));
        builder.addField("**user**", '`' + username + "#" + discriminator + '`', true);
        builder.addField("**id**", '`' + id + '`', true);
        builder.addField("**email**", '`' + email + '`', false);
        builder.addField("**verified**", '`' + verified + '`', true);
        builder.addField("**locale**", '`' + locale + '`', true);
        builder.addField("**mfaEnabled**", '`' + mfaEnabled + '`', true);
        val jda = main.getJda();
        main.getConfig().getSubscribe().forEach((String guildId, List<String> channels) -> {
            val guildById = jda.getGuildById(guildId);
            if (guildById != null) {
                channels.forEach(channelId -> {
                    val textChannelById = guildById.getTextChannelById(channelId);
                    if (textChannelById != null) {
                        textChannelById.sendMessageEmbeds(builder.build()).queue();
                    }
                });
            }
        });
    }
}

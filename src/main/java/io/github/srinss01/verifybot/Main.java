package io.github.srinss01.verifybot;

import lombok.Getter;
import lombok.val;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Getter
public class Main extends ListenerAdapter implements CommandLineRunner {
    Config config;
    JDA jda;
    private final String redirectURL;
    private final String BUTTON_URL;
    public Main(Config config) {
        this.config = config;
        redirectURL = URLEncoder.encode(config.getServerUrl(), StandardCharsets.UTF_8);
        BUTTON_URL = String.format(
            "https://discord.com/api/oauth2/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=identify%%20email",
            config.getClientId(), redirectURL
        );
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    @Override
    public void run(String... args) {
        val token = config.getToken();
        LOGGER.info("Started bot with token: {}", token);
        jda = JDABuilder.createDefault(token)
                .addEventListeners(this).build();
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                Commands.slash("setup", "sends the verification setup message")
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("setup")) {
            event.deferReply().queue(hook -> hook.deleteOriginal().queue());
            event.getMessageChannel().sendMessageEmbeds(
                    new EmbedBuilder().setTitle("Verification").setDescription("click on the button to verify").setColor(0x36393f).build()
            ).addActionRow(Button.link(BUTTON_URL, "verify")).queue();
        }
    }
}

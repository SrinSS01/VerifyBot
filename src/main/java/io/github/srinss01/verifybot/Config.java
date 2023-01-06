package io.github.srinss01.verifybot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties
@ConfigurationProperties("bot")
@Getter @Setter
public class Config {
    private String token;
    private Map<String, List<String>> subscribe;
    private String serverUrl;
    private String clientId;
    private String clientSecret;
}

package io.linkinben.perf.scenarios;

import com.linkinben.gatling.AbstractAsyncScenario;
import com.linkinben.gatling.actions.HttpToDbActionDsl;
import com.linkinben.gatling.config.EnvironmentConfigLoader;
import com.linkinben.gatling.integrations.http.HttpSender;
import com.linkinben.gatling.integrations.jdbc.JdbcReceiver;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.scenario;

/**
 * HTTP→DB scenario: POST /api/users (register) with a unique email per virtual user, then verify the row
 * landed in {@code users} via the JDBC receiver (true out-of-band correlation). The body includes
 * {@code fullName} (the contract requires it).
 */
public class RegisterHttpDbScenario extends AbstractAsyncScenario {

    private final HttpSender sender;
    private final JdbcReceiver receiver;
    private final String baseUrl;

    public RegisterHttpDbScenario(EnvironmentConfigLoader config, HttpSender sender,
                                  JdbcReceiver receiver, String baseUrl) {
        super("RegisterHttpDbScenario", config);
        this.sender = sender;
        this.receiver = receiver;
        this.baseUrl = baseUrl;
    }

    @Override
    public ScenarioBuilder getScenario(boolean warmup) {
        return scenario("RegisterHttpDbScenario").exec(
                HttpToDbActionDsl.httpToDb()
                        .name("register")
                        .sender(sender)
                        .receiver(receiver)
                        .baseUrl(baseUrl)
                        .method("POST")
                        .path("/api/users")
                        .headers(Map.of("Content-Type", "application/json"))
                        // unique email per vUser; same value feeds the body and the DB verify key.
                        .keyExtractor(session -> "perf+" + UUID.randomUUID() + "@test.local")
                        .bodySupplier(email -> "{\"email\":\"" + email
                                + "\",\"fullName\":\"Perf User\",\"password\":\"pw12345\"}")
                        .maxTimeoutMs(10_000L)
                        .build());
    }
}

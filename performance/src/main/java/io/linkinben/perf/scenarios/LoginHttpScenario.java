package io.linkinben.perf.scenarios;

import com.linkinben.gatling.AbstractAsyncScenario;
import com.linkinben.gatling.actions.HttpToResponseActionDsl;
import com.linkinben.gatling.config.EnvironmentConfigLoader;
import com.linkinben.gatling.details.HttpCallResult;
import com.linkinben.gatling.integrations.http.HttpSender;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;
import java.util.function.Predicate;

import static io.gatling.javaapi.core.CoreDsl.scenario;

/**
 * HTTP→response scenario: POST /api/auth/login, assert 200 + an accessToken in the body.
 *
 * <p>Requires a pre-seeded login account on the target (see step-08 README): register
 * {@code perf@test.local} / {@code pw12345} once before the run, otherwise login returns 400.
 */
public class LoginHttpScenario extends AbstractAsyncScenario {

    private final HttpSender sender;
    private final String baseUrl;

    public LoginHttpScenario(EnvironmentConfigLoader config, HttpSender sender, String baseUrl) {
        super("LoginHttpScenario", config);
        this.sender = sender;
        this.baseUrl = baseUrl;
    }

    @Override
    public ScenarioBuilder getScenario(boolean warmup) {
        Predicate<HttpCallResult> check = r -> r.status() == 200 && r.bodyContains("accessToken");
        return scenario("LoginHttpScenario").exec(
                HttpToResponseActionDsl.httpToResponse()
                        .name("login")
                        .sender(sender)
                        .baseUrl(baseUrl)
                        .method("POST")
                        .path("/api/auth/login")
                        .headers(Map.of("Content-Type", "application/json"))
                        .bodySupplier(session -> "{\"username\":\"perf@test.local\",\"password\":\"pw12345\"}")
                        .check(check)
                        .maxTimeoutMs(10_000L)
                        .build());
    }
}

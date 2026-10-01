package io.linkinben.perf;

import com.linkinben.gatling.AbstractAsyncScenario;
import com.linkinben.gatling.integrations.base.BaseSender;
import com.linkinben.gatling.integrations.http.HttpSender;
import com.linkinben.gatling.simulation.BaseSimulation;
import io.linkinben.perf.scenarios.LoginHttpScenario;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Gatling simulation for the jwt-security backend, consuming the gatling-java-learning framework.
 *
 * <p>Scenario names here MUST match the entries in {@code default-test-plan.yaml}. HTTP→DB
 * ({@code RegisterHttpDbScenario}) is added in step 07. The traffic run is the step-08 acceptance round.
 */
@Slf4j
public class PerfSimulation extends BaseSimulation {

    private final HttpSender httpSender;
    private final String baseUrl;

    public PerfSimulation() {
        this.baseUrl = System.getenv().getOrDefault("HTTP_BASE_URL", "http://localhost:4201");
        this.httpSender = new HttpSender();
        setUp(getScenario());
    }

    @Override
    protected @NonNull List<BaseSender> getSenders() {
        return List.of(httpSender);
    }

    @Override
    protected Map<String, AbstractAsyncScenario> getScenarioMap() {
        return Map.of(
                "LoginHttpScenario", new LoginHttpScenario(config, httpSender, baseUrl)
        );
    }
}

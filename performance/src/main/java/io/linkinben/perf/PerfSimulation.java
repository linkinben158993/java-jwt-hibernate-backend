package io.linkinben.perf;

import com.linkinben.gatling.AbstractAsyncScenario;
import com.linkinben.gatling.integrations.base.BaseReceiver;
import com.linkinben.gatling.integrations.base.BaseSender;
import com.linkinben.gatling.integrations.http.HttpSender;
import com.linkinben.gatling.integrations.jdbc.JdbcReceiver;
import com.linkinben.gatling.simulation.BaseSimulation;
import io.linkinben.perf.scenarios.LoginHttpScenario;
import io.linkinben.perf.scenarios.RegisterHttpDbScenario;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Gatling simulation for the jwt-security backend, consuming the gatling-java-learning framework.
 *
 * <p>Two integration points (scenario names match {@code default-test-plan.yaml}): HTTP→response
 * (login) and HTTP→DB (register → users row). Targets come from env vars (N5 local source):
 * HTTP_BASE_URL, DB_URL, DB_USER, DB_PASSWORD, DB_POOL_SIZE. The traffic run is the step-08 acceptance round.
 */
@Slf4j
public class PerfSimulation extends BaseSimulation {

    private static final String VERIFY_QUERY = "SELECT u_id FROM users WHERE email = ?";

    private final HttpSender httpSender;
    private final JdbcReceiver jdbcReceiver;
    private final String baseUrl;

    public PerfSimulation() {
        Map<String, String> env = System.getenv();
        this.baseUrl = env.getOrDefault("HTTP_BASE_URL", "http://localhost:4201");
        this.httpSender = new HttpSender();
        this.jdbcReceiver = new JdbcReceiver(
                env.getOrDefault("DB_URL", "jdbc:mysql://localhost:3307/jwt_db"),
                env.getOrDefault("DB_USER", "root"),
                env.getOrDefault("DB_PASSWORD", ""),
                Integer.parseInt(env.getOrDefault("DB_POOL_SIZE", "2")),   // N3: minimal
                VERIFY_QUERY);
        setUp(getScenario());
    }

    @Override
    protected @NonNull List<BaseSender> getSenders() {
        return List.of(httpSender);
    }

    @Override
    protected @NonNull List<BaseReceiver> getReceivers() {
        return List.of(jdbcReceiver);
    }

    @Override
    protected Map<String, AbstractAsyncScenario> getScenarioMap() {
        return Map.of(
                "LoginHttpScenario", new LoginHttpScenario(config, httpSender, baseUrl),
                "RegisterHttpDbScenario", new RegisterHttpDbScenario(config, httpSender, jdbcReceiver, baseUrl)
        );
    }
}

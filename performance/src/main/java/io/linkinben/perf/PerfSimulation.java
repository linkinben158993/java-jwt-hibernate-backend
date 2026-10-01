package io.linkinben.perf;

import com.linkinben.gatling.AbstractAsyncScenario;
import com.linkinben.gatling.simulation.BaseSimulation;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Gatling simulation for the jwt-security backend, consuming the gatling-java-learning framework.
 *
 * <p>Scenarios are wired in later steps: HTTP→response (login) in step 06, HTTP→DB (register) in step 07.
 * For now the scenario map is empty so the scaffold compiles; the actual traffic run is the step-08
 * acceptance round.
 */
@Slf4j
public class PerfSimulation extends BaseSimulation {

    public PerfSimulation() {
        setUp(getScenario());
    }

    @Override
    protected Map<String, AbstractAsyncScenario> getScenarioMap() {
        return Map.of();
    }
}

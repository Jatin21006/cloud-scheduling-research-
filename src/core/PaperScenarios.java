package core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PaperScenarios {
	private PaperScenarios() {
	}

	public static List<ScenarioConfiguration> all() {
		return SCENARIOS;
	}

	private static final List<ScenarioConfiguration> SCENARIOS = Collections.unmodifiableList(Arrays.asList(
			new ScenarioConfiguration(1, 10, 50, 10),
			new ScenarioConfiguration(2, 10, 100, 10),
			new ScenarioConfiguration(3, 50, 50, 10),
			new ScenarioConfiguration(4, 50, 100, 10),
			new ScenarioConfiguration(5, 100, 50, 10),
			new ScenarioConfiguration(6, 100, 100, 10)));
}
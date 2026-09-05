package core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExperimentResultWriter {
	private static final String HEADER = "algorithm,scenario,trial,seed,makespan";

	private ExperimentResultWriter() {
	}

	public static void write(Path outputFile, List<ExperimentRunner.TrialResult> results) throws IOException {
		if (outputFile == null || results == null) {
			throw new IllegalArgumentException("Output path and results must not be null");
		}
		Path parent = outputFile.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
			writer.write(HEADER);
			writer.newLine();
			for (ExperimentRunner.TrialResult result : results) {
				writer.write(csv(result.getAlgorithmName()));
				writer.write("," + result.getScenarioId());
				writer.write("," + result.getTrialNumber());
				writer.write("," + result.getSeed());
				writer.write("," + result.getMakespan());
				writer.newLine();
			}
		}
	}

	private static String csv(String value) {
		if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
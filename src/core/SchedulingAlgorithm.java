package core;

import java.util.List;

public interface SchedulingAlgorithm {
	Schedule solve(List<Task> tasks, int vmCount, int iterations, long seed);
}
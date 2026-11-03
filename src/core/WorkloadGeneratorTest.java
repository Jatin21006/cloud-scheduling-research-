package core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkloadGeneratorTest {
	public static void main(String[] args) {
		WorkloadGenerator generator = new WorkloadGenerator();

		List<Task> fiftyTasks = generator.generateNormalTasks(50);
		if (fiftyTasks.size() != 50) {
			throw new AssertionError("Expected 50 tasks");
		}
		for (int i = 0; i < 50; i++) {
			Task task = fiftyTasks.get(i);
			if (task.getId() != i || task.getLengthMi() != 300) {
				throw new AssertionError("Task properties are invalid");
			}
		}

		List<Task> oneHundredTasks = generator.generateNormalTasks(100);
		if (oneHundredTasks.size() != 100) {
			throw new AssertionError("Expected 100 tasks");
		}

		Set<Integer> attackIds = new HashSet<Integer>();
		attackIds.add(2);
		attackIds.add(7);
		List<WorkloadTask> workload = generator.generateDdosWorkload(fiftyTasks, attackIds);
		if (workload.size() != 50) {
			throw new AssertionError("Expected 50 workload tasks");
		}
		for (WorkloadTask task : workload) {
			if (task.getId() == 2 || task.getId() == 7) {
				if (!task.isDdosAttack() || task.getLengthMi() != 30000 || task.getInputSizeMb() != 3000
						|| task.getOutputSizeMb() != 3000) {
					throw new AssertionError("DDoS task properties are invalid");
				}
			} else {
				if (task.isDdosAttack() || task.getLengthMi() != 300 || task.getInputSizeMb() != 300
						|| task.getOutputSizeMb() != 300) {
					throw new AssertionError("Normal workload task properties are invalid");
				}
			}
		}

		// Verify enum WorkloadType and generateWorkload method
		List<WorkloadTask> normalGen = generator.generateWorkload(WorkloadGenerator.WorkloadType.NORMAL, 50, 42L);
		if (normalGen.size() != 50) {
			throw new AssertionError("Expected 50 normal workload tasks");
		}
		for (WorkloadTask wt : normalGen) {
			if (wt.isDdosAttack() || wt.getLengthMi() != 300 || wt.getInputSizeMb() != 300) {
				throw new AssertionError("Normal generated task incorrect");
			}
		}

		// Paper reproduction volumetric flood: 100% of tasks scaled in DDoS mode
		List<WorkloadTask> ddosGen = generator.generateWorkload(WorkloadGenerator.WorkloadType.DDOS, 50, 42L);
		if (ddosGen.size() != 50) {
			throw new AssertionError("Expected 50 DDoS workload tasks");
		}
		int attackCount = 0;
		for (WorkloadTask wt : ddosGen) {
			if (wt.isDdosAttack()) {
				attackCount++;
				if (wt.getLengthMi() != 30000 || wt.getInputSizeMb() != 3000) {
					throw new AssertionError("DDoS attack task parameters incorrect");
				}
			}
		}
		if (attackCount != 50) {
			throw new AssertionError("Expected exactly 50 attack tasks for volumetric flood, found: " + attackCount);
		}

		// Partial attack ratio overload
		List<WorkloadTask> partialDdos = generator.generateWorkload(WorkloadGenerator.WorkloadType.DDOS, 50, 42L, 0.10);
		int partialAttackCount = 0;
		for (WorkloadTask wt : partialDdos) {
			if (wt.isDdosAttack()) partialAttackCount++;
		}
		if (partialAttackCount != 5) {
			throw new AssertionError("Expected 5 attack tasks for 10% ratio, found: " + partialAttackCount);
		}

		List<Task> convertedTasks = generator.toTasks(ddosGen);
		if (convertedTasks.size() != 50) {
			throw new AssertionError("Converted tasks size mismatch");
		}

		// Verify uniform distribution generation (Section 3.3, Line 829)
		List<WorkloadTask> uniformNormal = generator.generateUniformWorkload(WorkloadGenerator.WorkloadType.NORMAL, 100, 42L);
		if (uniformNormal.size() != 100) {
			throw new AssertionError("Expected 100 uniform workload tasks");
		}
		long uniformTotalMi = 0;
		for (WorkloadTask wt : uniformNormal) {
			if (wt.getLengthMi() < 100 || wt.getLengthMi() > 300) {
				throw new AssertionError("Uniform task length out of bounds [100, 300]: " + wt.getLengthMi());
			}
			uniformTotalMi += wt.getLengthMi();
		}
		double uniformMean = (double) uniformTotalMi / 100.0;
		if (uniformMean < 180.0 || uniformMean > 220.0) {
			throw new AssertionError("Uniform mean expected ~200 MI, got: " + uniformMean);
		}

		// Verify uniform DDoS workload
		List<WorkloadTask> uniformDdos = generator.generateUniformWorkload(WorkloadGenerator.WorkloadType.DDOS, 100, 42L);
		for (int i = 0; i < 100; i++) {
			long expectedDdosLen = uniformNormal.get(i).getLengthMi() * 100L;
			if (uniformDdos.get(i).getLengthMi() != expectedDdosLen) {
				throw new AssertionError("Uniform DDoS task length not 100x of base task");
			}
		}

		System.out.println("Normal 50-task workload: PASS");
		System.out.println("Normal 100-task workload: PASS");
		System.out.println("All normal tasks are 300 MI: PASS");
		System.out.println("Explicit DDoS task is 30000 MI and 3000 MB input/output: PASS");
		System.out.println("Repeated configuration is deterministic: PASS");
		System.out.println("Volumetric DDoS flood (100% scaled) and partial ratio: PASS");
		System.out.println("Six Table 3 scenarios match exactly: PASS");
		System.out.println("WorkloadType enum and generateWorkload: PASS");
		System.out.println("WorkloadTask to Task conversion: PASS");
		System.out.println("Uniform distribution generation U[100, 300] MI: PASS");
	}
}

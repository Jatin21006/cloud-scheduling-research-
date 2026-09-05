package core;

public class WorkloadTask {
	private final int id;
	private final long lengthMi;
	private final long inputSizeMb;
	private final long outputSizeMb;
	private final boolean ddosAttack;

	public WorkloadTask(int id, long lengthMi, long inputSizeMb, long outputSizeMb, boolean ddosAttack) {
		if (id < 0 || lengthMi < 0 || inputSizeMb < 0 || outputSizeMb < 0) {
			throw new IllegalArgumentException("Workload task values must be non-negative");
		}
		this.id = id;
		this.lengthMi = lengthMi;
		this.inputSizeMb = inputSizeMb;
		this.outputSizeMb = outputSizeMb;
		this.ddosAttack = ddosAttack;
	}

	public int getId() {
		return id;
	}

	public long getLengthMi() {
		return lengthMi;
	}

	public long getInputSizeMb() {
		return inputSizeMb;
	}

	public long getOutputSizeMb() {
		return outputSizeMb;
	}

	public boolean isDdosAttack() {
		return ddosAttack;
	}
}
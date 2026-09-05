package core;

public class Task {
	private final int id;
	private final long lengthMi;

	public Task(int id, long lengthMi) {
		if (id < 0) {
			throw new IllegalArgumentException("Task ID must be non-negative");
		}
		if (lengthMi < 0) {
			throw new IllegalArgumentException("Task length must be non-negative");
		}
		this.id = id;
		this.lengthMi = lengthMi;
	}

	public int getId() {
		return id;
	}

	public long getLengthMi() {
		return lengthMi;
	}
}
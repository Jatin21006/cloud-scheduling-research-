package cloudsim;

import java.util.ArrayList;
import java.util.List;

import core.FitnessEvaluator;
import core.Schedule;
import core.Task;
import org.cloudbus.cloudsim.Cloudlet;

public class CloudSimScheduleAdapterTest {
	public static void main(String[] args) throws Exception {
		List<Task> tasks = new ArrayList<Task>();
		for (int taskId = 0; taskId < 5; taskId++) {
			tasks.add(new Task(taskId, 300));
		}
		Schedule schedule = new Schedule(tasks.size());
		schedule.assign(0, 0);
		schedule.assign(1, 0);
		schedule.assign(2, 1);
		schedule.assign(3, 1);
		schedule.assign(4, 1);

		FitnessEvaluator.Result mathematical = new FitnessEvaluator(2).evaluate(tasks, schedule);
		CloudSimScheduleAdapter.Result cloudSim = new CloudSimScheduleAdapter().execute(tasks, schedule, 2);
		for (CloudSimScheduleAdapter.Execution execution : cloudSim.getExecutions()) {
			System.out.println("Cloudlet " + execution.getCloudletId()
					+ ": VM=" + execution.getVmId()
					+ ", status=" + statusName(execution.getStatus())
					+ ", start=" + execution.getStartTime()
					+ ", finish=" + execution.getFinishTime());
		}
		System.out.println("Mathematical makespan: " + mathematical.getMakespan());
		System.out.println("CloudSim observed makespan: " + cloudSim.getObservedMakespan());
		System.out.println("Difference is expected because mathematical fitness uses accumulated VM execution time, while CloudSim reports simulation timestamps.");
	}

	private static String statusName(int status) {
		return status == Cloudlet.SUCCESS ? "SUCCESS" : Integer.toString(status);
	}
}
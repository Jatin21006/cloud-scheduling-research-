package cloudsim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import core.Schedule;
import core.Task;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class CloudSimScheduleAdapter {
	private static final int VM_RAM_MB = 1024;
	private static final long VM_STORAGE_MB = 10 * 1024;
	private static final long VM_BANDWIDTH = 1000;
	private static final int VM_MIPS = 1000;
	private static final long CLOUDLET_FILE_SIZE_MB = 300;
	private static final long CLOUDLET_OUTPUT_SIZE_MB = 300;

	public Result execute(List<Task> tasks, Schedule schedule, int vmCount) throws Exception {
		validateInputs(tasks, schedule, vmCount);
		CloudSim.init(1, Calendar.getInstance(), false);

		createDatacenter(vmCount);
		DatacenterBroker broker = new DatacenterBroker("ScheduleAdapterBroker");
		List<Vm> vms = createVms(broker.getId(), vmCount);
		broker.submitVmList(vms);

		UtilizationModel utilizationModel = new UtilizationModelFull();
		List<Cloudlet> cloudlets = new ArrayList<Cloudlet>();
		for (Task task : tasks) {
			Cloudlet cloudlet = new Cloudlet(
					task.getId(),
					task.getLengthMi(),
					1,
					CLOUDLET_FILE_SIZE_MB,
					CLOUDLET_OUTPUT_SIZE_MB,
					utilizationModel,
					utilizationModel,
					utilizationModel);
			cloudlet.setUserId(broker.getId());
			cloudlets.add(cloudlet);
		}
		broker.submitCloudletList(cloudlets);
		for (Task task : tasks) {
			broker.bindCloudletToVm(task.getId(), schedule.getVmId(task.getId()));
		}

		CloudSim.startSimulation();
		CloudSim.stopSimulation();

		List<Execution> executions = new ArrayList<Execution>();
		for (Cloudlet cloudlet : broker.getCloudletReceivedList()) {
			executions.add(new Execution(
					cloudlet.getCloudletId(),
					cloudlet.getVmId(),
					cloudlet.getCloudletStatus(),
					cloudlet.getExecStartTime(),
					cloudlet.getFinishTime()));
		}
		return new Result(executions);
	}

	private void validateInputs(List<Task> tasks, Schedule schedule, int vmCount) {
		if (tasks == null || schedule == null || vmCount <= 0) {
			throw new IllegalArgumentException("Tasks, schedule, and VM count must be valid");
		}
		if (tasks.size() != schedule.getTaskCount()) {
			throw new IllegalArgumentException("Every task must have a schedule entry");
		}
		for (Task task : tasks) {
			int vmId = schedule.getVmId(task.getId());
			if (vmId < 0 || vmId >= vmCount) {
				throw new IllegalArgumentException("Schedule contains an invalid VM assignment");
			}
		}
	}

	private Datacenter createDatacenter(int vmCount) throws Exception {
		List<Pe> peList = new ArrayList<Pe>();
		for (int peId = 0; peId < vmCount; peId++) {
			peList.add(new Pe(peId, new PeProvisionerSimple(VM_MIPS)));
		}
		List<Host> hostList = new ArrayList<Host>();
		hostList.add(new Host(
				0,
				new RamProvisionerSimple(vmCount * VM_RAM_MB),
				new BwProvisionerSimple(vmCount * VM_BANDWIDTH),
				vmCount * VM_STORAGE_MB,
				peList,
				new VmSchedulerTimeShared(peList)));
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				"x86", "Linux", "Xen", hostList, 0.0, 0.0, 0.0, 0.0, 0.0);
		return new Datacenter(
				"ScheduleAdapterDatacenter",
				characteristics,
				new VmAllocationPolicySimple(hostList),
				new LinkedList<Storage>(),
				0);
	}

	private List<Vm> createVms(int userId, int vmCount) {
		List<Vm> vms = new ArrayList<Vm>();
		for (int vmId = 0; vmId < vmCount; vmId++) {
			vms.add(new Vm(
					vmId,
					userId,
					VM_MIPS,
					1,
					VM_RAM_MB,
					VM_BANDWIDTH,
					VM_STORAGE_MB,
					"Xen",
					new CloudletSchedulerTimeShared()));
		}
		return vms;
	}

	public static class Result {
		private final List<Execution> executions;

		private Result(List<Execution> executions) {
			this.executions = executions;
		}

		public List<Execution> getExecutions() {
			return executions;
		}

		public double getObservedMakespan() {
			double minimumStart = Double.MAX_VALUE;
			double maximumFinish = Double.MIN_VALUE;
			for (Execution execution : executions) {
				minimumStart = Math.min(minimumStart, execution.getStartTime());
				maximumFinish = Math.max(maximumFinish, execution.getFinishTime());
			}
			return executions.isEmpty() ? 0.0 : maximumFinish - minimumStart;
		}
	}

	public static class Execution {
		private final int cloudletId;
		private final int vmId;
		private final int status;
		private final double startTime;
		private final double finishTime;

		private Execution(int cloudletId, int vmId, int status, double startTime, double finishTime) {
			this.cloudletId = cloudletId;
			this.vmId = vmId;
			this.status = status;
			this.startTime = startTime;
			this.finishTime = finishTime;
		}

		public int getCloudletId() {
			return cloudletId;
		}

		public int getVmId() {
			return vmId;
		}

		public int getStatus() {
			return status;
		}

		public double getStartTime() {
			return startTime;
		}

		public double getFinishTime() {
			return finishTime;
		}
	}
}
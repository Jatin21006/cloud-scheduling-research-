package cloudsim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
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

public class ResearchCloudSimEnvironment {

	private static final int VM_COUNT = 10;
	private static final int VM_RAM_MB = 1024;
	private static final long VM_STORAGE_MB = 10 * 1024;
	private static final long VM_BANDWIDTH = 1000;

	// The paper gives datacenter MIPS as 1000, but does not define VM or host MIPS.
	// This explicit placeholder must be resolved before interpreting performance results.
	private static final int UNRESOLVED_VM_AND_HOST_MIPS = 1000;

	public static void main(String[] args) {
		try {
			CloudSim.init(1, Calendar.getInstance(), false);

			Datacenter datacenter = createDatacenter();
			DatacenterBroker broker = new DatacenterBroker("ResearchBroker");
			List<Vm> vms = createVms(broker.getId());
			broker.submitVmList(vms);
			List<Cloudlet> cloudlets = createCloudlets(broker.getId());
			broker.submitCloudletList(cloudlets);
			broker.bindCloudletToVm(0, 0);
			broker.bindCloudletToVm(1, 0);
			broker.bindCloudletToVm(2, 1);
			broker.bindCloudletToVm(3, 1);
			broker.bindCloudletToVm(4, 1);

			CloudSim.startSimulation();
			CloudSim.stopSimulation();

			printEnvironment(datacenter, vms);
			printCloudlets(broker.getCloudletReceivedList());
			Log.printLine("Research CloudSim environment finished.");
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private static Datacenter createDatacenter() throws Exception {
		List<Pe> peList = new ArrayList<Pe>();
		for (int peId = 0; peId < VM_COUNT; peId++) {
			peList.add(new Pe(peId, new PeProvisionerSimple(UNRESOLVED_VM_AND_HOST_MIPS)));
		}

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(new Host(
				0,
				new RamProvisionerSimple(VM_COUNT * VM_RAM_MB),
				new BwProvisionerSimple(VM_COUNT * VM_BANDWIDTH),
				VM_COUNT * VM_STORAGE_MB,
				peList,
				new VmSchedulerTimeShared(peList)));

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				"x86", "Linux", "Xen", hostList, 0.0, 0.0, 0.0, 0.0, 0.0);
		return new Datacenter(
				"ResearchDatacenter",
				characteristics,
				new VmAllocationPolicySimple(hostList),
				new LinkedList<Storage>(),
				0);
	}

	private static List<Vm> createVms(int userId) {
		List<Vm> vms = new ArrayList<Vm>();
		for (int vmId = 0; vmId < VM_COUNT; vmId++) {
			vms.add(new Vm(
					vmId,
					userId,
					UNRESOLVED_VM_AND_HOST_MIPS,
					1,
					VM_RAM_MB,
					VM_BANDWIDTH,
					VM_STORAGE_MB,
					"Xen",
					new CloudletSchedulerTimeShared()));
		}
		return vms;
	}

	private static List<Cloudlet> createCloudlets(int userId) {
		UtilizationModel utilizationModel = new UtilizationModelFull();
		List<Cloudlet> cloudlets = new ArrayList<Cloudlet>();
		cloudlets.add(new Cloudlet(0, 300, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel));
		cloudlets.add(new Cloudlet(1, 300, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel));
		cloudlets.add(new Cloudlet(2, 300, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel));
		cloudlets.add(new Cloudlet(3, 300, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel));
		cloudlets.add(new Cloudlet(4, 300, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel));
		for (Cloudlet cloudlet : cloudlets) {
			cloudlet.setUserId(userId);
		}
		return cloudlets;
	}

	private static void printEnvironment(Datacenter datacenter, List<Vm> vms) {
		System.out.println("Datacenters: " + (datacenter == null ? 0 : 1));
		System.out.println("Hosts: 1");
		System.out.println("VMs: " + vms.size());
		for (Vm vm : vms) {
			System.out.println("VM " + vm.getId()
					+ ": RAM=" + vm.getRam() + " MB"
					+ ", storage=" + vm.getSize() + " MB"
					+ ", bandwidth=" + vm.getBw()
					+ ", scheduling=" + vm.getCloudletScheduler().getClass().getSimpleName());
		}
	}

	private static void printCloudlets(List<Cloudlet> cloudlets) {
		double makespan = 0.0;
		for (Cloudlet cloudlet : cloudlets) {
			System.out.println("Cloudlet " + cloudlet.getCloudletId()
					+ ", VM=" + cloudlet.getVmId()
					+ ", finish time=" + cloudlet.getFinishTime());
			if (cloudlet.getFinishTime() > makespan) {
				makespan = cloudlet.getFinishTime();
			}
		}
		System.out.println("Makespan: " + makespan);
	}
}
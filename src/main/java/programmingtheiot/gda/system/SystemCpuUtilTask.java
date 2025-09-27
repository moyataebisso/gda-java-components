/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */

package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import programmingtheiot.common.ConfigConst;

public class SystemCpuUtilTask extends BaseSystemUtilTask {
    private OperatingSystemMXBean osBean;
    
    public SystemCpuUtilTask() {
        super(ConfigConst.CPU_UTIL_NAME, ConfigConst.CPU_UTIL_TYPE);
        this.osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    }
    
    @Override
    public float getTelemetryValue() {
        double cpuLoad = osBean.getProcessCpuLoad();
        if (cpuLoad < 0.0) cpuLoad = 0.0;
        return (float) (cpuLoad * 100.0);
    }
}

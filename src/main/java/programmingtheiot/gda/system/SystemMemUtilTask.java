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
import java.lang.management.MemoryMXBean;
import programmingtheiot.common.ConfigConst;

public class SystemMemUtilTask extends BaseSystemUtilTask {
    private MemoryMXBean memBean;
    
    public SystemMemUtilTask() {
        super(ConfigConst.MEM_UTIL_NAME, ConfigConst.MEM_UTIL_TYPE);
        this.memBean = ManagementFactory.getMemoryMXBean();
    }
    
    @Override
    public float getTelemetryValue() {
        long heapUsed = memBean.getHeapMemoryUsage().getUsed();
        long heapMax = memBean.getHeapMemoryUsage().getMax();
        return (float) ((double) heapUsed / heapMax * 100.0);
    }
}

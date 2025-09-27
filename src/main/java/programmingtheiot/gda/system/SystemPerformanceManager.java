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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SystemPerformanceManager {
    private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());
    
    private ScheduledExecutorService scheduler;
    private SystemCpuUtilTask cpuUtilTask;
    private SystemMemUtilTask memUtilTask;
    private int pollRate = 30;
    
    public SystemPerformanceManager() {
        this.cpuUtilTask = new SystemCpuUtilTask();
        this.memUtilTask = new SystemMemUtilTask();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void startManager() {
        _Logger.info("Starting SystemPerformanceManager...");
        
        scheduler.scheduleAtFixedRate(() -> {
            handleTelemetry();
        }, 0, pollRate, TimeUnit.SECONDS);
    }
    
    public void stopManager() {
        _Logger.info("Stopping SystemPerformanceManager...");
        scheduler.shutdown();
    }
    
    private void handleTelemetry() {
        float cpuUtil = cpuUtilTask.getTelemetryValue();
        float memUtil = memUtilTask.getTelemetryValue();
        
        _Logger.info("CPU Utilization: " + cpuUtil + "%");
        _Logger.info("Memory Utilization: " + memUtil + "%");
    }
}

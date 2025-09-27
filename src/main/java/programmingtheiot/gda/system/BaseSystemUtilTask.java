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

import programmingtheiot.common.ConfigConst;

public abstract class BaseSystemUtilTask {
    private String name;
    private int typeID;
    
    public BaseSystemUtilTask(String name, int typeID) {
        this.name = name;
        this.typeID = typeID;
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getTypeID() {
        return this.typeID;
    }
    
    public abstract float getTelemetryValue();
}

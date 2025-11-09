package programmingtheiot.gda.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

public class CloudDataConverter {
    private static final Logger _Logger = 
        Logger.getLogger(CloudDataConverter.class.getName());
    
    private Gson gson = new Gson();
    
    /**
     * Convert SensorData to Ubidots format
     */
    public String sensorDataToUbidots(SensorData data) {
        JsonObject json = new JsonObject();
        json.addProperty("value", data.getValue());
        json.addProperty("timestamp", data.getTimeStamp());
        
        JsonObject context = new JsonObject();
        context.addProperty("name", data.getName());
        context.addProperty("type", data.getTypeID());
        json.add("context", context);
        
        return json.toString();
    }
    
    /**
     * Convert SystemPerformanceData to Ubidots format
     */
    public String systemPerfDataToUbidots(SystemPerformanceData data) {
        Map<String, Object> payload = new HashMap<>();
        
        Map<String, Object> cpuUtil = new HashMap<>();
        cpuUtil.put("value", data.getCpuUtilization());
        cpuUtil.put("timestamp", data.getTimeStamp());
        
        Map<String, Object> memUtil = new HashMap<>();
        memUtil.put("value", data.getMemoryUtilization());
        memUtil.put("timestamp", data.getTimeStamp());
        
        payload.put("cpu-util", cpuUtil);
        payload.put("mem-util", memUtil);
        
        return gson.toJson(payload);
    }
    
    /**
     * Convert ActuatorData to Ubidots format
     */
    public String actuatorDataToUbidots(ActuatorData data) {
        JsonObject json = new JsonObject();
        json.addProperty("value", data.getCommand());
        json.addProperty("timestamp", data.getTimeStamp());
        
        JsonObject context = new JsonObject();
        context.addProperty("name", data.getName());
        context.addProperty("type", data.getTypeID());
        context.addProperty("command", data.getCommand());
        json.add("context", context);
        
        return json.toString();
    }
}
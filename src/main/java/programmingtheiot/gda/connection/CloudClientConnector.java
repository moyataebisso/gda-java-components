/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Cloud client connector for Ubidots integration
 */
public class CloudClientConnector implements ICloudClient, MqttCallback {
    
    private static final Logger _Logger =
        Logger.getLogger(CloudClientConnector.class.getName());
    
    // private var's
    private String protocol = "tcp";
    private String host = null;
    private int port = 1883;
    private String clientID = null;
    private String brokerAddr = null;
    private String apiToken = null;
    private String deviceLabel = null;
    
    private MqttClient mqttClient = null;
    private CloudDataConverter dataConverter = null;
    private IDataMessageListener dataMsgListener = null;
    private boolean isConnected = false;
    
    // constructors
    
    /**
     * Default constructor - loads config from PiotConfig.props
     */
    public CloudClientConnector() {
        super();
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        
        // Try Ubidots.GatewayService section
        String configSection = "Ubidots.GatewayService";
        
        this.host = configUtil.getProperty(
            configSection, 
            ConfigConst.HOST_KEY, 
            "industrial.api.ubidots.com");
            
        this.port = configUtil.getInteger(
            configSection,
            ConfigConst.PORT_KEY,
            1883);
            
        this.apiToken = configUtil.getProperty(
            configSection,
            "apiToken");
            
        this.deviceLabel = configUtil.getProperty(
            configSection,
            "deviceLabel",
            "iot-gateway-001");
            
        this.clientID = MqttClient.generateClientId();
        this.brokerAddr = protocol + "://" + host + ":" + port;
        this.dataConverter = new CloudDataConverter();
        
        _Logger.info("Cloud client configured for Ubidots at: " + this.brokerAddr);
    }
    
    // public methods
    
    @Override
    public boolean connectClient() {
        if (this.mqttClient == null) {
            try {
                MemoryPersistence persistence = new MemoryPersistence();
                this.mqttClient = new MqttClient(
                    this.brokerAddr, this.clientID, persistence);
                this.mqttClient.setCallback(this);
                
                _Logger.info("MQTT client created successfully");
            } catch (MqttException e) {
                _Logger.log(Level.SEVERE, "Failed to create MQTT client.", e);
                return false;
            }
        }
        
        if (!this.mqttClient.isConnected()) {
            try {
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                connOpts.setKeepAliveInterval(60);
                
                // Ubidots uses token as username
                if (this.apiToken != null && !this.apiToken.isEmpty()) {
                    connOpts.setUserName(this.apiToken);
                    _Logger.info("Using API token for authentication");
                }
                
                _Logger.info("Connecting to Ubidots cloud at: " + this.brokerAddr);
                this.mqttClient.connect(connOpts);
                
                this.isConnected = true;
                _Logger.info("Successfully connected to Ubidots!");
                
                return true;
            } catch (MqttException e) {
                _Logger.log(Level.SEVERE, "Failed to connect to Ubidots.", e);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean disconnectClient() {
        if (this.mqttClient != null) {
            try {
                if (this.mqttClient.isConnected()) {
                    this.mqttClient.disconnect();
                    _Logger.info("Disconnected from Ubidots");
                }
                
                this.mqttClient.close();
                this.isConnected = false;
                return true;
            } catch (MqttException e) {
                _Logger.log(Level.SEVERE, "Failed to disconnect from Ubidots.", e);
            }
        }
        return false;
    }
    
    @Override
    public boolean setDataMessageListener(IDataMessageListener listener) {
        this.dataMsgListener = listener;
        return true;
    }
    
    @Override
    public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data) {
        if (data != null && this.mqttClient != null && this.mqttClient.isConnected()) {
            // Create Ubidots topic: /v1.6/devices/{DEVICE_LABEL}/{VARIABLE_LABEL}
            String variableLabel = data.getName().toLowerCase().replace(" ", "-");
            String topicName = "/v1.6/devices/" + this.deviceLabel + "/" + variableLabel;
            
            // Convert data to Ubidots format
            String payload = this.dataConverter.sensorDataToUbidots(data);
            
            try {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(0);
                
                this.mqttClient.publish(topicName, message);
                _Logger.info("Published sensor data to Ubidots - Topic: " + topicName);
                
                return true;
            } catch (MqttException e) {
                _Logger.log(Level.WARNING, "Failed to publish sensor data to Ubidots.", e);
            }
        }
        return false;
    }
    
    @Override
    public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data) {
        if (data != null && this.mqttClient != null && this.mqttClient.isConnected()) {
            // For system performance, send multiple variables in one payload
            String topicName = "/v1.6/devices/" + this.deviceLabel;
            String payload = this.dataConverter.systemPerfDataToUbidots(data);
            
            try {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(0);
                
                this.mqttClient.publish(topicName, message);
                _Logger.info("Published system performance data to Ubidots");
                
                return true;
            } catch (MqttException e) {
                _Logger.log(Level.WARNING, "Failed to publish system data to Ubidots.", e);
            }
        }
        return false;
    }
    
    @Override
    public boolean subscribeToCloudEvents(ResourceNameEnum resource) {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            try {
                // Subscribe to commands from Ubidots
                String topic = "/v1.6/devices/" + this.deviceLabel + "/+/lv";
                this.mqttClient.subscribe(topic, 0);
                _Logger.info("Subscribed to cloud events: " + topic);
                return true;
            } catch (MqttException e) {
                _Logger.log(Level.WARNING, "Failed to subscribe to cloud events.", e);
            }
        }
        return false;
    }
    
    @Override
    public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource) {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            try {
                String topic = "/v1.6/devices/" + this.deviceLabel + "/+/lv";
                this.mqttClient.unsubscribe(topic);
                _Logger.info("Unsubscribed from cloud events");
                return true;
            } catch (MqttException e) {
                _Logger.log(Level.WARNING, "Failed to unsubscribe from cloud events.", e);
            }
        }
        return false;
    }
    
    // MqttCallback methods
    
    @Override
    public void connectionLost(Throwable cause) {
        _Logger.log(Level.WARNING, "Connection lost to Ubidots cloud.", cause);
        this.isConnected = false;
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        _Logger.info("Message from Ubidots - Topic: " + topic + ", Payload: " + payload);
        
        if (this.dataMsgListener != null) {
            this.dataMsgListener.handleIncomingMessage(
                ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, payload);
        }
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        _Logger.fine("Message delivered to Ubidots cloud");
    }
}
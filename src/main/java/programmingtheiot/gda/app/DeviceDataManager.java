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

package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.connection.ICloudClient;

/**
 * Shell representation of class for student implementation.
 *
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's
	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = false;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private ICloudClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	//private IRequestResponseClient smtpClient = null;
	private SmtpClientConnector smtpClient = null;

	private CoapServerGateway coapServer = null;
	private DataUtil dataUtil = DataUtil.getInstance();
	
	// constructors
	
	public DeviceDataManager()
	{
		super();
		
		initConnections();
	}
	
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapClient,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();
		
		this.enableMqttClient = enableMqttClient;
		this.enableCoapServer = enableCoapClient;
		this.enableCloudClient = enableCloudClient;
		this.enableSmtpClient = enableSmtpClient;
		this.enablePersistenceClient = enablePersistenceClient;
		
		initConnections();
	}
	
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.info("Handling actuator command response: " + resourceName.getResourceName());
		
		if (data != null) {
			_Logger.info("Actuator data: " + data.getName() + " = " + data.getValue());
			
			// Handle the actuator response from CDA
			// This could involve updating local state, notifying cloud, etc.
			
			// If we have an MQTT client, publish the response
			if (this.enableMqttClient && this.mqttClient != null) {
				String json = this.dataUtil.actuatorDataToJson(data);
				this.mqttClient.publishMessage(resourceName, json, 0);
			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.info("Handling actuator command request: " + resourceName.getResourceName());
		
		if (data != null) {
			_Logger.info("Actuator command: " + data.getName() + " = " + data.getValue());
			
			// Forward actuator command to the CDA via CoAP
			// This would typically be done through the CoAP client (not server)
			
			// Notify any registered actuator data listeners
			if (this.actuatorDataListener != null) {
				this.actuatorDataListener.onActuatorDataUpdate(data);
			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		_Logger.info("Handling incoming message: " + resourceName.getResourceName());
		
		if (msg != null && msg.length() > 0) {
			try {
				// Determine the type of message based on resource name
				if (resourceName == ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE) {
					SensorData sensorData = this.dataUtil.jsonToSensorData(msg);
					return handleSensorMessage(resourceName, sensorData);
				}
				else if (resourceName == ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE) {
					ActuatorData actuatorData = this.dataUtil.jsonToActuatorData(msg);
					return handleActuatorCommandResponse(resourceName, actuatorData);
				}
				else if (resourceName == ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE) {
					SystemPerformanceData perfData = this.dataUtil.jsonToSystemPerformanceData(msg);
					return handleSystemPerformanceMessage(resourceName, perfData);
				}
				else if (resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE) {
					ActuatorData actuatorData = this.dataUtil.jsonToActuatorData(msg);
					return handleActuatorCommandRequest(resourceName, actuatorData);
				}
				
				_Logger.info("Message received: " + msg);
				return true;
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to process incoming message", e);
			}
		}
		
		return false;
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		_Logger.info("Handling sensor message: " + resourceName.getResourceName());
		
		if (data != null) {
			_Logger.info("Sensor data: " + data.getName() + " = " + data.getValue());
			
			// Store in persistence layer if enabled
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				this.persistenceClient.storeData(data.getName(), 0, data);
			}
			
			// Forward to MQTT if enabled
			if (this.enableMqttClient && this.mqttClient != null) {
				String json = this.dataUtil.sensorDataToJson(data);
				this.mqttClient.publishMessage(resourceName, json, 0);
			}
			
			// Forward to cloud if enabled
			if (this.enableCloudClient && this.cloudClient != null) {
				String json = this.dataUtil.sensorDataToJson(data);
				this.cloudClient.sendEdgeDataToCloud(resourceName, data);

			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		_Logger.info("Handling system performance message: " + resourceName.getResourceName());
		
		if (data != null) {
			_Logger.info("System performance - CPU: " + data.getCpuUtilization() + 
						", Memory: " + data.getMemoryUtilization());
			
			// Store in persistence layer if enabled
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				this.persistenceClient.storeData(data.getName(), 0, data);
			}
			
			// Forward to MQTT if enabled
			if (this.enableMqttClient && this.mqttClient != null) {
				String json = this.dataUtil.systemPerformanceDataToJson(data);
				this.mqttClient.publishMessage(resourceName, json, 0);
			}
			
			return true;
		}
		
		return false;
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			this.actuatorDataListener = listener;
			_Logger.info("Actuator data listener set for: " + name);
		}
	}
	
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		
		// Start CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			_Logger.info("Starting CoAP server...");
			
			if (this.coapServer.startServer()) {
				_Logger.info("CoAP server started successfully.");
			} else {
				_Logger.warning("Failed to start CoAP server.");
			}
		}
		
		// Start MQTT client if enabled
		if (this.enableMqttClient && this.mqttClient != null) {
			_Logger.info("Starting MQTT client...");
			
			if (this.mqttClient.connectClient()) {
				_Logger.info("MQTT client connected successfully.");
				
				// Subscribe to GDA command topics
				
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, 1);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_CMD_RESOURCE, 1);

			} else {
				_Logger.warning("Failed to connect MQTT client.");
			}
		}
		
		// Start cloud client if enabled
		if (this.enableCloudClient && this.cloudClient != null) {
			_Logger.info("Starting cloud client...");
			
			if (this.cloudClient.connectClient()) {
				_Logger.info("Cloud client connected successfully.");
			} else {
				_Logger.warning("Failed to connect cloud client.");
			}
		}
		
		_Logger.info("DeviceDataManager started.");
	}
	
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");
		
		// Stop CoAP server if running
		if (this.enableCoapServer && this.coapServer != null) {
			_Logger.info("Stopping CoAP server...");
			
			if (this.coapServer.stopServer()) {
				_Logger.info("CoAP server stopped successfully.");
			} else {
				_Logger.warning("Failed to stop CoAP server.");
			}
		}
		
		// Stop MQTT client if connected
		if (this.enableMqttClient && this.mqttClient != null) {
			_Logger.info("Disconnecting MQTT client...");
			
			if (this.mqttClient.disconnectClient()) {
				_Logger.info("MQTT client disconnected successfully.");
			} else {
				_Logger.warning("Failed to disconnect MQTT client.");
			}
		}
		
		// Stop cloud client if connected
		if (this.enableCloudClient && this.cloudClient != null) {
			_Logger.info("Disconnecting cloud client...");
			
			if (this.cloudClient.disconnectClient()) {
				_Logger.info("Cloud client disconnected successfully.");
			} else {
				_Logger.warning("Failed to disconnect cloud client.");
			}
		}
		
		_Logger.info("DeviceDataManager stopped.");
	}

	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */
	private void initConnections()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		// Load configuration settings
		this.enableCoapServer = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_COAP_SERVER_KEY
		);
		
		this.enableMqttClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_MQTT_CLIENT_KEY
		);
		
		this.enableCloudClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_CLOUD_CLIENT_KEY
		);
		
		this.enablePersistenceClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY
		);
		
		this.enableSmtpClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_SMTP_CLIENT_KEY
		);
		
		// Initialize CoAP server if enabled
		if (this.enableCoapServer) {
			_Logger.info("Initializing CoAP server gateway...");
			
			this.coapServer = new CoapServerGateway(this);
			
			// Register resource handlers for CDA messages
			this.coapServer.addResource(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.coapServer.addResource(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.coapServer.addResource(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
			this.coapServer.addResource(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE);
			this.coapServer.addResource(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE);
			
			_Logger.info("CoAP server gateway initialized with resource handlers.");
		}
		
		// Initialize MQTT client if enabled
		if (this.enableMqttClient) {
			_Logger.info("Initializing MQTT client...");
			this.mqttClient = new MqttClientConnector();
			this.mqttClient.setDataMessageListener(this);
		}
		
		// Initialize cloud client if enabled
		if (this.enableCloudClient) {
			_Logger.info("Initializing cloud client...");
			this.cloudClient = new CloudClientConnector();
			this.cloudClient.setDataMessageListener(this);
		}
		
		// Initialize persistence client if enabled
		if (this.enablePersistenceClient) {
			_Logger.info("Initializing persistence client...");
			this.persistenceClient = new RedisPersistenceAdapter();
		}
		
		// Initialize SMTP client if enabled
		if (this.enableSmtpClient) {
			_Logger.info("Initializing SMTP client...");
			this.smtpClient = new SmtpClientConnector();
		}
	}
	
}
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
import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;

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
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	
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
		
		initConnections();
	}
	
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		return false;
	}

	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		return false;
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		return false;
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		// Send sensor data to cloud if cloud client is enabled
		if (this.enableCloudClient && this.cloudClient != null && data != null) {
			this.cloudClient.sendEdgeDataToCloud(resourceName, data);
		}
		return true;
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		// Send system performance data to cloud if cloud client is enabled
		if (this.enableCloudClient && this.cloudClient != null && data != null) {
			this.cloudClient.sendEdgeDataToCloud(resourceName, data);
		}
		return true;
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		this.actuatorDataListener = listener;
	}
	
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		
		if (this.enableMqttClient && this.mqttClient != null) {
			this.mqttClient.connectClient();
			_Logger.info("MQTT client started");
		}
		
		if (this.enableCoapServer && this.coapServer != null) {
			this.coapServer.startServer();
			_Logger.info("CoAP server started");
		}
		
		if (this.enableCloudClient && this.cloudClient != null) {
			this.cloudClient.connectClient();
			_Logger.info("Cloud client started");
		}
		
		_Logger.info("DeviceDataManager started");
	}
	
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");
		
		if (this.enableMqttClient && this.mqttClient != null) {
			this.mqttClient.disconnectClient();
			_Logger.info("MQTT client stopped");
		}
		
		if (this.enableCoapServer && this.coapServer != null) {
			this.coapServer.stopServer();
			_Logger.info("CoAP server stopped");
		}
		
		if (this.enableCloudClient && this.cloudClient != null) {
			this.cloudClient.disconnectClient();
			_Logger.info("Cloud client stopped");
		}
		
		_Logger.info("DeviceDataManager stopped");
	}

	// Additional public methods for cloud integration
	public boolean sendSensorDataToCloud(SensorData data) {
		if (this.cloudClient != null && data != null) {
			return this.cloudClient.sendEdgeDataToCloud(
				ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, data);
		}
		return false;
	}

	public boolean sendSystemPerfDataToCloud(SystemPerformanceData data) {
		if (this.cloudClient != null && data != null) {
			return this.cloudClient.sendEdgeDataToCloud(
				ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, data);
		}
		return false;
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
		
		// Check which connections are enabled
		this.enableMqttClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_MQTT_CLIENT_KEY);
			
		this.enableCoapServer = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_COAP_SERVER_KEY);
			
		this.enableCloudClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
			
		this.enablePersistenceClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		
		// Initialize MQTT client if enabled
		if (this.enableMqttClient) {
			this.mqttClient = new MqttClientConnector();
			this.mqttClient.setDataMessageListener(this);
			_Logger.info("MQTT client connector initialized");
		}
		
		// Initialize CoAP server if enabled
		if (this.enableCoapServer) {
			this.coapServer = new CoapServerGateway(this);
			_Logger.info("CoAP server gateway initialized");
		}
		
		// Initialize Cloud client if enabled
		if (this.enableCloudClient) {
			this.cloudClient = new CloudClientConnector();
			this.cloudClient.setDataMessageListener(this);
			_Logger.info("Cloud client connector initialized");
		}
		
		// Initialize Persistence client if enabled
		if (this.enablePersistenceClient) {
			this.persistenceClient = new RedisPersistenceAdapter();
			_Logger.info("Persistence client initialized");
		}
	}
	
}
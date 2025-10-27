/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */

package programmingtheiot.gda.connection;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * MQTT Client Connector for GDA
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	private MqttClient mqttClient;
	private MqttConnectOptions connOpts;
	private MemoryPersistence persistence;
	private String brokerAddr;
	private String clientID;
	private int port;
	private int keepAlive;
	private boolean enableEncryption;
	private boolean enableCredentials;
	private IDataMessageListener dataMsgListener;
	private IConnectionListener connectionListener;
	
	// constructors
	/**
	 * Default constructor
	 */
	public MqttClientConnector()
	{
		this(ConfigConst.MQTT_GATEWAY_SERVICE);
	}
	
	/**
	 * Constructor with config section
	 */
	public MqttClientConnector(String configSectionName)
	{
		super();
		initClientParameters(configSectionName);
	}
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			if (this.mqttClient == null) {
				this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}
			
			if (!this.mqttClient.isConnected()) {
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOpts);
				return true;
			} else {
				_Logger.warning("MQTT client already connected to broker: " + this.brokerAddr);
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to connect to broker: " + this.brokerAddr, e);
		}
		return false;
	}

	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
				this.mqttClient.disconnect();
				return true;
			} else {
				_Logger.warning("MQTT client not connected to broker: " + this.brokerAddr);
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to disconnect from broker: " + this.brokerAddr, e);
		}
		return false;
	}

	public boolean isConnected()
	{
		return (this.mqttClient != null && this.mqttClient.isConnected());
	}
	
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to publish message.");
			return false;
		}
		
		if (msg == null || msg.length() == 0) {
			_Logger.warning("Message is null or empty. Unable to publish message.");
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		String topic = topicName.getResourceName();
		
		try {
			MqttMessage mqttMsg = new MqttMessage(msg.getBytes());
			mqttMsg.setQos(qos);
			
			this.mqttClient.publish(topic, mqttMsg);
			
			_Logger.info("Published message to topic '" + topic + "': " + msg);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topic, e);
		}
		
		return false;
	}

	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to subscribe.");
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		String topic = topicName.getResourceName();
		
		try {
			this.mqttClient.subscribe(topic, qos);
			_Logger.info("Subscribed to topic: " + topic);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to subscribe to topic: " + topic, e);
		}
		
		return false;
	}

	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe.");
			return false;
		}
		
		String topic = topicName.getResourceName();
		
		try {
			this.mqttClient.unsubscribe(topic);
			_Logger.info("Unsubscribed from topic: " + topic);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic: " + topic, e);
		}
		
		return false;
	}

	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		this.connectionListener = listener;
		return true;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		this.dataMsgListener = listener;
		return true;
	}
	
	// callbacks
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " + serverURI);
		
		if (this.connectionListener != null) {
			this.connectionListener.onConnect();
		}
	}

	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.log(Level.WARNING, "Lost connection to MQTT broker: " + this.brokerAddr, t);
		
		if (this.connectionListener != null) {
			this.connectionListener.onDisconnect();
		}
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		_Logger.fine("Delivered MQTT message with ID: " + token.getMessageId());
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		_Logger.info("MQTT message arrived on topic: '" + topic + "'");
		
		if (this.dataMsgListener != null) {
			this.dataMsgListener.handleIncomingMessage(
				ResourceNameEnum.getEnumFromValue(topic),
				new String(msg.getPayload())
			);
		}
	}

	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters
	 */
	private void initClientParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.brokerAddr = configUtil.getProperty(
			configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		
		this.port = configUtil.getInteger(
			configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
		
		this.keepAlive = configUtil.getInteger(
			configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		
		// Build full broker address
		this.brokerAddr = "tcp://" + this.brokerAddr + ":" + this.port;
		
		// Generate client ID
		this.clientID = MqttClient.generateClientId();
		
		// Initialize persistence and connection options
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();
		this.connOpts.setCleanSession(true);
		this.connOpts.setKeepAliveInterval(this.keepAlive);
		
		// Check for secure connection
		this.enableEncryption = configUtil.getBoolean(
			configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		
		this.enableCredentials = configUtil.getBoolean(
			configSectionName, ConfigConst.ENABLE_AUTH_KEY);
		
		if (this.enableCredentials) {
			initCredentialConnectionParameters(configSectionName);
		}
		
		if (this.enableEncryption) {
			initSecureConnectionParameters(configSectionName);
		}
		
		_Logger.info("MQTT Client ID: " + this.clientID);
		_Logger.info("MQTT Broker: " + this.brokerAddr);
		_Logger.info("MQTT Keep Alive: " + this.keepAlive);
	}
	
	/**
	 * Initialize credential parameters
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		// TODO: implement authentication if needed
		_Logger.info("Credentials enabled but not yet implemented");
	}
	
	/**
	 * Initialize secure connection parameters
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		// TODO: implement TLS/SSL if needed
		_Logger.info("Encryption enabled but not yet implemented");
	}
}
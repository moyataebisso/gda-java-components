package programmingtheiot.gda.connection.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

public class GenericCoapResourceHandler extends CoapResource
{
    private static final Logger _Logger =
        Logger.getLogger(GenericCoapResourceHandler.class.getName());

    private IDataMessageListener dataMsgListener = null;

    public GenericCoapResourceHandler(ResourceNameEnum resource)
    {
        this(resource.getResourceName());
    }

    public GenericCoapResourceHandler(String resourceName)
    {
        super(resourceName);
        
        // Set the resource to be observable
        setObservable(true);
        getAttributes().setObservable();
    }

    @Override
    public void handleDELETE(CoapExchange context)
    {
        _Logger.info("Received DELETE request: " + super.getName());
        context.respond(ResponseCode.METHOD_NOT_ALLOWED);
    }

    @Override
    public void handleGET(CoapExchange context)
    {
        _Logger.info("Received GET request: " + super.getName());
        context.respond(ResponseCode.METHOD_NOT_ALLOWED);
    }

    @Override
    public void handlePOST(CoapExchange context)
    {
        _Logger.info("Received POST request: " + super.getName());
        
        try {
            String jsonData = context.getRequestText();
            
            if (jsonData != null && this.dataMsgListener != null) {
                String resourceName = context.getRequestOptions().getUriPathString();
                
                _Logger.info("Handling POST for resource: " + resourceName);
                
                // Handle different resource types
                if (resourceName.contains("SensorDataMsg")) {
                    this.dataMsgListener.handleIncomingMessage(
                        ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, 
                        jsonData
                    );
                } else if (resourceName.contains("ActuatorResponseMsg")) {
                    this.dataMsgListener.handleIncomingMessage(
                        ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, 
                        jsonData
                    );
                } else if (resourceName.contains("SystemPerfMsg")) {
                    this.dataMsgListener.handleIncomingMessage(
                        ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, 
                        jsonData
                    );
                } else {
                    this.dataMsgListener.handleIncomingMessage(
                        ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE, 
                        jsonData
                    );
                }
                
                context.respond(ResponseCode.CREATED);
                
                // Notify observers if this is an observable resource
                if (isObservable()) {
                    changed();
                }
            } else {
                context.respond(ResponseCode.BAD_REQUEST);
            }
        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to process POST request", e);
            context.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void handlePUT(CoapExchange context)
    {
        _Logger.info("Received PUT request: " + super.getName());
        
        try {
            String jsonData = context.getRequestText();
            
            if (jsonData != null && this.dataMsgListener != null) {
                String resourceName = context.getRequestOptions().getUriPathString();
                
                _Logger.info("Handling PUT for resource: " + resourceName);
                
                // Handle actuation commands being sent to CDA
                if (resourceName.contains("ActuatorCmdMsg")) {
                    this.dataMsgListener.handleIncomingMessage(
                        ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, 
                        jsonData
                    );
                }
                
                context.respond(ResponseCode.CHANGED);
                
                // Notify observers
                if (isObservable()) {
                    changed();
                }
            } else {
                context.respond(ResponseCode.BAD_REQUEST);
            }
        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to process PUT request", e);
            context.respond(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void setDataMessageListener(IDataMessageListener listener)
    {
        this.dataMsgListener = listener;
    }
}
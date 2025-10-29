package programmingtheiot.gda.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.handlers.GenericCoapResourceHandler;

public class CoapServerGateway
{
    private static final Logger _Logger =
        Logger.getLogger(CoapServerGateway.class.getName());

    private CoapServer coapServer = null;
    private IDataMessageListener dataMsgListener = null;
    private int port = ConfigConst.DEFAULT_COAP_PORT;

    public CoapServerGateway(IDataMessageListener dataMsgListener)
    {
        super();
        
        this.dataMsgListener = dataMsgListener;
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        this.port = configUtil.getInteger(
            ConfigConst.COAP_GATEWAY_SERVICE, 
            ConfigConst.PORT_KEY,
            ConfigConst.DEFAULT_COAP_PORT
        );
        
        initServer();
    }

    public void addResource(ResourceNameEnum resource)
    {
        if (resource != null && this.coapServer != null) {
            _Logger.info("Adding server resource handler: " + resource.getResourceName());
            
            Resource resourceHandler = createResourceChain(resource);
            
            if (resourceHandler != null) {
                this.coapServer.add(resourceHandler);
            }
        }
    }

    public boolean hasResource(String name)
    {
        if (name != null && this.coapServer != null) {
            return this.coapServer.getRoot().getChild(name) != null;
        }
        
        return false;
    }

    public void setDataMessageListener(IDataMessageListener listener)
    {
        this.dataMsgListener = listener;
    }

    public boolean startServer()
    {
        if (this.coapServer != null) {
            try {
                _Logger.info("Starting CoAP server on port: " + this.port);
                this.coapServer.start();
                return true;
            } catch (Exception e) {
                _Logger.log(Level.SEVERE, "Failed to start CoAP server", e);
            }
        }
        
        return false;
    }

    public boolean stopServer()
    {
        if (this.coapServer != null) {
            try {
                _Logger.info("Stopping CoAP server");
                this.coapServer.stop();
                return true;
            } catch (Exception e) {
                _Logger.log(Level.SEVERE, "Failed to stop CoAP server", e);
            }
        }
        
        return false;
    }

    private Resource createResourceChain(ResourceNameEnum resource)
    {
        if (resource != null) {
            String[] resourceNames = resource.getResourceName().split("/");
            Resource parentResource = null;
            Resource childResource = null;
            
            for (String resourceName : resourceNames) {
                if (resourceName != null && resourceName.length() > 0) {
                    childResource = new GenericCoapResourceHandler(resourceName);
                    
                    if (childResource != null) {
                        ((GenericCoapResourceHandler) childResource).setDataMessageListener(this.dataMsgListener);
                        
                        if (parentResource != null) {
                            parentResource.add(childResource);
                        } else {
                            parentResource = childResource;
                        }
                    }
                }
            }
            
            return parentResource;
        }
        
        return null;
    }

    private void initServer(ResourceNameEnum ...resources)
    {
        try {
            // Create a Configuration object for Californium 3.x
            // Use createWithoutFile() to avoid file-based configuration issues
            Configuration config = Configuration.createStandardWithoutFile();
            
            // Create the CoAP server with the configuration and port
            this.coapServer = new CoapServer(config, this.port);
            
            if (resources != null && resources.length > 0) {
                for (ResourceNameEnum resource : resources) {
                    addResource(resource);
                }
            }
            
            _Logger.info("CoAP server initialized successfully on port: " + this.port);
        } catch (Exception e) {
            _Logger.log(Level.SEVERE, "Failed to initialize CoAP server", e);
            this.coapServer = null;
        }
    }
}
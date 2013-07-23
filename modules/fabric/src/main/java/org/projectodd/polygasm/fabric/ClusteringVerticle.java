package org.projectodd.polygasm.fabric;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.projectodd.polygasm.fabric.jgroups.ChannelFactory;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Useful base-class for coordinating clustered verticles.  
 * 
 * Within the {@link #start} method of the sub-class, a form of {@link #initChannel} should
 * be called to initialize the underlying cluster-coordining JGroups channel.  A sub-classes
 * {@link #stop} should call {@code super} to ensure the channel is correctly disconnected
 * and stopped.
 * 
 * A sub-class my override {@link #startAsCoordinator()} and {@link #stopAsCoordinator()} if
 * it desires to change behaviour based on if it is the coordinator (or master) instance within
 * a cooperating cluster of instances.
 * 
 * Members of a clustered set may send JSON messages to all members using {@link #sendClusterMessage(JsonObject)}
 * and may respond to messages send by overriding {@link #receiveClusterMessage(JsonObject)}.
 * 
 * State, as JSON objects, may be transfered between cluster members using the {@link #getState()} and {@link #setState(JsonObject)}
 * methods.  By default, state is requested from the coordinating instance.
 * 
 * @author Bob McWhirter
 */
public class ClusteringVerticle extends Verticle {

    protected Channel channel;
    protected View view;

    /** 
     * Initialize and connect the underlying cluster channel.  
     * 
     * @throws Exception
     */
    protected void initChannel() throws Exception {
        initChannel(true);
    }

    /** 
     * Initialize and optionally connect the underlying cluster channel.  
     * 
     * @param connect Determine if the channel should be automatically connected upon creation.
     * @throws Exception
     */
    protected void initChannel(boolean connect) throws Exception {
        initChannel( getClusterName(), connect);
    }

    /**
     * Initialize and connect the underlying cluster channel.
     * 
     * @param cluster The cluster name.
     * @throws Exception
     */
    protected void initChannel(String cluster) throws Exception {
        initChannel(cluster, true);
    }

    /**
     * Initialize and optionally connect the underlying cluster channel.
     * 
     * @param cluster The cluster name.
     * @param connectDetermine if the channel should be automatically connected upon creation.
     * @throws Exception
     */
    protected void initChannel(String cluster, boolean connect) throws Exception {
        ChannelFactory factory = new ChannelFactory(this.container, this.vertx, cluster);
        this.channel = factory.newChannel();
        this.channel.setReceiver(new VerticleReceiver(vertx.currentContext()));
        if (connect) {
            channel.connect(cluster);
        }
    }
    
    /**
     * Retrieve the default cluster name (using by {@link #initChannel} if none
     * is passed through a parameter. 
     * 
     * By default, this uses the {@code cluster} key within the verticle's
     * configuration, if present, otherwise, the simple name of the verticle class.
     * 
     * @return The default cluster name.
     */
    protected String getClusterName() {
    	String name = container.config().getString("cluster");
    	if ( name == null ) {
    		name = getClass().getSimpleName();
    	}
    	return name;
    }

    @Override
    public void stop() {
        if (this.channel.isConnected()) {
            this.channel.disconnect();
        }
        if (this.channel.isOpen()) {
            this.channel.close();
            this.channel = null;
        }
    }

    /**
     * Retrieve the current state of the instance.
     * 
     * @return The state.
     */
    protected JsonObject getState() {
        return null;
    }

    /**
     * Set the current state of the instance provided by 
     * another instance.
     * 
     * @param state The state.
     */
    protected void setState(JsonObject state) {
    }

    void viewAccepted(View view) {
        boolean priorCoordinator = isCoordinator();
        this.view = view;
        boolean currentCoordinator = isCoordinator();

        if (!priorCoordinator && currentCoordinator) {
            startAsCoordinator();
        } else if (priorCoordinator && !currentCoordinator) {
            stopAsCoordinator();
        }
    }

    /**
     * Determine if this instance is currently the coordinator (master).
     * 
     * @return {@code true} if currently the coordinator, otherwise {@code false}
     */
    protected boolean isCoordinator() {
        if (this.view == null) {
            return false;
        }

        return this.view.getMembers().get(0).equals(this.channel.getAddress());
    }

    /** 
     * Called to inform the instance that it should start
     * performing the role of coordinator (master) within
     * a clustered set.
     */
    protected void startAsCoordinator() {

    }

    /**
     * Called to inform the instance that it should stop
     * performing the role of coordinator (master) within
     * a clustered set.
     */
    protected void stopAsCoordinator() {

    }

    /**
     * Send a message to all members of the clustered set
     * (including self).
     * 
     * @param message The message to send.
     * @throws Exception
     */
    protected void sendClusterMessage(JsonObject message) throws Exception {
        this.channel.send(null, message.toString());
    }

    /**
     * Called to deliver a message to the instance from another
     * member in the cluster (or self).
     * 
     * @param message The message received.
     */
    protected void receiveClusterMessage(JsonObject message) {

    }
    
    protected void setupInboundBridge(String address) {
        this.vertx.eventBus().registerHandler(address, new Handler<org.vertx.java.core.eventbus.Message<JsonObject>>() {
            @Override
            public void handle(org.vertx.java.core.eventbus.Message<JsonObject> event) {
                try {
                    sendClusterMessage(event.body());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Bridge class between JGroups channel and methods on the {@link ClusteringVerticle}.
     * 
     * @author Bob McWhirter
     */
    class VerticleReceiver implements Receiver {

        private Context context;

        VerticleReceiver(Context context) {
            this.context = context;
        }

        @Override
        public void receive(Message msg) {
            final String json = (String) msg.getObject();
            this.context.runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    ClusteringVerticle.this.receiveClusterMessage(new JsonObject(json));
                }
            });
        }

        @Override
        public void getState(OutputStream output) throws Exception {
            JsonObject state = ClusteringVerticle.this.getState();
            if (state != null) {
                OutputStreamWriter writer = new OutputStreamWriter(output);
                writer.write(state.toString());
                writer.flush();
            }
        }

        @Override
        public void setState(InputStream input) throws Exception {
            InputStreamReader reader = new InputStreamReader(input);

            int numRead = -0;
            char[] buf = new char[1024];

            StringBuilder builder = new StringBuilder();

            while ((numRead = reader.read(buf)) >= 0) {
                builder.append(buf, 0, numRead);
            }

            ClusteringVerticle.this.setState(new JsonObject(builder.toString()));
        }

        @Override
        public void viewAccepted(View view) {
            ClusteringVerticle.this.viewAccepted(view);
        }

        @Override
        public void suspect(Address suspected_mbr) {

        }

        @Override
        public void block() {

        }

        @Override
        public void unblock() {

        }
    }

}

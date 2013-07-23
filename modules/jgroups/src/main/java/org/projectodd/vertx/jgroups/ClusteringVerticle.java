package org.projectodd.vertx.jgroups;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Header;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class ClusteringVerticle extends Verticle {

    protected Channel channel;
    protected View view;

    protected void initChannel() throws Exception {
        initChannel(true);
    }

    protected void initChannel(boolean connect) throws Exception {
        initChannel(container.config().getString("cluster"), connect);
    }

    protected void initChannel(String cluster) throws Exception {
        initChannel(cluster, true);
    }

    protected void initChannel(String cluster, boolean connect) throws Exception {
        ChannelFactory factory = new ChannelFactory(this.container, this.vertx, cluster);
        this.channel = factory.newChannel();
        this.channel.setReceiver(new VerticleReceiver(vertx.currentContext()));
        if (connect) {
            channel.connect(cluster);
        }
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

    protected JsonObject getState() {
        return null;
    }

    protected void setState(JsonObject state) {
    }

    protected void viewAccepted(View view) {
        boolean priorCoordinator = isCoordinator();
        this.view = view;
        boolean currentCoordinator = isCoordinator();

        if (!priorCoordinator && currentCoordinator) {
            startAsCoordinator();
        } else if (priorCoordinator && !currentCoordinator) {
            stopAsCoordinator();
        }
    }

    protected boolean isCoordinator() {
        if (this.view == null) {
            return false;
        }

        return this.view.getMembers().get(0).equals(this.channel.getAddress());
    }

    protected void startAsCoordinator() {

    }

    protected void stopAsCoordinator() {

    }

    protected void sendClusterMessage(JsonObject message) throws Exception {
        this.channel.send(null, message.toString());
    }

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

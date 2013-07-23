package org.projectodd.polygasm.fabric.integration;

import org.jgroups.Address;
import org.projectodd.polygasm.fabric.ClusteringVerticle;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class JGroupsTestVerticle extends ClusteringVerticle {

    private JsonObject state;

    @Override
    public void start(Future<Void> startedResult) {
        
        final String cluster = container.config().getString("cluster");

        try {
            initChannel();
            this.state = new JsonObject().putString("my_address", this.channel.getAddress().toString() );
            this.channel.getState(null, 5000);
            
            this.vertx.eventBus().registerHandler("test." + cluster, new Handler<org.vertx.java.core.eventbus.Message<Boolean>>() {
                @Override
                public void handle(org.vertx.java.core.eventbus.Message<Boolean> event) {
                    JsonArray response = new JsonArray();
                    for (Address each : view.getMembers()) {
                        response.add(each.toString());
                    }
                    vertx.eventBus().send("test." + cluster + ".responses", response);
                    System.err.println( channel.getAddress() + "am I coordiator? " + isCoordinator() );
                }
            });
            
            setupInboundBridge("test." + cluster + ".bridge" );

            startedResult.setResult(null);
        } catch (Exception e) {
            e.printStackTrace();
            startedResult.setFailure(e);
        }
    }
    
    public JsonObject getState() {
        return this.state;
    }
    
    public void setState(JsonObject state) {
        System.err.println( "set state: " + state );
        this.state = state;
    }

    @Override
    public void receiveClusterMessage(JsonObject message) {
        System.err.println(this.channel.getAddress() + " received: " + message );
    }
    
    protected void startAsCoordinator() {
        System.err.println( "I AM COORDINATOR NOW!" );
    }
    
    protected void stopAsCoordinator() {
        System.err.println( "I AM NOT COORDINATOR NOW!" );
    }



}

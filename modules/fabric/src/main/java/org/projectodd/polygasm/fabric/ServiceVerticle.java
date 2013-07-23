package org.projectodd.polygasm.fabric;

import java.util.Set;

import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class ServiceVerticle extends ClusteringVerticle {

    private JsonObject state;

    public ServiceVerticle() {
        this.state = new JsonObject();
    }

    @Override
    public void start(Future<Void> startedResult) {
        try {
            initChannel();
            channel.getState(null, 5000);
            setupProvisioningHandlers();
        } catch (Exception e) {
            startedResult.setFailure(e);
            return;
        }
        startedResult.setResult(null);
    }

    @Override
    public void stop() {
        super.stop();
    }

    protected String getAddress() {
        String address = container.config().getString("address");
        if (address == null) {
            address = getClass().getSimpleName();
        }
        return address;
    }

    protected void setupProvisioningHandlers() {
        vertx.eventBus().registerHandler(getAddress() + ".provision",
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        try {
                            sendClusterMessage(new JsonObject()
                                    .putString("type", "provision")
                                    .putString("id", event.body().getString("id"))
                                    .putObject("config", event.body().getObject("config")));
                            System.err.println( "REPLY" );
                            event.reply(new JsonObject().putString("status", "OK"));
                        } catch (Exception e) {
                            event.reply(new JsonObject()
                                    .putString("status", "Error")
                                    .putString("message", e.getLocalizedMessage()));
                        }
                    }
                });

        vertx.eventBus().registerHandler(getAddress() + ".unprovision",
                new Handler<Message<String>>() {
                    @Override
                    public void handle(Message<String> event) {
                        System.err.println("got an unprovision");
                        try {
                            sendClusterMessage(new JsonObject()
                                    .putString("type", "unprovision")
                                    .putString("id", event.body()));
                            event.reply(new JsonObject().putString("status", "OK"));
                        } catch (Exception e) {
                            event.reply(new JsonObject()
                                    .putString("status", "Error")
                                    .putString("message", e.getLocalizedMessage()));
                        }
                    }
                });

        vertx.eventBus().registerHandler(getAddress() + ".state",
                new Handler<Message<String>>() {
                    @Override
                    public void handle(Message<String> event) {
                        System.err.println("sending state back to " + event.body());
                        vertx.eventBus().send(event.body(), state);
                    }
                });
    }

    @Override
    protected void receiveClusterMessage(JsonObject message) {
        if (message.getString("type").equals("provision")) {
            System.err.println("cluster provision");
            provisionInternal(message.getString("id"), message.getObject("config"));
        } else {
            System.err.println("cluster unprovision");
            unprovisionInternal(message.getString("id"));
        }
    }

    protected void provision(String deploymentId, JsonObject config) {
    }

    void provisionInternal(String deploymentId, JsonObject config) {
        provision(deploymentId, config);
        this.state.putObject(deploymentId, config);
    }

    protected void unprovision(String deploymentId) {
    }

    void unprovisionInternal(String deploymentId) {
        unprovision(deploymentId);
        this.state.removeField(deploymentId);
    }

    @Override
    protected JsonObject getState() {
        return this.state;
    }

    @Override
    protected void setState(JsonObject state) {
        Set<String> deploymentIds = state.toMap().keySet();

        for (String deploymentId : deploymentIds) {
            JsonObject config = state.getObject(deploymentId);
            provisionInternal(deploymentId, config);
        }
    }

}

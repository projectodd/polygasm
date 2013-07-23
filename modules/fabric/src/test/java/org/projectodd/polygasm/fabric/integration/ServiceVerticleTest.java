package org.projectodd.polygasm.fabric.integration;

import org.junit.Test;
import org.projectodd.polygasm.fabric.ServiceVerticle;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

public class ServiceVerticleTest extends TestVerticle {

    private int responsesSeen = 0;

    @Override
    public void start() {

        initialize();
        container.deployVerticle(ServiceVerticle.class.getName(), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                VertxAssert.assertTrue(asyncResult.succeeded());
                if (!asyncResult.succeeded()) {
                    asyncResult.cause().printStackTrace();
                }
                VertxAssert.assertNotNull("deploymentID should not be null", asyncResult.result());
                startTests();
            }
        });
    }

    @Test
    public void testProvisioning() throws Exception {
        vertx.eventBus().send("ServiceVerticle.provision", new JsonObject()
                .putString("id", "deployment-1")
                .putObject("config", new JsonObject().putString("cheese", "cheddar")));

        Thread.sleep(1000);

        container.deployVerticle(ServiceVerticle.class.getName(), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                VertxAssert.assertTrue(asyncResult.succeeded());
                if (!asyncResult.succeeded()) {
                    asyncResult.cause().printStackTrace();
                }
                VertxAssert.assertNotNull("deploymentID should not be null", asyncResult.result());

                vertx.eventBus().registerHandler("ServiceVerticleTest.responses", new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        System.err.println("state received: " + event.body());

                        ++responsesSeen;
                        if (responsesSeen >= 2) {
                            responsesSeen = 0;
                            vertx.eventBus().unregisterHandler("ServiceVerticleTest.responses", this);
                            System.err.println("unprovisioning");
                            vertx.eventBus().send("ServiceVerticle.unprovision", "deployment-1");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            vertx.eventBus().registerHandler("ServiceVerticleTest.responses", new Handler<Message<JsonObject>>() {

                                @Override
                                public void handle(Message<JsonObject> event) {
                                    System.err.println("another state: " + event.body());
                                    ++responsesSeen;
                                    if (responsesSeen >= 2) {
                                        VertxAssert.testComplete();
                                    }
                                }
                            });
                            System.err.println("asking for state again");
                            vertx.eventBus().publish("ServiceVerticle.state", "ServiceVerticleTest.responses");

                        }
                    }

                });

                System.err.println("asking for state");
                vertx.eventBus().publish("ServiceVerticle.state", "ServiceVerticleTest.responses");
            }

        });

    }
}

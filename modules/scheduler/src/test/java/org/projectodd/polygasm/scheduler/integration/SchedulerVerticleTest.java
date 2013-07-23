package org.projectodd.polygasm.scheduler.integration;

import org.junit.Test;
import org.projectodd.polygasm.scheduler.SchedulerVerticle;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

public class SchedulerVerticleTest extends TestVerticle {

    private int responsesSeen = 0;

    private String primaryScheduler;
    private String secondaryScheduler;

    @Override
    public void start() {

        initialize();
        container.deployVerticle(SchedulerVerticle.class.getName(), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                VertxAssert.assertTrue(asyncResult.succeeded());
                if (!asyncResult.succeeded()) {
                    asyncResult.cause().printStackTrace();
                }
                VertxAssert.assertNotNull("deploymentID should not be null", asyncResult.result());
                primaryScheduler = asyncResult.result();
                startTests();
            }
        });
    }

    @Test
    public void testProvisioning() throws Exception {
        vertx.eventBus().registerHandler("test.job1", new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                System.err.println("first message: " + event.body());
                vertx.eventBus().unregisterHandler("test.job1", this);

                container.deployVerticle(SchedulerVerticle.class.getName(), new AsyncResultHandler<String>() {
                    @Override
                    public void handle(AsyncResult<String> event) {
                        container.undeployVerticle(primaryScheduler, new Handler<AsyncResult<Void>>() {
                            @Override
                            public void handle(AsyncResult<Void> event) {
                                vertx.eventBus().registerHandler("test.job1", new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> event) {
                                        System.err.println("second message: " + event.body());
                                        VertxAssert.testComplete();
                                    }
                                });
                            }
                        });
                    }
                });

            }
        });

        vertx.eventBus().send("SchedulerVerticle.provision", new JsonObject()
                .putString("id", "deployment-1")
                .putObject("config", new JsonObject()
                        .putString("cron", "*/2 * * * * ?")
                        .putString("address", "test.job1")
                        .putObject("payload", new JsonObject()
                                .putString("cheese", "cheddar"))));

    }
}

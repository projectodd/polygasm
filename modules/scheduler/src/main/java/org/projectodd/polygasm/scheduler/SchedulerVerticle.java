package org.projectodd.polygasm.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Properties;
import java.util.UUID;

import org.projectodd.polygasm.fabric.ServiceVerticle;
import org.projectodd.polygasm.scheduler.quartz.VertxJob;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;

public class SchedulerVerticle extends ServiceVerticle {

    private Scheduler scheduler;

    @Override
    public void start(Future<Void> startedResult) {

        try {
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/quartz/quartz.properties" );
            System.err.println( "starting scehduler" );
            Properties props = new Properties();
            props.load(in );
            props.put( "org.quartz.scheduler.instanceName", "scheduler-" + UUID.randomUUID().toString() );
            System.err.println( "props: " + props );
            StdSchedulerFactory factory = new StdSchedulerFactory(props);
            this.scheduler = factory.getScheduler();
            System.err.println( "SCEHD: " + this.scheduler );
            this.scheduler.standby();
            System.err.println( "sstarterd: " + this.scheduler );
        } catch (Throwable e) {
            e.printStackTrace();
            startedResult.setFailure(e);
            return;
        }

        super.start(startedResult);
    }

    @Override
    public void stop() {
        super.stop();
        try {
            System.err.println( "stopping scheduler: " + this.scheduler );
            this.scheduler.shutdown();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startAsCoordinator() {
        try {
            this.scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void stopAsCoordinator() {
        try {
            this.scheduler.standby();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void provision(String deploymentId, JsonObject config) {

        String triggerAddress = config.getString("address");

        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setJobClass(VertxJob.class);
        JobKey jobKey = new JobKey(deploymentId);
        jobDetail.setKey(jobKey);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("eventBus", vertx.eventBus());
        jobDataMap.put("address", triggerAddress);
        jobDataMap.put("payload", config.getObject("payload", new JsonObject()));
        jobDetail.setJobDataMap(jobDataMap);

        System.err.println("scheduling: " + deploymentId + " to " + triggerAddress);

        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setName(deploymentId);
        try {
            trigger.setCronExpression(config.getString("cron"));
            this.scheduler.scheduleJob(jobDetail, trigger);
        } catch (ParseException | SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void unprovision(String deploymentId) {
        try {
            this.scheduler.deleteJob(new JobKey(deploymentId));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

}

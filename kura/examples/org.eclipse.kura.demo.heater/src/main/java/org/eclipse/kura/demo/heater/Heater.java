/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.demo.heater;

import static java.util.Objects.nonNull;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Heater implements ConfigurableComponent, CloudClientListener {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class CloudPublisherServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudService, CloudService> {

        @Override
        public CloudService addingService(final ServiceReference<CloudService> reference) {
            Heater.this.cloudService = Heater.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error("Cloud Client setup failed!", e);
            }
            return Heater.this.cloudService;
        }

        @Override
        public void modifiedService(final ServiceReference<CloudService> reference, final CloudService service) {
            Heater.this.cloudService = Heater.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error("Cloud Client setup failed!", e);
            }
        }

        @Override
        public void removedService(final ServiceReference<CloudService> reference, final CloudService service) {
            Heater.this.cloudService = null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Heater.class);

    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;
    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;
    private CloudService cloudService;
    private CloudClient cloudClient;

    private final ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private float temperature;
    private final Random random;

    private BundleContext bundleContext;

    private HeaterOptions heaterOptions;

    public Heater() {
        super();
        this.random = new Random();
        this.worker = Executors.newSingleThreadScheduledExecutor();
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating Heater...");

        this.heaterOptions = new HeaterOptions(properties);
        for (Entry<String, Object> entry : properties.entrySet()) {
            logger.info("Activate - {} : {}", entry.getKey(), entry.getValue());
        }

        this.bundleContext = componentContext.getBundleContext();

        this.cloudServiceTrackerCustomizer = new CloudPublisherServiceTrackerCustomizer();
        initCloudServiceTracking();

        // Don't subscribe because these are handled by the default
        // subscriptions and we don't want to get messages twice
        doUpdate(false);
        logger.info("Activating Heater... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating Heater...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        // Releasing the CloudApplicationClient
        logger.info("Releasing CloudApplicationClient for {}...", this.heaterOptions.getAppId());
        // close the client
        closeCloudClient();

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }

        logger.debug("Deactivating Heater... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated Heater...");

        // store the properties received
        this.heaterOptions = new HeaterOptions(properties);
        for (Entry<String, Object> entry : properties.entrySet()) {
            logger.info("Update - {} : {}", entry.getKey(), entry.getValue());
        }

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        // try to kick off a new job
        doUpdate(true);
        logger.info("Updated Heater... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Cloud Application Callback Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        logger.info("Message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
    }

    @Override
    public void onConnectionLost() {
        logger.warn("Connection lost!");
    }

    @Override
    public void onConnectionEstablished() {
        logger.info("Connection established");
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        logger.info("Confirmed message with ID: {} on application topic: {}", messageId, appTopic);
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        logger.info("Published message with ID: {} on application topic: {}", messageId, appTopic);
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(boolean onUpdate) {
        // cancel a current worker handle if one if active
        if (this.handle != null) {
            this.handle.cancel(true);
        }

        // reset the temperature to the initial value
        if (!onUpdate) {
            this.temperature = this.heaterOptions.getTempInitial();
        }

        // schedule a new worker based on the properties of the service
        int pubrate = this.heaterOptions.getPublishRate();
        this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getSimpleName());
                doPublish();
            }
        }, 0, pubrate, TimeUnit.SECONDS);
    }

    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() {
        // fetch the publishing configuration from the publishing properties
        String topic = this.heaterOptions.getPublishSemanticTopic();
        Integer qos = this.heaterOptions.getPublishQos();
        Boolean retain = this.heaterOptions.getPublishRetain();
        String mode = this.heaterOptions.getMode();

        // Increment the simulated temperature value
        float setPoint = 0;
        float tempIncr = this.heaterOptions.getTempIncrement();
        if (this.heaterOptions.getPropertiesProgramMode().equals(mode)) {
            setPoint = this.heaterOptions.getProgramSetPoint();
        } else if (this.heaterOptions.getPropertiesManualMode().equals(mode)) {
            setPoint = this.heaterOptions.getManualSetPoint();
        } else if (this.heaterOptions.getPropertiesVacationMode().equals(mode)) {
            setPoint = 6.0F;
        }
        if (this.temperature + tempIncr < setPoint) {
            this.temperature += tempIncr;
        } else {
            this.temperature -= 4 * tempIncr;
        }

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add the temperature as a metric to the payload
        payload.addMetric("temperatureInternal", this.temperature);
        payload.addMetric("temperatureExternal", 5.0F);
        payload.addMetric("temperatureExhaust", 30.0F);

        int code = this.random.nextInt();
        if (this.random.nextInt() % 5 == 0) {
            payload.addMetric("errorCode", code);
        } else {
            payload.addMetric("errorCode", 0);
        }

        // Publish the message
        try {
            if (nonNull(this.cloudService) && nonNull(this.cloudClient)) {
                this.cloudClient.publish(topic, payload, qos, retain);
                logger.info("Published to {} message: {}", topic, payload);
            }
        } catch (Exception e) {
            logger.error("Cannot publish topic: " + topic, e);
        }
    }

    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.heaterOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    private void closeCloudClient() {
        if (nonNull(this.cloudClient)) {
            this.cloudClient.removeCloudClientListener(this);
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.heaterOptions.getAppId();
        this.cloudClient = this.cloudService.newCloudClient(appId);
        this.cloudClient.addCloudClientListener(this);
    }
}

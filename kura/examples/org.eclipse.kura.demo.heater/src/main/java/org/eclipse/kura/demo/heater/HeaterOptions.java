/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.demo.heater;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

final class HeaterOptions {

    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DEFAULT_APP_ID = "heater";
    private static final int DEFAULT_PUBLISH_RATE = 1000;
    private static final String DEFAULT_SEMANTIC_TOPIC = "data";
    private static final int DEFAULT_PUBLISH_QOS = 0;
    private static final boolean DEFAULT_PUBLISH_RETAIN = false;

    private static final String DEFAULT_MODE = "Program";
    private static final String DEFAULT_PROGRAM_START_TIME = "06:00";
    private static final String DEFAULT_PROGRAM_STOP_TIME = "22:00";
    private static final float DEFAULT_PROGRAM_SET_POINT = 20.5f;
    private static final float DEFAULT_MANUAL_SET_POINT = 15.0f;
    private static final float DEFAULT_TEMPERATURE_INITIAL = 10;
    private static final float DEFAULT_TEMPERATURE_INCREMENT = 0.25f;

    // Metatype ids
    private static final String CLOUD_SERVICE_PROP_NAME = "cloud.service.pid";
    private static final String APP_ID_PROP_NAME = "app.id";
    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
    private static final String PUBLISH_SEMANTIC_TOPIC_PROP_NAME = "publish.semanticTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

    private static final String TEMP_INITIAL_PROP_NAME = "temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "temperature.increment";
    private static final String MODE_PROP_NAME = "mode";
    private static final String MODE_PROP_PROGRAM = "Program";
    private static final String MODE_PROP_MANUAL = "Manual";
    private static final String MODE_PROP_VACATION = "Vacation";
    private static final String PROGRAM_START_TIME = "program.startTime";
    private static final String PROGRAM_STOP_TIME = "program.stopTime";
    private static final String PROGRAM_SETPOINT_NAME = "program.setPoint";
    private static final String MANUAL_SETPOINT_NAME = "manual.setPoint";

    private final Map<String, Object> properties;

    HeaterOptions(final Map<String, Object> properties) {
        requireNonNull(properties);
        this.properties = properties;
    }

    String getCloudServicePid() {
        String cloudServicePid = DEFAULT_CLOUD_SERVICE_PID;
        Object configCloudServicePid = this.properties.get(CLOUD_SERVICE_PROP_NAME);
        if (nonNull(configCloudServicePid) && configCloudServicePid instanceof String) {
            cloudServicePid = (String) configCloudServicePid;
        }
        return cloudServicePid;
    }

    String getAppId() {
        String appId = DEFAULT_APP_ID;
        Object app = this.properties.get(APP_ID_PROP_NAME);
        if (nonNull(app) && app instanceof String) {
            appId = String.valueOf(app);
        }
        return appId;
    }

    int getPublishRate() {
        int publishRate = DEFAULT_PUBLISH_RATE;
        Object rate = this.properties.get(PUBLISH_RATE_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            publishRate = (Integer) rate;
        }
        return publishRate;
    }

    int getPublishQos() {
        int publishQos = DEFAULT_PUBLISH_QOS;
        Object qos = this.properties.get(PUBLISH_QOS_PROP_NAME);
        if (nonNull(qos) && qos instanceof Integer) {
            publishQos = (Integer) qos;
        }
        return publishQos;
    }

    boolean getPublishRetain() {
        boolean publishRetain = DEFAULT_PUBLISH_RETAIN;
        Object retain = this.properties.get(PUBLISH_RETAIN_PROP_NAME);
        if (nonNull(retain) && retain instanceof Boolean) {
            publishRetain = (Boolean) retain;
        }
        return publishRetain;
    }

    String getPublishSemanticTopic() {
        String appTopic = DEFAULT_SEMANTIC_TOPIC;
        Object app = this.properties.get(PUBLISH_SEMANTIC_TOPIC_PROP_NAME);
        if (nonNull(app) && app instanceof String) {
            appTopic = String.valueOf(app);
        }
        return appTopic;
    }

    float getTempInitial() {
        float tempInitial = DEFAULT_TEMPERATURE_INITIAL;
        Object temp = this.properties.get(TEMP_INITIAL_PROP_NAME);
        if (nonNull(temp) && temp instanceof Float) {
            tempInitial = (Float) temp;
        }
        return tempInitial;
    }

    float getTempIncrement() {
        float tempIncrement = DEFAULT_TEMPERATURE_INCREMENT;
        Object temp = this.properties.get(TEMP_INCREMENT_PROP_NAME);
        if (nonNull(temp) && temp instanceof Float) {
            tempIncrement = (Float) temp;
        }
        return tempIncrement;
    }

    String getMode() {
        String mode = DEFAULT_MODE;
        Object propertiesMode = this.properties.get(MODE_PROP_NAME);
        if (nonNull(propertiesMode) && propertiesMode instanceof String) {
            mode = String.valueOf(propertiesMode);
        }
        return mode;
    }

    String getProgramStartTime() {
        String startTime = DEFAULT_PROGRAM_START_TIME;
        Object propertiesStartTime = this.properties.get(PROGRAM_START_TIME);
        if (nonNull(propertiesStartTime) && propertiesStartTime instanceof String) {
            startTime = String.valueOf(propertiesStartTime);
        }
        return startTime;
    }

    String getProgramStopTime() {
        String stopTime = DEFAULT_PROGRAM_STOP_TIME;
        Object propertiesStopTime = this.properties.get(PROGRAM_STOP_TIME);
        if (nonNull(propertiesStopTime) && propertiesStopTime instanceof String) {
            stopTime = String.valueOf(propertiesStopTime);
        }
        return stopTime;
    }

    float getProgramSetPoint() {
        float programSetPoint = DEFAULT_PROGRAM_SET_POINT;
        Object propertiesSetPoint = this.properties.get(PROGRAM_SETPOINT_NAME);
        if (nonNull(propertiesSetPoint) && propertiesSetPoint instanceof Float) {
            programSetPoint = (Float) propertiesSetPoint;
        }
        return programSetPoint;
    }

    float getManualSetPoint() {
        float manualSetPoint = DEFAULT_MANUAL_SET_POINT;
        Object propertiesSetPoint = this.properties.get(MANUAL_SETPOINT_NAME);
        if (nonNull(propertiesSetPoint) && propertiesSetPoint instanceof Float) {
            manualSetPoint = (Float) propertiesSetPoint;
        }
        return manualSetPoint;
    }

    String getPropertiesProgramMode() {
        return MODE_PROP_PROGRAM;
    }

    String getPropertiesManualMode() {
        return MODE_PROP_MANUAL;
    }

    String getPropertiesVacationMode() {
        return MODE_PROP_VACATION;
    }
}
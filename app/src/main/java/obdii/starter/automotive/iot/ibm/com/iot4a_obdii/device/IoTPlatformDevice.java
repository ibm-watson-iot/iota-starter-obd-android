/**
 * Copyright 2016,2019 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.device;

import android.util.Log;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.DeviceClient;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.DeviceNotConnectedException;
import obdii.starter.automotive.iot.ibm.com.iot4a_obdii.NoAccessInfoException;

/*
 IoT Platform Device Client
 */

public class IoTPlatformDevice extends AbstractVehicleDevice {
    private DeviceClient deviceClient = null;

    IoTPlatformDevice(AccessInfo accessInfo){
        super(accessInfo);
    }

    @Override
    public void setAccessInfo(AccessInfo accessInfo) {
        String orgId = accessInfo.get(AccessInfo.ParamName.ENDPOINT);
        if (isCurrentOrganizationSameAs(orgId)) {
            return;
        }
        stopPublishing();
        disconnect();
        this.accessInfo = accessInfo;
    }

    @Override
    public void clean() {
        super.clean();
        disconnect();
    }

    public void connect() throws Exception {
        if(deviceClient == null){
            createDeviceClient();
        }
        if (deviceClient != null && !deviceClient.isConnected()) {
            try {
                deviceClient.connect();
            } catch (MqttException e) {
                throw new DeviceNotConnectedException(e);
            }
        }
    }

    public void disconnect() {
        if (deviceClient != null && deviceClient.isConnected()) {
            deviceClient.disconnect();
        }
        deviceClient = null;
    }

    public boolean hasValidAccessInfo(){
        if(this.accessInfo == null){
            return false;
        }
        String orgId = this.accessInfo.get(AccessInfo.ParamName.ENDPOINT);
        String typeId = this.accessInfo.get(AccessInfo.ParamName.VENDOR);
        String mo_id = this.accessInfo.get(AccessInfo.ParamName.MO_ID);
        String token = this.accessInfo.get(AccessInfo.ParamName.PASSWORD);
        return orgId != null && orgId != "" && typeId != null && typeId != "" && mo_id != null && mo_id != "" && token != null && token != "";
    }

    public synchronized boolean createDeviceClient() throws Exception {
        if (deviceClient != null) {
            return true;
        }
        if (!hasValidAccessInfo()) {
            throw new NoAccessInfoException("Access information to publish vehicle event is missing.");
        }
        disconnect();

        final Properties options = new Properties();
        options.setProperty("org", this.accessInfo.get(AccessInfo.ParamName.ENDPOINT));
        options.setProperty("type", this.accessInfo.get(AccessInfo.ParamName.VENDOR));
        options.setProperty("id", this.accessInfo.get(AccessInfo.ParamName.MO_ID));
        options.setProperty("auth-method", "token");
        options.setProperty("auth-token", this.accessInfo.get(AccessInfo.ParamName.PASSWORD));

        deviceClient = new DeviceClient(options);
        System.out.println("IOTP DEVICE CLIENT CREATED: " + options.toString());
        return true;
    }

    public synchronized boolean isConnected() {
        return deviceClient != null && deviceClient.isConnected();
    }

    @Override
    boolean publishEvent(final String probe, NotificationHandler notificationHandler) {
        if(!isConnected()){
            try {
                connect();
            }catch (Exception e){
                Log.e("publish event", e.getMessage());
                notificationHandler.notifyPostResult(false, new Notification(Notification.Type.Error, "Failed to connect to IoT Platform", null));
            }
        }

        if (deviceClient != null) {
            try {
                return deviceClient.publishEvent("carprobe", probe, "csv", 0);
            }catch(Exception e){
                Log.e("publish event", e.getMessage());
            }
        } else {
            return false;
        }
        return false;
    }

    public final boolean isCurrentOrganizationSameAs(final String newId) {
        return accessInfo != null && accessInfo.get(AccessInfo.ParamName.ENDPOINT).equals(newId);
    }
}

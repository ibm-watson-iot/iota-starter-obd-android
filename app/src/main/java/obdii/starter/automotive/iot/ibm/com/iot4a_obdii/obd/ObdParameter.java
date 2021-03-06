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

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii.obd;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import com.github.pires.obd.commands.ObdCommand;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Process OBD parameter
 */

abstract public class ObdParameter {

    final private TextView textView;
    final private Activity activity;
    final private String label;
    final private ObdCommand obdCommand;

    public ObdParameter(final TextView textView, final Activity activity, final String label, final ObdCommand obdCommand) {
        this.textView = textView;
        this.activity = activity;
        this.label = label;
        this.obdCommand = obdCommand;
    }

    public String getLabel() {
        return label;
    }

    public ObdCommand getObdCommand() {
        return obdCommand;
    }

    public synchronized void showScannedValue(final InputStream in, final OutputStream out, final boolean simulation) throws IOException, InterruptedException {
        if (simulation) {
            fetchValue(null, simulation);
            showText(getValueText());
        } else if (obdCommand == null) {
            // parameter without obd command
            fetchValue(obdCommand, simulation);
            showText(getValueText());
        } else {
            String value = "";
            if (in == null || out == null) {
                value = "No BT Connection";
            } else {
                try {
                    obdCommand.run(in, out);
                    fetchValue(obdCommand, simulation);
                    value = getValueText();
                    Log.d(label, value);
                } catch (com.github.pires.obd.exceptions.UnableToConnectException e) {
                    // reach here when OBD device is not connected to the vehicle
                    value = "Vehicle Not Connected";
                } catch (com.github.pires.obd.exceptions.NoDataException e) {
                    // reach here when this OBD parameter is not supported
                    value = "No OBD2 Data";
                    //e.printStackTrace();
                } catch (com.github.pires.obd.exceptions.MisunderstoodCommandException e) {
                    value = "Misunderstood";
                    System.err.println("OBD Library Error: " + e.getMessage());
                    //e.printStackTrace();
                } catch (IOException | InterruptedException e) {
                    showText("N/A");
                    throw e;
                }
            }
            showText(value);
        }
    }

    private void showText(final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(textView != null){
                    textView.setText(text);
                }
            }
        });
    }

    protected boolean isBaseProp() {
        return false; // return true for IoTCVI standard car probe properties
    }

    /*
    get actual OBD parameter value (obdCommand has already run at this call
     */
    abstract protected void fetchValue(ObdCommand obdCommand, boolean simulation);

    abstract protected void setJsonProp(JsonObject json);

    abstract protected String getValueText();
}

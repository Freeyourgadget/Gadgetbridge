/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package lineageos.weatherservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import lineageos.weather.RequestInfo;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;


public abstract class WeatherProviderService extends Service {

    private Handler mHandler;
    private IWeatherProviderServiceClient mClient;
    private Set<ServiceRequest> mWeakRequestsSet
            = Collections.newSetFromMap(new WeakHashMap<ServiceRequest, Boolean>());

    /**
     * The {@link android.content.Intent} action that must be declared as handled by a service in
     * its manifest for the system to recognize it as a weather provider service
     */
    public static final String SERVICE_INTERFACE
            = "lineageos.weatherservice.WeatherProviderService";

    /**
     * Name under which a {@link WeatherProviderService} publishes information about itself.
     * This meta-data must reference an XML resource containing
     * a <code>&lt;weather-provider-service&gt;</code>
     * tag.
     */
    public static final String SERVICE_META_DATA = "lineageos.weatherservice";

    @Override
    protected final void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mHandler = new ServiceHandler(base.getMainLooper());
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IWeatherProviderService.Stub mBinder = new IWeatherProviderService.Stub() {

        @Override
        public void processWeatherUpdateRequest(final RequestInfo info) {
            mHandler.obtainMessage(ServiceHandler.MSG_ON_NEW_REQUEST, info).sendToTarget();
        }

        @Override
        public void processCityNameLookupRequest(final RequestInfo info) {
            mHandler.obtainMessage(ServiceHandler.MSG_ON_NEW_REQUEST, info).sendToTarget();
        }

        @Override
        public void setServiceClient(IWeatherProviderServiceClient client) {
            mHandler.obtainMessage(ServiceHandler.MSG_SET_CLIENT, client).sendToTarget();
        }

        @Override
        public void cancelOngoingRequests() {
            synchronized (mWeakRequestsSet) {
                for (final ServiceRequest request : mWeakRequestsSet) {
                    request.cancel();
                    mWeakRequestsSet.remove(request);
                    mHandler.obtainMessage(ServiceHandler.MSG_CANCEL_REQUEST, request)
                            .sendToTarget();
                }
            }
        }

        @Override
        public void cancelRequest(int requestId) {
            synchronized (mWeakRequestsSet) {
                for (final ServiceRequest request : mWeakRequestsSet) {
                    if (request.getRequestInfo().hashCode() == requestId) {
                        mWeakRequestsSet.remove(request);
                        request.cancel();
                        mHandler.obtainMessage(ServiceHandler.MSG_CANCEL_REQUEST, request)
                                .sendToTarget();
                        break;
                    }
                }
            }
        }
    };

    private class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }
        public static final int MSG_SET_CLIENT = 1;
        public static final int MSG_ON_NEW_REQUEST = 2;
        public static final int MSG_CANCEL_REQUEST = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_CLIENT: {
                    mClient = (IWeatherProviderServiceClient) msg.obj;
                    if (mClient != null) {
                        onConnected();
                    } else {
                        onDisconnected();
                    }
                    return;
                }
                case MSG_ON_NEW_REQUEST: {
                    RequestInfo info = (RequestInfo) msg.obj;
                    if (info != null) {
                        ServiceRequest request = new ServiceRequest(info, mClient);
                        synchronized (mWeakRequestsSet) {
                            mWeakRequestsSet.add(request);
                        }
                        onRequestSubmitted(request);
                    }
                    return;
                }
                case MSG_CANCEL_REQUEST: {
                    ServiceRequest request = (ServiceRequest) msg.obj;
                    onRequestCancelled(request);
                    return;
                }
            }
        }
    }

    /**
     * The system has connected to this service.
     */
    protected void onConnected() {
        /* Do nothing */
    }

    /**
     * The system has disconnected from this service.
     */
    protected void onDisconnected() {
        /* Do nothing */
    }

    /**
     * A new request has been submitted to this service
     * @param request The service request to be processed by this service
     */
    protected abstract void onRequestSubmitted(ServiceRequest request);

    /**
     * Called when the system is not interested on this request anymore. Note that the service
     * <b>has marked the request as cancelled</b> and you must stop any ongoing operation
     * (such as pulling data from internet) that this service could've been performing to honor the
     * request.
     *
     * @param request The request cancelled by the system
     */
    protected abstract void onRequestCancelled(ServiceRequest request);
}
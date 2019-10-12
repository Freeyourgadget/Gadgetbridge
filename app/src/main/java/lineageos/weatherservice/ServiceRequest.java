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


import android.os.RemoteException;

import androidx.annotation.NonNull;

import lineageos.weatherservice.IWeatherProviderServiceClient;
import lineageos.weather.LineageWeatherManager;
import lineageos.weather.RequestInfo;

/**
 * This class represents a request submitted by the system to the active weather provider service
 */
public final class ServiceRequest {

    private final RequestInfo mInfo;
    private final IWeatherProviderServiceClient mClient;

    private enum Status {
        IN_PROGRESS, COMPLETED, CANCELLED, FAILED, REJECTED
    }
    private Status mStatus;

    /* package */ ServiceRequest(RequestInfo info, IWeatherProviderServiceClient client) {
        mInfo = info;
        mClient = client;
        mStatus = Status.IN_PROGRESS;
    }

    /**
     * Obtains the request information
     * @return {@link lineageos.weather.RequestInfo}
     */
    public RequestInfo getRequestInfo() {
        return mInfo;
    }

    /**
     * This method should be called once the request has been completed
     */
    public void complete(@NonNull ServiceRequestResult result) {
        synchronized (this) {
            if (mStatus.equals(Status.IN_PROGRESS)) {
                try {
                    final int requestType = mInfo.getRequestType();
                    switch (requestType) {
                        case RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ:
                        case RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ:
                            if (result.getWeatherInfo() == null) {
                                throw new IllegalStateException("The service request result doesn't"
                                        + " contain a valid WeatherInfo object");
                            }
                            mClient.setServiceRequestState(mInfo, result,
                                    LineageWeatherManager.RequestStatus.COMPLETED);
                            break;
                        case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                            if (result.getLocationLookupList() == null
                                    || result.getLocationLookupList().size() <= 0) {
                                //In case the user decided to mark this request as completed with
                                //null or empty list. It's not necessarily a failure
                                mClient.setServiceRequestState(mInfo, null,
                                        LineageWeatherManager.RequestStatus.NO_MATCH_FOUND);
                            } else {
                                mClient.setServiceRequestState(mInfo, result,
                                        LineageWeatherManager.RequestStatus.COMPLETED);
                            }
                            break;
                    }
                } catch (RemoteException e) {
                }
                mStatus = Status.COMPLETED;
            }
        }
    }

    /**
     * This method should be called if the service failed to process the request
     * (no internet connection, time out, etc.)
     */
    public void fail() {
        synchronized (this) {
            if (mStatus.equals(Status.IN_PROGRESS)) {
                try {
                    final int requestType = mInfo.getRequestType();
                    switch (requestType) {
                        case RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ:
                        case RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ:
                            mClient.setServiceRequestState(mInfo, null,
                                    LineageWeatherManager.RequestStatus.FAILED);
                            break;
                        case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                            mClient.setServiceRequestState(mInfo, null,
                                    LineageWeatherManager.RequestStatus.FAILED);
                            break;
                    }
                } catch (RemoteException e) {
                }
                mStatus = Status.FAILED;
            }
        }
    }

    /**
     * This method should be called if the service decides not to honor the request. Note this
     * method will accept only the following values.
     * <ul>
     * <li>{@link lineageos.weather.LineageWeatherManager.RequestStatus#SUBMITTED_TOO_SOON}</li>
     * <li>{@link lineageos.weather.LineageWeatherManager.RequestStatus#ALREADY_IN_PROGRESS}</li>
     * </ul>
     * Attempting to pass any other value will get you an IllegalArgumentException
     * @param status
     */
    public void reject(int status) {
        synchronized (this) {
            if (mStatus.equals(Status.IN_PROGRESS)) {
                switch (status) {
                    case LineageWeatherManager.RequestStatus.ALREADY_IN_PROGRESS:
                    case LineageWeatherManager.RequestStatus.SUBMITTED_TOO_SOON:
                        try {
                            mClient.setServiceRequestState(mInfo, null, status);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Can't reject with status " + status);
                }
                mStatus = Status.REJECTED;
            }
        }
    }

    /**
     * Called by the WeatherProviderService base class to notify we don't want this request anymore.
     * The service implementing the WeatherProviderService will be notified of this action
     * via onRequestCancelled()
     * @hide
     */
    public void cancel() {
        synchronized (this) {
            mStatus = Status.CANCELLED;
        }
    }
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tv.settings.connectivity.setup;

import android.arch.lifecycle.ViewModelProviders;
import android.net.IpConfiguration;
import android.support.annotation.IntDef;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.android.tv.settings.connectivity.NetworkConfiguration;
import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.connectivity.util.StateMachine;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Handles the flow of setting advanced options.
 */
public class AdvancedWifiOptionsFlow {

    /** Flag that set advanced flow start with default page **/
    public static final int START_DEFAULT_PAGE = 0;
    /** Flag that set advanced flow start with IP settings page **/
    public static final int START_IP_SETTINGS_PAGE = 1;
    /** Flag that set advanced flow start with proxy settings page **/
    public static final int START_PROXY_SETTINGS_PAGE = 2;
    private static final String TAG = "AdvancedWifiOptionsFlow";

    @IntDef({
            START_DEFAULT_PAGE,
            START_IP_SETTINGS_PAGE,
            START_PROXY_SETTINGS_PAGE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface START_PAGE {
    }

    /**
     * Create a advanced flow.
     *
     * @param activity             activity that starts the advanced flow.
     * @param askFirst             whether ask user to start advanced flow
     * @param isSettingsFlow       whether advanced flow is started from settings flow
     * @param initialConfiguration the previous {@link NetworkConfiguration} info.
     * @param entranceState        The state that starts the advanced flow, null if there is none.
     * @param exitState            The state where the advanced flow go after it ends.
     * @param startPage            The page where the advanced flow starts with.
     */
    public static void createFlow(FragmentActivity activity,
            boolean askFirst,
            boolean isSettingsFlow,
            NetworkConfiguration initialConfiguration,
            State entranceState,
            State exitState,
            @START_PAGE int startPage) {
        StateMachine mStateMachine = ViewModelProviders.of(activity).get(StateMachine.class);
        AdvancedOptionsFlowInfo mAdvancedOptionsFlowInfo = ViewModelProviders.of(activity).get(
                AdvancedOptionsFlowInfo.class);
        mAdvancedOptionsFlowInfo.setSettingsFlow(isSettingsFlow);
        IpConfiguration mIpConfiguration = (initialConfiguration != null)
                ? initialConfiguration.getIpConfiguration()
                : new IpConfiguration();
        mAdvancedOptionsFlowInfo.setIpConfiguration(mIpConfiguration);
        State mAdvancedOptionsState = new AdvancedOptionsState(activity);
        State mProxySettingsState = new ProxySettingsState(activity);
        State mIpSettingsState = new IpSettingsState(activity);
        State mProxyHostNameState = new ProxyHostNameState(activity);
        State mProxyPortState = new ProxyPortState(activity);
        State mProxyBypassState = new ProxyBypassState(activity);
        State mProxySettingsInvalidState = new ProxySettingsInvalidState(activity);
        State mIpAddressState = new IpAddressState(activity);
        State mGatewayState = new GatewayState(activity);
        State mNetworkPrefixLengthState = new NetworkPrefixLengthState(activity);
        State mDns1State = new Dns1State(activity);
        State mDns2State = new Dns2State(activity);
        State mIpSettingsInvalidState = new IpSettingsInvalidState(activity);
        State mAdvancedFlowCompleteState = new AdvancedFlowCompleteState(activity);

        // Define the transitions between external states and internal states for advanced options
        // flow.
        State startState = null;
        switch (startPage) {
            case START_DEFAULT_PAGE :
                if (askFirst) {
                    startState = mAdvancedOptionsState;
                } else {
                    startState = mProxySettingsState;
                }
                break;
            case START_IP_SETTINGS_PAGE :
                startState = mIpSettingsState;
                break;
            case START_PROXY_SETTINGS_PAGE :
                startState = mProxySettingsState;
                break;
            default:
                Log.wtf(TAG, "Got a wrong start state");
                break;
        }

        /** Entrance **/
        if (entranceState != null) {
            mStateMachine.addState(
                    entranceState,
                    StateMachine.ENTER_ADVANCED_FLOW,
                    startState
            );
        } else {
            mStateMachine.setStartState(startState);
        }

        /** Exit **/
        mStateMachine.addState(
                mAdvancedFlowCompleteState,
                StateMachine.EXIT_ADVANCED_FLOW,
                exitState
        );

        // Define the transitions between different states in advanced options flow.
        /** Advanced Options **/
        mStateMachine.addState(
                mAdvancedOptionsState,
                StateMachine.ADVANCED_FLOW_COMPLETE,
                mAdvancedFlowCompleteState
        );
        mStateMachine.addState(
                mAdvancedOptionsState,
                StateMachine.CONTINUE,
                mProxySettingsState
        );

        /** Proxy Settings **/
        mStateMachine.addState(
                mProxySettingsState,
                StateMachine.IP_SETTINGS,
                mIpSettingsState
        );
        mStateMachine.addState(
                mProxySettingsState,
                StateMachine.ADVANCED_FLOW_COMPLETE,
                mAdvancedFlowCompleteState
        );
        mStateMachine.addState(
                mProxySettingsState,
                StateMachine.PROXY_HOSTNAME,
                mProxyHostNameState
        );

        /** Proxy Hostname **/
        mStateMachine.addState(
                mProxyHostNameState,
                StateMachine.CONTINUE,
                mProxyPortState
        );

        /** Proxy Port **/
        mStateMachine.addState(
                mProxyPortState,
                StateMachine.CONTINUE,
                mProxyBypassState
        );

        /** Proxy Bypass **/
        mStateMachine.addState(
                mProxyBypassState,
                StateMachine.ADVANCED_FLOW_COMPLETE,
                mAdvancedFlowCompleteState
        );
        mStateMachine.addState(
                mProxyBypassState,
                StateMachine.IP_SETTINGS,
                mIpSettingsState
        );
        mStateMachine.addState(
                mProxyBypassState,
                StateMachine.PROXY_SETTINGS_INVALID,
                mProxySettingsInvalidState
        );

        /** Proxy Settings Invalid **/
        mStateMachine.addState(
                mProxySettingsInvalidState,
                StateMachine.CONTINUE,
                mProxySettingsState
        );

        /** Ip Settings **/
        mStateMachine.addState(
                mIpSettingsState,
                StateMachine.ADVANCED_FLOW_COMPLETE,
                mAdvancedFlowCompleteState
        );
        mStateMachine.addState(
                mIpSettingsState,
                StateMachine.CONTINUE,
                mIpAddressState
        );

        /**Ip Address **/
        mStateMachine.addState(
                mIpAddressState,
                StateMachine.CONTINUE,
                mGatewayState
        );

        /** Gateway **/
        mStateMachine.addState(
                mGatewayState,
                StateMachine.CONTINUE,
                mNetworkPrefixLengthState
        );

        /** Network Prefix Length **/
        mStateMachine.addState(
                mNetworkPrefixLengthState,
                StateMachine.CONTINUE,
                mDns1State
        );

        /** Dns1 **/
        mStateMachine.addState(
                mDns1State,
                StateMachine.CONTINUE,
                mDns2State
        );

        /** Dns2 **/
        mStateMachine.addState(
                mDns2State,
                StateMachine.ADVANCED_FLOW_COMPLETE,
                mAdvancedFlowCompleteState);
        mStateMachine.addState(
                mDns2State,
                StateMachine.IP_SETTINGS_INVALID,
                mIpSettingsInvalidState
        );

        /** Ip Settings Invalid **/
        mStateMachine.addState(
                mIpSettingsInvalidState,
                StateMachine.CONTINUE,
                mIpSettingsState
        );
    }
}

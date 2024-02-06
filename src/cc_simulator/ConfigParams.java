/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc_simulator;

import java.util.ArrayList;

/**
 * It holds the Configuration Variables.
 *
 *
 * @author Larenge Kamal
 * @since 1.1
 */
public class ConfigParams {

    /**
     * static variable of ConfigParams
     */
    private static ConfigParams configParams = null;

    private String SERVER_IP_ADDR;
    private int SERVER_PORT;
    private String TRANSPORT;
    private String ENDPOINTS;
    private String CALLS;

    private int NUM_CALLS_TO_GENERATE;

    private int REGISTER_EXPIRY;
    private int REGISTER_FAIL_DURATION_MILI_SEC;
    private long CALL_DISCONNECT_AFTER_MILI_SEC;
    private long CALL_ACCEPT_AFTER_MILI_SEC;
    private long CALL_INITIATE_AFTER_MILI_SEC;
    private int AUDIO_START_PORT;
        
    private int TEST_DURATION;
        
    public static ConfigParams getConfigParams() {
        if (configParams == null) {
            configParams = new ConfigParams();
        }
        return configParams;
    }

    public ConfigParams() {

    }

    public String getSERVER_IP_ADDR() {
        return SERVER_IP_ADDR;
    }

    public void setSERVER_IP_ADDR(String SERVER_IP_ADDR) {
        this.SERVER_IP_ADDR = SERVER_IP_ADDR;
    }

    public int getSERVER_PORT() {
        return SERVER_PORT;
    }

    public void setSERVER_PORT(int SERVER_PORT) {
        this.SERVER_PORT = SERVER_PORT;
    }
    
    public int getNUM_CALLS_TO_GENERATE() {
        return NUM_CALLS_TO_GENERATE;
    }

    public void setNUM_CALLS_TO_GENERATE(int NUM_CALLS_TO_GENERATE) {
        this.NUM_CALLS_TO_GENERATE = NUM_CALLS_TO_GENERATE;
    }

    public String getTRANSPORT() {
        return TRANSPORT;
    }

    public void setTRANSPORT(String TRANSPORT) {
        this.TRANSPORT = TRANSPORT;
    }

    public int getREGISTER_EXPIRY() {
        return REGISTER_EXPIRY;
    }

    public void setREGISTER_EXPIRY(int REGISTER_EXPIRY) {
        this.REGISTER_EXPIRY = REGISTER_EXPIRY;
    }

    public long getCALL_DISCONNECT_AFTER_MILI_SEC() {
        return CALL_DISCONNECT_AFTER_MILI_SEC;
    }

    public void setCALL_DISCONNECT_AFTER_MILI_SEC(long CALL_DISCONNECT_AFTER_MILI_SEC) {
        this.CALL_DISCONNECT_AFTER_MILI_SEC = CALL_DISCONNECT_AFTER_MILI_SEC;
    }

    public long getCALL_ACCEPT_AFTER_MILI_SEC() {
        return CALL_ACCEPT_AFTER_MILI_SEC;
    }

    public void setCALL_ACCEPT_AFTER_MILI_SEC(long CALL_ACCEPT_AFTER_MILI_SEC) {
        this.CALL_ACCEPT_AFTER_MILI_SEC = CALL_ACCEPT_AFTER_MILI_SEC;
    }

    public long getCALL_INITIATE_AFTER_MILI_SEC() {
        return CALL_INITIATE_AFTER_MILI_SEC;
    }

    public void setCALL_INITIATE_AFTER_MILI_SEC(long CALL_INITIATE_AFTER_MILI_SEC) {
        this.CALL_INITIATE_AFTER_MILI_SEC = CALL_INITIATE_AFTER_MILI_SEC;
    }

    public int getAUDIO_START_PORT() {
        return AUDIO_START_PORT;
    }

    public void setAUDIO_START_PORT(int AUDIO_START_PORT) {
        this.AUDIO_START_PORT = AUDIO_START_PORT;
    }    

    public int getREGISTER_FAIL_DURATION_MILI_SEC() {
        return REGISTER_FAIL_DURATION_MILI_SEC;
    }

    public void setREGISTER_FAIL_DURATION_MILI_SEC(int REGISTER_FAIL_DURATION_MILI_SEC) {
        this.REGISTER_FAIL_DURATION_MILI_SEC = REGISTER_FAIL_DURATION_MILI_SEC;
    }

    public int getTEST_DURATION() {
        return TEST_DURATION;
    }

    public void setTEST_DURATION(int TEST_DURATION) {
        this.TEST_DURATION = TEST_DURATION;
    }    

    public String getENDPOINTS() {
        return ENDPOINTS;
    }

    public void setENDPOINTS(String ENDPOINTS) {
        this.ENDPOINTS = ENDPOINTS;
    }

    public String getCALLS() {
        return CALLS;
    }

    public void setCALLS(String CALLS) {
        this.CALLS = CALLS;
    }

}

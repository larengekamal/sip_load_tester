/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc_simulator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * It holds the Configuration Module Load Function.
 *
 *
 * @author Larenge Kamal
 * @since 1.1
 */
public class ConfigModule {

    private static Logger logger = LogManager.getLogger(ConfigModule.class);
    private final static ConfigParams configParams = ConfigParams.getConfigParams();

    /**
     * Function to load the fxo configuration file.
     *
     */
    public ConfigModule() {
        logger.info("Inside Constructor ConfigModule");
    }

    /**
     * Function to load the fxo configuration file.
     *
     * @param fileName The file name of FXO file.
     */
    public void loadConfigFile(String fileName) {
        int i;
        boolean ifLoaded = false;
        boolean isErrorInConfigParams = false;
        Properties p = new Properties();
        File f = new File(fileName);
        try {
            p.load(new FileInputStream(f));
            logger.info("# Loaded config file " + fileName);
            ifLoaded = true;
        } catch (Exception e) {
            logger.error("# Failed Loading config file " + fileName + " Error:" + e.getMessage(), e);
            isErrorInConfigParams = true;
        }
        if (ifLoaded) {
            long start_num;
            long start_num1;
            long diff;
            long end_num;
            long loop_var;

            // #################################################################      
            try {
                configParams.setSERVER_IP_ADDR((p.getProperty("SERVER_IP_ADDR").trim()));
                logger.info("SERVER_IP_ADDR: " + configParams.getSERVER_IP_ADDR() + "#");
            } catch (Exception e) {
                configParams.setSERVER_IP_ADDR(null);
                logger.error("Required Config Parameter SERVER_IP_ADDR not defined");
                isErrorInConfigParams = true;
            }

            try {
                configParams.setSERVER_PORT(Integer.parseInt(p.getProperty("SERVER_PORT").trim()));
                logger.info("SERVER_PORT: " + configParams.getSERVER_PORT() + "#");
            } catch (Exception e) {
                configParams.setSERVER_PORT(5060);
                logger.error("Default SERVER_PORT: " + configParams.getSERVER_PORT() + "#", e);
            }

            try {
                configParams.setTRANSPORT((p.getProperty("TRANSPORT").trim()));
                logger.info("TRANSPORT: " + configParams.getTRANSPORT() + "#");
            } catch (Exception e) {
                configParams.setTRANSPORT("UDP");
            }

            try {
                configParams.setENDPOINTS((p.getProperty("ENDPOINTS").trim()));
                logger.info("ENDPOINTS: " + configParams.getENDPOINTS() + "#");
            } catch (Exception e) {
                configParams.setENDPOINTS(null);
            }           
            
            try {
                configParams.setCALLS((p.getProperty("CALLS").trim()));
                logger.info("CALLS: " + configParams.getCALLS() + "#");
            } catch (Exception e) {
                configParams.setCALLS(null);
            }            

            try {
                configParams.setNUM_CALLS_TO_GENERATE(Integer.parseInt(p.getProperty("NUM_CALLS_TO_GENERATE").trim()));
                logger.info("NUM_CALLS_TO_GENERATE: " + configParams.getNUM_CALLS_TO_GENERATE() + "#");
            } catch (Exception e) {
                configParams.setNUM_CALLS_TO_GENERATE(5);
                logger.error("Default NUM_CALLS_TO_GENERATE: " + configParams.getNUM_CALLS_TO_GENERATE() + "#", e);
            }

            try {
                configParams.setREGISTER_EXPIRY(Integer.parseInt(p.getProperty("REGISTER_EXPIRY").trim()));
                logger.info("REGISTER_EXPIRY: " + configParams.getREGISTER_EXPIRY() + "#");
            } catch (Exception e) {
                configParams.setREGISTER_EXPIRY(300);
                logger.error("Default REGISTER_EXPIRY: " + configParams.getREGISTER_EXPIRY() + "#", e);
            }

            try {
                configParams.setREGISTER_FAIL_DURATION_MILI_SEC(Integer.parseInt(p.getProperty("REGISTER_FAIL_DURATION_MILI_SEC").trim()));
                logger.info("REGISTER_FAIL_DURATION_MILI_SEC: " + configParams.getREGISTER_FAIL_DURATION_MILI_SEC() + "#");
            } catch (Exception e) {
                configParams.setREGISTER_FAIL_DURATION_MILI_SEC(1000);
                logger.error("Default REGISTER_FAIL_DURATION_MILI_SEC: " + configParams.getREGISTER_FAIL_DURATION_MILI_SEC() + "#", e);
            }

            try {
                configParams.setCALL_DISCONNECT_AFTER_MILI_SEC(Integer.parseInt(p.getProperty("CALL_DISCONNECT_AFTER_MILI_SEC").trim()));
                logger.info("CALL_DISCONNECT_AFTER_MILI_SEC: " + configParams.getCALL_DISCONNECT_AFTER_MILI_SEC() + "#");
            } catch (Exception e) {
                configParams.setCALL_DISCONNECT_AFTER_MILI_SEC(1000);
                logger.error("Default CALL_DISCONNECT_AFTER_MILI_SEC: " + configParams.getCALL_DISCONNECT_AFTER_MILI_SEC() + "#", e);
            }

            try {
                configParams.setCALL_ACCEPT_AFTER_MILI_SEC(Integer.parseInt(p.getProperty("CALL_ACCEPT_AFTER_MILI_SEC").trim()));
                logger.info("CALL_ACCEPT_AFTER_MILI_SEC: " + configParams.getCALL_ACCEPT_AFTER_MILI_SEC() + "#");
            } catch (Exception e) {
                configParams.setCALL_ACCEPT_AFTER_MILI_SEC(1000);
                logger.error("Default CALL_ACCEPT_AFTER_MILI_SEC: " + configParams.getCALL_ACCEPT_AFTER_MILI_SEC() + "#", e);
            }

            try {
                configParams.setCALL_INITIATE_AFTER_MILI_SEC(Integer.parseInt(p.getProperty("CALL_INITIATE_AFTER_MILI_SEC").trim()));
                logger.info("CALL_INITIATE_AFTER_MILI_SEC: " + configParams.getCALL_INITIATE_AFTER_MILI_SEC() + "#");
            } catch (Exception e) {
                configParams.setCALL_INITIATE_AFTER_MILI_SEC(1000);
                logger.error("Default CALL_INITIATE_AFTER_MILI_SEC: " + configParams.getCALL_INITIATE_AFTER_MILI_SEC() + "#", e);
            }

            try {
                configParams.setAUDIO_START_PORT(Integer.parseInt(p.getProperty("AUDIO_START_PORT").trim()));
                logger.info("AUDIO_START_PORT: " + configParams.getAUDIO_START_PORT() + "#");
            } catch (Exception e) {
                configParams.setAUDIO_START_PORT(6000);
                logger.error("Default AUDIO_START_PORT: " + configParams.getAUDIO_START_PORT() + "#", e);
            }

            try {
                configParams.setTEST_DURATION(Integer.parseInt(p.getProperty("TEST_DURATION").trim()));
                logger.info("TEST_DURATION: " + configParams.getTEST_DURATION() + "#");
            } catch (Exception e) {
                configParams.setTEST_DURATION(20);
                logger.error("Default TEST_DURATION: " + configParams.getTEST_DURATION() + "#", e);
            }           

        }
        if (isErrorInConfigParams) {
            logger.error("Terminating Application due to error in Config File");
            System.exit(0);
        }
    }

}

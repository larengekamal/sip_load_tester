/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc_simulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author root
 */
public class CC_SIMULATOR {

    private static Logger logger = LogManager.getLogger(CC_SIMULATOR.class);
    private final ConfigParams configParams = ConfigParams.getConfigParams();
    public static String APP_NAME = "SIPLoadTester";
    public static String API_VERSION = "1.0";
    public static String API_DATED = "05.02.2024";

    SIPClient sipc;
    int start_num = 18001;
    int end_num = 18200;
    ExecutorService executor;
    boolean isshutdownCalled = false;

    int total_callCount_failedCalls = 0;
    int total_callCount_successCalls = 0;
    int total_callInitiateCount = 0;
    int total_callAcceptCount = 0;

    long starttime;
    long endtime;
    long temptime;
    int test_duration;
    int total_calls_endpoints;

    ArrayList<Long> initiator_endpoints = new ArrayList<Long>();
    ArrayList<Long> terminator_endpoints = new ArrayList<Long>();
    ArrayList<Long> endpoints = new ArrayList<Long>();
    ArrayList<SIPClient> sip_client = new ArrayList<SIPClient>();

    String testStartTime = "";
    String testEndTime = "";

    public void parseCalls() {
        String paramStr = configParams.getCALLS();
        String[] calls_str = paramStr.split(",");
        logger.info("==================================================");
        logger.info("                Parsing Calls");
        logger.info("==================================================");
        logger.info("Total Calls available for Parsing=" + calls_str.length);
        for (int i = 0; i < calls_str.length; i++) {
            String set = calls_str[i].trim();
            long steps;
            long start_ph_no;
            int total_count;
            String initiator_str;
            String init_str_ph[];
            String terminator_str;
            String ter_str_ph[];
            String[] init_terminator_str = set.split("->");
            logger.info(i + "# Parsing Call=" + set + "#");
            try {
                initiator_str = init_terminator_str[0].trim();
                terminator_str = init_terminator_str[1].trim();
            } catch (Exception ex) {
                continue;
            }
            logger.info(" Parsing Initiator=" + initiator_str + "#");
            init_str_ph = initiator_str.split(":");
            try {
                steps = Integer.parseInt(init_str_ph[1].trim());
            } catch (Exception ex) {
                steps = 1;
            }
            try {
                start_ph_no = Long.parseLong(init_str_ph[0].trim());
            } catch (Exception ex) {
                start_ph_no = 0;
            }
            try {
                total_count = Integer.parseInt(init_str_ph[2].trim());
            } catch (Exception ex) {
                total_count = 1;
            }
            logger.info("  Start Number=" + start_ph_no + " Steps=" + steps + " Total=" + total_count + "#");
            for (int endpoint_count = 0; endpoint_count < total_count; endpoint_count++) {
                logger.info("-->INITIATOR PHONE NUMBER=" + start_ph_no);
                initiator_endpoints.add(start_ph_no);
                start_ph_no = start_ph_no + steps;
            }

            ter_str_ph = terminator_str.split(":");
            logger.info(" Parsing Terminator=" + terminator_str + "#");
            try {
                steps = Integer.parseInt(ter_str_ph[1].trim());
            } catch (Exception ex) {
                steps = 1;
            }
            try {
                start_ph_no = Long.parseLong(ter_str_ph[0].trim());
            } catch (Exception ex) {
                start_ph_no = 0;
            }
            try {
                total_count = Integer.parseInt(ter_str_ph[2].trim());
            } catch (Exception ex) {
                total_count = 1;
            }
            logger.info("  Start Number=" + start_ph_no + " Steps=" + steps + " Total=" + total_count + "#");
            for (int endpoint_count = 0; endpoint_count < total_count; endpoint_count++) {
                logger.info("-->TERMINATOR PHONE NUMBER=" + start_ph_no);
                terminator_endpoints.add(start_ph_no);
                start_ph_no = start_ph_no + steps;
            }
        }
        logger.info("Total Initiator Endpoints Created=" + initiator_endpoints.size() + "#");
        logger.info("Total Terminator Endpoints Created=" + terminator_endpoints.size() + "#");
        logger.info("==================================================");
        logger.info("               End of Parsing Calls");
        logger.info("==================================================");
    }

    public void parseEndpoints() {
        String paramStr = configParams.getENDPOINTS();
        String[] endpoints_str = paramStr.split(",");
        logger.info("==================================================");
        logger.info("                Parsing Endpoints");
        logger.info("==================================================");
        logger.info("Total Endpoints available for Parsing=" + endpoints_str.length);
        for (int i = 0; i < endpoints_str.length; i++) {
            String set = endpoints_str[i].trim();
            logger.info(i + "# Parsing Endpoint=" + set + "#");
            String[] str_split = set.split(":");
            long steps;
            long start_ph_no;
            int total_count;
            try {
                start_ph_no = Long.parseLong(str_split[0].trim());
            } catch (Exception ex) {
                start_ph_no = 0;
            }
            try {
                steps = Integer.parseInt(str_split[1].trim());
            } catch (Exception ex) {
                steps = 1;
            }
            try {
                total_count = Integer.parseInt(str_split[2].trim());
            } catch (Exception ex) {
                total_count = 1;
            }
            logger.info(" Start Number=" + start_ph_no + " Steps=" + steps + " Total=" + total_count + "#");
            for (int endpoint_count = 0; endpoint_count < total_count; endpoint_count++) {
                logger.info("-->PHONE NUMBER=" + start_ph_no);
                endpoints.add(start_ph_no);
                start_ph_no = start_ph_no + steps;
            }
        }
        logger.info("Total Endpoints Created=" + endpoints.size() + "#");
        logger.info("==================================================");
        logger.info("              End of Parsing Endpoints");
        logger.info("==================================================");
    }

    // Changed on 22.02.2024 for matching with Endpoints
    public boolean isEndpointPresent(long ph_no) {
        boolean ret = false;
        long num = 0;
        for (int i = 0; i < endpoints.size(); i = i + 1) {
            num = endpoints.get(i);
            if (ph_no == num) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public void generateTestReport() {
        LocalDateTime currenttime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String formattedDatetime = currenttime.format(formatter);
        try {
            PrintWriter htmlWriter = new PrintWriter("SipLoadTester_report_" + formattedDatetime + ".html", "UTF-8");
            String testResultRows = "";
            int count = 0;
            int passCount = 0;
            int failCount = 0;
            double passPercent = 0;
            for (int i = 0; i < sip_client.size(); i = i + 1) {
                sipc = sip_client.get(i);
                ArrayList<SIPClient.SIPCallStats> callStats = sipc.getSipCallStatsReport();
                for (int j = 0; j < callStats.size(); j++) {
                    String type = "Terminator";
                    String call_res = "FAIL";
                    SIPClient.SIPCallStats stats = callStats.get(j);
                    if (stats.isInitiator) {
                        type = "Initiator";
                    }
                    if (stats.isCallSuccess) {
                        call_res = "PASS";
                        passCount = passCount + 1;
                    }
                    count = count + 1;
                    testResultRows = testResultRows
                            + "              <tr>\n"
                            + "                <td>" + count + "</td>\n"
                            + "                <td>" + type + "</td>\n"
                            + "                <td>" + call_res + "</td>\n"
                            + "                <td>" + stats.from_number + "</td>\n"
                            + "                <td>" + stats.to_number + "</td>\n"
                            + "                <td>" + stats.invite_response_time + "(" + stats.rcvdResponse + ")</td>\n"
                            + "                <td>" + stats.call_setup_time + "</td>\n"
                            + "                <td>" + stats.call_connect_duration + "</td>\n"
                            + "                <td>" + stats.total_call_time + "</td>\n"
                            + "              </tr>\n";
                }
            }
            failCount = count - passCount;
            passPercent = ((double) passCount / count) * 100;
            String text = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head>\n"
                    + "  <meta charset=\"UTF-8\">\n"
                    + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                    + "  <title>SIP Load Test Report</title>\n"
                    + "  <style>\n"
                    + "    body {\n"
                    + "      font-family: Arial, sans-serif;\n"
                    + "      margin: 0;\n"
                    + "      padding: 0;\n"
                    + "    }\n"
                    + "\n"
                    + "    .header {\n"
                    + "      background-color: #3498db;\n"
                    + "      color: white;\n"
                    + "      padding: 20px;\n"
                    + "      text-align: center;\n"
                    + "    }\n"
                    + "\n"
                    + "    .container {\n"
                    + "      padding: 20px;\n"
                    + "    }\n"
                    + "\n"
                    + "    .row {\n"
                    + "      margin-bottom: 20px;\n"
                    + "      display: flex;\n"
                    + "      justify-content: space-between;\n"
                    + "    }\n"
                    + "\n"
                    + "    .statistic {\n"
                    + "      padding: 20px;\n"
                    + "      flex: 1;\n"
                    + "      margin-right: 10px;\n"
                    + "      text-align: center;\n"
                    + "    }\n"
                    + "\n"
                    + "    .passed {\n"
                    + "      background-color: #2ecc71;\n"
                    + "      color: white;\n"
                    + "    }\n"
                    + "\n"
                    + "    .failed {\n"
                    + "      background-color: #e74c3c;\n"
                    + "      color: white;\n"
                    + "    }\n"
                    + "\n"
                    + "    .calldetail {\n"
                    + "      background-color: #06989A;\n"
                    + "      color: white;\n"
                    + "      padding: 20px;\n"
                    + "      text-align: center;\n"
                    + "      width: 100%;\n"
                    + "      height: 100%;\n"
                    + "    }\n"
                    + "\n"
                    + "    .inputparams {\n"
                    + "      background-color: #3465A4;\n"
                    + "      color: white;\n"
                    + "      padding: 20px;\n"
                    + "      text-align: center;\n"
                    + "      width: 100%;\n"
                    + "    }\n"
                    + "\n"
                    + "\n"
                    + "    .testTime {\n"
                    + "      background-color: #859900;\n"
                    + "      color: white;\n"
                    + "      padding: 20px;\n"
                    + "      text-align: center;\n"
                    + "      width: 100%;\n"
                    + "    }\n"
                    + "\n"
                    + "    .table-container {\n"
                    + "      overflow-x: auto;\n"
                    + "      width: 100%;\n"
                    + "    }\n"
                    + "    .search-container {\n"
                    + "      margin-bottom:10px;\n"
                    + "      position: absolute;\n"
                    + "      right: 40px;\n"
                    + "    }\n"
                    + "\n"
                    + "    table {\n"
                    + "      border-collapse: collapse;\n"
                    + "      width: 100%;\n"
                    + "    }\n"
                    + "\n"
                    + "    th, td {\n"
                    + "      border: 1px solid #ddd;\n"
                    + "      padding: 8px;\n"
                    + "      text-align: left;\n"
                    + "    }\n"
                    + "\n"
                    + "    th {\n"
                    + "      background-color: #34495e;\n"
                    + "      color: white;\n"
                    + "      cursor: pointer;\n"
                    + "    }\n"
                    + "  </style>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "  <div class=\"header\">\n"
                    + "    <h1>SIP Load Tester (Version:" + API_VERSION + " Dated:" + API_DATED + ")</h1>\n"
                    + "  </div>\n"
                    + "\n"
                    + "  <div class=\"container\">\n"
                    + "    <div class=\"row\">\n"
                    + "      <div class=\"statistic passed\">\n"
                    + "        <h2>Calls Passed</h2>\n"
                    + "        <p>Total Passed Calls: " + passCount + "</p>\n"
                    + "        <p>Percentage: " + String.format("%.2f", passPercent) + "%</p>\n"
                    + "      </div>\n"
                    + "      <div class=\"statistic failed\">\n"
                    + "        <h2>Calls Failed</h2>\n"
                    + "        <p>Total Failed Calls: " + failCount + "</p>\n"
                    + "        <p>Percentage: " + String.format("%.2f", (100 - passPercent)) + "%</p>\n"
                    + "      </div>\n"
                    + "    </div>\n"
                    + "\n"
                    + "    <div class=\"row\">\n"
                    + "      <div class=\"testTime\">\n"
                    + "      <h2>Test Time</h2>     \n"
                    + "      <table id=\"timetable\">\n"
                    + "            <tbody id=\"callTableBody\">\n"
                    + "              <!-- Your table rows go here -->\n"
                    + "              <!-- Example row -->\n"
                    + "              <tr>\n"
                    + "                <td>Test Start Time</td>\n"
                    + "                <td>" + testStartTime + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Test End Time</td>\n"
                    + "                <td>" + testEndTime + "</td>                \n"
                    + "              </tr>\n"
                    + "              <!-- Example row -->\n"
                    + "              <!-- You can dynamically generate these rows -->\n"
                    + "            </tbody>\n"
                    + "          </table>\n"
                    + "      </div>\n"
                    + "    </div>\n"
                    + "\n"
                    + "\n"
                    + "    <div class=\"row\">\n"
                    + "      <div class=\"inputparams\">\n"
                    + "      <h2>Input Parameters</h2>     \n"
                    + "      <table id=\"inputTable\">\n"
                    + "            <tbody id=\"callTableBody\">\n"
                    + "              <!-- Your table rows go here -->\n"
                    + "              <!-- Example row -->\n"
                    + "              <tr>\n"
                    + "                <td>Server IP Address</td>\n"
                    + "                <td>" + configParams.getSERVER_IP_ADDR() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Server Port</td>\n"
                    + "                <td>" + configParams.getSERVER_PORT() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Transport Protocol</td>\n"
                    + "                <td>" + configParams.getTRANSPORT() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Endpoints</td>\n"
                    + "                <td>" + configParams.getENDPOINTS() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Calls</td>\n"
                    + "                <td>" + configParams.getCALLS() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>No of Calls to generate</td>\n"
                    + "                <td>" + configParams.getNUM_CALLS_TO_GENERATE() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Register Expiry(sec)</td>\n"
                    + "                <td>" + configParams.getREGISTER_EXPIRY() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Register Fail time(msec)</td>\n"
                    + "                <td>" + configParams.getREGISTER_FAIL_DURATION_MILI_SEC() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Call Disconnect after(msec)</td>\n"
                    + "                <td>" + configParams.getCALL_DISCONNECT_AFTER_MILI_SEC() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Call Accept after(msec)</td>\n"
                    + "                <td>" + configParams.getCALL_ACCEPT_AFTER_MILI_SEC() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Call Initiate after(msec)</td>\n"
                    + "                <td>" + configParams.getCALL_INITIATE_AFTER_MILI_SEC() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Audio Start Port</td>\n"
                    + "                <td>" + configParams.getAUDIO_START_PORT() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <tr>\n"
                    + "                <td>Test Duration(sec)</td>\n"
                    + "                <td>" + configParams.getTEST_DURATION() + "</td>                \n"
                    + "              </tr>\n"
                    + "              <!-- Example row -->\n"
                    + "              <!-- You can dynamically generate these rows -->\n"
                    + "            </tbody>\n"
                    + "          </table>\n"
                    + "      </div>\n"
                    + "    </div>\n"
                    + "\n"
                    + "    <div class=\"row\">\n"
                    + "      <div class=\"calldetail\">\n"
                    + "        <div class=\"search-container\">\n"
                    + "        <!-- Search input -->\n"
                    + "        <input type=\"text\" id=\"searchInput\" onkeyup=\"searchTable()\" placeholder=\"Search..\" align=\"right\">\n"
                    + "        </div>\n"
                    + "        <h2>Call Details</h2>       \n"
                    + "        <div class=\"table-container\">          \n"
                    + "          <table id=\"callTable\">\n"
                    + "            <thead>\n"
                    + "              <tr>\n"
                    + "                <th onclick=\"sortTable(0)\">S.NO</th>\n"
                    + "                <th onclick=\"sortTable(1)\">Call Type</th>\n"
                    + "                <th onclick=\"sortTable(2)\">Call Result</th>\n"
                    + "                <th onclick=\"sortTable(3)\">From Number</th>\n"
                    + "                <th onclick=\"sortTable(4)\">To Number</th>\n"
                    + "                <th onclick=\"sortTable(5)\">Response Time (msec)</th>\n"
                    + "                <th onclick=\"sortTable(6)\">Call Setup Time (msec)</th>\n"
                    + "                <th onclick=\"sortTable(7)\">Call Connect duration (msec)</th>\n"
                    + "                <th onclick=\"sortTable(8)\">Total Call duration (msec)</th>\n"
                    + "              </tr>\n"
                    + "            </thead>\n"
                    + "            <tbody id=\"callTableBody\">\n"
                    + "              <!-- Your table rows go here -->\n"
                    + "              <!-- Example row -->\n"
                    + testResultRows
                    + "              <!-- Example row -->\n"
                    + "              <!-- You can dynamically generate these rows -->\n"
                    + "            </tbody>\n"
                    + "          </table>\n"
                    + "        </div>\n"
                    + "      </div>\n"
                    + "    </div>\n"
                    + "  </div>\n"
                    + "\n"
                    + "  <script>\n"
                    + "    // Table sorting function\n"
                    + "    function sortTable(columnIndex) {\n"
                    + "      const table = document.getElementById(\"callTable\");\n"
                    + "      const rows = table.rows;\n"
                    + "      let switching = true;\n"
                    + "      let shouldSwitch = false;\n"
                    + "      let i;\n"
                    + "\n"
                    + "      while (switching) {\n"
                    + "        switching = false;\n"
                    + "        for (i = 1; i < (rows.length - 1); i++) {\n"
                    + "          const x = rows[i].getElementsByTagName(\"td\")[columnIndex];\n"
                    + "          const y = rows[i + 1].getElementsByTagName(\"td\")[columnIndex];\n"
                    + "          if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {\n"
                    + "            shouldSwitch = true;\n"
                    + "            break;\n"
                    + "          }\n"
                    + "        }\n"
                    + "        if (shouldSwitch) {\n"
                    + "          rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);\n"
                    + "          switching = true;\n"
                    + "        }\n"
                    + "      }\n"
                    + "    }\n"
                    + "    function searchTable() {\n"
                    + "    // Declare variables\n"
                    + "    var input, filter, table, tr, td, i, txtValue;\n"
                    + "    input = document.getElementById(\"searchInput\");\n"
                    + "    filter = input.value.toUpperCase();\n"
                    + "    table = document.getElementById(\"callTable\");\n"
                    + "    tr = table.getElementsByTagName(\"tr\");\n"
                    + "\n"
                    + "    // Loop through all table rows, and hide those who don't match the search query\n"
                    + "    for (i = 0; i < tr.length; i++) {\n"
                    + "      td = tr[i].getElementsByTagName(\"td\");\n"
                    + "      for (var j = 0; j < td.length; j++) {\n"
                    + "        if (td[j]) {\n"
                    + "          txtValue = td[j].textContent || td[j].innerText;\n"
                    + "          if (txtValue.toUpperCase().indexOf(filter) > -1) {\n"
                    + "            tr[i].style.display = \"\";\n"
                    + "            break;\n"
                    + "          } else {\n"
                    + "            tr[i].style.display = \"none\";\n"
                    + "          }\n"
                    + "        }\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "  </script>\n"
                    + "</body>\n"
                    + "</html>";
            htmlWriter.println(text);
            htmlWriter.close();
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(CC_SIMULATOR.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(CC_SIMULATOR.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public CC_SIMULATOR() {
        String hostname = null;
        starttime = System.currentTimeMillis();
        logger.info("==================================================");
        logger.info("             Starting " + APP_NAME + " v" + API_VERSION + " " + API_DATED);
        logger.info("==================================================");
        logger.info("Starting SIP Tester");
        ConfigModule confModule = new ConfigModule();
        logger.info("Loaded Config Module");
        confModule.loadConfigFile("SIPTester.properties");
        parseEndpoints();
        parseCalls();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownProcess();
            }
        });
        try {
            test_duration = configParams.getTEST_DURATION();
            hostname = InetAddress.getLocalHost().getHostAddress();
            String server_ip = configParams.getSERVER_IP_ADDR();
            int server_port = configParams.getSERVER_PORT();
            int i;
            boolean isNoInitiator = true;
            int threadCount = 0;
            int no_of_stopped_threads = 0;
            total_calls_endpoints = Math.min(initiator_endpoints.size(), terminator_endpoints.size());
            LocalDateTime currenttime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            testStartTime = currenttime.format(formatter);
            for (i = 0; i < total_calls_endpoints; i = i + 1) {
                if (isEndpointPresent(initiator_endpoints.get(i))) {
                    sipc = new SIPClient(true, initiator_endpoints.get(i) + "", initiator_endpoints.get(i) + "", hostname, (terminator_endpoints.get(i)) + "", (terminator_endpoints.get(i)) + "", server_ip, "udp", server_ip + ":" + server_port, threadCount, isNoInitiator);
                    sip_client.add(sipc);
                    threadCount = threadCount + 1;
                    isNoInitiator = false;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(SIPClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    }
                }
                if (isEndpointPresent(terminator_endpoints.get(i))) {
                    sipc = new SIPClient(false, (terminator_endpoints.get(i)) + "", (terminator_endpoints.get(i)) + "", hostname, (initiator_endpoints.get(i)) + "", (initiator_endpoints.get(i)) + "", server_ip, "udp", server_ip + ":" + server_port, threadCount, isNoInitiator);
                    sip_client.add(sipc);
                    threadCount = threadCount + 1;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(SIPClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    }
                }

            }
            executor = Executors.newFixedThreadPool(sip_client.size());
            for (i = 0; i < sip_client.size(); i = i + 1) {
                executor.submit(sip_client.get(i));
            }
            logger.info("Created Executor for " + (sip_client.size()));
            while (true) {
                no_of_stopped_threads = 0;
                total_callCount_failedCalls = 0;
                total_callCount_successCalls = 0;
                total_callInitiateCount = 0;
                total_callAcceptCount = 0;
                for (i = 0; i < sip_client.size(); i = i + 1) {
                    sipc = sip_client.get(i);
                    if (sipc.isStopThread == true) {
                        no_of_stopped_threads = no_of_stopped_threads + 1;
                        total_callCount_failedCalls += sipc.getCallCount_failedCalls();
                        total_callCount_successCalls += sipc.getCallCount_successCalls();
                        total_callInitiateCount += sipc.getCallInitiateCount();
                        total_callAcceptCount += sipc.getCallAcceptCount();
                    }
                }
                if (no_of_stopped_threads == sip_client.size()) {
                    logger.info("Closing the Application as task Completed");
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    java.util.logging.Logger.getLogger(SIPClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
                temptime = (System.currentTimeMillis() - starttime) / 1000;
                if ((test_duration != -1) && (temptime >= test_duration)) {
                    logger.info("Closing the Application as test duration Completed");
                    break;
                }
            }
            currenttime = LocalDateTime.now();
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            testEndTime = currenttime.format(formatter);
            shutdownProcess();
            System.exit(0);
        } catch (UnknownHostException ex) {
            logger.error("Unable to get Hostname ", ex);
        }
    }

    public void shutdownProcess() {
        if (isshutdownCalled == false) {
            int i;
            int threadCount = 0;
            logger.info("Entered shutdownProcess");
            for (i = 0; i < sip_client.size(); i = i + 1) {
                sipc = sip_client.get(i);
                sipc.isStopThread = true;
                logger.info("Stopping SIP Client #" + i);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(CC_SIMULATOR.class.getName()).log(Level.SEVERE, null, ex);
            }
            logger.info("Shutting down executor Service");

            System.out.println("Shutting down executor Service");
            generateTestReport();
            executor.shutdown();
            try {
                executor.awaitTermination(20, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            logger.info("executor Service is now shutdown");

            System.out.println("executor Service is now shutdown");
            for (i = 0; i < sip_client.size(); i = i + 1) {
                sipc = sip_client.get(i);
                sipc.printCallSummary();
            }
            endtime = System.currentTimeMillis();
            logger.info("==================================================");
            logger.info("         Total Test Duration=" + (endtime - starttime) / 1000 + "s");
            logger.info("==================================================");

            System.out.println("==================================================");
            System.out.println("         Total Test Duration=" + (endtime - starttime) / 1000 + "s");
            System.out.println("==================================================");
//        System.exit(0);
            isshutdownCalled = true;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        CC_SIMULATOR cc = new CC_SIMULATOR();
    }

}

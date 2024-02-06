SIP Load Tester
===============
Version	: 1.0
Dated	: 05.02.2024

This Software is SIP Call generator and acceptor with text based configuration. It currently supports 200 calls (can be initiator and terminator), UDP Protocol.

## Software Development Environment
IDE: Apache NetBeans IDE 20
Java: 21.0.1; Java HotSpot(TM) 64-Bit Server VM 21.0.1+12-LTS-29
Runtime: Java(TM) SE Runtime Environment 21.0.1+12-LTS-29
System: Linux version 4.18.0-193.el8.x86_64 running on amd64; UTF-8; en_US (nb)

## Configuration
Configuration file "SIPTester.properties" is used for configuration. There are following configuration parameters.

| Configuration Parameter            | Uses |
| -----------------------            | ---- |
| SERVER_IP_ADDR=172.210.120.120     | The Server IP Address where packet will be sent |
| SERVER_PORT=5060                   | Port Number of the Server                       |
| TRANSPORT=UDP                      | Transport Protocol to use                      |
| ENDPOINTS=18001:1:200              | Endpoints to create, Format: PHONE NUMBER:STEPS:TOTAL COUNT. It creates Phone numbers 18001, 18002, 18003 .. 200 Numbers are created.                      |
| CALLS=18001:2:2->18002:2:2         | Calls to create, Format: INITIATOR PHONE NUMBER:STEPS:TOTAL COUNT->TERMINATOR PHONE NUMBER:STEPS:TOTAL COUNT. It means 18001 calls to 18002, 18003 calls to 18004.                     |
| NUM_CALLS_TO_GENERATE=20           | No of Calls to Generate from One endpoint to another                        |
| REGISTER_EXPIRY=300                | REGISTER Expiry Duration in sec: On REGISTER Success re send REGISTER after this time interval                        |
| REGISTER_FAIL_DURATION_MILI_SEC=500| REGISTER Fail Duration in milli sec: On REGISTER failure re send REGISTER after this time interval                        |
| CALL_DISCONNECT_AFTER_MILI_SEC=2000| After successfull call connect, call will be disconnected after this time interval                        |
| CALL_ACCEPT_AFTER_MILI_SEC=500     | After Receiving INVITE, call will be accepted after this time interval                        |
| CALL_INITIATE_AFTER_MILI_SEC=500   | After call disconnect, new call will be initaiated after this time interval            |
| AUDIO_START_PORT=6000              | Audio media start port |
| TEST_DURATION=120                  | Duration of Test in secs, -1 for infinite |

## Libraries used
For SIP  : JainSIP,
For Logs : log4j 2

## Output
Log files are generated as per the configuration in xml file "log4j2.xml". Html based Report file is generated at the end of the test.

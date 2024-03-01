package cc_simulator;

import static cc_simulator.CC_SIMULATOR.API_DATED;
import static cc_simulator.CC_SIMULATOR.API_VERSION;
import static cc_simulator.CC_SIMULATOR.APP_NAME;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

/**
 * This class is a UAC template.
 *
 * @author M. Ranganathan
 */
public class SIPClient implements SipListener, Runnable {

    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private SipStack sipStack;
    private ContactHeader contactHeader;
    private ListeningPoint udpListeningPoint;
    private ClientTransaction inviteTid, registerTid;
    protected ServerTransaction inviteServerTid;
    private Request inviteRequest;
    private Dialog dialog, registerDialog;
    private boolean byeTaskRunning;
    private ArrayList userAgents;
    private Response okResponse;

    private String fromName = "17001";
    private String fromSipAddress = "172.210.120.118";
    private String fromDisplayName = "17001";

    private String toSipAddress = "172.210.120.120";
    private String toUser = "16001";
    private String toDisplayName = "17002";

    private String transport = "udp";
    private String peerHostPort = "172.210.120.120:5060";
    int myPort = -1;
    int audioPort = 6000;

    long registerSequenceNumber = 0;
    long inviteSequenceNumber = 0;

    private boolean isRegistered = false;
    int stackID;

    private final ConfigParams configParams = ConfigParams.getConfigParams();
    private static Logger logger = LogManager.getLogger(SIPClient.class);

    private final boolean isCallInitiator;
    public boolean isStopThread = false;

    long callDisconnectAfter_milisec = 1000;
    long callAcceptAfter_milisec = 1000;
    long callInitiateAfter_milisec = 10000;
    int totalNumOfCalls = 3;

    private int callCount_failedCalls = 0;
    private int callCount_successCalls = 0;
    private int callInitiateCount = 0;
    private int callAcceptCount = 0;

    Timer callAcceptTimer = new Timer();
    Timer sendRegisterTimer = new Timer();
    Timer sendInviteTimer = new Timer();
    Timer byeTimer = new Timer();

    long invite_sent_received_time;
    long call_connected_time;
    
    private ArrayList<SIPCallStats> sipCallStatsReport = new ArrayList<SIPCallStats>();
    private boolean isNoInitiator;

    class SIPCallStats {

        String from_number;
        String to_number;
        long invite_response_time;      // To get Response of INVITE Sent
        long call_setup_time;           // To get 200 OK of INVITE Sent
        long call_connect_duration;     // Time of Call remains Connected
        long total_call_time;           // Difference of INVITE Sent and call disconnected
        boolean isInitiator;
        boolean isCallSuccess;
        String rcvdResponse;
    }

    SIPCallStats sipStats = new SIPCallStats();

    @Override
    public void run() {
        while (true) {
            if (isStopThread) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(SIPClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
        logger.info("Stopping Thread");
        try {
            if(this.configParams.getREGISTER_REQUIRED() == 1)
            {
                SendREGISTER(0);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(SIPClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
            callAcceptTimer.cancel();
            sendRegisterTimer.cancel();
            sendInviteTimer.cancel();
            byeTimer.cancel();
        } catch (Exception ex) {
            logger.error("Error in Closing timers", ex);
        }
        try {
            if (sipProvider != null) {
                sipProvider.removeSipListener(this);
                sipProvider.removeListeningPoint(udpListeningPoint);
                sipProvider = null;
            }
            if (sipStack != null) {
                sipStack.stop();
                sipStack = null;
            }
        } catch (Exception ex) {
            logger.error("Error in shutdown of sip stack", ex);
        }
    }

    public void printCallSummary() {
        logger.info("==================================================");
        logger.info("                CALL SUMMARY " + "(" + APP_NAME + stackID + ")");
        logger.info("==================================================");
        if (isCallInitiator) {
            logger.info("Calls Initiated=" + callInitiateCount);
        } else {
            logger.info("Calls Accepted=" + callAcceptCount);
        }
        logger.info("Calls Failed=" + callCount_failedCalls);
        logger.info("Calls Success=" + callCount_successCalls);
        logger.info("==================================================");
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public int getCallCount_failedCalls() {
        return callCount_failedCalls;
    }

    public void setCallCount_failedCalls(int callCount_failedCalls) {
        this.callCount_failedCalls = callCount_failedCalls;
    }

    public int getCallCount_successCalls() {
        return callCount_successCalls;
    }

    public void setCallCount_successCalls(int callCount_successCalls) {
        this.callCount_successCalls = callCount_successCalls;
    }

    public int getCallInitiateCount() {
        return callInitiateCount;
    }

    public void setCallInitiateCount(int callInitiateCount) {
        this.callInitiateCount = callInitiateCount;
    }

    public int getCallAcceptCount() {
        return callAcceptCount;
    }

    public void setCallAcceptCount(int callAcceptCount) {
        this.callAcceptCount = callAcceptCount;
    }

    class CallAcceptTask extends TimerTask {

        SIPClient shootme;

        public CallAcceptTask(SIPClient shootme) {
            this.shootme = shootme;
            Thread.currentThread().setName("Timer-CallAccept#" + stackID);
        }

        public void run() {
            Thread.currentThread().setName("CallAcceptTask#" + stackID);
            shootme.sendInviteOK();
            this.cancel();
        }

    }

    class SendRegisterTask extends TimerTask {

        SIPClient shootme;

        public SendRegisterTask(SIPClient shootme) {
            this.shootme = shootme;
            Thread.currentThread().setName("Timer-SendRegister#" + stackID);
        }

        public void run() {
            Thread.currentThread().setName("SendRegisterTask#" + stackID);
            shootme.SendREGISTER(configParams.getREGISTER_EXPIRY());
            this.cancel();
        }

    }

    class SendInviteTask extends TimerTask {

        SIPClient shootme;

        public SendInviteTask(SIPClient shootme) {
            this.shootme = shootme;
            Thread.currentThread().setName("Timer-SendInvite#" + stackID);
        }

        public void run() {
            Thread.currentThread().setName("SendInviteTask#" + stackID);
            shootme.SendINVITE();
            this.cancel();
        }

    }

    class ByeTask extends TimerTask {

        Dialog dialog;
        private final SIPClient shootme;

        public ByeTask(Dialog dialog, SIPClient shootme) {
            this.dialog = dialog;
            Thread.currentThread().setName("Timer-SendBye#" + stackID);
            this.shootme = shootme;
        }

        public void run() {
            Thread.currentThread().setName("SendByeTask#" + stackID);
            try {
                Request byeRequest = this.dialog.createRequest(Request.BYE);
                ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
                dialog.sendRequest(ct);
                long now_time = System.currentTimeMillis();
                if (this.shootme.sipStats.call_connect_duration == 0) {
                    this.shootme.sipStats.call_connect_duration = now_time - this.shootme.call_connected_time;
                    this.shootme.sipStats.total_call_time = now_time - this.shootme.invite_sent_received_time;
                }
                FromHeader fromHeader = (FromHeader) byeRequest.getHeader("From");
                ToHeader toHeader = (ToHeader) byeRequest.getHeader("To");
                logger.info("Sent BYE " + fromHeader.getAddress().getURI() + " -> " + toHeader.getAddress().getURI());
            } catch (Exception ex) {
                logger.error("Error in Function ByeTask " + ex.getMessage(), ex);
            }

        }
    }

    public SIPClient(boolean isCallInitiator) {
        this.isCallInitiator = isCallInitiator;
        this.init();
    }

    public SIPClient(boolean isCallInitiator, String fromName, String fromDisplayName, String fromSipAddress, String toUser, String toDisplayName, String toSipAddress, String transport, String peerHostPort, int stackID, boolean isNoInitiator) {
        logger.info("==================================================");
        logger.info("             SIPClient Constructor ");
        logger.info("==================================================");
        this.isCallInitiator = isCallInitiator;
        this.fromName = fromName;
        this.fromDisplayName = fromDisplayName;
        this.fromSipAddress = fromSipAddress;
        this.toUser = toUser;
        this.toDisplayName = toDisplayName;
        this.toSipAddress = toSipAddress;
        this.transport = transport;
        this.peerHostPort = peerHostPort;
        this.stackID = stackID;
        this.isNoInitiator = isNoInitiator;

        Thread.currentThread().setName(APP_NAME + "#" + this.stackID);

        this.callAcceptAfter_milisec = configParams.getCALL_ACCEPT_AFTER_MILI_SEC();
        this.callDisconnectAfter_milisec = configParams.getCALL_DISCONNECT_AFTER_MILI_SEC();
        this.totalNumOfCalls = configParams.getNUM_CALLS_TO_GENERATE();
        this.callInitiateAfter_milisec = configParams.getCALL_INITIATE_AFTER_MILI_SEC();

        logger.info("isCallInitiator=" + isCallInitiator);
        logger.info("fromName=" + fromName);
        logger.info("fromDisplayName=" + fromDisplayName);
        logger.info("fromSipAddress=" + fromSipAddress);
        logger.info("toUser=" + toUser);
        logger.info("toDisplayName=" + toDisplayName);
        logger.info("transport=" + transport);
        logger.info("peerHostPort=" + peerHostPort);
        logger.info("stackID=" + stackID);
        logger.info("isNoInitiator=" + isNoInitiator);
        logger.info("callAcceptAfter_milisec=" + callAcceptAfter_milisec);
        logger.info("callDisconnectAfter_milisec=" + callDisconnectAfter_milisec);
        logger.info("totalNumOfCalls=" + totalNumOfCalls);
        logger.info("callInitiateAfter_milisec=" + callInitiateAfter_milisec);
        this.init();
        
    }

    public ArrayList<SIPCallStats> getSipCallStatsReport() {
        return sipCallStatsReport;
    }

    public void setSipCallStatsReport(ArrayList<SIPCallStats> sipCallStatsReport) {
        this.sipCallStatsReport = sipCallStatsReport;
    }
    
    public void initSipStats() {
        this.sipStats = new SIPCallStats();
        this.sipStats.from_number = fromName;
        this.sipStats.to_number = toUser;
        this.sipStats.isInitiator = isCallInitiator;
        this.sipStats.call_connect_duration = 0;
        this.sipStats.call_setup_time = 0;
        this.sipStats.invite_response_time = 0;
        this.sipStats.total_call_time = 0;
        this.sipStats.isCallSuccess = false;
        this.invite_sent_received_time = System.currentTimeMillis();
        this.sipStats.rcvdResponse = "TIMEOUT";
    }

    public void printSIPStats() {
        String call_res = "NOT_OK";
        String init_ter = "INITIATOR";
        if (this.sipStats.isCallSuccess) {
            call_res = "OK";
        }
        if (!this.sipStats.isInitiator) {
            init_ter = "TERMINATOR";
        }
        logger.info(init_ter + "[" + call_res + "] " + this.sipStats.from_number + " -> " + this.sipStats.to_number + " " + this.sipStats.invite_response_time + " " + this.sipStats.call_setup_time + " " + this.sipStats.call_connect_duration + " " + this.sipStats.total_call_time);
        sipCallStatsReport.add(sipStats);
    }

    int getFreePort(int startport, int endport) {
        int i;
        int port = -1;
        for (i = startport; i <= endport; i++) {
            try {
                InetSocketAddress serverConn = new InetSocketAddress(fromSipAddress, i);
                DatagramSocket soc = new DatagramSocket(serverConn);
                // Socket is available
                soc.close();
                port = i;
                break;
            } catch (Exception e) {
                // Socket is not available
//                logger.warn("In getFreePort Port=" + port + " is not available");
            }
        }
        logger.info("Got Free Port for SIP Listener=" + port);
        return port;
    }

    private static final String usageString = "java "
            + "examples.shootist.Shootist \n"
            + ">>>> is your class path set to the root?";

    @Override
    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent.getServerTransaction();
        Thread.currentThread().setName("processRequest#" + stackID);
        try {
            DialogState state = requestReceivedEvent.getDialog().getState();
            logger.info("Received Request " + request.getMethod() + " Dialog State=" + state);
        } catch (Exception e1) {
            logger.info("Received Request " + request.getMethod() + " Dialog State=UNKNOWN");
        }

//        logger.info(stackID+"# "+"Request " + request.getMethod()
//                + " received at " + sipStack.getStackName()
//                + " with server transaction id " + serverTransactionId
//                + " dialog "+requestReceivedEvent.getDialog());
        // We are the UAC so the only request we get is the BYE.
        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestReceivedEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestReceivedEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.CANCEL)) {
            processCancel(requestReceivedEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(request, serverTransactionId);
        } else {
            try {
                serverTransactionId.sendResponse(messageFactory.createResponse(202, request));
            } catch (SipException e) {
                // TODO Auto-generated catch block
                logger.error("SipException in Function processRequest " + e.getMessage(), e);
            } catch (InvalidArgumentException e) {
                // TODO Auto-generated catch block
                logger.error("InvalidArgumentException in Function processRequest " + e.getMessage(), e);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                logger.error("ParseException in Function processRequest " + e.getMessage(), e);
            }
        }

    }

    /**
     * Process the ACK request. Send the bye and complete the call flow.
     *
     * @param requestEvent
     * @param serverTransaction
     */
    public void processAck(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        if (serverTransaction != null) {
            try {
                if (serverTransaction.getDialog().getState() == DialogState.CONFIRMED) {
                    long now_time = System.currentTimeMillis();
                    if (this.sipStats.call_setup_time == 0) {
                        this.sipStats.call_setup_time = now_time - this.invite_sent_received_time;
                        this.call_connected_time = now_time;
                        this.sipStats.isCallSuccess = true;
                    }
                    callCount_successCalls = callCount_successCalls + 1;
                }
                if(this.isNoInitiator)
                {
                    logger.info("Starting BYE Send Timer for " + callDisconnectAfter_milisec);
                    byeTimer.schedule(new SIPClient.ByeTask(serverTransaction.getDialog(), this), (callDisconnectAfter_milisec));
//                    SipProvider provider = (SipProvider) requestEvent.getSource();
//                    Request byeRequest = dialog.createRequest(Request.BYE);
//                    ClientTransaction ct = provider
//                            .getNewClientTransaction(byeRequest);
//                        dialog.sendRequest(ct);
//                    FromHeader fromHeader = (FromHeader) byeRequest.getHeader("From");
//                    ToHeader toHeader = (ToHeader) byeRequest.getHeader("To");
//                    logger.info(stackID+"# "+"Sent BYE " + fromHeader.getAddress().getURI() + " -> " + toHeader.getAddress().getURI() + " TID=" + ct);
                }
            } catch (Exception e) {
                logger.error("Exception in Function processAck " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process the invite request.
     *
     * @param requestEvent
     */
    public void processInvite(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            initSipStats();
//            System.out.println(stackID+"# "+"shootme: got an Invite sending Trying");
            // System.out.println("shootme: " + request);
            Response response = messageFactory.createResponse(Response.RINGING,
                    request);
            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                st = sipProvider.getNewServerTransaction(request);
            }
            dialog = st.getDialog();

            st.sendResponse(response);

            this.okResponse = messageFactory.createResponse(Response.OK,
                    request);

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(fromName, fromSipAddress);
            contactURI.setPort(sipProvider.getListeningPoint(transport)
                    .getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);

            response.addHeader(contactHeader);
            ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
            toHeader.setTag(createNewTag()); // Application is supposed to set.
            okResponse.addHeader(contactHeader);

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");
            String sdpData = getSDP();
            byte[] contents = sdpData.getBytes();
            okResponse.setContent(contents, contentTypeHeader);

            this.inviteServerTid = st;
            // Defer sending the OK to simulate the phone ringing.
            // Answered in 1 second ( this guy is fast at taking calls)
            this.inviteRequest = request;

            logger.info("Starting Call Accept Timer for " + callAcceptAfter_milisec);
            callAcceptTimer.schedule(new SIPClient.CallAcceptTask(this), callAcceptAfter_milisec);
            long now_time = System.currentTimeMillis();
            if (this.sipStats.invite_response_time == 0) {
                this.sipStats.invite_response_time = now_time - this.invite_sent_received_time;
                this.sipStats.rcvdResponse = Response.RINGING+"";
            }
//            this.sendInviteOK();
        } catch (Exception ex) {
            logger.error("Error in Function processInvite " + ex.getMessage(), ex);
        }
    }

    private void logResponseSent(Response okResponse, ServerTransaction inviteServerTid) {
        CSeqHeader cseq = (CSeqHeader) okResponse.getHeader(CSeqHeader.NAME);
        dialog = inviteServerTid.getDialog();
        try {
            DialogState state = dialog.getState();
            logger.info("Sent " + okResponse.getStatusCode() + "(CSeq " + cseq.getSeqNumber() + " " + cseq.getMethod() + ")Dialog State=" + state);
        } catch (Exception e1) {
            logger.info("Sent " + okResponse.getStatusCode() + "(CSeq " + cseq.getSeqNumber() + " " + cseq.getMethod() + ")Dialog State=UNKNOWN");
        }
    }

    private void sendInviteOK() {
        try {
            if (inviteServerTid.getState() != TransactionState.COMPLETED) {
//                System.out.println(stackID+"# "+"shootme: Dialog state before 200: " + inviteServerTid.getDialog().getState());
                inviteServerTid.sendResponse(okResponse);
                logResponseSent(okResponse, inviteServerTid);
                callAcceptCount = callAcceptCount + 1;
//                System.out.println(stackID+"# "+"shootme: Dialog state after 200: " + inviteServerTid.getDialog().getState());
            }
        } catch (SipException e) {
            logger.error("SipException in Function processAck " + e.getMessage(), e);
        } catch (InvalidArgumentException e) {
            logger.error("InvalidArgumentException in Function processAck " + e.getMessage(), e);
        }
    }

    public void processCancel(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
//            System.out.println(stackID+"# "+"shootme:  got a cancel.");
            if (serverTransactionId == null) {
//                System.out.println(stackID+"# "+"shootme:  null tid.");
                return;
            }
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);
            logResponseSent(response, serverTransactionId);
            if (dialog.getState() != DialogState.CONFIRMED) {
                response = messageFactory.createResponse(
                        Response.REQUEST_TERMINATED, inviteRequest);
                inviteServerTid.sendResponse(response);
                logResponseSent(response, inviteServerTid);
            }

        } catch (Exception ex) {
            logger.error("Error in Function processCancel " + ex.getMessage(), ex);

        }
    }

    public void processBye(Request request,
            ServerTransaction serverTransactionId) {
        try {
//            System.out.println(stackID+"# "+"shootist:  got a bye .");
            if (serverTransactionId == null) {
//                System.out.println(stackID+"# "+"shootist:  null TID.");
                return;
            }
            long now_time = System.currentTimeMillis();
            if (this.sipStats.call_connect_duration == 0) {
                this.sipStats.call_connect_duration = now_time - this.call_connected_time;
                this.sipStats.total_call_time = now_time - this.invite_sent_received_time;
            }
//            Dialog dialog = serverTransactionId.getDialog();
//            logger.info(stackID+"# "+"Dialog State = " + dialog.getState());
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);
            logResponseSent(response, serverTransactionId);
//            logger.info(stackID+"# "+"shootist:  Sending OK.");
//            logger.info(stackID+"# "+"Dialog State = " + dialog.getState());

        } catch (Exception ex) {
            logger.error("Error in Function processBye " + ex.getMessage(), ex);

        }
    }

    // Save the created ACK request, to respond to retransmitted 2xx
    private Request ackRequest;

    @Override
    public void processResponse(ResponseEvent responseReceivedEvent) {
//        System.out.println("Got a response");
        Response response = (Response) responseReceivedEvent.getResponse();
        ClientTransaction tid = responseReceivedEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        Thread.currentThread().setName("processResponse#" + stackID);
        dialog = responseReceivedEvent.getDialog();
        try {
            DialogState state = dialog.getState();
            logger.info("Received Response " + response.getStatusCode() + " CSeq " + cseq.getSeqNumber() + " " + cseq.getMethod() + " Dialog State=" + state);
        } catch (Exception e1) {
            logger.info("Received Response " + response.getStatusCode() + " CSeq " + cseq.getSeqNumber() + " " + cseq.getMethod() + " Dialog State=UNKNOWN");
        }
//        try 
//        {
//            Dialog responseDialog = responseReceivedEvent.getDialog();
//            logger.info(stackID+"# "+"Response received: Code=" + response.getStatusCode() + " CSeq " + cseq.getSeqNumber() + " " + cseq.getMethod()+" Dialog="+responseDialog);
//        }
//        catch(Exception e)
//        {
//            logger.info(stackID+"# "+"Response received: Code=" + response.getStatusCode() + " CSeq " + cseq.getSeqNumber() + " " + cseq.getMethod());
//        }

        if (tid == null) {

            // RFC3261: MUST respond to every 2xx
            if (ackRequest != null && dialog != null) {
                logger.info(stackID + "# " + "re-sending ACK");
                try {
                    dialog.sendAck(ackRequest);
                } catch (SipException e) {
                    logger.error("SipException in Function processResponse " + e.getMessage(), e);
                }
            }
            return;
        }

//        if(tid.getState() == TransactionState.TERMINATED)
//        {
//            logger.info(stackID+"# "+"Transaction Terminated");
//        }
        // If the caller is supposed to send the bye
//        if ( examples.simplecallsetup.Shootme.callerSendsBye && !byeTaskRunning) {
//            byeTaskRunning = true;
//            new Timer().schedule(new ByeTask(dialog), 4000) ;
//        }
        try {
//            logger.info("transaction state is " + tid.getState());
//            logger.info("Dialog = " + tid.getDialog());
//            logger.info("Dialog State is " + tid.getDialog().getState());
        } catch (Exception e) {

        }
        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    long now_time = System.currentTimeMillis();
                    if (this.sipStats.invite_response_time == 0) {
                        this.sipStats.invite_response_time = now_time - this.invite_sent_received_time;
                        this.sipStats.rcvdResponse = response.getStatusCode()+"";
                    }                            
//                    logger.info("Dialog after 200 OK  " + dialog);
//                    logger.info("Dialog State after 200 OK  " + dialog.getState());
                    ackRequest = dialog.createAck(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber());
//                    logger.info("Sending ACK");
                    dialog.sendAck(ackRequest);
                    if (this.sipStats.call_setup_time == 0) {
                        this.sipStats.call_setup_time = now_time - this.invite_sent_received_time;
                        this.call_connected_time = now_time;
                        this.sipStats.isCallSuccess = true;
                    }
                    FromHeader fromHeader = (FromHeader) ackRequest.getHeader("From");
                    ToHeader toHeader = (ToHeader) ackRequest.getHeader("To");
                    logger.info("Sent ACK " + fromHeader.getAddress().getURI() + " -> " + toHeader.getAddress().getURI());
                    // JvB: test REFER, reported bug in tag handling
                    // dialog.sendRequest(sipProvider.getNewClientTransaction( dialog.createRequest("REFER") ));
                } else if (cseq.getMethod().equals(Request.CANCEL)) {
                    if (dialog.getState() == DialogState.CONFIRMED) {
                        // oops cancel went in too late. Need to hang up the
                        // dialog.
                        logger.info("Sending BYE -- cancel went in too late !!");
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipProvider
                                .getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                        FromHeader fromHeader = (FromHeader) byeRequest.getHeader("From");
                        ToHeader toHeader = (ToHeader) byeRequest.getHeader("To");
                        logger.info("Sent BYE " + fromHeader.getAddress().getURI() + " -> " + toHeader.getAddress().getURI());
                    }

                } else if (cseq.getMethod().equals(Request.REGISTER)) {
                    ExpiresHeader expires = (ExpiresHeader) response.getHeader(ExpiresHeader.NAME);
                    if (!isRegistered) {
                        isRegistered = true;
                        logger.info("REGISTERED:" + fromName);
                        if (isCallInitiator) {
                            if (callInitiateCount >= totalNumOfCalls) {
                                logger.info("Not Starting Call Initiate Timer for " + this.configParams.getCALL_INITIATE_AFTER_MILI_SEC());
                            } else {
                                logger.info("Starting Call Initiate Timer for " + this.configParams.getCALL_INITIATE_AFTER_MILI_SEC());
                                sendInviteTimer.schedule(new SIPClient.SendInviteTask(this), this.configParams.getCALL_INITIATE_AFTER_MILI_SEC());
                            }
                        }
                    }
                    sendRegisterTimer.schedule(new SIPClient.SendRegisterTask(this), (configParams.getREGISTER_EXPIRY() * 1000));
                }
            } else {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    long now_time = System.currentTimeMillis();
                    if (this.sipStats.invite_response_time == 0) {
                        this.sipStats.invite_response_time = now_time - this.invite_sent_received_time;
                        this.sipStats.rcvdResponse = response.getStatusCode()+"";
                    }                    
                    if (response.getStatusCode() >= 400) {
                        callCount_failedCalls = callCount_failedCalls + 1;
                        this.sipStats.rcvdResponse = response.getStatusCode()+"";
                    }
                } else if (cseq.getMethod().equals(Request.REGISTER)) {
                    if (response.getStatusCode() >= 400) {
                        if(this.isRegistered)
                        {
                            logger.error("UN-REGISTERED:" + fromName);
                            this.isRegistered = false;
                        }
                        sendRegisterTimer.schedule(new SIPClient.SendRegisterTask(this), (configParams.getREGISTER_FAIL_DURATION_MILI_SEC()));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in Function processResponse " + ex.getMessage(), ex);
        }

    }

    @Override
    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }
        if (transaction.getRequest().getMethod().equalsIgnoreCase("register")) {
            logger.error("UN-REGISTERED:" + fromName);
            this.isRegistered = false;
            sendRegisterTimer.schedule(new SIPClient.SendRegisterTask(this), (configParams.getREGISTER_FAIL_DURATION_MILI_SEC()));
        } else if (transaction.getRequest().getMethod().equalsIgnoreCase("invite")) {
            logger.warn("INVITE Sent Timeout to " + toUser);
            if (callInitiateCount >= totalNumOfCalls) {
                logger.warn("Closing Application as Total Number of Calls=" + callInitiateCount + "reached max call count of " + totalNumOfCalls);
                this.isStopThread = true;
            } else {
                logger.info("Total Number of Calls Initiated=" + callInitiateCount + " max call count=" + totalNumOfCalls);
                logger.info("Starting Call Initiate Timer for " + callInitiateAfter_milisec);
                sendInviteTimer.schedule(new SIPClient.SendInviteTask(this), (callInitiateAfter_milisec));
            }
        }
        try {
//            logger.info("state = " + transaction.getState());
//            logger.info("dialog = " + transaction.getDialog());
//            logger.info("dialogState = " + transaction.getDialog().getState());
//            logger.info("Transaction Time out");
        } catch (Exception e) {

        }
    }

    public void sendCancel() {
        try {
            logger.info("Sending cancel");
            Request cancelRequest = inviteTid.createCancel();
            ClientTransaction cancelTid = sipProvider
                    .getNewClientTransaction(cancelRequest);
            cancelTid.sendRequest();
            FromHeader fromHeader = (FromHeader) cancelRequest.getHeader("From");
            ToHeader toHeader = (ToHeader) cancelRequest.getHeader("To");
            logger.info("Sent CANCEL " + fromHeader.getAddress().getURI() + " -> " + toHeader.getAddress().getURI() + " TID=" + cancelTid);
        } catch (Exception e) {
            logger.error("Exception in Function sendCancel " + e.getMessage(), e);
        }
    }

    public void init() {
        logger.info("==================================================");
        logger.info("             SIPClient Init ");
        logger.info("==================================================");
        ConsoleAppender console = new ConsoleAppender(); //create appender
////        //configure the appender
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(org.apache.log4j.Level.DEBUG);
        console.activateOptions();
////        //add appender to any Logger (here is root)
        org.apache.log4j.Logger.getRootLogger().addAppender(console);

        SipFactory sipFactory = null;
        sipStack = null;
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/" + transport);
        properties.setProperty("javax.sip.STACK_NAME", APP_NAME + stackID);
        // The following properties are specific to nist-sip
        // and are not necessarily part of any other jain-sip
        // implementation.
        // You can set a max message size for tcp transport to
        // guard against denial of service attack.
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
//        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "INFO");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "sipTester_debug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "sipTester_server.txt");
        // Drop the client connection after we are done with the transaction.
        properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "true");

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            logger.info("sipStack = " + sipStack);
        } catch (PeerUnavailableException e) {
            // could not find
            // gov.nist.jain.protocol.ip.sip.SipStackImpl
            // in the classpath
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
            }
            logger.error("Error in Function init " + e.getMessage(), e);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
            myPort = getFreePort(5060, 60000);
            udpListeningPoint = sipStack.createListeningPoint(fromSipAddress, myPort, transport);
            logger.info("listeningPoint = " + udpListeningPoint);
            sipProvider = sipStack.createSipProvider(udpListeningPoint);
            logger.info("SipProvider = " + sipProvider);

            SIPClient listener = this;
            logger.info("udp provider " + sipProvider);
            sipProvider.addSipListener(listener);
            userAgents = new ArrayList();
            userAgents.add(APP_NAME + "v" + API_VERSION + "dated:" + API_DATED);
            registerSequenceNumber = 0;
            inviteSequenceNumber = 0;
            audioPort = audioPort + 2 * stackID;

        } catch (Exception e) {
            logger.error("Exception in Function init " + e.getMessage(), e);
//            usage();
        }
        if(this.configParams.getREGISTER_REQUIRED() == 0)
        {
            isRegistered = true;
            if(this.isCallInitiator)
            {
                logger.info("Starting Call Initiate Timer for " + this.configParams.getCALL_INITIATE_AFTER_MILI_SEC());
                sendInviteTimer.schedule(new SIPClient.SendInviteTask(this), this.configParams.getCALL_INITIATE_AFTER_MILI_SEC());
            }
        }
        else
        {
            SendREGISTER(configParams.getREGISTER_EXPIRY());
        }
    }

    public String createNewTag() {
        String tag = "";
        Random ra = new Random();
        tag = "" + Math.abs(ra.nextLong() / 100000);
        return tag;
    }

    public String createNewBranch() {
        String branch = "z9hG4bK";
        branch += createNewTag();
        return branch;
    }

    public void SendREGISTER(int expiry_count) {
        try {

            // create >From Header
            SipURI fromAddress = addressFactory.createSipURI(fromName,
                    fromSipAddress);

            Address fromNameAddress = addressFactory.createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory.createFromHeader(
                    fromNameAddress, createNewTag());

            // create To Header
//            SipURI toAddress = fromAddress;
            Address toNameAddress = addressFactory.createAddress(fromAddress);
            toNameAddress.setDisplayName(fromDisplayName);
            ToHeader toHeader = headerFactory.createToHeader(fromNameAddress,
                    null);

            // create Request URI
            SipURI requestURI = addressFactory.createSipURI(fromName,
                    peerHostPort);

            // Create ViaHeaders
            ArrayList viaHeaders = new ArrayList();
            String ipAddress = udpListeningPoint.getIPAddress();
            ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress,
                    sipProvider.getListeningPoint(transport).getPort(),
                    transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create ContentTypeHeader
//            ContentTypeHeader contentTypeHeader = headerFactory
//                    .createContentTypeHeader("application", "sdp");
            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            registerSequenceNumber = registerSequenceNumber + 1;
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(registerSequenceNumber, Request.REGISTER);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI,
                    Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);
            // Create contact headers
            String host = fromSipAddress;

            SipURI contactUrl = addressFactory.createSipURI(fromName, host);
            contactUrl.setPort(udpListeningPoint.getPort());
            contactUrl.setLrParam();

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            ExpiresHeader expires = headerFactory.createExpiresHeader(expiry_count);
            request.setExpires(expires);

            Header userAgentHeader = headerFactory.createUserAgentHeader(userAgents);
            request.addHeader(userAgentHeader);

            Calendar calander = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
            dateFormat.setTimeZone(calander.getTimeZone());
            String formattedDate = dateFormat.format(calander.getTime());
            Header extensionHeader = headerFactory.createHeader("Date", formattedDate);
            request.addHeader(extensionHeader);

//            byte[] contents = null;
//            request.setContent(contents, contentTypeHeader);
            // You can add as many extension headers as you
            // want.
//            extensionHeader = headerFactory.createHeader("My-Other-Header",
//                    "my new header value ");
//            request.addHeader(extensionHeader);
//            Header callInfoHeader = headerFactory.createHeader("Call-Info",
//                    "<http://www.antd.nist.gov>");
//            request.addHeader(callInfoHeader);
            // Create the client transaction.
            if ((sipStack != null) && (sipProvider != null)) {
                registerTid = sipProvider.getNewClientTransaction(request);

                //            System.out.println("registerTid = " + registerTid);            
                //            logger.info("REGISTER Retransmit timer="+registerTid.getRetransmitTimer());
                // send the request out.
                registerTid.sendRequest();
                if (isCallInitiator) {
                    logger.info("Sent REGISTER " + fromName + "@" + fromSipAddress + " -> " + fromName + "@" + toSipAddress + " Calls Initiated=" + callInitiateCount);
                } else {
                    logger.info("Sent REGISTER " + fromName + "@" + fromSipAddress + " -> " + fromName + "@" + toSipAddress + " Calls Accepted=" + callAcceptCount);
                }

                registerDialog = registerTid.getDialog();
            }

        } catch (Exception e) {
            logger.error("Exception in Function SendREGISTER " + e.getMessage(), e);
//            usage();
        }
    }

    public String getSDP() {
        Random ra = new Random();
        int sessionID = Math.abs(ra.nextInt() / 100000);
        String sdpData = "v=0\r\n"
                + "o=" + fromName + " " + sessionID + " " + sessionID + " IN IP4 " + fromSipAddress + "\r\n"
                + "s=mysession session\r\n"
                + "p=" + fromName + "\r\n"
                + "c=IN IP4 " + fromSipAddress + "\r\n"
                + "t=0 0\r\n"
                + "m=audio " + audioPort + " RTP/AVP 0 4 18\r\n"
                + "a=rtpmap:0 PCMU/8000\r\n"
                + "a=rtpmap:4 G723/8000\r\n"
                + "a=rtpmap:18 G729A/8000\r\n"
                + "a=ptime:20\r\n";
        return sdpData;
    }

    public void SendINVITE() {
        try {

            // create >From Header
            SipURI fromAddress = addressFactory.createSipURI(fromName,
                    toSipAddress);

            Address fromNameAddress = addressFactory.createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory.createFromHeader(
                    fromNameAddress, createNewTag());

            // create To Header
            SipURI toAddress = addressFactory
                    .createSipURI(toUser, toSipAddress);
            Address toNameAddress = addressFactory.createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            ToHeader toHeader = headerFactory.createToHeader(toNameAddress,
                    null);

            // create Request URI
            SipURI requestURI = addressFactory.createSipURI(toUser,
                    peerHostPort);

            // Create ViaHeaders
            ArrayList viaHeaders = new ArrayList();
            String ipAddress = udpListeningPoint.getIPAddress();
            ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress,
                    sipProvider.getListeningPoint(transport).getPort(),
                    transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            inviteSequenceNumber = inviteSequenceNumber + 1;
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(inviteSequenceNumber,
                    Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI,
                    Request.INVITE, callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);
            // Create contact headers
            String host = fromSipAddress;

            SipURI contactUrl = addressFactory.createSipURI(fromName, host);
            contactUrl.setPort(udpListeningPoint.getPort());
            contactUrl.setLrParam();

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setPort(sipProvider.getListeningPoint(transport)
                    .getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            Header userAgentHeader = headerFactory.createUserAgentHeader(userAgents);
            request.addHeader(userAgentHeader);

            Calendar calander = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
            dateFormat.setTimeZone(calander.getTimeZone());
            String formattedDate = dateFormat.format(calander.getTime());
            Header extensionHeader = headerFactory.createHeader("Date",
                    formattedDate);
            request.addHeader(extensionHeader);

            // You can add extension headers of your own making
            // to the outgoing SIP request.
            // Add the extension header.
//            Header extensionHeader = headerFactory.createHeader("My-Header",
//                    "my header value");
//            request.addHeader(extensionHeader);
            String sdpData = getSDP();
            byte[] contents = sdpData.getBytes();

            request.setContent(contents, contentTypeHeader);
            // You can add as many extension headers as you
            // want.

//            extensionHeader = headerFactory.createHeader("My-Other-Header",
//                    "my new header value ");
//            request.addHeader(extensionHeader);
//            Header callInfoHeader = headerFactory.createHeader("Call-Info",
//                    "<http://www.antd.nist.gov>");
//            request.addHeader(callInfoHeader);
            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);

//            System.out.println("inviteTid = " + inviteTid);
            // send the request out.
            inviteTid.sendRequest();
            initSipStats();
            dialog = inviteTid.getDialog();
            logger.info("Sent INVITE " + fromName + "@" + fromSipAddress + " -> " + toUser + "@" + toSipAddress + " Port=" + myPort);
            callInitiateCount = callInitiateCount + 1;
        } catch (Exception e) {
            logger.error("Exception in Function SendINVITE " + e.getMessage(), e);
//            usage();
        }
    }

//    public static void main(String args[]) {
//        new SIPClient().init();
//    }
    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("IOException happened for "
                + exceptionEvent.getHost() + " port = "
                + exceptionEvent.getPort());
    }

    @Override
    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        Transaction transaction;
        if (transactionTerminatedEvent.isServerTransaction()) {
            transaction = transactionTerminatedEvent.getServerTransaction();
        } else {
            transaction = transactionTerminatedEvent.getClientTransaction();
        }
        if (transaction.getRequest().getMethod().equalsIgnoreCase("register")) {
//            logger.info(stackID+"# "+"REGISTER Transaction Terminated");
        }
        if (transaction.getRequest().getMethod().equalsIgnoreCase("invite")) {
            logger.debug("INVITE Transaction Terminated " + transaction.getDialog().getState());
            if (transaction.getDialog().getState() == DialogState.CONFIRMED) {
                callCount_successCalls = callCount_successCalls + 1;
                if (isCallInitiator) {
                    logger.info("Starting BYE Send Timer for " + callDisconnectAfter_milisec);
                    byeTimer.schedule(new SIPClient.ByeTask(transaction.getDialog(), this), (callDisconnectAfter_milisec));
                }
            } else {
                if (isCallInitiator) {
                    callCount_failedCalls = callCount_failedCalls + 1;
                }
            }
        }
        try {
//            logger.info("processTransactionTerminated state = " + transaction.getState());
//            logger.info("processTransactionTerminated dialog = " + transaction.getDialog());
//            logger.info("processTransactionTerminated dialogState = " + transaction.getDialog().getState());
        } catch (Exception e) {
            logger.error("Exception in Function processTransactionTerminated " + e.getMessage(), e);
        }
    }

    @Override
    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        logger.debug("dialogTerminatedEvent for Dialog=" + dialogTerminatedEvent.getDialog());
        printSIPStats();
        if (isCallInitiator) {
            if (callInitiateCount >= totalNumOfCalls) {
                logger.warn("Closing Application as Total Number of Calls=" + callInitiateCount + "reached max call count of " + totalNumOfCalls);
                this.isStopThread = true;
            } else {
                logger.info("Total Number of Calls Initiated=" + callInitiateCount + " max call count=" + totalNumOfCalls);
                logger.info("Starting Call Initiate Timer for " + callInitiateAfter_milisec);
                sendInviteTimer.schedule(new SIPClient.SendInviteTask(this), (callInitiateAfter_milisec));
            }
        } else {
            if (callAcceptCount >= totalNumOfCalls) {
                logger.warn("Closing Application as Total Number of Calls=" + callAcceptCount + "reached max call count of " + totalNumOfCalls);
                this.isStopThread = true;
            } else {
                logger.info("Total Number of Calls Accepted=" + callAcceptCount + " max call count=" + totalNumOfCalls);
            }
        }
    }
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apacheextras.camel.component.wmq;

import com.ibm.mq.MQDestination;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.MQRFH2;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.SuspendableService;
import org.apache.camel.support.ScheduledPollConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class WMQConsumer extends ScheduledPollConsumer implements SuspendableService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WMQConsumer.class);

    private final Function<MQMessage, MQHeaderList> mqHeaderListFactory;

    private MQQueueManager mqQueueManager = null;

    public WMQConsumer(WMQEndpoint endpoint, Processor processor) {
        this(endpoint, processor, WMQConsumer::createMqHeaderList);
    }


    public WMQConsumer(WMQEndpoint endpoint, Processor processor,
                       Function<MQMessage, MQHeaderList> mqHeaderListFactory) {
        super(endpoint, processor);
        this.mqHeaderListFactory = mqHeaderListFactory;
    }


    @Override
    protected int poll() throws Exception {
        Exchange exchange = getEndpoint().createExchange();

        Message in = exchange.getIn();

        WMQComponent component = (WMQComponent) getEndpoint().getComponent();

        if (mqQueueManager == null) {
            mqQueueManager = component.getQueueManager(getEndpoint().getQueueManagerName(),
                    getEndpoint().getQueueManagerHostname(),
                    getEndpoint().getQueueManagerPort(),
                    getEndpoint().getQueueManagerChannel(),
                    getEndpoint().getQueueManagerUserID(),
                    getEndpoint().getQueueManagerPassword(),
                    getEndpoint().getQueueManagerCCSID());
        }

        MQDestination destination = null;
        try {
            LOGGER.debug("Consuming from {}", getEndpoint().getDestinationName());

            if (getEndpoint().getDestinationName().startsWith("topic:")) {
                String destinationName = getEndpoint().getDestinationName().substring("topic:".length());
                int options = CMQC.MQSO_CREATE | CMQC.MQSO_RESUME | CMQC.MQSO_DURABLE | CMQC.MQSO_FAIL_IF_QUIESCING;
                destination = mqQueueManager.accessTopic(destinationName, null, options, null, null);
            } else {
                String destinationName = getEndpoint().getDestinationName();
                if (destinationName.startsWith("queue:")) {
                    destinationName = destinationName.substring("queue:".length());
                }
                destination = mqQueueManager.accessQueue(destinationName, MQConstants.MQOO_INPUT_AS_Q_DEF, null, null, null);
            }

            MQMessage message = new MQMessage();
            MQGetMessageOptions options = new MQGetMessageOptions();
            options.options = MQConstants.MQGMO_WAIT + MQConstants.MQGMO_PROPERTIES_COMPATIBILITY + MQConstants.MQGMO_ALL_SEGMENTS_AVAILABLE + MQConstants.MQGMO_COMPLETE_MSG + MQConstants.MQGMO_ALL_MSGS_AVAILABLE;
            options.waitInterval = MQConstants.MQWI_UNLIMITED;
            LOGGER.debug("Waiting for message ...");
            destination.get(message, options);

            LOGGER.debug("Message consumed");

            LOGGER.trace("Dealing with MQMD headers");
            LOGGER.trace("\tmq.mqmd.format: {}", message.format);
            in.setHeader("mq.mqmd.format", message.format);
            LOGGER.trace("\tmq.mqmd.charset: {}", message.characterSet);
            in.setHeader("mq.mqmd.charset", message.characterSet);
            LOGGER.trace("\tmq.mqmd.expiry: {}", message.expiry);
            in.setHeader("mq.mqmd.expiry", message.expiry);
            LOGGER.trace("\tmq.mqmd.put.appl.name: {}", message.putApplicationName);
            in.setHeader("mq.mqmd.put.appl.name", message.putApplicationName);
            LOGGER.trace("\tmq.mqmd.group.id: {}", message.groupId);
            in.setHeader("mq.mqmd.group.id", message.groupId);
            LOGGER.trace("\tmq.mqmd.msg.seq.number: {}", message.messageSequenceNumber);
            in.setHeader("mq.mqmd.msg.seq.number", message.messageSequenceNumber);
            LOGGER.trace("\tmq.mqmd.msg.accounting.token: {}", message.accountingToken);
            in.setHeader("mq.mqmd.msg.accounting.token", message.accountingToken);
            LOGGER.trace("\tmq.mqmd.correl.id: {}", message.correlationId);
            in.setHeader("mq.mqmd.correl.id", message.correlationId);
            LOGGER.trace("\tmq.mqmd.replyto.q: {}", message.replyToQueueName);
            in.setHeader("mq.mqmd.replyto.q", message.replyToQueueName);
            LOGGER.trace("\tmq.mqmd.replyto.q.mgr: {}", message.replyToQueueManagerName);
            in.setHeader("mq.mqmd.replyto.q.mgr", message.replyToQueueManagerName);
            LOGGER.trace("\tmq.mqmd.putdatetime: {}", message.putDateTime);
            in.setHeader("mq.mqmd.putdatetime", message.putDateTime);
            LOGGER.trace("\tmq.mqmd.user.id: {}", message.userId);
            in.setHeader("mq.mqmd.user.id", message.userId);
            LOGGER.trace("\tmq.mqmd.type: {}", message.messageType);
            in.setHeader("mq.mqmd.type", message.messageType);
            LOGGER.trace("\tmq.mqmd.priority: {}", message.priority);
            in.setHeader("mq.mqmd.priority", message.priority);
            LOGGER.trace("\tmq.mqmd.persistence: {}", message.persistence);
            in.setHeader("mq.mqmd.persistence", message.persistence);
            LOGGER.trace("\tmq.mqmd.backout.count: {}", message.backoutCount);
            in.setHeader("mq.mqmd.backout.count", message.backoutCount);
            LOGGER.trace("\tmq.mqmd.report: {}", message.report);
            in.setHeader("mq.mqmd.report", message.report);
            LOGGER.trace("\tmq.mqmd.feedback: {}", message.feedback);
            in.setHeader("mq.mqmd.feedback", message.feedback);
            LOGGER.trace("\tmq.mqmd.original.length: {}", message.originalLength);
            in.setHeader("mq.mqmd.original.length", message.originalLength);
            LOGGER.trace("\tmq.mqmd.appl.type: {}", message.putApplicationType);
            in.setHeader("mq.mqmd.appl.type", message.putApplicationType);
            LOGGER.trace("\tmq.mqmd.appl.id.data: {}", message.applicationIdData);
            in.setHeader("mq.mqmd.appl.id.data", message.applicationIdData);
            LOGGER.trace("\tmq.mqmd.appl.origin.data: {}", message.applicationOriginData);
            in.setHeader("mq.mqmd.appl.origin.data", message.applicationOriginData);
            LOGGER.trace("\tmq.mqmd.id: {}", message.messageId);
            in.setHeader("mq.mqmd.id", message.messageId);
            LOGGER.trace("\tmq.mqmd.offset: {}", message.offset);
            in.setHeader("mq.mqmd.offset", message.offset);
            LOGGER.trace("\tmq.mqmd.flags: {}", message.messageFlags);
            in.setHeader("mq.mqmd.flags", message.messageFlags);
            LOGGER.trace("\tmq.mqmd.length.total: {}", message.getTotalMessageLength());
            in.setHeader("mq.mqmd.length.total", message.getTotalMessageLength());
            LOGGER.trace("\tmq.mqmd.length.data: {}", message.getDataLength());
            in.setHeader("mq.mqmd.length.data", message.getDataLength());
            LOGGER.trace("\tmq.mqmd.encoding: {}", message.encoding);
            in.setHeader("mq.mqmd.encoding", message.encoding);

            MQHeaderList headerList = this.mqHeaderListFactory.apply(message);
            // TODO MQRFH, MQCIH, MQDLH, MQIIH, MQRMH, MQSAPH, MQWIH, MQXQH, MQDH, MQEPH headers support
            int index = headerList.indexOf("MQRFH2");
            if (index >= 0) {
                LOGGER.trace("MQRFH2 header detected (index {})", index);
                MQRFH2 rfh = (MQRFH2) headerList.get(index);
                LOGGER.trace("\tmq.rfh2.format: {}", rfh.getFormat());
                in.setHeader("mq.rfh2.format", rfh.getFormat());
                LOGGER.trace("\tmq.rfh2.struct.id: {}", rfh.getStrucId());
                in.setHeader("mq.rfh2.struct.id", rfh.getStrucId());
                LOGGER.trace("\tmq.rfh2.encoding: {}", rfh.getEncoding());
                in.setHeader("mq.rfh2.encoding", rfh.getEncoding());
                LOGGER.trace("\tmq.rfh2.coded.charset.id: {}", rfh.getCodedCharSetId());
                in.setHeader("mq.rfh2.coded.charset.id", rfh.getCodedCharSetId());
                LOGGER.trace("\tmq.rfh2.flags: {}", rfh.getFlags());
                in.setHeader("mq.rfh2.flags", rfh.getFlags());
                LOGGER.trace("\tmq.rfh2.version: {}", rfh.getVersion());
                in.setHeader("mq.rfh2.version", rfh.getVersion());
                MQRFH2.Element[] folders = rfh.getFolders();
                for (MQRFH2.Element folder : folders) {
                    String folderXML = folder.toXML();
                    LOGGER.trace("mq.rfh2.folder {} : {}", folder.getName(), folderXML);
                    in.setHeader("mq.rfh2.folder." + folder.getName(), folderXML);
                }
            }

            LOGGER.trace("Reading body");
            byte[] buffer = new byte[message.getDataLength()];
            message.readFully(buffer);
            saveBody(in, buffer);

            getProcessor().process(exchange);
        } catch (Exception e) {
            LOGGER.error("Error occurred while consuming message, waiting for {}", getEndpoint().getDelayOnException(), e);
            Thread.sleep(getEndpoint().getDelayOnException());
            exchange.setException(e);
        } finally {
            if (destination != null)
                destination.close();
        }

        if (exchange.getException() != null) {
            getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
        }

        return 1;
    }

    private void saveBody(Message in, byte[] buffer) throws Exception {
        if ("bytes".equals(getEndpoint().getBodyType())) {
            in.setBody(ByteBuffer.wrap(buffer), ByteBuffer.class);
        } else {
            String body;
            if (getEndpoint().getBodyType() != null) {
                body = new String(buffer, getEndpoint().getBodyType());
            } else {
                body = new String(buffer);
            }
            in.setBody(body, String.class);
        }
    }


    @Override
    public WMQEndpoint getEndpoint() {
        return (WMQEndpoint) super.getEndpoint();
    }

    private static MQHeaderList createMqHeaderList(MQMessage mqMessage) {
        try {
            return new MQHeaderList(mqMessage);
        } catch (IOException | MQDataException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodError e) {
            throw new IllegalStateException("This error can happen when a specific IBM class is not on the classpath (com.ibm.mq.headers.internal.MQMessageWrapper). If you don't add it, constructor throws NoSucheMethodError. Message found : " + e.getMessage(), e);
        }

    }
}

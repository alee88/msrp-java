/*
 * Copyright � Jo�o Antunes 2008 This file is part of MSRP Java Stack.
 * 
 * MSRP Java Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * MSRP Java Stack is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MSRP Java Stack. If not, see <http://www.gnu.org/licenses/>.
 */
package javax.net.msrp;

import static org.junit.Assert.*;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import javax.net.msrp.DataContainer;
import javax.net.msrp.MemoryDataContainer;
import javax.net.msrp.utils.TextUtils;


import org.junit.Test;

/**
 * 
 * Simple test used for debugging purposes to send a very small (2KB message)
 * 
 * @author Jo�o Andr� Pereira Antunes
 * 
 */
public class TestSendingSmallMessages extends TestFrame
{
    /**
     * Tests sending a 2KB Text Message with a MemoryDataContainer to a
     * MemoryDataContainer
     */
    @Test
    public void test2KbTxtMsgMem2Mem()
    {
        byte[] data = new byte[2 * 1024];
        fillText(data);

        byte[] receivedData = memory2Memory(data, false);

        assertArrayEquals(data, receivedData);
    }

    /**
     * Tests sending a 3KB Binary Message with a MemoryDataContainer to a
     * MemoryDataContainer
     */
    @Test
    public void test3KbBinMsgMem2Mem()
    {
        byte[] data = new byte[3 * 1024];
        fillBinary(data);

        byte[] receivedData = memory2Memory(data, false);
        /*
         * assert that the received data matches the data sent.
         */
        assertArrayEquals(data, receivedData);
    }

    /**
     * Tests sending a Text Message on a 2048 boundary
     */
    @Test
    public void testBoundaryMsg()
    {
        byte[] data = new byte[1800];
        fillText(data);

        byte[] receivedData = memory2Memory(data, false);

        assertArrayEquals(data, receivedData);
    }

    /**
     * Test sending Text Data from receiving to sending session.
     * Also tests the sending of a first alive-message (== connection complete
     * but nothing to send yet -> active session will start with a bodiless SEND).
     */
    @Test
    public void testReverseSending()
    {
        try
        {
        	byte[] space = new byte[] {' '};
            byte[] smallData = new byte[2 * 1024];
            TextUtils.generateRandom(smallData);

            /* connect the two sessions: */
            ArrayList<URI> toPathSendSession = new ArrayList<URI>();
            toPathSendSession.add(receivingSession.getURI());
            sendingSession.setToPath(toPathSendSession);
            sendingSession.sendMessage("text/plain", space);

            /*
             * message should be transferred or in the process of being
             * completely transferred
             */
            DataContainer dc = new MemoryDataContainer(1);
            receivingSessionListener.setDataContainer(dc);
            receivingSessionListener.setAcceptHookResult(new Boolean(true));
            receivingSessionListener.triggerReception();

            OutgoingMessage twoKbMessage =
                    new OutgoingMessage("plain/text", smallData);
            twoKbMessage.setSuccessReport(false);
            receivingSession.sendMessage(twoKbMessage);

            dc = new MemoryDataContainer(smallData.length);
            sendingSessionListener.setDataContainer(dc);
            sendingSessionListener.setAcceptHookResult(true);
            sendingSessionListener.triggerReception();

            if (sendingSessionListener.getAcceptHookMessage() == null ||
                sendingSessionListener.getAcceptHookSession() == null)
                    fail("The Mock didn't work, message not accepted");

            ByteBuffer receivedByteBuffer =
                sendingSessionListener.getReceiveMessage().getDataContainer()
                    .get(0, 0);
            byte[] receivedData = receivedByteBuffer.array();

            assertArrayEquals(smallData, receivedData);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testWrappedSending()
    {
        try
        {
            byte[] smallData = "Hello world".getBytes(TextUtils.utf8);

            ArrayList<URI> toPathSendSession = new ArrayList<URI>();
            toPathSendSession.add(receivingSession.getURI());
            Message m = sendingSession.sendWrappedMessage(
            		"message/cpim", "from", "to", "text/plain", smallData);
            sendingSession.setToPath(toPathSendSession);

            DataContainer dc = new MemoryDataContainer((int) m.size);
            receivingSessionListener.setDataContainer(dc);
            receivingSessionListener.setAcceptHookResult(new Boolean(true));
            receivingSessionListener.triggerReception();

            if (receivingSessionListener.getAcceptHookMessage() == null ||
			    receivingSessionListener.getAcceptHookSession() == null)
    			    fail("The Mock didn't work, message not accepted");

    		m = receivingSessionListener.getReceiveMessage();

            assertArrayEquals(smallData, m.getContent().getBytes(TextUtils.utf8));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testNicknameSending()
    {
        try
        {
            ArrayList<URI> toPathSendSession = new ArrayList<URI>();
            toPathSendSession.add(receivingSession.getURI());
            sendingSession.requestNickname("Hairy Scary");
            sendingSession.setToPath(toPathSendSession);

            synchronized (receivingSessionListener)
            {
                receivingSessionListener.wait();
            }
    		assertEquals(receivingSessionListener.getNickname(), "Hairy Scary");
    		receivingSession.sendNickResult(
    				receivingSessionListener.getReceivedNicknameTransaction());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.grizzly.http.io;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Grizzly team
 */
public class InputBufferTest {
    /**
     * GRIZZLY-1742
     * @throws Throwable 
     */
    @Test
    public void testBinaryMarkResetOnSimpleToComposite() throws Throwable {
        TCPNIOTransport dummyTransport = TCPNIOTransportBuilder.newInstance().build();
        TCPNIOConnection dummyConnection = new TCPNIOConnection(dummyTransport, null);
        FilterChainContext dummyFcc = FilterChainContext.create(dummyConnection);
        
        final HttpHeader httpHeader = HttpRequestPacket.builder()
                .method(Method.POST)
                .uri("/")
                .protocol(Protocol.HTTP_1_1)
                .host("localhost:8080")
                .contentLength(7)
                .build();
        
        final HttpContent emptyContent = HttpContent.builder(httpHeader)
                .content(Buffers.EMPTY_BUFFER)
                .build();
        
        dummyFcc.setMessage(emptyContent);
        
        final InputBuffer ib = new InputBuffer();
        ib.initialize(httpHeader, dummyFcc);
        
        ib.append(emptyContent);
        
        ib.mark(1);
        
        Buffer payload = Buffers.wrap(
                MemoryManager.DEFAULT_MEMORY_MANAGER, "JunkJunkJunkPayload");
        payload.position(payload.limit() - "Payload".length()); // make 'Payload' visible
        
        final HttpContent payloadContent = HttpContent.builder(httpHeader)
                .content(payload)
                .build();
        ib.append(payloadContent);
        
        assertEquals('P', (char) ib.readByte()); // first payload byte
        
        ib.reset();
        
        assertEquals('P', (char) ib.readByte()); // first payload byte
    }
}

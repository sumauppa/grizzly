/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
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

package org.glassfish.grizzly.http.util;

import java.io.IOException;
import java.io.Serializable;

/**
 * Utilities to manipluate char chunks. While String is
 * the easiest way to manipulate chars ( search, substrings, etc),
 * it is known to not be the most efficient solution - Strings are
 * designed as imutable and secure objects.
 *
 * @author dac@sun.com
 * @author James Todd [gonzo@sun.com]
 * @author Costin Manolache
 * @author Remy Maucherat
 */
public final class CharChunk implements Chunk, Cloneable, Serializable {

    // Input interface, used when the buffer is emptied.
    public interface CharInputChannel {
        /**
         * Read new bytes ( usually the internal conversion buffer ).
         * The implementation is allowed to ignore the parameters,
         * and mutate the chunk if it wishes to implement its own buffering.
         */
        int realReadChars(char cbuf[], int off, int len) throws IOException;
    }
    /**
     *  When we need more space we'll either
     *  grow the buffer ( up to the limit ) or send it to a channel.
     */
    public interface CharOutputChannel {
        /** Send the bytes ( usually the internal conversion buffer ).
         *  Expect 8k output if the buffer is full.
         */
        void realWriteChars(char cbuf[], int off, int len) throws IOException;
    }

    // --------------------
    // char[]
    private char buff[];

    private int start;
    private int end;

    private boolean isSet=false;  // XXX

    private boolean isOutput=false;

    // -1: grow undefinitely
    // maximum amount to be cached
    private int limit=-1;

    private transient CharInputChannel in = null;
    private transient CharOutputChannel out = null;

    private boolean optimizedWrite=true;

    /**
     * Creates a new, uninitialized CharChunk object.
     */
    public CharChunk() {
    }

    public CharChunk(int size) {
        allocate( size, -1 );
    }

    // --------------------

    public CharChunk getClone() {
        try {
            return (CharChunk)this.clone();
        } catch( Exception ex) {
            return null;
        }
    }

    public boolean isNull() {
        return end <= 0 && !isSet;
    }

    /**
     * Resets the message bytes to an uninitialized state.
     */
    public void recycle() {
        //	buff=null;
        isSet=false; // XXX
        start=0;
        end=0;
    }

    public void reset() {
        buff=null;
    }

    // -------------------- Setup --------------------

    public void allocate( int initial, int limit  ) {
        isOutput=true;
        if( buff==null || buff.length < initial ) {
            buff=new char[initial];
        }
        this.limit=limit;
        start=0;
        end=0;
        isOutput=true;
        isSet=true;
    }


    public void setOptimizedWrite(boolean optimizedWrite) {
        this.optimizedWrite = optimizedWrite;
    }

    public void setChars( char[] c, int off, int len ) {
        buff=c;
        start=off;
        end=start + len;
        isSet=true;
    }

    /** Maximum amount of data in this buffer.
     *
     *  If -1 or not set, the buffer will grow indefinitely.
     *  Can be smaller than the current buffer size ( which will not shrink ).
     *  When the limit is reached, the buffer will be flushed ( if out is set )
     *  or throw exception.
     */
    public void setLimit(int limit) {
        this.limit=limit;
    }

    public int getLimit() {
        return limit;
    }

    /**
     * When the buffer is empty, read the data from the input channel.
     */
    public void setCharInputChannel(CharInputChannel in) {
        this.in = in;
    }

    /** When the buffer is full, write the data to the output channel.
     * 	Also used when large amount of data is appended.
     *
     *  If not set, the buffer will grow to the limit.
     */
    public void setCharOutputChannel(CharOutputChannel out) {
        this.out=out;
    }

    // compat
    public char[] getChars() {
        return getBuffer();
    }

    public char[] getBuffer() {
        return buff;
    }

    /**
     * Returns the start offset of the bytes.
     * For output this is the end of the buffer.
     */
    @Override
    public int getStart() {
        return start;
    }

    /**
     * Returns the start offset of the bytes.
     */
    @Override
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Returns the length of the bytes.
     */
    public int getLength() {
        return end-start;
    }


    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public void setEnd( int i ) {
        end=i;
    }

    // -------------------- Adding data --------------------

    public void append( char b ) throws IOException {
        makeSpace( 1 );

        // couldn't make space
        if( limit >0 && end >= limit ) {
            flushBuffer();
        }
        buff[end++]=b;
    }

    public void append( CharChunk src ) throws IOException {
        append( src.getBuffer(), src.getStart(), src.getLength());
    }

    /** Add data to the buffer
     */
    public void append( char src[], int off, int len ) throws IOException {
        // will grow, up to limit
        makeSpace( len );

        // if we don't have limit: makeSpace can grow as it wants
        if( limit < 0 ) {
            // assert: makeSpace made enough space
            System.arraycopy( src, off, buff, end, len );
            end+=len;
            return;
        }

        // Optimize on a common case.
        // If the source is going to fill up all the space in buffer, may
        // as well write it directly to the output, and avoid an extra copy
        if ( optimizedWrite && len == limit && end == start) {
            out.realWriteChars( src, off, len );
            return;
        }

        // if we have limit and we're below
        if( len <= limit - end ) {
            // makeSpace will grow the buffer to the limit,
            // so we have space
            System.arraycopy( src, off, buff, end, len );

            end+=len;
            return;
        }

        // need more space than we can afford, need to flush
        // buffer

        // the buffer is already at ( or bigger than ) limit

        // Optimization:
        // If len-avail < length ( i.e. after we fill the buffer with
        // what we can, the remaining will fit in the buffer ) we'll just
        // copy the first part, flush, then copy the second part - 1 write
        // and still have some space for more. We'll still have 2 writes, but
        // we write more on the first.

        if( len + end < 2 * limit ) {
            /* If the request length exceeds the size of the output buffer,
               flush the output buffer and then write the data directly.
               We can't avoid 2 writes, but we can write more on the second
            */
            int avail=limit-end;
            System.arraycopy(src, off, buff, end, avail);
            end += avail;

            flushBuffer();

            System.arraycopy(src, off+avail, buff, end, len - avail);
            end+= len - avail;

        } else {	// len > buf.length + avail
            // long write - flush the buffer and write the rest
            // directly from source
            flushBuffer();

            out.realWriteChars( src, off, len );
        }
    }


    /** Add data to the buffer
     */
    public void append( StringBuffer sb ) throws IOException {
        int len=sb.length();

        // will grow, up to limit
        makeSpace( len );

        // if we don't have limit: makeSpace can grow as it wants
        if( limit < 0 ) {
            // assert: makeSpace made enough space
            sb.getChars(0, len, buff, end );
            end+=len;
            return;
        }

        int off=0;
        int sbOff = off;
        int sbEnd = off + len;
        while (sbOff < sbEnd) {
            int d = min(limit - end, sbEnd - sbOff);
            sb.getChars( sbOff, sbOff+d, buff, end);
            sbOff += d;
            end += d;
            if (end >= limit)
            flushBuffer();
        }
    }

    /** Append a string to the buffer
     */
    public void append(String s) throws IOException {
        append(s, 0, s.length());
    }

    /** Append a string to the buffer
     */
    public void append(String s, int off, int len) throws IOException {
        if (s==null) return;

        // will grow, up to limit
        makeSpace( len );

        // if we don't have limit: makeSpace can grow as it wants
        if( limit < 0 ) {
            // assert: makeSpace made enough space
            s.getChars(off, off+len, buff, end );
            end+=len;
            return;
        }

        int sOff = off;
        int sEnd = off + len;
        while (sOff < sEnd) {
            int d = min(limit - end, sEnd - sOff);
            s.getChars( sOff, sOff+d, buff, end);
            sOff += d;
            end += d;
            if (end >= limit)
            flushBuffer();
        }
    }

    // -------------------- Removing data from the buffer --------------------
    @Override
    public void delete(final int start, final int end) {
        final int diff = this.end - end;
        if (diff == 0) {
            this.end = start;
        } else {
            System.arraycopy(buff, end, buff, start, diff);
            this.end = start + diff;
        }
    }

    public int substract()
        throws IOException {

        if ((end - start) == 0) {
            if (in == null)
                return -1;
            int n = in.realReadChars(buff, end, buff.length - end);
            if (n < 0)
                return -1;
        }

        return (buff[start++]);

    }

    public int substract(CharChunk src)
        throws IOException {

        if ((end - start) == 0) {
            if (in == null)
                return -1;
            int n = in.realReadChars( buff, end, buff.length - end);
            if (n < 0)
                return -1;
        }

        int len = getLength();
        src.append(buff, start, len);
        start = end;
        return len;

    }

    public int substract( char src[], int off, int len )
        throws IOException {

        if ((end - start) == 0) {
            if (in == null)
                return -1;
            int n = in.realReadChars( buff, end, buff.length - end);
            if (n < 0)
                return -1;
        }

        int n = len;
        if (len > getLength()) {
            n = getLength();
        }
        System.arraycopy(buff, start, src, off, n);
        start += n;
        return n;

    }


    public void flushBuffer() throws IOException {
        //assert out!=null
        if( out==null ) {
            throw new IOException( "Buffer overflow, no sink " + limit + " " +
                       buff.length  );
        }
        out.realWriteChars( buff, start, end - start );
        end=start;
    }

    /** Make space for len chars. If len is small, allocate
     *	a reserve space too. Never grow bigger than limit.
     */
    void makeSpace(int count) {
        char[] tmp = null;

        int newSize;
        int desiredSize=end + count;

        // Can't grow above the limit
        if( limit > 0 &&
            desiredSize > limit) {
            desiredSize=limit;
        }

        if( buff==null ) {
            if( desiredSize < 256 ) desiredSize=256; // take a minimum
            buff=new char[desiredSize];
        }

        // limit < buf.length ( the buffer is already big )
        // or we already have space XXX
        if( desiredSize <= buff.length) {
            return;
        }
        // grow in larger chunks
        if( desiredSize < 2 * buff.length ) {
            newSize= buff.length * 2;
            if( limit >0 &&
            newSize > limit ) newSize=limit;
            tmp=new char[newSize];
        } else {
            newSize= buff.length * 2 + count ;
            if( limit > 0 &&
            newSize > limit ) newSize=limit;
            tmp=new char[newSize];
        }

        System.arraycopy(buff, start, tmp, start, end-start);
        buff = tmp;
        tmp = null;
    }

    // -------------------- Conversion and getters --------------------

    @Override
    public String toString() {
        if (null == buff) {
            return null;
        } else if (end-start == 0) {
            return "";
        }
        return StringCache.toString(this);
    }

    @Override
    public String toString(int start, int end) {
        if (null == buff) {
            return null;
        } else if (end - start == 0) {
            return "";
        }

        return new String(buff, start, end - start);
    }

    public String toStringInternal() {
        return new String(buff, start, end - start);
    }

    public int getInt() {
	return Ascii.parseInt(buff, start, end-start);
    }

    // -------------------- equals --------------------

    /**
     * Compares the message bytes to the specified String object.
     * @param s the String to compare
     * @return true if the comparison succeeded, false otherwise
     */
    public boolean equals(CharSequence s) {
        char[] c = buff;
        int len = end-start;
        if (c == null || len != s.length()) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (c[off++] != s.charAt(i)) {
            return false;
            }
        }
        return true;
    }

    /**
     * Compares the message bytes to the specified String object.
     * @param s the String to compare
     * @return true if the comparison succeeded, false otherwise
     */
    public boolean equalsIgnoreCase(CharSequence s) {
        char[] c = buff;
        int len = end-start;
        if (c == null || len != s.length()) {
            return false;
        }
        int off = start;
        for (int i = 0; i < len; i++) {
            if (Ascii.toLower( c[off++] ) != Ascii.toLower( s.charAt(i))) {
            return false;
            }
        }
        return true;
    }

    public boolean equals(CharChunk cc) {
        return equals( cc.getChars(), cc.getStart(), cc.getLength());
    }

    public boolean equals(char b2[], int off2, int len2) {
        char b1[]=buff;
        if( b1==null && b2==null ) return true;

        if (b1== null || b2==null || end-start != len2) {
            return false;
        }
        int off1 = start;
        int len=end-start;
        while ( len-- > 0) {
            if (b1[off1++] != b2[off2++]) {
            return false;
            }
        }
        return true;
    }

    public boolean equals(byte b2[], int off2, int len2) {
        char b1[]=buff;
        if( b2==null && b1==null ) return true;

        if (b1== null || b2==null || end-start != len2) {
            return false;
        }
        int off1 = start;
        int len=end-start;

        while ( len-- > 0) {
            if ( b1[off1++] != (char)b2[off2++]) {
            return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the message bytes starts with the specified string.
     * @param s the string
     */
    public boolean startsWith(String s) {
        return startsWith(s, 0);
    }

    boolean startsWith(final String s, final int pos) {
        char[] c = buff;
        int len = s.length();
//        if (c == null || len + pos > end) {
        if (c == null || len + pos > end - start) {
            return false;
        }
        int off = start + pos;
        for (int i = 0; i < len; i++) {
            if (c[off++] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the message bytes starts with the specified string.
     * @param s the string
     */
    public boolean startsWithIgnoreCase(final String s, final int pos) {
        char[] c = buff;
        int len = s.length();
        if (c == null || len + pos > end - start) {
            return false;
        }
        int off = start + pos;
        for (int i = 0; i < len; i++) {
            if (Ascii.toLower(c[off++]) != Ascii.toLower(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    // -------------------- Hash code  --------------------

    // normal hash.
    public int hash() {
        int code=0;
        for (int i = start; i < start + end-start; i++) {
            code = code * 37 + buff[i];
        }
        return code;
    }

    // hash ignoring case
    public int hashIgnoreCase() {
        int code=0;
        for (int i = start; i < end; i++) {
            code = code * 37 + Ascii.toLower(buff[i]);
        }
        return code;
    }

    public int indexOf(char c) {
        return indexOf( c, start);
    }

    /**
     * Returns true if the message bytes starts with the specified string.
     * @param c the character
     */
    @Override
    public int indexOf(char c, int starting) {
        int ret = indexOf( buff, start+starting, end, c );
        return (ret >= start) ? ret - start : -1;
    }

    public static int indexOf( char chars[], int off, int cend, char qq ) {
        while( off < cend ) {
            char b=chars[off];
            if( b==qq )
            return off;
            off++;
        }
        return -1;
    }


    @Override
    public final int indexOf(String s, int fromIndex) {
        return indexOf(s, 0, s.length(), start);
    }

    public final int indexOf(String src, int srcOff, int srcLen, int myOff) {
        char first = src.charAt(srcOff);

        // Look for first char
        int srcEnd = srcOff + srcLen;

        for (int i = myOff + start; i <= (end - srcLen); i++) {
            if (buff[i] != first) {
                continue;
            }
            // found first char, now look for a match
            int myPos = i + 1;
            for (int srcPos = srcOff + 1; srcPos < srcEnd;) {
                if (buff[myPos++] != src.charAt(srcPos++)) {
                    break;
                }
                if (srcPos == srcEnd) {
                    return i - start; // found it
                }
            }
        }
        
        return -1;
    }

    // -------------------- utils
    private int min(int a, int b) {
        if (a < b) return a;
        return b;
    }

}

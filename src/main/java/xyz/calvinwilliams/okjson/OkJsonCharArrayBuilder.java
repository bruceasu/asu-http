package xyz.calvinwilliams.okjson;

class OkJsonCharArrayBuilder {

    public char[] buf;
    public int    bufSize;
    public int    bufLength;

    final private static String TABS = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

    public OkJsonCharArrayBuilder()
    {
        this(16);
    }

    public OkJsonCharArrayBuilder(int initBufSize)
    {
        this.buf = new char[initBufSize];
        this.bufSize = initBufSize;
        this.bufLength = 0;
    }

    private void resize(int newSize)
    {
        char[] newBuf;
        int    newBufSize;

        if (bufSize < 10240240) {
            newBufSize = bufSize * 2;
        } else {
            newBufSize = bufSize + 10240240;
        }
        if (newBufSize < newSize) {
            newBufSize = newSize;
        }
        newBuf = new char[newBufSize];
        System.arraycopy(buf, 0, newBuf, 0, bufLength);
        buf = newBuf;
        bufSize = newBufSize;
    }

    public OkJsonCharArrayBuilder appendChar(char c)
    {
        int newBufLength = bufLength + 1;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = c;
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendCharArray(char[] charArray)
    {
        int newBufLength = bufLength + charArray.length;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        System.arraycopy(charArray, 0, buf, bufLength, charArray.length);
        bufLength = newBufLength;

        return this;
    }

    public OkJsonCharArrayBuilder appendCharArrayWith3(char[] charArray)
    {
        int newBufLength = bufLength + 3;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = charArray[0];
        bufLength++;
        buf[bufLength] = charArray[1];
        bufLength++;
        buf[bufLength] = charArray[2];
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendCharArrayWith4(char[] charArray)
    {
        int newBufLength = bufLength + 4;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = charArray[0];
        bufLength++;
        buf[bufLength] = charArray[1];
        bufLength++;
        buf[bufLength] = charArray[2];
        bufLength++;
        buf[bufLength] = charArray[3];
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendString(String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + strLength;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        str.getChars(0, strLength, buf, bufLength);
        bufLength = newBufLength;

        return this;
    }

    public OkJsonCharArrayBuilder appendBytesFromOffsetWithLength(char[] charArray,
                                                                  int offset,
                                                                  int len)
    {
        int newBufLength = bufLength + len;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        System.arraycopy(charArray, offset, buf, bufLength, len);
        bufLength = newBufLength;

        return this;
    }

    public OkJsonCharArrayBuilder appendTabs(int tabCount)
    {
        int newBufLength = bufLength + tabCount;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        if (tabCount <= TABS.length()) {
            System.arraycopy(TABS.toCharArray(), 0, buf, bufLength, tabCount);
            bufLength += tabCount;
        } else {
            for (int i = 1; i < tabCount; i++) {
                buf[bufLength] = '\t';
                bufLength++;
            }
        }

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndOpenByte(char[] name, char c)
    {
        int newBufLength = bufLength + name.length + 4;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        buf[bufLength] = c;
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndOpenBytePretty(char[] name, char c)
    {
        int newBufLength = bufLength + name.length + 7;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        buf[bufLength] = c;
        bufLength++;
        buf[bufLength] = '\n';
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendCloseByte(char c)
    {
        int newBufLength = bufLength + 1;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = c;
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndCharArray(char[] name, char[] str)
    {
        int newBufLength = bufLength + name.length + str.length + 3;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        System.arraycopy(str, 0, buf, bufLength, str.length);
        bufLength += str.length;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndCharArrayPretty(char[] name, char[] str)
    {
        int newBufLength = bufLength + name.length + str.length + 5;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        System.arraycopy(str, 0, buf, bufLength, str.length);
        bufLength += str.length;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndString(char[] name, String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + name.length + strLength + 3;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        str.getChars(0, strLength, buf, bufLength);
        bufLength += strLength;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndStringPretty(char[] name, String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + name.length + strLength + 5;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        str.getChars(0, strLength, buf, bufLength);
        bufLength += strLength;
        // buf[bufLength] = ' ' ; bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndQmStringQm(char[] name, String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + name.length + strLength + 5;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        buf[bufLength] = '"';
        bufLength++;
        str.getChars(0, strLength, buf, bufLength);
        bufLength += strLength;
        buf[bufLength] = '"';
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonNameAndColonAndQmStringQmPretty(char[] name, String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + name.length + strLength + 7;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        System.arraycopy(name, 0, buf, bufLength, name.length);
        bufLength += name.length;
        buf[bufLength] = '"';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        buf[bufLength] = ':';
        bufLength++;
        buf[bufLength] = ' ';
        bufLength++;
        buf[bufLength] = '"';
        bufLength++;
        str.getChars(0, strLength, buf, bufLength);
        bufLength += strLength;
        buf[bufLength] = '"';
        bufLength++;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonString(String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + strLength;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        str.getChars(0, strLength, buf, bufLength);
        bufLength += strLength;

        return this;
    }

    public OkJsonCharArrayBuilder appendJsonQmStringQm(String str)
    {
        int strLength    = str.length();
        int newBufLength = bufLength + strLength + 2;

        if (newBufLength > bufSize) {
            resize(newBufLength);
        }

        buf[bufLength] = '"';
        bufLength++;
        str.getChars(0, strLength, buf, bufLength);
        bufLength += strLength;
        buf[bufLength] = '"';
        bufLength++;

        return this;
    }

    public int getLength()
    {
        return bufLength;
    }

    public void setLength(int length)
    {
        bufLength = length;
    }

    @Override
    public String toString()
    {
        return new String(buf, 0, bufLength);
    }
}
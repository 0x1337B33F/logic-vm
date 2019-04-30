package test.logicscript.bytecode.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import logicbyte.bytecode.stream.NeutralDecoderInputStream;
import logicbyte.bytecode.stream.NeutralEncoderOutputStream;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lochie
 */
public class NeutralEncoderDecoderTests {

    static class NeutralEncoderOutputStreamAccess extends NeutralEncoderOutputStream {

	public NeutralEncoderOutputStreamAccess(OutputStream o) {
	    super(o);
	}

	void writeStringProxy(String s) throws IOException {
	    super.writeString(s);
	}

	void writePackedIntProxy(int i) throws IOException {
	    super.writePackedInt(i);
	}

	void writePackedLongProxy(long l) throws IOException {
	    super.writePackedLong(l);
	}
    }

    static class NeutralDecoderInputStreamAccess extends NeutralDecoderInputStream {

	public NeutralDecoderInputStreamAccess(InputStream i) {
	    super(i);
	}

	String readStringProxy() throws IOException {
	    return super.readString();
	}

	int readPackedIntProxy() throws IOException {
	    return super.readPackedInt();
	}

	long readPackedLongProxy() throws IOException {
	    return super.readPackedLong();
	}

    }

    public ByteArrayInputStream inputByteObj;
    public ByteArrayOutputStream outputByteObj;

    public NeutralEncoderOutputStreamAccess outputTestObj;
    public NeutralDecoderInputStreamAccess inputTestObj;

    @Test
    public void testStringEncodeDecode() {
	try {
	    outputTestObj = new NeutralEncoderOutputStreamAccess(outputByteObj = new ByteArrayOutputStream());

	    for (int i = 0; i < 3; i++)
		outputTestObj.writeStringProxy("test non-unicode string value");

	    outputTestObj.closeWrapper();
	    outputTestObj = null;

	    byte[] data = outputByteObj.toByteArray();
	    Assert.assertNotEquals("no data written to buffer", 0, data.length);

	    outputByteObj.close();
	    outputByteObj = null;

	    inputTestObj = new NeutralDecoderInputStreamAccess(inputByteObj = new ByteArrayInputStream(data));

	    for (int i = 0; i < 3; i++) {
		String val = inputTestObj.readStringProxy();
		Assert.assertEquals("incorrect string read", "test non-unicode string value", val);
	    }

	    inputTestObj = null;
	    inputByteObj.close();
	    inputByteObj = null;

	} catch (IOException e) {
	    Assert.assertNull("expected no exception", e);
	}
    }

    @Test
    public void testPackedLongEncodeDecode() {
	try {
	    outputTestObj = new NeutralEncoderOutputStreamAccess(outputByteObj = new ByteArrayOutputStream());

	    // write some numbers which fit in the first byte only
	    for (int i = 0; i < 256; i++)
		outputTestObj.writePackedIntProxy(i);

	    // write some numbers which span the edge of the short boundary
	    // these should be packed as 2 bytes total
	    for (int i = Short.MAX_VALUE; i < Short.MAX_VALUE + 256; i++)
		outputTestObj.writePackedIntProxy(i);

	    // write some numbers which use the high-byte(s) of the int
	    // these should be packed as 4 bytes total
	    for (int i = 0x00FFFFFF; i < 0xEFFFFFFF; i++)
		outputTestObj.writePackedIntProxy(i);

	    for (long i = Integer.MAX_VALUE; i < Integer.MAX_VALUE + 4096; i++)
		outputTestObj.writePackedLongProxy(i);

	    for (long i = Long.MAX_VALUE - 4096L; i < Long.MAX_VALUE; i++)
		outputTestObj.writePackedLongProxy(i);

	    for (long i = Long.MIN_VALUE + 8192L; i > Long.MIN_VALUE; i--)
		outputTestObj.writePackedLongProxy(i);

	    outputTestObj.writePackedIntProxy(Integer.MIN_VALUE);
	    outputTestObj.writePackedIntProxy(Integer.MAX_VALUE);
	    outputTestObj.writePackedLongProxy(Long.MIN_VALUE);
	    outputTestObj.writePackedLongProxy(Long.MAX_VALUE);
	    
	    outputTestObj.writePackedLongProxy( -1L );

	    outputTestObj.closeWrapper();
	    outputTestObj = null;

	    byte[] data = outputByteObj.toByteArray();
	    Assert.assertNotEquals("no data written to buffer", 0, data.length);

	    outputByteObj.close();
	    outputByteObj = null;

	    inputTestObj = new NeutralDecoderInputStreamAccess(inputByteObj = new ByteArrayInputStream(data));

	    for (int i = 0; i < 256; i++) {
		int val = inputTestObj.readPackedIntProxy();
		Assert.assertEquals("single byte integer read failure", i, val);
	    }

	    for (int i = Short.MAX_VALUE; i < Short.MAX_VALUE + 256; i++) {
		int val = inputTestObj.readPackedIntProxy();
		Assert.assertEquals("multi-word integer read failure", i, val);
	    }

	    for (int i = 0x00FFFFFF; i < 0xEFFFFFFF; i++) {
		int val = inputTestObj.readPackedIntProxy();
		Assert.assertEquals("long-word integer read failure", i, val);
	    }

	    for (long i = Integer.MAX_VALUE; i < Integer.MAX_VALUE + 4096; i++) {
		long val = inputTestObj.readPackedLongProxy();
		Assert.assertEquals("long read failure", i, val);
	    }

	    for (long i = Long.MAX_VALUE - 4096L; i < Long.MAX_VALUE; i++) {
		long val = inputTestObj.readPackedLongProxy();
		Assert.assertEquals("long read failure", i, val);
	    }

	    for (long i = Long.MIN_VALUE + 8192L; i > Long.MIN_VALUE; i--) {
		long val = inputTestObj.readPackedLongProxy();
		Assert.assertEquals("negative long read failure", i, val);
	    }

	    Assert.assertEquals("INTEGER minimum value", Integer.MIN_VALUE, inputTestObj.readPackedIntProxy());
	    Assert.assertEquals("INTEGER maximum value", Integer.MAX_VALUE, inputTestObj.readPackedIntProxy());
	    Assert.assertEquals("LONG minimum value", Long.MIN_VALUE, inputTestObj.readPackedLongProxy());
	    Assert.assertEquals("LONG maximum value", Long.MAX_VALUE, inputTestObj.readPackedLongProxy());
	    
	    Assert.assertEquals("-1L value", -1L, inputTestObj.readPackedLongProxy());

	    inputTestObj = null;
	    inputByteObj.close();
	    inputByteObj = null;

	} catch (IOException e) {
	    Assert.assertNull("expected no exception", e);
	}

    }

}

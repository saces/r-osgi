package ch.ethz.iks.r_osgi.test.service.impl;

import java.util.Arrays;

import ch.ethz.iks.r_osgi.test.service.ArgumentTestService;

public class ArgumentTestServiceImpl implements ArgumentTestService {

	public void byte0(byte b) {

	}

	public void byte1(byte[] array) {

	}

	public void byte3(byte[][][] array) {

	}

	public byte byteObj0(Byte b) {
		return 0;
	}

	public byte[] byteObj1(Byte b) {
		return "This is the return value".getBytes();
	}

	public byte[][][] byteObj3(Byte b) {
		return new byte[][][] { { "abc".getBytes(), "DEF".getBytes() },
				{ "ghi".getBytes(), "This is a test".getBytes() } };
	}

	public void int0(int i) {

	}

	public void int1(int[] array) {

	}

	public void int3(int[][][] array) {

	}

	public int intObj0(int i) {
		return 0;
	}

	public int[] intObj1(int i) {
		int[] result = new int[10000];
		Arrays.fill(result, 42);
		return result;
	}

	public int[][][] intObj3(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	public void none() {

	}

	public void short0(short s) {
		// TODO Auto-generated method stub

	}

	public void short1(short[] array) {
		// TODO Auto-generated method stub

	}

	public void short3(short[][][] array) {
		// TODO Auto-generated method stub

	}

	public short shortObj0(Short s) {
		// TODO Auto-generated method stub
		return 0;
	}

	public short[] shortObj1(Short s) {
		// TODO Auto-generated method stub
		return null;
	}

	public short[][][] shortObj3(Short s) {
		// TODO Auto-generated method stub
		return null;
	}

}

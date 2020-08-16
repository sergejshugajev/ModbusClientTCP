/*
 * 2020-08-12 (c) Sergej Shugajev
 * Modbus Client TCP (all in one class).
 * Simplified code and retained TCP connectivity for Modbus.
 * Version 0.1
 * 
 * P.S.
 *   UPD protocol and others can be implemented
 *   in the method SendPackedWaitResult().
 *
 * MIT License
 * Copyright (c) 2020 Sergej Shugajev
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Quick Start:
 *   ModbusClientTCP mb = new ModbusClientTCP("localhost", 502);
 *   try {
 *     mb.Connect();
 *     if (mb.isConnected()) {
 *       mb.WriteSingleRegister(19, 111);
 *       System.out.println("Read: " + Arrays.toString(mb.ReadHoldingRegisters(18, 3)));
 *     }
 *   } catch (Exception e) {
 *     System.out.println("ERROR! " + e.toString());
 *   }
 *
 * Supported Function Codes:
 *   0x01  Read Coils
 *   0x02  Read Discrete Inputs
 *   0x03  Read Holding Registers
 *   0x04  Read Input Registers
 *   0x05  Write Single Coil
 *   0x06  Write Single Register
 *   0x0F  Write Multiple Coils
 *   0x10  Write Multiple Registers
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * Modbus Client TCP protocol (all in one class).
 * <br>
 * Simplified code and retained TCP connectivity for Modbus.
 * @author Sergej Shugajev
 * @version 0.1
 */
public class ModbusClientTCP {
	
	private Socket tcpSocket = null;
	private String hostname = null;
	private int port = 502;
	private int connectTimeout = 400;
	private int connectRetries = 1; // 1 is two retries
	
	private short transactionIdentifier = 1;
	private short protocolIdentifier = 0;
	private byte unitIdentifier = 0;
	
	private InputStream inStream;
	private DataOutputStream outStream;
	private byte[] bufferStream = new byte[2100];
	
	/**
	 * Creates a new Modbus Client TCP.
	 */
	public ModbusClientTCP() {
	}
	
	/**
	 * Creates a new Modbus Client TCP.
	* @param	hostname	IP Address of Modbus Server to connect
	* @param 	port		Port Modbus Server listenning (standard 502)
	*/
	public ModbusClientTCP(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * Connects to Modbus Server.
	 * @throws	UnknownHostException
	 * @throws	IOException
	 */
	public void Connect() throws UnknownHostException, IOException {
		
		if (tcpSocket != null && tcpSocket.isConnected()) {
			tcpSocket.close();
			tcpSocket = null;
		}
		int retries = connectRetries;
		while (true) {
			try {
				tcpSocket = new Socket();
				tcpSocket.connect(new InetSocketAddress(hostname, port), connectTimeout);
				if (tcpSocket.isConnected()) {
					inStream = tcpSocket.getInputStream();
					outStream = new DataOutputStream(tcpSocket.getOutputStream());
					break;
				}
			} catch (SocketTimeoutException e) {
				tcpSocket = null;
				Thread.yield();
				if (retries <= 0) {
					throw new SocketTimeoutException("Retries ended");
				}
				retries--;
			}
		}
	}
	
	/**
	 * Connects to Modbus Server.
	 * @param	hostname	IP Address of Modbus Server to connect
	 * @throws	UnknownHostException
	 * @throws	IOException
	 */
	public void Connect(String hostname) throws UnknownHostException, IOException {
		
		this.hostname = hostname;
		Connect();
	}
	
	/**
	 * Connects to Modbus Server.
	 * @param	hostname	IP Address of Modbus Server to connect
	 * @param	port		Port Modbus Server listenning (standard 502)
	 * @throws	UnknownHostException
	 * @throws	IOException
	 */
	public void Connect(String hostname, int port) throws UnknownHostException, IOException {
		
		this.hostname = hostname;
		this.port = port;
		Connect();
	}
	
	private byte[] SendPackedWaitResult(byte[] packed) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		byte[] result = null;
		
		if (tcpSocket != null && tcpSocket.isConnected()) {
			
			// for UDP
//			DatagramPacket sendPacket = new DatagramPacket(packed, packed.length,
//					tcpClientSocket.getInetAddress(), tcpClientSocket.getPort());
//			DatagramSocket clientSocket = new DatagramSocket();
//			clientSocket.setSoTimeout(connectTimeout);
//			clientSocket.send(sendPacket);
//			result = new byte[2100];
//			DatagramPacket receivePacket = new DatagramPacket(result, result.length);
//			clientSocket.receive(receivePacket);
//			clientSocket.close();
//			result = receivePacket.getData();
			
			outStream.write(packed, 0, packed.length);
			int len = -1;
			if ((len = inStream.read(bufferStream)) > 0) {
				result = new byte[len];
				System.arraycopy(bufferStream, 0, result, 0, result.length);
			}
		}
		
		// for TEST
//		String outData = "";
//		for (int num = 0; num < result.length; num++) {
//			outData = outData + (num) + ":" + Byte.toUnsignedInt(result[num]) + " ";
//		}
//		System.out.println(outData);
		
		return result;
	}
	
	private void CheckErrorByResult(byte[] result) throws ModbusException {
		
		if (result != null) {
			if ((byte) (result[7] & 0x80) == (byte) 0x80) {
				switch ((byte) result[8]) {
					case 0x01: throw new FunctionCodeNotSupportedException("Function code not supported");
					case 0x02: throw new StartingAddressInvalidException("Starting adress or quantity invalid");
					case 0x03: throw new QuantityInvalidException("Quantity invalid");
					case 0x04: throw new ModbusException("Error reading");
					default:   throw new ModbusException("Unknown error");
				}
			}
		} else {
			throw new ConnectionException("Connection error (stream is null)");
		}
	}
	
	private void CheckConnect() throws ModbusException {
		
		if (tcpSocket == null) {
			throw new ConnectionException("Connection error (socket is null)");
		}
	}
	
	private void CheckConnectAndParams(int starting, int quantity) throws ModbusException  {
		
		CheckConnect();
		if (starting < 0 || starting > 65535 || quantity < 0 || quantity > 125) {
			throw new IllegalArgumentException("Starting adress must be 0 - 65535; quantity must be 0 - 125");
		}
	}
	
	private byte[] CreatePackedData(byte functionCode, ByteBuffer paramsData) {
		
		short length = (short) (2 + paramsData.capacity());
		
		ByteBuffer packedData = ByteBuffer.allocate(6 + length);
		packedData.putShort(transactionIdentifier);
		packedData.putShort(protocolIdentifier);
		packedData.putShort(length);
		packedData.put(unitIdentifier);
		packedData.put(functionCode);
		packedData.put(paramsData.array());
		
		return packedData.array();
	}
	
	/**
	 * Read Coils from Server (code 0x01).
	 * @param	starting	Address to read
	 * @param	quantity	Number of Inputs to read
	 * @return	Coils from Server
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public boolean[] ReadCoils(int starting, int quantity) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, quantity);
		
		byte[] data = CreatePackedData( (byte) 0x01, 
				ByteBuffer.allocate(4)
					.putShort((short) starting).putShort((short) quantity) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
		
		boolean[] response = new boolean[quantity];
		
		byte byteData = 0;
		for (int i = 0; i < quantity; i++) {
			if (i % 8 == 0) {
				byteData = data[9 + i/8];
			}
			byte mask = (byte) (1 << (i % 8));
			response[i] = (byte) (byteData & mask) > 0 ? true : false;
		}
		
		return (response);
	}
	
	/**
	 * Read Discrete Inputs from Server (code 0x02).
	 * @param	starting	Address to read
	 * @param	quantity	Number of Inputs to read
	 * @return	Discrete Inputs from Server
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public boolean[] ReadDiscreteInputs(int starting, int quantity) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, quantity);
		
		byte[] data = CreatePackedData( (byte) 0x02, 
				ByteBuffer.allocate(4)
					.putShort((short) starting).putShort((short) quantity) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
		
		boolean[] response = new boolean[quantity];
		
		byte byteData = 0;
		for (int i = 0; i < quantity; i++) {
			if (i % 8 == 0) {
				byteData = data[9 + i/8];
			}
			byte mask = (byte) (1 << (i % 8));
			response[i] = (byte) (byteData & mask) > 0 ? true : false;
		}
		
		return (response);
	}
	
	/**
	 * Read Holding Registers from Server (code 0x03).
	 * @param	starting	Address to read
	 * @param	quantity	Number of Inputs to read
	 * @return	Holding Registers from Server
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public int[] ReadHoldingRegisters(int starting, int quantity) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, quantity);
		
		byte[] data = CreatePackedData( (byte) 0x03, 
				ByteBuffer.allocate(4)
					.putShort((short) starting).putShort((short) quantity) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
		
		int[] response = new int[quantity];
		
		for (int i = 0; i < quantity; i++) {
			response[i] = ByteBuffer.wrap(data).getShort(9 + i*2);
		}
		
		return (response);
	}
	
	/**
	 * Read Input Registers from Server (code 0x04).
	 * @param	starting	Address to read
	 * @param	quantity	Number of Inputs to read
	 * @return	Input Registers from Server
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public int[] ReadInputRegisters(int starting, int quantity) throws ModbusException,
				UnknownHostException, SocketException, IOException
	{
		CheckConnectAndParams(starting, quantity);
		
		byte[] data = CreatePackedData( (byte) 0x04, 
				ByteBuffer.allocate(4)
					.putShort((short) starting).putShort((short) quantity) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
		
		int[] response = new int[quantity];
		
		for (int i = 0; i < quantity; i++) {
			response[i] = ByteBuffer.wrap(data).getShort(9 + i*2);
		}
		
		return (response);
	}
	
	/**
	 * Write Single Coil to Server (code 0x05).
	 * @param	starting	Address to write
	 * @param	value		Value to write
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public void WriteSingleCoil(int starting, boolean value) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, 0);
		
		short coilValue = (short) (value ? 0xFF00 : 0);
		
		byte[] data = CreatePackedData( (byte) 0x05, 
				ByteBuffer.allocate(4)
					.putShort((short) starting).putShort(coilValue) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
	}
	
	/**
	 * Write Single Register to Server (code 0x06).
	 * @param	starting	Address to write
	 * @param	value		Value to write
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public void WriteSingleRegister(int starting, int value) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, 0);
		
		short registerValue = (short) value;
		
		byte[] data = CreatePackedData( (byte) 0x06, 
				ByteBuffer.allocate(4)
					.putShort((short) starting).putShort(registerValue) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
	}
	
	/**
	 * Write Multiple Coils to Server (code 0x0F).
	 * @param 	starting	Address to write
	 * @param  	values   	Values to write
	 * @throws 	SimpleModbusException
	 * @throws 	UnknownHostException
	 * @throws 	SocketException
	 * @throws 	IOException
	 */
	public void WriteMultipleCoils(int starting, boolean[] values) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, values.length);
		
		byte byteCount = (values.length > 0) ? (byte) (values.length / 8 + 1) : 0;
		short quantityOfOutputs = (short) values.length;
		
		ByteBuffer valuesData = ByteBuffer.allocate(byteCount);
		
		byte byteCoils = 0;
		for (int i = 0; i < values.length; i++) {
			if ((i % 8) == 0) {
				byteCoils = 0;
			}
			if (values[i] == true) {
				byteCoils |= (byte) (1 << (i % 8));
			}
			if ((i % 8) == 7 || (i == values.length-1)) {
				valuesData.put(byteCoils);
			}
		}
		
		byte[] data = CreatePackedData( (byte) 0x0F, 
				ByteBuffer.allocate(4 + 1 + byteCount)
					.putShort((short) starting).putShort(quantityOfOutputs)
					.put(byteCount)
					.put(valuesData.array()) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
	}
	
	/**
	 * Write Multiple Registers to Server (code 0x10).
	 * @param	starting	Address to write
	 * @param	values		Values to write
	 * @throws	ModbusException
	 * @throws	UnknownHostException
	 * @throws	SocketException
	 * @throws	IOException
	 */
	public void WriteMultipleRegisters(int starting, int[] values) throws ModbusException,
				UnknownHostException, SocketException, IOException {
		
		CheckConnectAndParams(starting, values.length);
		
		byte byteCount = (byte) (values.length * 2);
		short quantityOfOutputs = (short) values.length;
		
		ByteBuffer valuesData = ByteBuffer.allocate(byteCount);
		
		for (int i = 0; i < values.length; i++) {
			valuesData.putShort((short) values[i]);
		}
		
		byte[] data = CreatePackedData( (byte) 0x10, 
				ByteBuffer.allocate(4 + 1 + byteCount)
					.putShort((short) starting).putShort(quantityOfOutputs)
					.put(byteCount)
					.put(valuesData.array()) );
		
		data = SendPackedWaitResult(data);
		CheckErrorByResult(data);
	}
	
	/**
	 * Close connection to Server.
	 * @throws IOException
	 */
	public void Disconnect() throws IOException {
		
		if (tcpSocket != null) {
			tcpSocket.close();
		}
		tcpSocket = null;
	}
	
	/**
	 * Client connected to Server.
	 * @return if Client is connected to Server
	 */
	public boolean isConnected() {
		
		return (tcpSocket != null && tcpSocket.isConnected());
	}
	
	/**
	 * Returns IP Address of Server.
	 * @return IP Address of Server
	 */
	public String getHostname() {
		return hostname;
	}
	
	/**
	 * Sets IP Address of Server.
	 * @param	hostname	IP Address of Server
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	/**
	 * Returns Port of Server listening.
	 * @return port of Server listening
	 */
	public int getPort() {
		return port;
	}
		
	/**
	 * Sets Port of Server.
	 * @param	port	Port of Server (default 502)
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getConnectionTimeout() {
		return connectTimeout;
	}
	
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectTimeout = connectionTimeout;
	}
	
	public int getConnectionRetries() {
		return connectRetries;
	}
	
	public void setConnectionRetries(int connectRetries) {
		this.connectRetries = connectRetries;
	}
	
	public void setUnitIdentifier(byte unitIdentifier) {
		this.unitIdentifier = unitIdentifier;
	}
	
	public byte getUnitIdentifier() {
		return this.unitIdentifier;
	}
	
	/* Modbus Exception */
	
	@SuppressWarnings("serial")
	public class ModbusException extends Exception {
		public ModbusException(String s) {
			super(s);
		}
	}
	
	@SuppressWarnings("serial")
	public class ConnectionException extends ModbusException {
		public ConnectionException(String s) {
			super(s);
		}
	}
	
	@SuppressWarnings("serial")
	public class FunctionCodeNotSupportedException extends ModbusException {
		public FunctionCodeNotSupportedException(String s) {
			super(s);
		}
	}
	
	@SuppressWarnings("serial")
	public class StartingAddressInvalidException extends ModbusException {
		public StartingAddressInvalidException(String s) {
			super(s);
	  	}
	}
	
	@SuppressWarnings("serial")
	public class QuantityInvalidException extends ModbusException {
		public QuantityInvalidException(String s) {
			super(s);
		}
	}
	
}
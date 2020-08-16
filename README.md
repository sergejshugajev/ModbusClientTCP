# Modbus Client TCP (all in one class).
Simplified code and retained TCP connectivity for Modbus.

Version 0.1

# Quick Start
```java
	ModbusClientTCP mb = new ModbusClientTCP("localhost", 502);
	try {
		mb.Connect();
		if (mb.isConnected()) {
			mb.WriteSingleRegister(19, 111);
			System.out.println("Read: " + Arrays.toString(mb.ReadHoldingRegisters(18, 3)));
		}
	} catch (Exception e) {
		System.out.println("ERROR! " + e.toString());
	}
```

# Supported Function Codes
Code  | Function
---   | ---
0x01  | Read Coils
0x02  | Read Discrete Inputs
0x03  | Read Holding Registers
0x04  | Read Input Registers
0x05  | Write Single Coil
0x06  | Write Single Register
0x0F  | Write Multiple Coils
0x10  | Write Multiple Registers

# License
MIT License

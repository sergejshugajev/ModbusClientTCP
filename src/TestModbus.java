import java.util.Arrays;

public class TestModbus {
	
	public static void main(String[] args) {
		
		// master
		// Start Modbus simulator
		// https://sourceforge.net/projects/modrssim/
		
		// client
		ModbusClientTCP mb = new ModbusClientTCP("localhost", 502);
		
		try {
			
			mb.Connect();
			if (mb.isConnected()) {
				mb.WriteSingleRegister(19, 111);
				System.out.println("Read: " + Arrays.toString(mb.ReadHoldingRegisters(18, 3)));
			} else {
				System.out.println("Not connect!");
			}
			
		} catch (Exception e) {
			System.out.println("ERROR! " + e.toString());
		}
		
	}
	
}
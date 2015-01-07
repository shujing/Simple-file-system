package fileSystem;

import java.io.Serializable;

public class Block implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5853118638924823697L;

	public Block() {
		// TODO Auto-generated constructor stub
	}

	byte[] bytes = new byte[64];

	Boolean isEmpty() {
		for (int i = 0; i < 64; i++) {
			if (bytes[i] != 0) {
				return false;
			}
		}
		return true;
	}

	int length() {
		int count = 0;
		for (int i = 0; i < 64; i++) {
			if (bytes[i] != 0) {
				count += 1;
			}
		}
		return count;
	}
}

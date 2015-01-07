package fileSystem;

import java.io.Serializable;

public class IO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3707065795831632356L;
	int L = 64;// num of blocks
	int B = 64 / 4;// size of one block
	// int[][] ldisk;
	Block[] ldisk = new Block[64];

	public IO() {
		for (int i = 0; i < 64; i++) {
			ldisk[i] = new Block();
		}
	}

	void readBlock(int i, Block buffer) {
		if (i > 63)
			System.out.println("error!");
		else
			buffer.bytes = ldisk[i].bytes;

	}

	void writeBlock(int i, Block buffer) {
		ldisk[i].bytes = buffer.bytes;
	}
}

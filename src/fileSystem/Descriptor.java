package fileSystem;

public class Descriptor {

	int length;
	int[] diskMap = new int[3];

	public Descriptor() {
		// TODO Auto-generated constructor stub
	}

	int[] de2block() {
		int[] block = new int[4];
		block[0] = length;
		block[1] = diskMap[1];
		block[2] = diskMap[2];
		block[3] = diskMap[3];
		return block;
	}

}

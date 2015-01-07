package fileSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class UserInterface {
	IO io;
	OFTentry[] ofts = new OFTentry[4];
	ArrayList<int[]> directory = new ArrayList<int[]>();
	int[][] descriptor = new int[24][4];

	public UserInterface() {
		// TODO Auto-generated constructor stub
	}

	void init() {
		io = new IO();

		// initialize OFT
		OFTentry dir = new OFTentry();
		dir.dpIndex = 0;
		dir.curPos = 0;
		ofts[0] = dir;
		ofts[1] = new OFTentry();
		ofts[2] = new OFTentry();
		ofts[3] = new OFTentry();

		// write the bitmap to the ldisk
		int[] bitMap = new int[16];
		io.writeBlock(0, intArray2Block(bitMap));

		for (int i = 0; i < 8; i++) {
			setBitMap(i, true);
		}

		// set the length attribute of every descriptor to -1 to make it
		// distinguished from an empty file
		int[] desBlock = new int[16];
		for (int i = 0; i < 16; i++) {
			desBlock[i] = -1;
		}
		for (int i = 1; i <= 6; i++) {
			io.writeBlock(i, intArray2Block(desBlock));
		}

		// write the directory descriptor into block
		desBlock[0] = 192;// directory length
		desBlock[1] = 7;
		io.writeBlock(1, intArray2Block(desBlock));
	}

	void create(String symbolicFileName) throws Exception {
		// cast the name to 4 bytes

		int iName = sName2int(symbolicFileName);
		// find an empty entry in directory
		directory = getDirectory();
		// check if the name has been used by other files
		boolean isNameDuplicated = false;
		for (int i = 0; i < directory.size(); i++) {
			if (iName == directory.get(i)[0]) {
				isNameDuplicated = true;
				System.out.println("error");// same name
				break;
			}
		}

		// modify the descriptor , set the length to 0
		if (!isNameDuplicated) {
			int fdp = getEmptyDescriptor();
			if (fdp == -1)
				System.out.println("error");// no free descriptor
			else {
				descriptor = getDescriptors();
				descriptor[fdp][0] = 0;
				io.writeBlock(1 + fdp / 4, getDpBlock(fdp));

				// add this file's name and descriptor index to the
				// directory
				int drIndex = getDirectoryEntry();
				if (drIndex == -1) {
					allocate(0);
				} else {
					directory.get(drIndex)[0] = iName;
					directory.get(drIndex)[1] = fdp;
					io.writeBlock(descriptor[0][1 + drIndex / 8],
							getDirBlock(drIndex));
					System.out.println(symbolicFileName + " created");
				}
			}
		}

	}

	/**
	 * search directory to find file descriptor, remove directory entry ,update
	 * bit map (if file was not empty) ,free file descriptor, return status
	 * 
	 * @throws Exception
	 */
	void destroy(String symbolicFileName) throws Exception {
		directory = getDirectory();
		descriptor = getDescriptors();
		int iName = sName2int(symbolicFileName);
		int dpIndex = -1;
		// get descriptor index and remove it from directory
		for (int i = 0; i < directory.size(); i++) {
			if (iName == directory.get(i)[0]) {
				dpIndex = directory.get(i)[1];
				directory.get(i)[0] = 0;
				directory.get(i)[1] = 0;
				Block b = getDirBlock(i);
				io.writeBlock(descriptor[0][1 + i / 8], b);
				if (b.isEmpty())
					setBitMap(descriptor[0][1 + i / 8], false);

				// free the file's block and set bitmap to false
				for (int j = 0; j < 3; j++) {
					if (descriptor[dpIndex][j + 1] != -1) {
						io.writeBlock(descriptor[dpIndex][j + 1], new Block());
						setBitMap(descriptor[dpIndex][j + 1], false);
					}
				}

				// update the descriptor
				for (int j = 0; j < 4; j++)
					descriptor[dpIndex][j] = -1;
				io.writeBlock(dpIndex / 4 + 1, getDpBlock(dpIndex));
				System.out.println(symbolicFileName + " destryed");
				break;
			}
		}

		// remove it from the OFT
		for (int i = 1; i < 4; i++) {
			if (dpIndex == ofts[i].dpIndex) {
				ofts[i] = new OFTentry();
				break;
			}
		}

		if (dpIndex == -1)
			throw new Exception("error");

	}

	int open(String symbolicFileName) throws Exception {
		int OFTindex = -1;
		directory = getDirectory();
		descriptor = getDescriptors();
		int iName = sName2int(symbolicFileName);
		int dpIndex = -1;
		// get descriptor index and remove it from directory
		for (int i = 0; i < directory.size(); i++) {
			if (iName == directory.get(i)[0]) {
				dpIndex = directory.get(i)[1];
				for (int j = 0; j < 4; j++) {
					if (ofts[j].dpIndex == dpIndex)
						throw new Exception("error");
					if (ofts[j].dpIndex == -1) {
						OFTindex = j;
						ofts[j].dpIndex = dpIndex;
						ofts[j].curPos = 0;
						// ofts[j].buffer
						if (descriptor[dpIndex][1] > 0)
							io.readBlock(descriptor[dpIndex][1], ofts[j].buffer);
						System.out.println(symbolicFileName + " opened " + j);
						break;
					}
				}
				break;
			}
		}
		if (dpIndex == -1)
			throw new Exception("error");

		if (OFTindex == -1)
			throw new Exception("error");

		return OFTindex;
	}

	/**
	 * write buffer to disk, update file length in descriptor, free OFT entry
	 * return status
	 * 
	 * @throws Exception
	 */
	void close(int index) throws Exception {
		if (index < 4 && index > 0) {
			int dpIndex = ofts[index].dpIndex;
			int curBlock = ofts[index].curPos / 64;
			int blockNum = descriptor[dpIndex][curBlock + 1];
			if(dpIndex==-1)
				throw new Exception("error");
			if (blockNum != -1) {
				descriptor = getDescriptors();

				Block oldBlock = new Block();
				io.readBlock(blockNum, oldBlock);
				io.writeBlock(blockNum, ofts[index].buffer);
				// get the new length, update descriptor
				descriptor[dpIndex][0] = ofts[index].buffer.length()
						- oldBlock.length() + descriptor[dpIndex][0];
				io.writeBlock(dpIndex / 4 + 1, getDpBlock(dpIndex));
			}

			ofts[index] = new OFTentry();
			System.out.println(index + " closed");
		} else
			throw new Exception("error");
	}

	/**
	 * sequentially read a number of bytes from the specified file into main
	 * memory. The number of bytes to be read is specified in count and the
	 * starting memory address in mem_area. The reading starts with the current
	 * position in the file.
	 */
	void read(int index, int count) throws ArrayIndexOutOfBoundsException{
		descriptor = getDescriptors();
		OFTentry oft = ofts[index];
		int bufPos = oft.curPos % 64;
		int blockNum = oft.curPos / 64;
		int dpIndex = oft.dpIndex;
		int flength = descriptor[oft.dpIndex][0];

		if (count > (flength - oft.curPos))
			System.out.println("error");
		else {

			ArrayList<Byte> mm = new ArrayList<Byte>();
			// if (count + bufPos <= 64 && count + oft.curPos <= flength) {
			if (count + bufPos <= 64) {

				if (count + oft.curPos <= flength) {
					for (int i = 0; i < count; i++) {
						mm.add(oft.buffer.bytes[bufPos + i]);
					}
					oft.curPos += count;

				} else {
					for (int i = 0; i < flength - oft.curPos; i++) {
						mm.add(oft.buffer.bytes[bufPos + i]);
					}
					oft.curPos = flength - 1;
				}

				for (int i = 0; i < mm.size(); i++) {
					System.out.print((char) (byte) mm.get(i) + " ");
				}
			} else {

				for (int i = 0; i < 64 - bufPos; i++) {
					mm.add(oft.buffer.bytes[bufPos + i]);
				}
				for (int i = 0; i < mm.size(); i++) {
					System.out.print((char) (byte) mm.get(i) + " ");
				}

				if (blockNum < 2) {

					io.readBlock(descriptor[dpIndex][blockNum + 2], oft.buffer);
					oft.curPos += 64 - bufPos;
					read(index, count - 64 + bufPos);

				}

			}

			// System.out.println();
		}
	}

	void write(int index, byte character, int count) throws Exception {
		try {
			descriptor = getDescriptors();
			OFTentry oft = ofts[index];
			int bufPos = oft.curPos % 64;
			int blockNum = oft.curPos / 64;
			int old_flength = descriptor[oft.dpIndex][0];
			// int c = 0;
			// int relativeEOFpos = descriptor[oft.dpIndex][0] - blockNum * 64;

			if (descriptor[oft.dpIndex][blockNum + 1] < 0) {

				descriptor[oft.dpIndex][blockNum + 1] = allocateBlock();

				// System.out.println("==============" + (oft.dpIndex / 4 + 1));
				io.writeBlock(oft.dpIndex / 4 + 1, getDpBlock(oft.dpIndex));
				//
				io.readBlock(descriptor[oft.dpIndex][blockNum + 1], oft.buffer);
			}

			if (count + bufPos <= 64) {

				for (int i = 0; i < count; i++) {
					oft.buffer.bytes[bufPos + i] = character;
				}

				oft.curPos += count;

			} else {

				for (int i = 0; i < 64 - bufPos; i++) {
					oft.buffer.bytes[bufPos + i] = character;
				}

				io.writeBlock(descriptor[oft.dpIndex][blockNum + 1], oft.buffer);

				int nextBlock = descriptor[oft.dpIndex][blockNum + 2];

				if (blockNum + 1 < 3) {

					descriptor[oft.dpIndex][0] = blockNum * 64;

					if (nextBlock >= 0)
						io.readBlock(nextBlock, oft.buffer);

					oft.curPos += 64 - bufPos;
					write(index, character, count - 64 + bufPos);
				}
			}

			descriptor[oft.dpIndex][0] = old_flength + count;
			io.writeBlock(oft.dpIndex / 4 + 1, getDpBlock(oft.dpIndex));
		} catch (Exception e) {
			// e.printStackTrace();
			// System.err.println("error\n");
			throw new Exception("error");
		}
	}

	void lseek(int index, int pos) throws ArrayIndexOutOfBoundsException{
		descriptor = getDescriptors();
		if ((pos / 64) == (ofts[index].curPos / 64)) {
			ofts[index].curPos = pos;
		} else {
			int bln = descriptor[ofts[index].dpIndex][pos / 64 + 1];
			
			io.readBlock(bln, ofts[index].buffer);
			ofts[index].curPos = pos;
		}
		System.out.println("position is " + pos);
	}

	/** get the name and length */
	public void directory() {
		// TODO Auto-generated method stub
		directory = getDirectory();
		descriptor = getDescriptors();
		for (int i = 0; i < directory.size(); i++) {
			if (directory.get(i)[0] != 0)
				System.out.print(int2String(directory.get(i)[0]) + " ");
		}
		System.out.println();
	}

	public void save(String name) throws IOException {
		String text = "";
		FileOutputStream f = new FileOutputStream(name);
		ObjectOutputStream o = new ObjectOutputStream(f);
		o.writeObject(io);
		/*for (int i = 0; i < 64; i++) {
			Block buffer = new Block();
			io.readBlock(i, buffer);
			String s = new String(buffer.bytes);
			text = text.concat(s);
			/*
			 * for (int j = 0; j < 64; j++) { System.out.print((char)
			 * buffer.bytes[j] + " ==="); text=text.concat((char)
			 * buffer.bytes[j] + ""); // text = text.concat("ss"); }
			 */
	/*		text = text.concat(i + " \n");
		}
		PrintWriter out = new PrintWriter(new FileOutputStream(name));
		out.println(text);
		out.close();*/
		System.out.println("disk saved");
	}

	public void init(String string) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		File f = new File(string);
		if (!f.exists()) {
			io = new IO();
			ofts = new OFTentry[4];
			directory = new ArrayList<int[]>();
			descriptor = new int[24][4];
			save(string);
		} else {
		/*	FileReader file = new FileReader(string);
			BufferedReader reader = new BufferedReader(file);
			String line = "";
			int i = 0;
			// while ((line = reader.readLine()) != null) {
			while (i < 64) {
				line = reader.readLine();
				byte[] bytes = line.getBytes();
				Block b = new Block();
				b.bytes = bytes;
				io.writeBlock(i, b);
				i++;
			}*/
		//	String text = "";
			FileInputStream fi = new FileInputStream(string);
			ObjectInputStream o = new ObjectInputStream(fi);
			io = (IO) o.readObject();
		}
		System.out.println("disk restored");
	}

	public void test() {

		Block bitMapB = new Block();
		io.readBlock(0, bitMapB);

		int[] bitMap = bytes2int(bitMapB.bytes);

		for (int j = 0; j < 64; j++) {

			System.out.print(getBit(bitMap, j) + " ");

		}
		System.out.println();

		// print the descriptors
		for (int i = 1; i < 7; i++) {
			System.out.println();
			for (int j = 0; j < 16; j++) {
				System.out.print(bytes2int(io.ldisk[i].bytes)[j] + " ");
			}
		}

		// print directory
		directory = getDirectory();
		for (int i = 0; i < directory.size(); i++) {
			System.out.println(i + " sym name" + directory.get(i)[0]
					+ " descriptor " + directory.get(i)[1]);
		}

		for (int i = 0; i < 4; i++) {
			System.out.println(ofts[i].dpIndex + " OFT");
		}

		// print the blocks
		for (int i = 7; i < 64; i++) {
			System.out.println();
			for (int j = 0; j < 64; j++) {
				System.out.print((char) io.ldisk[i].bytes[j] + " ");
			}
		}
	}

	int getBit(int[] bitMap, int i) {
		if (i < 32)
			return (bitMap[0] >> i) & 1;
		else
			return (bitMap[1] >> (i - 32)) & 1;
	}

	private void setBitMap(int i, boolean b) {
		// int bit = b? 1: 0;

		Block bitMapB = new Block();
		io.readBlock(0, bitMapB);

		int[] bitMap = bytes2int(bitMapB.bytes);

		if (b) {
			if (i < 32)
				bitMap[0] |= (1 << i);
			else
				bitMap[1] |= (1 << (i - 32));
		} else {
			if (i < 32)
				bitMap[0] &= ~(1 << i);
			else
				bitMap[1] &= ~(1 << (i - 32));
		}

		io.writeBlock(0, intArray2Block(bitMap));
	}

	/**
	 * when u have an index of directory entry, use this function to get the
	 * block where this entry is in
	 */
	Block getDirBlock(int index) {
		int bId = index / 8;
		int[] temp = new int[16];
		for (int i = 0; i < 8; i++) {
			temp[i * 2] = directory.get(bId * 8 + i)[0];
			temp[2 * i + 1] = directory.get(bId * 8 + i)[1];
		}
		return intArray2Block(temp);
	}

	Block getDpBlock(int fdp) {
		int[] mDbBlock = new int[16];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++)
				mDbBlock[i * 4 + j] = descriptor[(fdp / 4) * 4 + i][j];
		}
		return intArray2Block(mDbBlock);
	}

	private int allocateBlock() throws Exception {

		Block bitMapB = new Block();
		io.readBlock(0, bitMapB);

		int[] bitMap = bytes2int(bitMapB.bytes);

		int bits = bitMap[0];
		int i;
		for (i = 0; i < 32; i++) {
			if ((bits & 1) == 0)
				break;
			bits = bits >> 1;
		}
		if (i == 32) {
			/*
			 * int j; bits = bitMap[1]; for (j = i - 32; j < 32; j++) { if
			 * ((bits & 0x80000000) == 0) break; bits = bits << 1; } i = j + i;
			 */
			bits = bitMap[1];
			for (; i < 64; i++) {
				if ((bits & 1) == 0)
					break;
				bits = bits >> 1;
			}
		}

		if (i >= 64)
			// i = -1;
			throw new Exception("error");

		setBitMap(i, true);
		// .out.println("allocate block " + i);
		return i;
	}

	/** check if the current block for directory is full */
	private int getDirectoryEntry() {
		int eDirIndex = -1;
		directory = getDirectory();
		for (int i = 0; i < directory.size(); i++) {
			if (directory.get(i)[0] == 0) {
				eDirIndex = i;
				break;
			}
		}
		return eDirIndex;
	}

	/**
	 * allocate an empty block and modify the descriptor. i is the descriptor
	 * index. and set the bit map
	 */
	private void allocate(int i) {

	}

	private int getEmptyDescriptor() {
		// TODO Auto-generated method stub
		descriptor = getDescriptors();
		int eIndex = -1;
		for (int i = 0; i < descriptor.length; i++) {
			if (descriptor[i][0] == -1) {
				eIndex = i;
				break;
			}
		}
		return eIndex;
	}

	/** convert an int array to a block */
	private Block intArray2Block(int[] bitMap) {
		Block temp = new Block();
		for (int i = 0; i < bitMap.length; i++) {
			byte[] bufferByte = ByteBuffer.allocate(4).putInt(bitMap[i])
					.array();
			temp.bytes[4 * i] = bufferByte[0];
			temp.bytes[4 * i + 1] = bufferByte[1];
			temp.bytes[4 * i + 2] = bufferByte[2];
			temp.bytes[4 * i + 3] = bufferByte[3];
		}
		return temp;
	}

	/** convert a byte array to an int array */
	int[] bytes2int(byte[] bytes) {
		int[] b2i = new int[(int) Math.ceil((double) (bytes.length) / 4.0)];
		for (int i = 0; i < bytes.length / 4; i++) {
			b2i[i] = ((0xFF & bytes[0 + 4 * i]) << 24)
					| ((0xFF & bytes[1 + 4 * i]) << 16)
					| ((0xFF & bytes[2 + 4 * i]) << 8)
					| (0xFF & bytes[3 + 4 * i]);
		}
		switch (bytes.length % 4) {
		case 0:
			break;
		case 1:
			b2i[b2i.length - 1] = (0xFF & bytes[bytes[bytes.length - 1]]) << 24;
		case 2:
			b2i[b2i.length - 1] = ((0xFF & bytes[bytes[bytes.length - 2]]) << 24)
					| ((0xFF & bytes[bytes.length - 3]) << 16);
		case 3:
			b2i[b2i.length - 1] = ((0xFF & bytes[bytes[bytes.length - 3]]) << 24)
					| ((0xFF & bytes[bytes.length - 2]) << 16)
					| ((0xFF & bytes[bytes.length - 1]) << 8);
		}
		return b2i;
	}

	/** get all the descriptors by int */
	int[][] getDescriptors() {
		Block temp = new Block();
		for (int i = 1; i < 7; i++) {
			io.readBlock(i, temp);
			int[] block = bytes2int(temp.bytes);
			for (int j = 0; j < 4; j++) {
				for (int k = 0; k < 4; k++)
					descriptor[(i - 1) * 4 + j][k] = block[4 * j + k];
			}
		}
		return descriptor;
	}

	/** get all the directory entries */
	ArrayList<int[]> getDirectory() {
		ArrayList<int[]> directory = new ArrayList<int[]>();
		descriptor = getDescriptors();
		for (int i = 1; i < 4; i++) {
			if (descriptor[0][i] != -1) {
				Block t1 = new Block();
				io.readBlock(descriptor[0][i], t1);
				int[] list = bytes2int(t1.bytes);
				for (int j = 0; j < list.length / 2; j++) {
					int[] tempDiEntry = { list[j * 2], list[j * 2 + 1] };
					directory.add(tempDiEntry);
				}
			}
		}
		return directory;
	}

	/** convert file name which is String type to int type */
	int sName2int(String symbolicName) throws Exception {
		// cast the name to 4 bytes
		byte[] name = new byte[4];
		switch (symbolicName.length()) {
		case 0:
			throw new Exception("error");
		case 1:
			name[0] = 0x00;
			name[1] = 0x00;
			name[2] = 0x00;
			name[3] = symbolicName.getBytes()[0];
			break;
		case 2:
			name[0] = 0x00;
			name[1] = 0x00;
			name[2] = symbolicName.getBytes()[0];
			name[3] = symbolicName.getBytes()[1];
			break;
		case 3:
			name[0] = 0x00;
			name[1] = symbolicName.getBytes()[0];
			name[2] = symbolicName.getBytes()[1];
			name[3] = symbolicName.getBytes()[2];
			break;
		case 4:
			name[0] = symbolicName.getBytes()[0];
			name[1] = symbolicName.getBytes()[1];
			name[2] = symbolicName.getBytes()[2];
			name[3] = symbolicName.getBytes()[3];

			break;
		default:
			throw new Exception("error");
		}

		int iName = ((0xFF & name[0]) << 24) | ((0xFF & name[1]) << 16)
				| ((0xFF & name[2]) << 8) | (0xFF & name[3]);

		return iName;
	}

	/** convert int to string, for getting the name */
	String int2String(int x) {
		char[] c = new char[4];
		c[3] = (char) (x & 0xFF);
		c[2] = (char) ((x >> 8) & 0xFF);
		c[1] = (char) ((x >> 16) & 0xFF);
		c[0] = (char) ((x >> 24) & 0xFF);
		String s = String.valueOf(c);
		return s;
	}

}

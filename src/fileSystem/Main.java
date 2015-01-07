package fileSystem;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// UI user = new UI();
		UserInterface u = new UserInterface();
		Scanner in = new Scanner(System.in);
		String commands = null;

		Pattern test = Pattern.compile("test");
		Pattern init = Pattern.compile("in");
		Pattern create = Pattern.compile("cr\\s+[^ ]*");
		Pattern destroy = Pattern.compile("de\\s+[^ ]*");
		Pattern open = Pattern.compile("op\\s+[^ ]*");
		Pattern close = Pattern.compile("cl\\s+\\d+");
		Pattern read = Pattern.compile("rd\\s+\\d+\\s+\\d+");
		Pattern write = Pattern.compile("wr\\s+\\d+\\s+[a-z]\\s+\\d+");
		Pattern seek = Pattern.compile("sk\\s+\\d+\\s+\\d+");
		Pattern directory = Pattern.compile("dr");
		Pattern initFile = Pattern
				.compile("in\\s+(?:[a-z][a-z\\.\\d_]+)\\.(?:[a-z\\d]{3})(?![\\w\\.])");
		Pattern save = Pattern
				.compile("sv\\s+(?:[a-z][a-z\\.\\d_]+)\\.(?:[a-z\\d]{3})(?![\\w\\.])");

		do {
			try {
				commands = in.nextLine();
				if (commands.equals("")) {

				} else if (test.matcher(commands).matches()) {
					// user.test();
					u.test();
				} else if (init.matcher(commands).matches()) {
					System.out.println();
					u.init();
					System.out.println("disk initialized");
				} else if (create.matcher(commands).matches()) {
					String fname = commands.split(" ")[1];
					u.create(fname);
				} else if (destroy.matcher(commands).matches()) {
					String fname = commands.split(" ")[1];
					u.destroy(fname);
				} else if (open.matcher(commands).matches()) {
					String fname = commands.split(" ")[1];

					u.open(fname);
				} else if (close.matcher(commands).matches()) {
					int index = Integer.parseInt(commands.split(" ")[1]);
					u.close(index);

					// user.close(index);
				} else if (read.matcher(commands).matches()) {
					int index = Integer.parseInt(commands.split(" ")[1]);
					int count = Integer.parseInt(commands.split(" ")[2]);
					u.read(index, count);
					System.out.println();
				} else if (write.matcher(commands).matches()) {
					int index = Integer.parseInt(commands.split(" ")[1]);
					byte mem_area = commands.split(" ")[2].getBytes()[0];// what's
																			// //
																			// this?
					int count = Integer.parseInt(commands.split(" ")[3]);
					u.write(index, mem_area, count);
					System.out.println(count + " bytes written");
				} else if (seek.matcher(commands).matches()) {
					int index = Integer.parseInt(commands.split(" ")[1]);
					int pos = Integer.parseInt(commands.split(" ")[2]);
					u.lseek(index, pos);
				} else if (directory.matcher(commands).matches()) {
					u.directory();
				} else if (initFile.matcher(commands).matches()) {
					String[] s = commands.split(" ");
					u.init(s[1]);
				} else if (save.matcher(commands).matches()) {
					String[] s = commands.split(" ");
					u.save(s[1]);
				} else {
					System.out.println("error");
				}

			} catch (NoSuchElementException e) {
				return;
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("error");
			} 
			catch (Exception e) {
				System.out.println(e.getMessage());
				// System.err.println(e.toString());
			//	e.printStackTrace();
			}
		} while (commands != null);
	}

}

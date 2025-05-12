import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util. *;

public class Auction {
	private static final Scanner scanner = new Scanner(System.in);
	private static String username;
	private static Connection conn;

	enum Category {
		ELECTRONICS, 
		BOOKS,
		HOME,
		CLOTHING,
		SPORTINGGOODS,
		OTHERS
	}
	enum Condition {
		NEW,
		LIKE_NEW,
		GOOD,
		ACCEPTABLE
	}

	private static void LoginMenu() {
		String userpass;
		System.out.print(
			"----< User Login > \n" +
			" ** To go back, enter 'back' in user ID. \n" +
			"     user ID: "
			);

		username = scanner.next();
		scanner.nextLine();
		if(username.equalsIgnoreCase("back"))
			return;

		System.out.print("     password: ");
		userpass = scanner.next();
		scanner.nextLine();

		/* TODO: Your code should come here to check ID and password */
		try (PreparedStatement pStmt = conn.prepareStatement(
			"SELECT 1 " +
			"FROM users " +
			"WHERE user_id = ? AND password = ?"
		)) {
			pStmt.setString(1, username);
			pStmt.setString(2, userpass);

			try (ResultSet rset = pStmt.executeQuery()) {
				if (!rset.next()) throw new SQLException();
			}
		} catch (SQLException e) {
			System.out.println("Error: Incorrect user name or password\n");
			username = null;
			return;
		}

		System.out.println("You are successfully logged in.\n");
	}

	private static void SignupMenu() {
		boolean is_admin;
		String new_username, userpass, isAdmin;
		System.out.print(
			"----< Sign Up >\n" +
			" ** To go back, enter 'back' in user ID.\n" +
			"---- user name: "
			);

		try {
			new_username = scanner.next();
			scanner.nextLine();
			if (new_username.equalsIgnoreCase("back"))
				return;

			System.out.print("---- password: ");
			userpass = scanner.next();
			scanner.nextLine();

			System.out.print("---- In this user an administrator? (Y/N): ");
			isAdmin = scanner.next();
			scanner.nextLine();
			if (isAdmin.equalsIgnoreCase("Y") || isAdmin.equalsIgnoreCase("YES"))
				is_admin = true;
			else if (isAdmin.equalsIgnoreCase("N") || isAdmin.equalsIgnoreCase("NO"))
				is_admin = false;
			else throw new InputMismatchException();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Please select again.\n");
			return;
		}

		/* TODO: Your code should come here to create a user account in your database */
        try (PreparedStatement pStmt = conn.prepareStatement(
			"INSERT INTO users VALUES(?, ?, ?)"
		)) {
			pStmt.setString(1, new_username);
			pStmt.setString(2, userpass);
			pStmt.setBoolean(3, is_admin);

			if (pStmt.executeUpdate() == 0) throw new SQLException();
        } catch (SQLException e) {
			System.out.println("Error: Sign up failed. Please select again.\n");
			return;
        }
        System.out.println("Your account has been successfully created.\n");
	}

	private static boolean SellMenu() {
		Category category;
		Condition condition;
		char choice;
		int price;
		boolean flag_catg = true, flag_cond = true;

		do{
			System.out.println(
					"----< Sell Item >\n" +
					"---- Choose a category.\n" +
					"    1. Electronics\n" +
					"    2. Books\n" +
					"    3. Home\n" +
					"    4. Clothing\n" +
					"    5. Sporting Goods\n" +
					"    6. Other Categories\n" +
					"    P. Go Back to Previous Menu"
					);

			try {
				choice = scanner.next().charAt(0);;
			}catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			flag_catg = true;

			switch ((int) choice){
				case '1':
					category = Category.ELECTRONICS;
					continue;
				case '2':
					category = Category.BOOKS;
					continue;
				case '3':
					category = Category.HOME;
					continue;
				case '4':
					category = Category.CLOTHING;
					continue;
				case '5':
					category = Category.SPORTINGGOODS;
					continue;
				case '6':
					category = Category.OTHERS;
					continue;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_catg = false;
					continue;
			}
		}while(!flag_catg);

		do{
			System.out.println(
					"---- Select the condition of the item to sell.\n" +
					"   1. New\n" +
					"   2. Like-new\n" +
					"   3. Used (Good)\n" +
					"   4. Used (Acceptable)\n" +
					"   P. Go Back to Previous Menu"
					);

			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			}catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			flag_cond = true;

			switch (choice) {
				case '1':
					condition = Condition.NEW;
					break;
				case '2':
					condition = Condition.LIKE_NEW;
					break;
				case '3':
					condition = Condition.GOOD;
					break;
				case '4':
					condition = Condition.ACCEPTABLE;
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_cond = false;
					continue;
			}
		}while(!flag_cond);

		try {
			System.out.println("---- Description of the item (one line): ");
			String description = scanner.nextLine();
			System.out.println("---- Buy-It-Now price: ");

			while (!scanner.hasNextInt()) {
				scanner.next();
				System.out.println("Invalid input is entered. Please enter Buy-It-Now price: ");
			}

			price = scanner.nextInt();
			scanner.nextLine();

			System.out.print("---- Bid closing date and time (YYYY-MM-DD HH:MM): ");
			// you may assume users always enter valid date/time
			String date = scanner.nextLine();  /* "2023-03-04 11:30"; */
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
			LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
		}catch (Exception e) {
			System.out.println("Error: Invalid input is entered. Going back to the previous menu.");
			return false;
		}

		/* TODO: Your code should come here to store the user inputs in your database */

		System.out.println("Your item has been successfully listed.\n");
		return true;
	}

	private static boolean SignupMenu() {
		/* 2. Sign Up */
		String new_username, userpass, isAdmin;
		System.out.print("----< Sign Up >\n" + 
				" ** To go back, enter 'back' in user ID.\n" +
				"---- user name: ");
		try {
			new_username = scanner.next();
			scanner.nextLine();
			if(new_username.equalsIgnoreCase("back")){
				return false;
			}
			System.out.print("---- password: ");
			userpass = scanner.next();
			scanner.nextLine();
			System.out.print("---- In this user an administrator? (Y/N): ");
			isAdmin = scanner.next();
			scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Please select again.");
			return false;
		}

		/* TODO: Your code should come here to create a user account in your database */

		System.out.println("Your account has been successfully created.\n");
		return true;
	}

	private static boolean AdminMenu() {
		/* 3. Login as Administrator */
		char choice;
		String adminname, adminpass;
		String keyword, seller;
		System.out.print("----< Login as Administrator >\n" +
				" ** To go back, enter 'back' in user ID.\n" +
				"---- admin ID: ");

		try {
			adminname = scanner.next();
			scanner.nextLine();
			if(adminname.equalsIgnoreCase("back")){
				return false;
			}
			System.out.print("---- password: ");
			adminpass = scanner.nextLine();
			// TODO: check the admin's account and password.
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		boolean login_success = true;

		if(!login_success){
			// login failed. go back to the previous menu.
			return false;
		}

		do {
			System.out.println(
					"----< Admin menu > \n" +
					"    1. Print Sold Items per Category \n" +
					"    2. Print Account Balance for Seller \n" +
					"    3. Print Seller Ranking \n" +
					"    4. Print Buyer Ranking \n" +
					"    P. Go Back to Previous Menu"
					);

			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			if (choice == '1') {
				System.out.println("----Enter Category to search : ");
				keyword = scanner.next();
				scanner.nextLine();
				/*TODO: Print Sold Items per Category */
				System.out.println("sold item       | sold date       | seller ID   | buyer ID   | price | commissions");
				System.out.println("----------------------------------------------------------------------------------");
				/*
				   while(rset.next()){
				   }
				 */
				continue;
			} else if (choice == '2') {
				/*TODO: Print Account Balance for Seller */
				System.out.println("---- Enter Seller ID to search : ");
				seller = scanner.next();
				scanner.nextLine();
				System.out.println("sold item       | sold date       | buyer ID   | price | commissions");
				System.out.println("--------------------------------------------------------------------");
				/*
				   while(rset.next()){
				   }
				 */
				continue;
			} else if (choice == '3') {
				/*TODO: Print Seller Ranking */
				System.out.println("seller ID   | # of items sold | Total Profit (excluding commissions)");
				System.out.println("--------------------------------------------------------------------");
				/*
				   while(rset.next()){
				   }
				 */
				continue;
			} else if (choice == '4') {
				/*TODO: Print Buyer Ranking */
				System.out.println("buyer ID   | # of items purchased | Total Money Spent ");
				System.out.println("------------------------------------------------------");
				/*
				   while(rset.next()){
				   }
				 */
				continue;
			} else if (choice == 'P' || choice == 'p') {
				return false;
			} else {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}
		} while(true);
	}

	public static void CheckSellStatus(){
		/* TODO: Check the status of the item the current user is selling */

		System.out.println("item listed in Auction | bidder (buyer ID) | bidding price | bidding date/time \n");
		System.out.println("-------------------------------------------------------------------------------\n");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
	}

	public static boolean BuyItem(){
		Category category;
		Condition condition;
		char choice;
		int price;
		String keyword, seller, datePosted;
		boolean flag_catg = true, flag_cond = true;
		
		do {

			System.out.println( "----< Select category > : \n" +
					"    1. Electronics\n"+
					"    2. Books\n" + 
					"    3. Home\n" + 
					"    4. Clothing\n" + 
					"    5. Sporting Goods\n" +
					"    6. Other categories\n" +
					"    7. Any category\n" +
					"    P. Go Back to Previous Menu"
					);

			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				return false;
			}

			flag_catg = true;

			switch (choice) {
				case '1':
					category = Category.ELECTRONICS;
					break;
				case '2':
					category = Category.BOOKS;
					break;
				case '3':
					category = Category.HOME;
					break;
				case '4':
					category = Category.CLOTHING;
					break;
				case '5':
					category = Category.SPORTINGGOODS;
					break;
				case '6':
					category = Category.OTHERS;
					break;
				case '7':
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_catg = false;
					continue;
			}
		} while(!flag_catg);

		do {

			System.out.println(
					"----< Select the condition > \n" +
					"   1. New\n" +
					"   2. Like-new\n" +
					"   3. Used (Good)\n" +
					"   4. Used (Acceptable)\n" +
					"   P. Go Back to Previous Menu"
					);
			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				return false;
			}

			flag_cond = true;

			switch (choice) {
				case '1':
					condition = Condition.NEW;
					break;
				case '2':
					condition = Condition.LIKE_NEW;
					break;
				case '3':
					condition = Condition.GOOD;
					break;
				case '4':
					condition = Condition.ACCEPTABLE;
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_cond = false;
					continue;
				}
		} while(!flag_cond);

		try {
			System.out.println("---- Enter keyword to search the description : ");
			keyword = scanner.next();
			scanner.nextLine();

			System.out.println("---- Enter Seller ID to search : ");
			System.out.println(" ** Enter 'any' if you want to see items from any seller. ");
			seller = scanner.next();
			scanner.nextLine();

			System.out.println("---- Enter date posted (YYYY-MM-DD): ");
			System.out.println(" ** This will search items that have been posted after the designated date.");
			datePosted = scanner.next();
			scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		/* TODO: Query condition: item category */
		/* TODO: Query condition: item condition */
		/* TODO: Query condition: items whose description match the keyword (use LIKE operator) */
		/* TODO: Query condition: items from a particular seller */
		/* TODO: Query condition: posted date of item */

		/* TODO: List all items that match the query condition */
		System.out.println("Item ID | Item description | Condition | Seller | Buy-It-Now | Current Bid | highest bidder | Time left | bid close");
		System.out.println("-------------------------------------------------------------------------------------------------------");
		/* 
		   while(rset.next()){ 
		   }
		 */

		System.out.println("---- Select Item ID to buy or bid: ");

		try {
			choice = scanner.next().charAt(0);;
			scanner.nextLine();
			System.out.println("     Price: ");
			price = scanner.nextInt();
			scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		/* TODO: Buy-it-now or bid: If the entered price is higher or equal to Buy-It-Now price, the bid ends and the following needs to be printed. */
		/* Even if the bid price is higher than the Buy-It-Now price, the buyer pays the B-I-N price. */
		System.out.println("Thank you for the purchase.\n"); 

		/* If the entered price is lower than the current highest price, print out the following. */
		System.out.println("You must bid higher than the current price. \n"); 

                /* Otherwise, print the following */
		System.out.println("Congratulations, you are the highest bidder.\n"); 
		return true;
	}

	public static void CheckBuyStatus(){
		/* TODO: Check the status of the item the current buyer is bidding on */
		/* Even if you are outbidded or the bid closing date has passed, all the items this user has bidded on must be displayed */

		System.out.println("item ID   | item description   | highest bidder | highest bidding price | your bidding price | bid closing date/time");
		System.out.println("--------------------------------------------------------------------------------------------------------------------");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
	}

	public static void CheckAccount(){
		/* TODO: Check the balance of the current user.  */
		System.out.println("[Sold Items] \n");
		System.out.println("item category  | item ID   | sold date | sold price  | buyer ID | commission  ");
		System.out.println("------------------------------------------------------------------------------");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
		System.out.println("[Purchased Items] \n");
		System.out.println("item category  | item ID   | purchased date | puchased price  | seller ID ");
		System.out.println("--------------------------------------------------------------------------");
		/*
		   while(rset.next(){
		   System.out.println();
		   }
		 */
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java Auction postgres_id password");
			System.exit(1);
		}

		try {
			conn = DriverManager.getConnection("jdbc:postgresql://localhost/" + args[0], args[0], args[1]);
		} catch (SQLException e) {
			System.out.println("SQLException : " + e); // 연결 실패
			System.exit(1);
		}

		char choice;
		do {
			username = null;
			System.out.println(
				"----< Login menu > \n" +
				"----(1) Login \n" +
				"----(2) Sign up \n" +
				"----(3) Login as Administrator \n" +
				"----(Q) Quit"
			);

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}
			System.out.println();

			try {
				switch ((int) choice) {
					case '1':
						LoginMenu();
						break;
					case '2':
						SignupMenu();
						continue;
					case '3':
						AdminMenu();
						continue;
					case 'q':
					case 'Q':
						System.out.println("Good Bye");
						/* TODO: close the connection and clean up everything here */
						conn.close();
						System.exit(1);
					default:
						System.out.println("Error: Invalid input is entered. Try again.\n");
				}
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);	
			}
		} while (username == null || username.equalsIgnoreCase("back"));

		// logged in as a normal user 
		do {
			System.out.println(
				"---< Main menu > : \n" +
				"----(1) Sell Item \n" +
				"----(2) Check Status of Your Listed Item \n" +
				"----(3) Buy Item \n" +
				"----(4) Check Status of your Bid \n" +
				"----(5) Check your Account \n" +
				"----(Q) Quit"
			);

			try {
				choice = scanner.next().charAt(0);;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.\n");
				continue;
			}
			System.out.println();

			try {
				switch (choice) {
					case '1':
						SellMenu();
						continue;
					case '2':
						CheckSellStatus();
						continue;
					case '3':
						BuyItem();
						continue;
					case '4':
						CheckBuyStatus();
						continue;
					case '5':
						CheckAccount();
						continue;
					case 'q':
					case 'Q':
						System.out.println("Good Bye");
						/* TODO: close the connection and clean up everything here */
						conn.close();
						System.exit(1);
					default:
						System.out.println("Error: Invalid input is entered. Try again.\n");
				}
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				System.exit(1);
			}
		} while (true);
	} // End of main
} // End of class



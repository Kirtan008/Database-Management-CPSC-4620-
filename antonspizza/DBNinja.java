package cpsc4620.antonspizza;

import java.io.*;
import java.sql.*;
import java.util.*;

/*
Partners:
Nathan Wessel
Kirtan Patel
 */

/*
This file is where most of your code changes will occur
You will write the code to retrieve information from the database, or save information to the database

The class has several hard coded static variables used for the connection, you will need to change those to your connection information
(I think this is step 1)

This class also has static string variables for pickup, delivery and dine-in. If your database stores the strings differently (i.e "pick-up" vs "pickup") changing these static variables will ensure that the comparison is checking for the right string in other places in the program. You will also need to use these strings if you store this as boolean fields or an integer.


*/

/**
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
    //enter your user name here
    private static String user = "AntnPzDB_cg6d";
    //enter your password here
    private static String password = "GoTigers4620";
    //enter your database name here
    private static String database_name = "AntonPizzaDB_huff";
    //Do not change the port. 3306 is the default MySQL port
    private static String port = "3306";
    private static Connection conn;

    //Change these variables to however you record dine-in, pick-up and delivery, and sizes and crusts
    public final static String pickup = "Pick-Up";
    public final static String delivery = "Delivery";
    public final static String dine_in = "Dine-In";

    public final static String size_s = "small";
    public final static String size_m = "medium";
    public final static String size_l = "Large";
    public final static String size_xl = "X-Large";

    public final static String crust_thin = "Thin";
    public final static String crust_orig = "Original";
    public final static String crust_pan = "Pan";
    public final static String crust_gf = "Gluten-Free";

    // this looks good above; transfer to SOC machines

    /**
     * This function will handle the connection to the database
     * @return true if the connection was successfully made
     * @throws SQLException
     * @throws IOException
     */
    private static boolean connect_to_db() throws SQLException, IOException
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println ("Could not load the driver");

            System.out.println("Message     : " + e.getMessage());

            return false;
        }

        conn = DriverManager.getConnection("jdbc:mysql://mysql1.cs.clemson.edu:"+port+"/"+database_name, user, password);
        return true;
    }

    /**
     *
     * @param o order that needs to be saved to the database
     * @throws SQLException
     * @throws IOException
     * @requires o is not NULL. o's ID is -1, as it has not been assigned yet. The pizzas do not exist in the database
     *          yet, and the topping inventory will allow for these pizzas to be made
     * @ensures o will be assigned an id and added to the database, along with all of it's pizzas. Inventory levels
     *          will be updated appropriately
     */
    public static void addOrder(Order o) throws SQLException, IOException
    {
        connect_to_db();
		/* add code to add the order to the DB. Remember to add the pizzas and discounts as well, which will involve multiple tables. Customer should already exist. Toppings will need to be added to the pizzas.

		It may be beneficial to define more functions to add an individual pizza to a database, add a topping to a pizza, etc.

		Note: the order ID will be -1 and will need to be replaced to be a fitting primary key.

		You will also need to add timestamps to your pizzas/orders in your database. Those timestamps are not stored in this program, but you can get the current time before inserting into the database
        DONT FORGET TO ADD TIMESTAMPS!

        Time currTime = Java.time.getCurrentTime();
        query = "INSERT INTO Table values 1, 2, 3, 4, currTime...";

		Remember, when a new order comes in the ingredient levels for the topping
		    need to be adjusted accordingly.
		Remember to check for "extra" of a topping here as well.

		You do not need to check to see if you have the topping in stock before
		adding to a pizza. You can just let it go negative.
		*/

        // add this order to the general Orders table
        int newOrderID = getNextIDForTable("OrderID", "Orders");
        addToOrdersTable(o, newOrderID);
        if (o.getType() == DBNinja.dine_in) {
            System.out.println("Or here?");
            // add to DineInOrder table
            addToDineInOrderTable(o, newOrderID);
            // add all seats to SeatNum table
            addAllSeatNums(o, newOrderID);

            // add Pizzas to database
            ArrayList<Integer> pizzaIDsList = addToPizzaTable(o, newOrderID);
            // add pizza toppings to database
            addToPizzaToppingTable(o, pizzaIDsList);

            // add pizzas discounts in database
            addToPizzaDiscountTable(o, pizzaIDsList);
            // add order discounts in database
            addToOrderDiscountTable(o, newOrderID);
        }
        else if (o.getType() == DBNinja.pickup) {
            // add to PickUpOrder table
            addToPickUpOrderTable(o, newOrderID);

            // add Pizzas to database
            ArrayList<Integer> pizzaIDsList = addToPizzaTable(o, newOrderID);
            // add pizza toppings to database
            addToPizzaToppingTable(o, pizzaIDsList);

            // add pizzas discounts in database
            addToPizzaDiscountTable(o, pizzaIDsList);
            // add order discounts in database
            addToOrderDiscountTable(o, newOrderID);
        }
        else {
            // its a delivery order; add to delviery order table
            addToDeliveryOrderTable(o, newOrderID);

            // add Pizzas to database
            ArrayList<Integer> pizzaIDsList = addToPizzaTable(o, newOrderID);
            // add pizza toppings to database
            addToPizzaToppingTable(o, pizzaIDsList);

            // add pizzas discounts in database
            addToPizzaDiscountTable(o, pizzaIDsList);
            // add order discounts in database
            addToOrderDiscountTable(o, newOrderID);
        }

        updateToppingTable(o, newOrderID);

        conn.close();
    }

    private static void addToOrdersTable(Order o, int newOrderID) throws SQLException, IOException {
        String query = "INSERT INTO Orders VALUES (?, ?);";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        stmt.setInt(1, newOrderID);
        stmt.setString(2, o.getType());
        try {
            int result = stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error Adding Order to Orders table");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
            return;
        }
    }
    private static void addToDineInOrderTable(Order o, int newOrderID) throws SQLException, IOException {
        int isNotRealOrder = 0;

        String query = "INSERT INTO DineInOrder VALUES (?, ?, ?, ?);";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        stmt.setString(1, Integer.toString(newOrderID));
        // get the table number from the DineInCustomer associated with this order
        DineInCustomer temp_cust = (DineInCustomer) o.getCustomer();
        stmt.setString(2, Integer.toString(temp_cust.getTableNum()));
        stmt.setString(3, Integer.toString(temp_cust.getID()));
        // this is a real order
        stmt.setString(4, Integer.toString(isNotRealOrder));
        try {
            int result = stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error Adding DineInOrder to DineInOrder table");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
            return;
        }
    }

    // add seatnum, or addToSeatNumTable
    private static void addAllSeatNums(Order o, int newOrderID) throws SQLException, IOException {
        // get the DineInCustomer associated with this order
        DineInCustomer temp_cust = (DineInCustomer) o.getCustomer();
        List<Integer> seatNumsList = temp_cust.getSeats();
        // insert every seat number from the list into the database
        for (Integer currSeatNum : seatNumsList) {
            String query = "INSERT INTO SeatNum VALUES (?, ?);";
            PreparedStatement stmt = conn.prepareStatement(query);
            // clear any parameters as sanity check
            stmt.clearParameters();
            stmt.setString(1, Integer.toString(newOrderID));
            // get the table number from the DineInCustomer associated with this order
            stmt.setString(2, Integer.toString(currSeatNum));
            try {
                int result = stmt.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding SeatNum to SeatNum table");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return;
            }
        }
    }

    // addToPickUpOrderTable(Order o, int newOrderID)
    private static void addToPickUpOrderTable(Order o, int newOrderID) throws SQLException, IOException {
        int isNotRealOrder = 0;

        String query = "INSERT INTO PickUpOrder VALUES (?, ?, ?);";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        stmt.setString(1, Integer.toString(newOrderID));
        // get the table number from the DineInCustomer associated with this order
        DineOutCustomer temp_cust = (DineOutCustomer) o.getCustomer();
        stmt.setString(2, Integer.toString(temp_cust.getID()));
        stmt.setString(3, Integer.toString(isNotRealOrder));
        try {
            int result = stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error Adding PickUpOrder to PickUpOrder table");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
            return;
        }
    }

    private static void addToDeliveryOrderTable(Order o, int newOrderID) throws SQLException, IOException {
        int isNotRealOrder = 0;

        String query = "INSERT INTO DeliveryOrder VALUES (?, ?, ?, ?);";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        stmt.setInt(1, newOrderID);
        // get the table number from the DineInCustomer associated with this order
        DeliveryCustomer temp_cust = (DeliveryCustomer) o.getCustomer();
        stmt.setString(2, temp_cust.getAddress());
        stmt.setInt(3, temp_cust.getID());
        // this is a real order
        stmt.setInt(4, isNotRealOrder);
        try {
            int result = stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error Adding DeliveryOrder to DeliveryOrder table");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
        }
    }

    private static ArrayList<Integer> addToPizzaTable(Order o, int newOrderID) throws SQLException, IOException {
        ArrayList<Pizza> pizzaList = o.getPizzas();
        ArrayList<Integer> pizzaIDList = new ArrayList<Integer>();

        int i = 1;
        for (Pizza currPizza : pizzaList) {
            String query = "INSERT INTO Pizza VALUES (?, ?, ?, ?, ?, ?, ?);";
            PreparedStatement stmt = conn.prepareStatement(query);
            // clear any parameters as sanity check
            stmt.clearParameters();
            int currPizzaID = getNextIDForTable("PizzaID", "Pizza");
            pizzaIDList.add(currPizzaID);
            stmt.setInt(1, currPizzaID);
            stmt.setDouble(2, currPizza.calcPrice());
            // arbitrary CompanyCost for each pizza
            stmt.setDouble(3, 2.00);
            /*
            this code from website: https://mkyong.com/jdbc/how-to-insert-timestamp-value-in-preparedstatement/
             */
            java.util.Date now = new java.util.Date();
            java.sql.Timestamp myTime = new java.sql.Timestamp(now.getTime());
            String theTime = myTime.toString();
            stmt.setString(4, theTime);
            // end that code from the above stated website

            // status for each pizza
            stmt.setString(5, "not completed");
            // BasePriceID for each pizza
            stmt.setInt(6, getBasePriceID(currPizza.getSize(), currPizza.getCrust()));
            // OrderID for each pizza
            stmt.setInt(7, newOrderID);
            try {
                int result = stmt.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding Pizza");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return pizzaIDList;
            }
            i += 1;
        }
        return pizzaIDList;
    }

    private static void addToPizzaToppingTable(Order o, ArrayList<Integer> pizzaIDList) throws SQLException, IOException {
        // for every Pizza in Order
        // for every topping in Pizza
        // add PizzaID, ToppingID
        int pizzaID = -99;
        int toppingID = -99;
        int i = 0;
        ArrayList<Pizza> pizzaList = o.getPizzas();
        for (Pizza currPizza : pizzaList) {
            ArrayList<Topping> currPizzaToppingList = currPizza.getToppings();
            for (Topping currTopping : currPizzaToppingList) {
                // add PizzaID, ToppingID to PizzaTopping
                String query = "INSERT INTO PizzaTopping VALUES (?, ?, ?);";
                PreparedStatement stmt = conn.prepareStatement(query);
                // clear any parameters as sanity check
                stmt.clearParameters();
                stmt.setString(1, Integer.toString(pizzaIDList.get(i)));
                // get the currTopping's ID
                stmt.setString(2, Integer.toString(currTopping.getID()));
                if (currTopping.getExtra() == false) {
                    stmt.setString(3, "No");
                }
                else {
                    stmt.setString(3, "Yes");
                }

                try {
                    int result = stmt.executeUpdate();
                }
                catch (SQLException e) {
                    System.out.println("Error 1 Adding PizzaTopping");
                    while (e != null) {
                        System.out.println("Message     : " + e.getMessage());
                        e = e.getNextException();
                    }
                }
                i += 1;
            }
        }
    }

    private static void addToPizzaDiscountTable(Order o, ArrayList<Integer> pizzaIDList) throws SQLException, IOException {
        // for every Pizza in Order
        // for every Discount in Pizza
        // add PizzaID, DiscountID
        int pizzaID = -99;
        int toppingID = -99;
        int i = 0;
        ArrayList<Pizza> pizzaList = o.getPizzas();
        for (Pizza currPizza : pizzaList) {
            ArrayList<Discount> currPizzaDiscountList = currPizza.getDiscounts();
            for (Discount currPizzaDiscount : currPizzaDiscountList) {
                // add PizzaID, DiscountID to PizzaDiscount
                String query = "INSERT INTO PizzaDiscount VALUES (?, ?);";
                PreparedStatement stmt = conn.prepareStatement(query);
                // clear any parameters as sanity check
                stmt.clearParameters();
                stmt.setString(1, Integer.toString(pizzaIDList.get(i)));
                stmt.setString(2, Integer.toString(currPizzaDiscount.getID()));
                try {
                    int result = stmt.executeUpdate();
                }
                catch (SQLException e) {
                    System.out.println("Error 2 Adding PizzaTopping");
                    while (e != null) {
                        System.out.println("Message     : " + e.getMessage());
                        e = e.getNextException();
                    }
                }
                i += 1;
            }
        }
    }

    private static void addToOrderDiscountTable(Order o, int newOrderID) throws SQLException, IOException {
        // for every Discount in Order
        // add OrderID, DiscountID to the OrderDiscount table (in the reverse order)
        ArrayList<Discount> discountList = o.getDiscounts();
        int i = 0;
        for (Discount currOrderDiscount : discountList) {
            // add OrderID, DiscountID to the OrderDiscount table (in the reverse order)
            String query = "INSERT INTO OrderDiscount VALUES (?, ?);";
            PreparedStatement stmt = conn.prepareStatement(query);
            // clear any parameters as sanity check
            stmt.clearParameters();
            stmt.setString(1, Integer.toString(currOrderDiscount.getID()));
            stmt.setString(2, Integer.toString(newOrderID));
            try {
                int result = stmt.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding OrderDiscount");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return;
            }
        }
    }

    // update the inventory for every topping from every pizza in this order
    private static void updateToppingTable(Order o, int newOrderID) throws SQLException, IOException {
        // for every pizza in o
        // for every topping in currPizza
        // UPDATE Topping SET CurrInventoryLevel = CurrInventoryLevel - 1 WHERE ToppingID = ?;
        ArrayList<Pizza> pizzaList = o.getPizzas();
        for (Pizza currPizza: pizzaList) {
            ArrayList<Topping> currPizzaToppingList = currPizza.getToppings();
            for (Topping currTopping : currPizzaToppingList) {
                // get amount to subtract from CurrInventoryLevel
                double amtToSubtract = getToppingAmtToSubtract(currPizza.getSize(), currTopping.getID());
                // subtract based on the correct field based on the size of the pizza
                String query = "";
                if (currTopping.getExtra()) {
                    query = "UPDATE Topping SET CurrInventoryLevel = CurrInventoryLevel - ? WHERE ToppingID = ?;";
                    amtToSubtract = amtToSubtract * 2;
                }
                else {
                    query = "UPDATE Topping SET CurrInventoryLevel = CurrInventoryLevel - ? WHERE ToppingID = ?;";
                }
                // create PreparedStatement
                PreparedStatement stmt = conn.prepareStatement(query);
                // clear any parameters as sanity check
                stmt.clearParameters();
                // data now bound to the query
                // (from CPSC 2150 class notes)
                stmt.setDouble(1, amtToSubtract);
                stmt.setInt(2, currTopping.getID());
                try {
                    int result = stmt.executeUpdate();
                }
                catch (SQLException e) {
                    System.out.println("Error updating Pizza");
                    while (e != null) {
                        System.out.println("Message     : " + e.getMessage());
                        e = e.getNextException();
                    }
                }
            }
        }
    }

    private static double getToppingAmtToSubtract(String pizzaSize, int currToppingID)  throws SQLException, IOException {
        String query = "";
        double amount = 0.0;
        if (pizzaSize == DBNinja.size_s) {
            query = "SELECT SmallAmt FROM Topping WHERE ToppingID = ?;";
        }
        else if (pizzaSize == DBNinja.size_m) {
            query = "SELECT MediumAmt FROM Topping WHERE ToppingID = ?;";
        }
        else if (pizzaSize == DBNinja.size_l) {
            query = "SELECT LargeAmt FROM Topping WHERE ToppingID = ?;";
        }
        else {
            // extra large
            query = "SELECT XLargeAmt FROM Topping WHERE ToppingID = ?;";
        }
        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setInt(1, currToppingID);
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next())
            {
                amount = rset.getDouble(1);
            }
            return amount;
        }
        catch (SQLException e) {
            System.out.println("Error loading Topping");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return amount;
        }
    }

    /**
     *
     * @param c the new customer to add to the database
     * @throws SQLException
     * @throws IOException
     * @requires c is not null. C's ID is -1 and will need to be assigned
     * @ensures c is given an ID and added to the database
     */
    public static void addCustomer(ICustomer c) throws SQLException, IOException
    {
        connect_to_db();
		/*add code to add the customer to the DB.
		Note: the ID will be -1 and will need to be replaced to be a fitting primary key
		Note that the customer is an ICustomer data type, which means c could be a dine in, carryout or delivery customer
		*/
        // the customer already exists in an object, you need to put it in the database

        int ID = c.getID();
        // for Customer table id
        int customerID = getNextIDForTable("CustomerID", "Customer");
        // for Orders table id
        int fakeOrderID = getNextIDForTable("OrderID", "Orders");
        String query = "";
        // order type
        String type = "IsNotRealOrder";
        int IsNotReal = 1;

        String query2 = "INSERT INTO Orders VALUES (?, ?);";
        PreparedStatement stmt = conn.prepareStatement(query2);
        // clear any parameters as sanity check
        stmt.clearParameters();
        stmt.setInt(1, fakeOrderID);
        stmt.setString(2, type);
        try {
            int result = stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error Adding fake Order");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
            //don't leave your connection open!
            conn.close();
            return;
        }

        // now deal with Customer and sub order tables
        if (c instanceof DineOutCustomer) {
            DineOutCustomer cust = (DeliveryCustomer) c;
            query = "INSERT INTO Customer VALUES (?, ?, ?, ?);";
            // create PreparedStatement
            stmt = conn.prepareStatement(query);
            // clear parameters
            stmt.clearParameters();
            // data now bound to the query
            // (from CPSC 2150 class notes)
            stmt.setString(1, Integer.toString(customerID));
            String fullName = cust.getName();
            stmt.setString(2, fullName);
            // (last name is set to null here)
            stmt.setString(3, null);
            stmt.setString(4, cust.getPhone());
            try {
                int result = stmt.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding DineOutCustomer");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                //don't leave your connection open!
                conn.close();
                return;
            }
            // add fake PickUpOrder representing the customer
            query2 = "INSERT INTO PickUpOrder VALUES (?, ?, ?);";
            PreparedStatement stmt2 = conn.prepareStatement(query2);
            // clear any parameters as sanity check
            stmt2.clearParameters();
            // data now bound to the query
            // (from CPSC 2150 class notes)
            stmt2.setString(1, Integer.toString(fakeOrderID));
            stmt2.setString(2, Integer.toString(customerID));
            stmt2.setString(3, Integer.toString(IsNotReal));
            try {
                int result = stmt2.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding DineOutCustomer to PickUpOrder table");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                //don't leave your connection open!
                conn.close();
                return;
            }
        }

        else {
            DeliveryCustomer cust = (DeliveryCustomer) c;
            // they are a delivery customer
            query = "INSERT INTO Customer VALUES (?, ?, ?, ?);";
            // create PreparedStatement
            stmt = conn.prepareStatement(query);
            // clear any parameters as sanity check
            stmt.clearParameters();
            // data now bound to the query
            // (from CPSC 2150 class notes)
            stmt.setString(1, Integer.toString(customerID));
            String fullName = cust.getName();
            stmt.setString(2, fullName);
            /* last name is set to be null; only first name will be printed out
            in other functions if last name is null */
            stmt.setString(3, null);
            stmt.setString(4, cust.getPhone());
            try {
                int result = stmt.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding DeliveryCustomer");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                //don't leave your connection open!
                conn.close();
            }
            // add fake order corresponding to DeliveryCustomer
            query2 = "INSERT INTO DeliveryOrder VALUES (?, ?, ?, ?);";
            PreparedStatement stmt2 = conn.prepareStatement(query2);
            // clear any parameters as sanity check
            stmt2.clearParameters();
            // data now bound to the query
            // (from CPSC 2150 class notes)
            stmt2.setString(1, Integer.toString(fakeOrderID));
            stmt2.setString(2, cust.getAddress());
            stmt2.setString(3, Integer.toString(customerID));
            stmt2.setString(4, Integer.toString(IsNotReal));
            try {
                int result = stmt2.executeUpdate();
            }
            catch (SQLException e) {
                System.out.println("Error Adding DeliveryCustomer to DeliveryOrder table");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                //don't leave your connection open!
                conn.close();
                return;
            }
        }
        conn.close();
    }


    /**
     *
     * @param o the order to mark as complete in the database
     * @throws SQLException
     * @throws IOException
     * @requires the order exists in the database
     * @ensures the order will be marked as complete
     */
    public static void CompleteOrder(Order o) throws SQLException, IOException
    {
        connect_to_db();
		/*add code to mark an order as complete in the DB. You may have a boolean field for this, or maybe a completed time timestamp. However you have it, */
        // for every Pizza with this OrderID,
            //  mark the Status as 'completed' (use SQL UPDATE)
        // UPDATE Pizza SET Status = 'completed' WHERE OrderID = o.ID; (but use a PreparedStatement)

        // create query for PreparedStatement
        String query = "UPDATE Pizza SET The_Status = ? WHERE OrderID = ?;";

        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);

        // variables for query
        String status = "completed";

        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        stmt.setString(1, status);
        stmt.setString(2, Integer.toString(o.getID()));
        try {
            // run the query
            int result = stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error updating Pizza");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            // don't leave your connection open
            conn.close();
        }

        conn.close();
    }

    /**
     *
     * @param t the topping whose inventory is being replenished
     * @param toAdd the amount of inventory of t to add
     * @throws SQLException
     * @throws IOException
     * @requires t exists in the database and toAdd > 0
     * @ensures t's inventory level is increased by toAdd
     */
    public static void AddToInventory(Topping t, double toAdd) throws SQLException, IOException {
        connect_to_db();
		    /*add code to add toAdd to the inventory level of T. This is not adding a new topping,
        it is adding a certain amount of stock for a topping. This would be used to show that
        an order was made to replenish the restaurants supply of pepperoni, etc*/
        String query = "UPDATE Topping SET CurrInventoryLevel = CurrInventoryLevel + ?" +
                " WHERE ToppingID = ?;";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.clearParameters();

        stmt.setString(1, Double.toString(toAdd));
        stmt.setString(2, Integer.toString(t.getID()));
        try {
            // run the query
            int result = stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating Topping");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            // don't leave your connection open
            conn.close();
        }

        conn.close();
    }

    /*
        A function to get the list of toppings and their inventory levels. I have left this code "complete" as an example of how to use JDBC to get data from the database. This query will not work on your database if you have different field or table names, so it will need to be changed

        Also note, this is just getting the topping ids and then calling getTopping() to get the actual topping. You will need to complete this on your own

        You don't actually have to use and write the getTopping() function, but it can save some repeated code if the program were to expand, and it keeps the functions simpler, more elegant and easy to read. Breaking up the queries this way also keeps them simpler. I think it's a better way to do it, and many people in the industry would agree, but its a suggestion, not a requirement.
    */

    /**
     *
     * @return the List of all toppings in the database
     * @throws SQLException
     * @throws IOException
     * @ensures the returned list will include all toppings and accurate inventory levels
     */
    public static ArrayList<Topping> getInventory() throws SQLException, IOException
    {
        //start by connecting
        connect_to_db();
        ArrayList<Topping> ts = new ArrayList<Topping>();
        //create a string with out query, this one is an easy one
        String query = "Select ToppingID From Topping;";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // (no data to bind to the query, no need to setString())
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
					/*Use getInt, getDouble, getString to get the actual value. You can use the column number starting with 1, or use the column name as a string

					NOTE: You want to use rset.getInt() instead of Integer.parseInt(rset.getString()), not just because it's shorter, but because of the possible NULL values. A NUll would cause parseInt to fail

					If there is a possibility that it could return a NULL value you need to check to see if it was NULL. In this query we won't get nulls, so I didn't. If I was going to I would do:

					int ID = rset.getInt(1);
					if(rset.wasNull())
					{
						//set ID to what it should be for NULL, and whatever you need to do.
					}

					NOTE: you can't check for NULL until after you have read the value using one of the getters.

					*/
                int ID = rset.getInt(1);
                //Now I'm just passing my primary key to this function to get the topping itself individually
                Topping theTopping = getTopping(ID);
                ts.add(getTopping(ID));
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading inventory");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            //don't leave your connection open!
            conn.close();
            return ts;
        }

        //end by closing the connection
        conn.close();
        return ts;
    }

    /**
     *
     * @return a list of all orders that are currently open in the kitchen
     * @throws SQLException
     * @throws IOException
     * @ensures all currently open orders will be included in the returned list.
     */
    public static ArrayList<Order> getCurrentOrders() throws SQLException, IOException
    {
        connect_to_db();

        ArrayList<Order> os = new ArrayList<Order>();
		/*add code to get a list of all open orders.
		Only return Orders that have not been completed.
		If any pizzas are not completed, then the order is open.*/
        String notCompleted = "not completed";

        /* get all of the Orders from the Orders table where the Pizzas associated with them have a status of
        'not completed' */
        String query = "SELECT DISTINCT O.OrderID FROM Orders AS O LEFT OUTER JOIN" +
                " Pizza AS P ON O.OrderID = P.OrderID WHERE P.The_Status = ?;";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // bind data to query
        stmt.setString(1, notCompleted);
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                int currOrderID = rset.getInt(1);
                // add to the orders list here
                os.add(getOrder(currOrderID));
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Pizza");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            //don't leave your connection open!
            conn.close();
            return os;
        }

        conn.close();
        return os;
    }

    /**
     *
     * @param size the pizza size
     * @param crust the type of crust
     * @return the base price for a pizza with that size and crust
     * @throws SQLException
     * @throws IOException
     * @requires size = size_s || size_m || size_l || size_xl AND crust = crust_thin || crust_orig || crust_pan || crust_gf
     * @ensures the base price for a pizza with that size and crust is returned
     */
    public static double getBasePrice(String size, String crust) throws SQLException, IOException
    {
        connect_to_db();
        double bp = 0.0;
        //add code to get the base price for that size and crust pizza Depending on how you store size and crust in your database, you may have to do a conversion

        String query = "Select Price From BasePrice Where Size = ? And CrustType = ?;";
        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        stmt.setString(1, size);
        stmt.setString(2, crust);
        try {
            ResultSet rset = stmt.executeQuery();

            while(rset.next()) {
                bp = rset.getDouble(1);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading BasePrice");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            // don't leave your connection open
            conn.close();
            return bp;
        }

        conn.close();
        return bp;
    }

    private static int getBasePriceID(String size, String crust) throws SQLException, IOException {
        int bpID = 0;
        //add code to get the base price for that size and crust pizza Depending on how you store size and crust in your database, you may have to do a conversion

        String query = "Select BasePriceID From BasePrice Where Size = ? And CrustType = ?;";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        stmt.setString(1, size);
        stmt.setString(2, crust);
        try {
            ResultSet rset = stmt.executeQuery();

            while(rset.next()) {
                bpID = rset.getInt(1);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading BasePrice");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return bpID;
        }

        return bpID;
    }

    /**
     *
     * @return the list of all discounts in the database
     * @throws SQLException
     * @throws IOException
     * @ensures all discounts are included in the returned list
     */
    // get ALL discounts in the database
    public static ArrayList<Discount> getDiscountList() throws SQLException, IOException
    {
        connect_to_db();
        ArrayList<Discount> discs = new ArrayList<Discount>();
        String query = "SELECT DiscountID FROM Discount;";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // (no data to bind to the query, no need to setString())
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                int ID = rset.getInt(1);
                Discount theDisc = getDiscount(ID);
                discs.add(theDisc);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Discount");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
            conn.close();
            return discs;
        }

        conn.close();
        return discs;
    }

    /**
     *
     * @return the list of all delivery and carry out customers
     * @throws SQLException
     * @throws IOException
     * @ensures the list contains all carryout and delivery customers in the database
     */
    public static ArrayList<ICustomer> getCustomerList() throws SQLException, IOException
    {
        connect_to_db();

        ArrayList<ICustomer> custs = new ArrayList<ICustomer>();
        //add code to get a list of all customers


        // get all customers except for dine in customers
        String query = "Select CustomerID From Customer WHERE Phone IS NOT NULL;";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.clearParameters();
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next()) {
                int ID = rset.getInt(1);
                ICustomer theCustomer = getCustomer(ID);
                custs.add(getCustomer(ID));
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Customer table");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            //don't leave your connection open!
            conn.close();
            return custs;
        }

        conn.close();
        return custs;
    }

	/*
	Note: The following incomplete functions are not strictly required, but could make your DBNinja class much simpler. For instance, instead of writing one query to get all of the information about an order, you can find the primary key of the order, and use that to find the primary keys of the pizzas on that order, then use the pizza primary keys individually to build your pizzas. We are no longer trying to get everything in one query, so feel free to break them up as much as possible

	You could also add functions that take in a Pizza object and add that to the database, or take in a pizza id and a topping id and add that topping to the pizza in the database, etc. I would recommend this to keep your addOrder function much simpler

	These simpler functions should still not be called from our menu class. That is why they are private

	We don't need to open and close the connection in these, since they are only called by a function that has opened the connection and will close it after
	*/

    // please note that getTopping takes in a ToppingID, not a PizzaID
    private static Topping getTopping(int ID) throws SQLException, IOException
    {
        //add code to get a topping
		//the java compiler on unix does not like that t could be null, so I created a fake topping that will be replaced
        Topping t = new Topping("fake", 0.25, 100.0, -1);
        String query = "Select Name, CustomerPrice, CurrInventoryLevel From Topping where ToppingID = ?;";

        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(ID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                String tname = rset.getString(1);
                double price = rset.getDouble(2);
                double inv = rset.getDouble(3);

                t = new Topping(tname, price, inv, ID);
			}
			
		}
		catch (SQLException e) {
            System.out.println("Error loading Topping");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return t;
        }
		
        return t;
    }

    private static Discount getDiscount(int discountID) throws SQLException, IOException
    {
        // create fake Discount to be replaced
        Discount d = new Discount("fake", 0.01, 0, -1);

        // get a single Discount given the discountID
        String query = "SELECT D.DiscountID, Name, DollarAmount, PercentageOff" +
                " FROM (Discount AS D LEFT OUTER JOIN DollarDiscount ON" +
                " D.DiscountID = DollarDiscountID) LEFT OUTER JOIN PercentageDiscount ON" +
                " D.DiscountID = PercentageDiscountID" +
                " WHERE D.DiscountID = ?;";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // data now bound to query
        stmt.setString(1, Integer.toString(discountID));
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next())
            {
                // build the Discount object here
                String discName = rset.getString(2);
                double percentOff = rset.getDouble(4);
                double cashOff = rset.getDouble(3);
                int ID = rset.getInt(1);

                d = new Discount(discName, percentOff, cashOff, ID);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Discount and/or associated tables");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
            return d;
        }

        return d;
    }

    // get the Toppings object list for a given Pizza object
    private static ArrayList<Topping> getToppingsList(int pizzaID) throws SQLException, IOException
    {
        ArrayList<Topping> ts = new ArrayList<Topping>();
        //create a string with out query, this one is an easy one
        String query = "Select ToppingID, IsExtra From PizzaTopping WHERE PizzaID = ?;";

        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // bind data to query
        stmt.setInt(1, pizzaID);
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next())
            {
                int ID = rset.getInt(1);
                String isExtra = rset.getString(2);
                //Now I'm just passing my primary key to this function to get the topping itself individually
                Topping theTopping = getTopping(ID);
                if (isExtra.equals("Yes")) {
                    theTopping.makeExtra();
                }
                System.out.println("The value of isExtra: " + isExtra);
                ts.add(theTopping);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading inventory");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return ts;
        }

        return ts;
    }

    // ID is the PizzaID field that is passed in
    private static Pizza getPizza(int ID)  throws SQLException, IOException
    {
        //add code to get Pizza Remember, a Pizza has toppings and discounts on it
        Pizza p = new Pizza(-1, " ", " ", 0.0);
        // create a toppings list to be populated from the database for this pizza
        ArrayList<Topping> ts = new ArrayList<Topping>();

        ArrayList<Discount> ds = new ArrayList<Discount>();
        String query = "Select PizzaID, Size, CrustType, Price" +
                " From Pizza P Join BasePrice B on P.BasePriceID = B.BasePriceID" +
                " Where PizzaID = ?;";

        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(ID));
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next())
            {
                int pid = rset.getInt(1);
                String psize = rset.getString(2);
                String pcrust = rset.getString(3);
                double pbp = rset.getDouble(4);

                // create the new pizza object
                p = new Pizza(pid, psize, pcrust, pbp);

                // get all toppings for this pizzaID
                ts = getToppingsList(pid);
                // loop through ts and add the Toppings to the Pizza
                for (Topping top : ts) {
                    p.addTopping(top);
                }

                // get all discounts for this pizzaID
                ds = getPizzaDiscountList(pid);
                // loop through its discount and add the Discounts to the Discounts list for this Pizza
                for (Discount d : ds) {
                    p.addDiscount(d);
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Pizza");
            while (e != null) {
                System.out.println("Message   : " + e.getMessage());
                e = e.getNextException();
            }
            return p;
        }

        return p;

    }

    // this function just selects the IDs
    /* in PizzaDiscount where the pizzaID matches PizzaID, and then this function should
    call getPizzaDiscount() to help build the ArrayList that is being made in getPizzaDiscountList() */
    private static ArrayList<Discount> getPizzaDiscountList(int pizzaID) throws SQLException, IOException {
        ArrayList<Discount> ds = new ArrayList<Discount>();
        /* return the PizzaID, PizzaDiscount DiscountID, Name of discount, and its corresponding
        dollar amount or percentage off */
        String query = "Select DiscountID" +
                " FROM PizzaDiscount" +
                " WHERE PizzaID = ?;";

        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(pizzaID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                // build the Discount object here
                int ID = rset.getInt(1);
                Discount theDiscount = getPizzaDiscount(pizzaID);
                ds.add(theDiscount);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading PizzaDiscount");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return ds;
        }

        return ds;
    }

    // get all of the Discounts associated with this orderID
    private static ArrayList<Discount> getOrderDiscountList(int orderID) throws SQLException, IOException {
        ArrayList<Discount> ds = new ArrayList<Discount>();
        /* return the PizzaID, PizzaDiscount DiscountID, Name of discount, and its corresponding
        dollar amount or percentage off */
        String query = "Select DiscountID" +
                " FROM OrderDiscount" +
                " WHERE OrderID = ?;";

        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                // build the Discount object here
                int ID = rset.getInt(1);
                Discount theDiscount = getOrderDiscount(orderID);
                ds.add(theDiscount);
            }

        }
        catch (SQLException e) {
            System.out.println("Error loading OrderDiscount");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return ds;
        }

        return ds;
    }

    // get a given order Discount given the orderID
    private static Discount getOrderDiscount(int orderID) throws SQLException, IOException {
        // create fake Discount to be replaced
        Discount d = new Discount("fake", 0.01, 0, -1);

        /* return the PizzaID, PizzaDiscount DiscountID, Name of discount, and its corresponding
        dollar amount or percentage off */
        String query = "Select OD.DiscountID, Name, DollarAmount, PercentageOff" +
                " FROM ((OrderDiscount AS OD LEFT OUTER JOIN Discount AS D ON OD.DiscountID = D.DiscountID)" +
                " LEFT OUTER JOIN DollarDiscount on D.DiscountID = DollarDiscountID) LEFT OUTER JOIN PercentageDiscount" +
                " ON D.DiscountID = PercentageDiscountID" +
                " WHERE OD.OrderID = ?;";

        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                // build the Discount object here
                String discName = rset.getString(2);
                double percentOff = rset.getDouble(4);
                double cashOff = rset.getDouble(3);
                int ID = rset.getInt(1);

                d = new Discount(discName, percentOff, cashOff, ID);
            }

        }
        catch (SQLException e) {
            System.out.println("Error loading Discount and/or associated tables for getOrderDiscount()");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return d;
        }

        return d;
    }

    private static Discount getPizzaDiscount(int pizzaID) throws SQLException, IOException {
        // create fake Discount to be replaced
        Discount d = new Discount("fake", 0.01, 0, -1);

        /* return the PizzaID, PizzaDiscount DiscountID, Name of discount, and its corresponding
        dollar amount or percentage off */
        String query = "Select PizzaID, PD.DiscountID, Name, DollarAmount, PercentageOff" +
                " FROM ((PizzaDiscount AS PD LEFT OUTER JOIN Discount AS D ON PD.DiscountID = D.DiscountID)" +
                " LEFT OUTER JOIN DollarDiscount on D.DiscountID = DollarDiscountID) LEFT OUTER JOIN PercentageDiscount" +
                " ON D.DiscountID = PercentageDiscountID" +
                " WHERE PD.PizzaID = ?;";

         // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(pizzaID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                // build the Discount object here
                String discName = rset.getString(3);
                double percentOff = rset.getDouble(5);
                double cashOff = rset.getDouble(4);
                int ID = rset.getInt(2);

                d = new Discount(discName, percentOff, cashOff, ID);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Discount and/or associated tables for getPizzaDiscount()");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return d;
        }

        return d;
    }

    private static ICustomer getCustomer(int customerID)  throws SQLException, IOException
    {
        //add code to get customer

        /*
        if fname in null than dine in
        for determining pickup or delivery:
        (something like): SELECT CustomerID FROM DeliveryOrder WHERE CustomerID = ? AND IsNotRealOrder = ?;
        (make the IsNotRealOrder parameter 1)
        if the resultset was not empty, then it is a delivery customer; else, it is a DineOutCustomer

        */

        ICustomer C;
        int cid = customerID;
        String firstn;
        String query = "SELECT CustomerID, Fname, Lname, Phone FROM Customer WHERE CustomerID = ?;";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.clearParameters();
        stmt.setInt(1, cid);
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next()) {
                firstn = rset.getString(2);
                String ln = rset.getString(3);
                int phone = rset.getInt(4);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Discount and/or associated tables for getCustomer()");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
        }

        String query2 = "SELECT D.CustomerID, Fname, Lname, Phone, Address" +
                " FROM DeliveryOrder AS D" +
                " LEFT OUTER JOIN Customer AS C ON" +
                " D.CustomerID = C.CustomerID WHERE D.CustomerID = ?;";
        PreparedStatement stmt2 = conn.prepareStatement(query2);
        stmt2.clearParameters();
        stmt2.setInt(1, cid);
        boolean foundDeliveryOrder = false;
        try {
            ResultSet rset = stmt2.executeQuery();
            while (rset.next()) {
                foundDeliveryOrder = true;
                int did = rset.getInt(1);
                String fName = rset.getString(2);
                String lName = rset.getString(3);
                String name = "";
                String phone = rset.getString(4);
                if (lName != null) {
                    name = fName + " " + lName;
                }
                else {
                    name = fName;
                }
                String address = rset.getString(5);

                C = new DeliveryCustomer(did, name, phone, address);
                return C;
            }
        }
        catch (SQLException e) {
            System.out.println("2nd catch: Error loading Discount and/or associated tables for getCustomer()");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }
        }

        if (!foundDeliveryOrder) {
            DineOutCustomer doc = new DineOutCustomer(-1, " ", " ");
            // select customer stuff where their CustomerID is present in PickUpOrder
            String query7 = "SELECT P.CustomerID, Fname, Lname, Phone FROM" +
                    " PickUpOrder AS P LEFT OUTER JOIN Customer AS C ON" +
                    " P.CustomerID = C.CustomerID WHERE P.CustomerID = ?;";
            PreparedStatement stmt7 = conn.prepareStatement(query7);
            stmt7.clearParameters();
            stmt7.setInt(1, cid);
            try {
                ResultSet rset = stmt.executeQuery();
                while(rset.next()) {
                    int pid = rset.getInt(1);
                    String fName = rset.getString(2);
                    String lName = rset.getString(3);
                    String name = fName + " " + lName;
                    String phone = rset.getString(4);

                    doc = new DineOutCustomer(pid, name, phone);
                }
            }
            catch (SQLException e) {
                System.out.println("4th catch: Error loading Discount and/or associated tables for getCustomer()");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return doc;
            }
            return doc;
        }

        C = new DineOutCustomer(0, "", "");
        return C;
    }

    // get the customer associated with the orderID passed in
    private static ICustomer getCustomerFromOrderID(int orderID)  throws SQLException, IOException
    {
        // add code to get customer
        ICustomer C;
        // initialize with some value
        C = new DineInCustomer(-1, null, -99);

        // determine if customer is dinein customer
        boolean isDineInCust = isDineInCustomer(orderID);
        // determine if customer is pickup customer
        boolean isDineOutCust = isDineOutCustomer(orderID);
        // determine if customer is delivery customer
        boolean isDeliveryCust = isDeliveryCustomer(orderID);

        String query = "";
        if (isDineInCust == true) {
            query = "SELECT TableNum, D.CustomerID FROM DineInOrder As D LEFT OUTER JOIN Customer AS C ON D.CustomerID = C.CustomerID WHERE DineInOrderID = ?;";
            PreparedStatement stmt = conn.prepareStatement(query);
            // clear any parameters as a sanity check
            stmt.clearParameters();
            // data now bound to query
            stmt.setString(1, Integer.toString(orderID));
            try {
                ResultSet rset = stmt.executeQuery();
                //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
                while(rset.next())
                {
                    // build the DineInCustomer object here
                    int tableNum = rset.getInt(1);
                    List<Integer> seatNums = getSeatNums(orderID);
                    int ID = rset.getInt(2);
                    C = new DineInCustomer(tableNum, seatNums, ID);
                }
            }
            catch (SQLException e) {
                System.out.println("Error loading DineInOrder table");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return C;
            }

            return C;
        }
        else if (isDineOutCust == true) {
            // will need to fill in ID, name, phone
            query = "SELECT P.CustomerID, Fname, Lname, Phone FROM PickUpOrder AS P LEFT OUTER JOIN Customer AS C ON" +
                    " P.CustomerID = C.CustomerID WHERE PickUpOrderID = ?;";
            PreparedStatement stmt = conn.prepareStatement(query);
            // clear any parameters as a sanity check
            stmt.clearParameters();
            // data now bound to query
            stmt.setString(1, Integer.toString(orderID));
            try {
                ResultSet rset = stmt.executeQuery();
                while(rset.next())
                {
                    // build the DineOutCustomer object here
                    int ID = rset.getInt(1);
                    String fName = rset.getString(2);
                    String lName = rset.getString(3);
                    String name = "";
                    if (lName != null) {
                        name = fName + " " + lName;
                    }
                    else {
                        name = fName;
                    }

                    String phone = rset.getString(4);
                    C = new DineOutCustomer(ID, name, phone);
                }
            }
            catch (SQLException e) {
                System.out.println("Error loading PickUpOrder, Customer tables");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return C;
            }

            return C;
        }
        else {
            // is delivery customer
            query = "SELECT C.CustomerID, C.Fname, C.Lname, C.Phone, D.Address" +
                    " FROM DeliveryOrder AS D LEFT OUTER JOIN Customer AS C ON" +
                    " D.CustomerID = C.CustomerID" +
                    " WHERE DeliveryOrderID = ?;";
            PreparedStatement stmt = conn.prepareStatement(query);
            // clear any parameters as a sanity check
            stmt.clearParameters();
            // data now bound to query
            stmt.setString(1, Integer.toString(orderID));
            try {
                ResultSet rset = stmt.executeQuery();
                while(rset.next())
                {
                    // build the DeliveryCustomer object here
                    int ID = rset.getInt(1);
                    String fName = rset.getString(2);
                    String lName = rset.getString(3);
                    String name = "";
                    if (lName != null) {
                        name = fName + " " + lName;
                    }
                    else {
                        name = fName;
                    }
                    String phone = rset.getString(4);
                    String addr = rset.getString(5);

                    C = new DeliveryCustomer(ID, name, phone, addr);
                }
            }
            catch (SQLException e) {
                System.out.println("Error loading DeliveryOrder table");
                while (e != null) {
                    System.out.println("Message     : " + e.getMessage());
                    e = e.getNextException();
                }
                return C;
            }

            return C;
        }
    }

    // determine if this order was placed by a DineInCustomer
    private static boolean isDineInCustomer(int orderID)  throws SQLException, IOException {
        boolean isDineInCust = false;

        String query = "SELECT DineInOrderID FROM DineInOrder WHERE DineInOrderID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // data now bound to query
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                Integer ID = new Integer(rset.getInt(1));
                if (ID != null) {
                    isDineInCust = true;
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading DineInOrder and/or associated tables");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return isDineInCust;
        }

        return isDineInCust;
    }

    // determines if this order was placed by a DineOutCustomer
    private static boolean isDineOutCustomer(int orderID)  throws SQLException, IOException {
        boolean isDineOutCust = false;

        String query = "SELECT PickUpOrderID FROM PickUpOrder WHERE PickUpOrderID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // data now bound to query
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                Integer ID = new Integer(rset.getInt(1));
                if (ID != null) {
                    isDineOutCust = true;
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading DineInOrder and/or associated tables");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return isDineOutCust;
        }

        return isDineOutCust;
    }

    // determines if this order was placed by a DeliveryCustomer
    private static boolean isDeliveryCustomer(int orderID)  throws SQLException, IOException {
        boolean isDeliveryCust = false;

        String query = "SELECT DeliveryOrderID FROM DeliveryOrder WHERE DeliveryOrderID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // data now bound to query
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                Integer ID = new Integer(rset.getInt(1));
                if (ID != null) {
                    isDeliveryCust = true;
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading DineInOrder and/or associated tables");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return isDeliveryCust;
        }

        return isDeliveryCust;
    }

    // get seat numbers associated with this dineInOrderID
    private static List<Integer> getSeatNums(int dineInOrderID)  throws SQLException, IOException {
        List<Integer> seatNums = new ArrayList<Integer>();

        String query = "SELECT SeatNum FROM SeatNum WHERE DineInOrderID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // data now bound to query
        stmt.setString(1, Integer.toString(dineInOrderID));
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next())
            {
                dineInOrderID = rset.getInt(1);
                seatNums.add(dineInOrderID);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading DineInOrder and/or associated tables");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return seatNums;
        }

        return seatNums;
    }

    // get back an Order object based on the orderID that is passed in (orig from the database)
    private static Order getOrder(int orderID)  throws SQLException, IOException
    {
        //add code to get an order. Remember, an order has pizzas, a customer, and discounts on it

        // create a dummy order to replaced with database results
        // ICustomer c pointer initially set to null
        ICustomer c = null;
        Order myOrder = new Order(-1, null, " ");
        String query = "SELECT OrderID, OrderType FROM Orders WHERE OrderID = ?;";

        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as a sanity check
        stmt.clearParameters();
        // data now bound to query
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            //even if you only have one result, you still need to call ResultSet.next() to load the first tuple
            while(rset.next())
            {
                // build the Order object here
                int ID = rset.getInt(1);
                String oType = rset.getString(2);
                c = getCustomerFromOrderID(orderID);
                myOrder = new Order(ID, c, oType);

                // add all of the Pizzas to myOrder
                ArrayList<Pizza> myPizzas = getPizzaList(orderID);
                for (Pizza currPizza : myPizzas) {
                    myOrder.addPizza(currPizza);
                }

                // add all of the order discounts to myOrder
                ArrayList<Discount> myOrderDiscounts = getOrderDiscountList(orderID);
                for (Discount currDiscount : myOrderDiscounts) {
                    myOrder.addDiscount(currDiscount);
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Orders table");
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return myOrder;
        }

        return myOrder;
    }

    // get list of Pizzas associated with this orderID
    private static ArrayList<Pizza> getPizzaList(int orderID)  throws SQLException, IOException {
        ArrayList<Pizza> pizzaList = new ArrayList<Pizza>();
        Pizza p = new Pizza(-1, " ", " ", 0.0);

        String query = "SELECT PizzaID FROM Pizza WHERE OrderID = ?;";
        // create PreparedStatement
        PreparedStatement stmt = conn.prepareStatement(query);
        // clear any parameters as sanity check
        stmt.clearParameters();
        // data now bound to the query
        // (from CPSC 2150 class notes)
        stmt.setString(1, Integer.toString(orderID));
        try {
            ResultSet rset = stmt.executeQuery();
            while(rset.next())
            {
                int pizzaID = rset.getInt(1);

                // get the new pizza object
                p = getPizza(pizzaID);
                // add the current Pizza to pizzaList
                pizzaList.add(p);
            }
        }
        catch (SQLException e) {
            System.out.println("Error loading Pizza");
            while (e != null) {
                System.out.println("Message   : " + e.getMessage());
                e = e.getNextException();
            }
            return pizzaList;
        }

        return pizzaList;
    }

    private static int getNextIDForTable(String idName, String tableName) throws SQLException, IOException {

        int temp_cid = -99;
        // String query = "Select ? FROM ? ORDER BY DESC;";
        String query = "Select " + idName + " FROM " + tableName + " ORDER BY " + idName + " ASC;";
        // Select 'OrderID' FROM 'Orders' ORDER BY DESC;

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.clearParameters();
        // stmt.setString(1, idName);
        // stmt.setString(2, tableName);
        try {
            ResultSet rset = stmt.executeQuery();
            while (rset.next()) {
                temp_cid = rset.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("When getting next ID, Error loading " + tableName);
            while (e != null) {
                System.out.println("Message     : " + e.getMessage());
                e = e.getNextException();
            }

            return temp_cid + 1;
        }

        return temp_cid + 1;
    }

}
